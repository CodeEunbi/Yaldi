package com.yaldi.domain.datamodel.service;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.entity.SyncStatus;
import com.yaldi.domain.datamodel.dto.request.CreateDtoRequest;
import com.yaldi.domain.datamodel.dto.request.CreateEntityRequest;
import com.yaldi.domain.datamodel.dto.request.SelectedColumnDto;
import com.yaldi.domain.datamodel.dto.request.UpdateNameRequest;
import com.yaldi.domain.datamodel.dto.response.DataModelDetailResponse;
import com.yaldi.domain.datamodel.dto.response.DataModelResponse;
import com.yaldi.domain.datamodel.entity.DataModel;
import com.yaldi.domain.datamodel.entity.DataModelErdColumnRelation;
import com.yaldi.domain.datamodel.repository.DataModelErdColumnRelationRepository;
import com.yaldi.domain.datamodel.repository.DataModelRepository;
import com.yaldi.domain.datamodel.util.NamingConverter;
import com.yaldi.domain.datamodel.util.SyncStatusCalculator;
import com.yaldi.domain.erd.entity.ErdColumn;
import com.yaldi.domain.erd.entity.ErdTable;
import com.yaldi.domain.erd.repository.ErdColumnRepository;
import com.yaldi.domain.erd.repository.ErdTableRepository;
import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 데이터 모델 서비스
 *
 * <p>Entity/DTO 자동 생성 및 관리 기능 제공</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataModelService {

    private final DataModelRepository dataModelRepository;
    private final DataModelErdColumnRelationRepository dataModelErdColumnRelationRepository;
    private final ErdTableRepository erdTableRepository;
    private final ErdColumnRepository erdColumnRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;

    /**
     * Entity 생성
     *
     * <p>선택한 테이블의 모든 컬럼으로 Entity를 자동 생성합니다.</p>
     * <p>Entity 이름: {테이블명PascalCase}Entity (예: users → UsersEntity)</p>
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param request Entity 생성 요청 (tableKey만 포함)
     * @return 생성된 Entity 정보
     */
    @Transactional
    public DataModelResponse createEntity(Integer userKey, Long projectKey, CreateEntityRequest request) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. 테이블 조회
        ErdTable table = erdTableRepository.findById(request.tableKey())
                .orElseThrow(() -> new GeneralException(ErrorStatus.TABLE_NOT_FOUND));

        // 3. 프로젝트 일치 확인
        if (!table.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.TABLE_PROJECT_MISMATCH);
        }

        // 4. Entity 이름 자동 생성
        String entityName = NamingConverter.toEntityName(table.getPhysicalName());

        // 5. 이름 중복 체크
        if (dataModelRepository.existsByProjectKeyAndName(projectKey, entityName)) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_NAME_DUPLICATE);
        }

        // 6. 테이블의 모든 컬럼 조회
        List<ErdColumn> columns = erdColumnRepository.findByTableKey(request.tableKey());
        if (columns.isEmpty()) {
            throw new GeneralException(ErrorStatus.TABLE_HAS_NO_COLUMNS);
        }

        // 7. DataModel 엔티티 생성
        DataModel dataModel = DataModel.builder()
                .projectKey(projectKey)
                .name(entityName)
                .type(DataModelType.ENTITY)
                .sourceTableKey(request.tableKey())
                .lastSyncedAt(OffsetDateTime.now())
                .build();

        dataModel = dataModelRepository.save(dataModel);

        // 8. DataModelErdColumnRelation 생성 (모든 컬럼 포함)
        Long modelKey = dataModel.getModelKey();
        List<DataModelErdColumnRelation> relations = columns.stream()
                .map(column -> DataModelErdColumnRelation.builder()
                        .modelKey(modelKey)
                        .columnKey(column.getColumnKey())
                        .build())
                .collect(Collectors.toList());

        dataModelErdColumnRelationRepository.saveAll(relations);

        log.info("Entity created: modelKey={}, name={}, sourceTableKey={}, columnCount={}",
                dataModel.getModelKey(), dataModel.getName(), request.tableKey(), columns.size());

        // 9. DTO 변환 및 반환
        return convertToResponse(dataModel, columns, table);
    }

    /**
     * DTO 생성
     *
     * <p>여러 테이블의 컬럼을 선택하여 DTO를 생성합니다.</p>
     * <p>컬럼명 충돌 시 자동으로 테이블명 prefix 추가 (예: users.id → userId, orders.id → orderId)</p>
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param request DTO 생성 요청 (name, type, selectedColumns)
     * @return 생성된 DTO 정보
     */
    @Transactional
    public DataModelResponse createDto(Integer userKey, Long projectKey, CreateDtoRequest request) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. 이름 중복 체크
        if (dataModelRepository.existsByProjectKeyAndName(projectKey, request.name())) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_NAME_DUPLICATE);
        }

        // 3. DTO 타입 검증
        if (request.type() != DataModelType.DTO_REQUEST && request.type() != DataModelType.DTO_RESPONSE) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_INVALID_TYPE);
        }

        // 4. 선택된 컬럼들 조회 및 검증
        List<ErdColumn> selectedColumns = new ArrayList<>();
        Set<Long> tableKeys = new HashSet<>();

        for (SelectedColumnDto selected : request.selectedColumns()) {
            ErdColumn column = erdColumnRepository.findById(selected.columnKey())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.COLUMN_NOT_FOUND));

            // 테이블 키 일치 확인
            if (!column.getTableKey().equals(selected.tableKey())) {
                throw new GeneralException(ErrorStatus.COLUMN_TABLE_MISMATCH);
            }

            // 프로젝트 일치 확인 (컬럼의 테이블이 해당 프로젝트에 속하는지)
            ErdTable table = erdTableRepository.findById(column.getTableKey())
                    .orElseThrow(() -> new GeneralException(ErrorStatus.TABLE_NOT_FOUND));

            if (!table.getProjectKey().equals(projectKey)) {
                throw new GeneralException(ErrorStatus.TABLE_PROJECT_MISMATCH);
            }

            selectedColumns.add(column);
            tableKeys.add(column.getTableKey());
        }

        // 5. DataModel 엔티티 생성
        Long sourceTableKey = tableKeys.size() == 1 ? tableKeys.iterator().next() : null;

        DataModel dataModel = DataModel.builder()
                .projectKey(projectKey)
                .name(request.name())
                .type(request.type())
                .sourceTableKey(sourceTableKey)
                .lastSyncedAt(OffsetDateTime.now())
                .build();

        dataModel = dataModelRepository.save(dataModel);

        // 6. DataModelErdColumnRelation 생성
        Long modelKey = dataModel.getModelKey();
        List<DataModelErdColumnRelation> relations = selectedColumns.stream()
                .map(column -> DataModelErdColumnRelation.builder()
                        .modelKey(modelKey)
                        .columnKey(column.getColumnKey())
                        .build())
                .collect(Collectors.toList());

        dataModelErdColumnRelationRepository.saveAll(relations);

        log.info("DTO created: modelKey={}, name={}, type={}, columnCount={}, tableCount={}",
                dataModel.getModelKey(), dataModel.getName(), dataModel.getType(),
                selectedColumns.size(), tableKeys.size());

        // 7. DTO 변환 및 반환
        return convertToResponse(dataModel, selectedColumns, null);
    }

    /**
     * 데이터 모델 목록 조회 (페이지네이션 + 타입 필터링)
     *
     * <p>고정 페이지 크기: 10개, 최신순 정렬</p>
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param type 타입 필터 (optional)
     * @param page 페이지 번호 (0부터 시작)
     * @return 데이터 모델 목록
     */
    public Page<DataModelResponse> getDataModels(Integer userKey, Long projectKey, DataModelType type, int page) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. 고정 페이지 크기: 10, 최신순 정렬
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. 타입 필터링 여부에 따라 조회
        Page<DataModel> dataModelsPage;
        if (type != null) {
            dataModelsPage = dataModelRepository.findByProjectKeyAndType(projectKey, type, pageable);
        } else {
            dataModelsPage = dataModelRepository.findByProjectKey(projectKey, pageable);
        }

        // 4. N+1 방지: 모든 모델의 컬럼들을 배치로 조회
        List<Long> modelKeys = dataModelsPage.getContent().stream()
                .map(DataModel::getModelKey)
                .collect(Collectors.toList());

        // 컬럼 관계 배치 조회
        List<DataModelErdColumnRelation> allRelations = modelKeys.isEmpty()
                ? Collections.emptyList()
                : dataModelErdColumnRelationRepository.findAll().stream()
                        .filter(rel -> modelKeys.contains(rel.getModelKey()))
                        .collect(Collectors.toList());

        // modelKey별로 그룹화
        Map<Long, List<Long>> columnKeysByModel = allRelations.stream()
                .collect(Collectors.groupingBy(
                        DataModelErdColumnRelation::getModelKey,
                        Collectors.mapping(DataModelErdColumnRelation::getColumnKey, Collectors.toList())
                ));

        // 모든 관련 컬럼 조회
        Set<Long> allColumnKeys = allRelations.stream()
                .map(DataModelErdColumnRelation::getColumnKey)
                .collect(Collectors.toSet());

        Map<Long, ErdColumn> columnsMap = allColumnKeys.isEmpty()
                ? Collections.emptyMap()
                : erdColumnRepository.findAllById(allColumnKeys).stream()
                        .collect(Collectors.toMap(ErdColumn::getColumnKey, col -> col));

        // 모든 관련 테이블 조회
        Set<Long> allTableKeys = columnsMap.values().stream()
                .map(ErdColumn::getTableKey)
                .collect(Collectors.toSet());

        Map<Long, ErdTable> tablesMap = allTableKeys.isEmpty()
                ? Collections.emptyMap()
                : erdTableRepository.findAllById(allTableKeys).stream()
                        .collect(Collectors.toMap(ErdTable::getTableKey, table -> table));

        // 5. DTO 변환
        return dataModelsPage.map(dataModel -> {
            List<Long> columnKeys = columnKeysByModel.getOrDefault(dataModel.getModelKey(), Collections.emptyList());
            List<ErdColumn> columns = columnKeys.stream()
                    .map(columnsMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            ErdTable sourceTable = dataModel.getSourceTableKey() != null
                    ? tablesMap.get(dataModel.getSourceTableKey())
                    : null;

            return convertToResponse(dataModel, columns, sourceTable);
        });
    }

    /**
     * 데이터 모델 상세 조회 (생성된 코드 포함)
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param modelKey 모델 키
     * @return 데이터 모델 상세 정보
     */
    public DataModelDetailResponse getDataModelDetail(Integer userKey, Long projectKey, Long modelKey) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. DataModel 조회
        DataModel dataModel = dataModelRepository.findById(modelKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DATA_MODEL_NOT_FOUND));

        // 3. 프로젝트 일치 확인
        if (!dataModel.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_FORBIDDEN);
        }

        // 4. 컬럼 관계 조회
        List<DataModelErdColumnRelation> relations = dataModelErdColumnRelationRepository.findByModelKey(modelKey);
        List<Long> columnKeys = relations.stream()
                .map(DataModelErdColumnRelation::getColumnKey)
                .collect(Collectors.toList());

        // 5. 컬럼 정보 조회
        List<ErdColumn> columns = columnKeys.isEmpty()
                ? Collections.emptyList()
                : erdColumnRepository.findAllById(columnKeys);

        // 6. 테이블 정보 조회
        Set<Long> tableKeys = columns.stream()
                .map(ErdColumn::getTableKey)
                .collect(Collectors.toSet());

        Map<Long, ErdTable> tablesMap = tableKeys.isEmpty()
                ? Collections.emptyMap()
                : erdTableRepository.findAllById(tableKeys).stream()
                        .collect(Collectors.toMap(ErdTable::getTableKey, table -> table));

        // 7. Sync status 계산
        boolean hasDeletedColumns = columns.size() < columnKeys.size();
        OffsetDateTime lastErdUpdatedAt = calculateLastErdUpdatedAt(columns, tablesMap.values());
        SyncStatus syncStatus = SyncStatusCalculator.calculate(
                dataModel.getLastSyncedAt(),
                hasDeletedColumns,
                lastErdUpdatedAt
        );
        String syncMessage = SyncStatusCalculator.getMessage(syncStatus);

        // 8. 관련 테이블 정보 수집
        List<DataModelResponse.TableInfo> relatedTables = collectRelatedTables(columns, tablesMap);

        // 9. 컬럼 상세 정보 생성
        List<DataModelDetailResponse.ColumnDetailDto> columnDetails = columns.stream()
                .map(column -> {
                    ErdTable table = tablesMap.get(column.getTableKey());
                    String alias = NamingConverter.toCamelCase(column.getPhysicalName());

                    return new DataModelDetailResponse.ColumnDetailDto(
                            column.getColumnKey(),
                            column.getTableKey(),
                            table != null ? table.getPhysicalName() : null,
                            column.getLogicalName(),
                            column.getPhysicalName(),
                            column.getDataType(),
                            column.getDataDetail(),
                            column.getIsNullable(),
                            column.getIsPrimaryKey(),
                            column.getIsForeignKey(),
                            column.getIsUnique(),
                            alias
                    );
                })
                .collect(Collectors.toList());

        // 10. 코드 생성 (TODO: 실제 코드 생성 로직 구현)
        Map<String, String> generatedCode = generateCode(dataModel, columns, tablesMap);

        return new DataModelDetailResponse(
                dataModel.getModelKey(),
                dataModel.getProjectKey(),
                dataModel.getName(),
                dataModel.getType(),
                syncStatus,
                syncMessage,
                dataModel.getLastSyncedAt(),
                generatedCode,
                columnDetails,
                relatedTables,
                dataModel.getCreatedAt(),
                dataModel.getUpdatedAt()
        );
    }

    /**
     * 데이터 모델 Refresh (동기화)
     *
     * <p>ERD 변경사항을 반영하고 last_synced_at를 현재 시각으로 업데이트합니다.</p>
     * <p>INVALID 상태(컬럼 삭제)인 경우 Refresh 불가</p>
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param modelKey 모델 키
     * @return 업데이트된 데이터 모델 정보
     */
    @Transactional
    public DataModelResponse refreshDataModel(Integer userKey, Long projectKey, Long modelKey) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. DataModel 조회
        DataModel dataModel = dataModelRepository.findById(modelKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DATA_MODEL_NOT_FOUND));

        // 3. 프로젝트 일치 확인
        if (!dataModel.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_FORBIDDEN);
        }

        // 4. 컬럼 관계 조회
        List<DataModelErdColumnRelation> relations = dataModelErdColumnRelationRepository.findByModelKey(modelKey);
        List<Long> columnKeys = relations.stream()
                .map(DataModelErdColumnRelation::getColumnKey)
                .collect(Collectors.toList());

        // 5. 컬럼 정보 조회
        List<ErdColumn> columns = columnKeys.isEmpty()
                ? Collections.emptyList()
                : erdColumnRepository.findAllById(columnKeys);

        // 6. INVALID 상태 확인 (삭제된 컬럼 존재)
        if (columns.size() < columnKeys.size()) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_CANNOT_REFRESH);
        }

        // 7. last_synced_at 업데이트
        dataModel.updateLastSyncedAt(OffsetDateTime.now());
        dataModelRepository.save(dataModel);

        log.info("DataModel refreshed: modelKey={}, name={}", dataModel.getModelKey(), dataModel.getName());

        // 8. DTO 변환 및 반환
        ErdTable sourceTable = dataModel.getSourceTableKey() != null
                ? erdTableRepository.findById(dataModel.getSourceTableKey()).orElse(null)
                : null;

        return convertToResponse(dataModel, columns, sourceTable);
    }

    /**
     * 데이터 모델 이름 수정
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param modelKey 모델 키
     * @param request 이름 수정 요청
     * @return 수정된 데이터 모델 정보
     */
    @Transactional
    public DataModelResponse updateName(Integer userKey, Long projectKey, Long modelKey, UpdateNameRequest request) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. DataModel 조회
        DataModel dataModel = dataModelRepository.findById(modelKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DATA_MODEL_NOT_FOUND));

        // 3. 프로젝트 일치 확인
        if (!dataModel.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_FORBIDDEN);
        }

        // 4. 이름 변경 (중복 체크)
        if (!request.name().equals(dataModel.getName())) {
            if (dataModelRepository.existsByProjectKeyAndName(projectKey, request.name())) {
                throw new GeneralException(ErrorStatus.DATA_MODEL_NAME_DUPLICATE);
            }
            dataModel.updateName(request.name());
            dataModelRepository.save(dataModel);
        }

        log.info("DataModel name updated: modelKey={}, oldName={}, newName={}",
                dataModel.getModelKey(), dataModel.getName(), request.name());

        // 5. 컬럼 정보 조회
        List<DataModelErdColumnRelation> relations = dataModelErdColumnRelationRepository.findByModelKey(modelKey);
        List<Long> columnKeys = relations.stream()
                .map(DataModelErdColumnRelation::getColumnKey)
                .collect(Collectors.toList());

        List<ErdColumn> columns = columnKeys.isEmpty()
                ? Collections.emptyList()
                : erdColumnRepository.findAllById(columnKeys);

        // 6. DTO 변환 및 반환
        ErdTable sourceTable = dataModel.getSourceTableKey() != null
                ? erdTableRepository.findById(dataModel.getSourceTableKey()).orElse(null)
                : null;

        return convertToResponse(dataModel, columns, sourceTable);
    }

    /**
     * 데이터 모델 삭제 (Soft Delete)
     *
     * @param userKey 사용자 키
     * @param projectKey 프로젝트 키
     * @param modelKey 모델 키
     */
    @Transactional
    public void deleteDataModel(Integer userKey, Long projectKey, Long modelKey) {
        // 1. 권한 검증
        validateProjectMember(userKey, projectKey);

        // 2. DataModel 조회
        DataModel dataModel = dataModelRepository.findById(modelKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.DATA_MODEL_NOT_FOUND));

        // 3. 프로젝트 일치 확인
        if (!dataModel.getProjectKey().equals(projectKey)) {
            throw new GeneralException(ErrorStatus.DATA_MODEL_FORBIDDEN);
        }

        // 4. Soft Delete
        dataModel.softDelete();
        dataModelRepository.save(dataModel);

        log.info("DataModel soft deleted: modelKey={}, name={}", dataModel.getModelKey(), dataModel.getName());
    }

    // ==================== Private Helper Methods ====================

    /**
     * 프로젝트 멤버 권한 검증
     */
    private void validateProjectMember(Integer userKey, Long projectKey) {
        if (!projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey)) {
            throw new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
        }
    }

    /**
     * DataModel을 DataModelResponse로 변환
     *
     * @param dataModel 데이터 모델 엔티티
     * @param columns 관련 컬럼 목록
     * @param sourceTable 소스 테이블 (Entity의 경우, DTO는 null 가능)
     * @return DataModelResponse
     */
    private DataModelResponse convertToResponse(DataModel dataModel, List<ErdColumn> columns, ErdTable sourceTable) {
        // 1. 관련 테이블 정보 수집
        Set<Long> tableKeys = columns.stream()
                .map(ErdColumn::getTableKey)
                .collect(Collectors.toSet());

        Map<Long, ErdTable> tablesMap = tableKeys.isEmpty()
                ? Collections.emptyMap()
                : erdTableRepository.findAllById(tableKeys).stream()
                        .collect(Collectors.toMap(ErdTable::getTableKey, table -> table));

        List<DataModelResponse.TableInfo> relatedTables = collectRelatedTables(columns, tablesMap);

        // 2. Sync status 계산
        List<Long> columnKeys = columns.stream()
                .map(ErdColumn::getColumnKey)
                .collect(Collectors.toList());

        List<DataModelErdColumnRelation> relations = dataModelErdColumnRelationRepository.findByModelKey(dataModel.getModelKey());
        boolean hasDeletedColumns = columns.size() < relations.size();

        OffsetDateTime lastErdUpdatedAt = calculateLastErdUpdatedAt(columns, tablesMap.values());
        SyncStatus syncStatus = SyncStatusCalculator.calculate(
                dataModel.getLastSyncedAt(),
                hasDeletedColumns,
                lastErdUpdatedAt
        );
        String syncMessage = SyncStatusCalculator.getMessage(syncStatus);

        return new DataModelResponse(
                dataModel.getModelKey(),
                dataModel.getProjectKey(),
                dataModel.getName(),
                dataModel.getType(),
                syncStatus,
                syncMessage,
                dataModel.getLastSyncedAt(),
                relatedTables,
                columns.size(),
                dataModel.getCreatedAt(),
                dataModel.getUpdatedAt()
        );
    }

    /**
     * 관련 테이블 정보 수집
     */
    private List<DataModelResponse.TableInfo> collectRelatedTables(List<ErdColumn> columns, Map<Long, ErdTable> tablesMap) {
        return columns.stream()
                .map(ErdColumn::getTableKey)
                .distinct()
                .map(tablesMap::get)
                .filter(Objects::nonNull)
                .map(table -> new DataModelResponse.TableInfo(
                        table.getTableKey(),
                        table.getPhysicalName(),
                        table.getLogicalName()
                ))
                .collect(Collectors.toList());
    }

    /**
     * ERD의 마지막 업데이트 시각 계산
     *
     * @param columns 컬럼 목록
     * @param tables 테이블 목록
     * @return 가장 최근 업데이트 시각 (없으면 null)
     */
    private OffsetDateTime calculateLastErdUpdatedAt(List<ErdColumn> columns, Collection<ErdTable> tables) {
        OffsetDateTime maxColumnUpdatedAt = columns.stream()
                .map(ErdColumn::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        OffsetDateTime maxTableUpdatedAt = tables.stream()
                .map(ErdTable::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(null);

        if (maxColumnUpdatedAt == null) return maxTableUpdatedAt;
        if (maxTableUpdatedAt == null) return maxColumnUpdatedAt;

        return maxColumnUpdatedAt.isAfter(maxTableUpdatedAt) ? maxColumnUpdatedAt : maxTableUpdatedAt;
    }

    /**
     * 코드 생성
     *
     * @param dataModel 데이터 모델
     * @param columns 컬럼 목록
     * @param tablesMap 테이블 맵
     * @return 언어별 생성된 코드 맵
     */
    private Map<String, String> generateCode(DataModel dataModel, List<ErdColumn> columns, Map<Long, ErdTable> tablesMap) {
        Map<String, String> codeMap = new HashMap<>();

        if (dataModel.getType() == DataModelType.ENTITY) {
            // Entity 코드 생성
            ErdTable sourceTable = tablesMap.get(dataModel.getSourceTableKey());
            if (sourceTable != null) {
                String javaCode = com.yaldi.domain.datamodel.codegen.JavaEntityCodeGenerator.generate(
                        dataModel.getName(),
                        sourceTable,
                        columns
                );
                codeMap.put("java", javaCode);

                String tsCode = com.yaldi.domain.datamodel.codegen.TypeScriptInterfaceCodeGenerator.generateForEntity(
                        dataModel.getName(),
                        sourceTable,
                        columns
                );
                codeMap.put("typescript", tsCode);
            }
        } else {
            // DTO 코드 생성 (DTO_REQUEST or DTO_RESPONSE)
            // 컬럼명 충돌 해결을 위한 alias 생성
            Map<Long, String> columnAliases = generateColumnAliases(columns, tablesMap);

            String javaCode = com.yaldi.domain.datamodel.codegen.JavaDtoCodeGenerator.generate(
                    dataModel.getName(),
                    dataModel.getType(),
                    columns,
                    columnAliases
            );
            codeMap.put("java", javaCode);

            String tsCode = com.yaldi.domain.datamodel.codegen.TypeScriptInterfaceCodeGenerator.generateForDto(
                    dataModel.getName(),
                    columns,
                    columnAliases
            );
            codeMap.put("typescript", tsCode);
        }

        return codeMap;
    }

    /**
     * 컬럼명 충돌 해결을 위한 alias 생성
     *
     * <p>여러 테이블의 컬럼 중 중복된 이름이 있으면 테이블명을 prefix로 추가합니다.</p>
     * <p>예: users.id, orders.id → userId, orderId</p>
     *
     * @param columns 컬럼 목록
     * @param tablesMap 테이블 맵
     * @return columnKey → alias 맵
     */
    private Map<Long, String> generateColumnAliases(List<ErdColumn> columns, Map<Long, ErdTable> tablesMap) {
        Map<Long, String> aliases = new HashMap<>();

        // 1. 각 컬럼의 기본 alias (camelCase) 생성
        Map<Long, String> baseAliases = new HashMap<>();
        for (ErdColumn column : columns) {
            String baseAlias = NamingConverter.toCamelCase(column.getPhysicalName());
            baseAliases.put(column.getColumnKey(), baseAlias);
        }

        // 2. 중복된 alias 찾기
        Map<String, List<Long>> aliasCounts = new HashMap<>();
        for (Map.Entry<Long, String> entry : baseAliases.entrySet()) {
            aliasCounts.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        // 3. 중복된 alias에 테이블명 prefix 추가
        for (ErdColumn column : columns) {
            String baseAlias = baseAliases.get(column.getColumnKey());
            List<Long> duplicates = aliasCounts.get(baseAlias);

            if (duplicates != null && duplicates.size() > 1) {
                // 중복됨 → 테이블명 prefix 추가
                ErdTable table = tablesMap.get(column.getTableKey());
                if (table != null) {
                    String tablePrefix = NamingConverter.toCamelCase(table.getPhysicalName());
                    // 첫 글자 대문자로 변환 (userId → UserId)
                    String capitalizedAlias = baseAlias.substring(0, 1).toUpperCase() + baseAlias.substring(1);
                    String newAlias = tablePrefix + capitalizedAlias;
                    aliases.put(column.getColumnKey(), newAlias);
                } else {
                    aliases.put(column.getColumnKey(), baseAlias);
                }
            } else {
                // 중복 없음 → 그대로 사용
                aliases.put(column.getColumnKey(), baseAlias);
            }
        }

        return aliases;
    }
}
