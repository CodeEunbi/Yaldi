package com.yaldi.domain.version.service;

import com.yaldi.domain.project.repository.ProjectMemberRelationRepository;
import com.yaldi.domain.project.repository.ProjectRepository;
import com.yaldi.domain.version.dto.response.VersionResponse;
import com.yaldi.domain.version.dto.response.compare.*;
import com.yaldi.domain.version.entity.Version;
import com.yaldi.domain.version.repository.VersionRepository;
import com.yaldi.global.exception.GeneralException;
import com.yaldi.global.response.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionCompareService {

    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRelationRepository projectMemberRelationRepository;

    @Transactional(readOnly = true)
    public VersionCompareResponse compareVersion(Integer userKey, Long versionKey) {
        Version currentVersion = versionRepository.findById(versionKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.VERSION_NOT_FOUND));

        Long projectKey = currentVersion.getProjectKey();
        projectRepository.findById(projectKey)
                .orElseThrow(() -> new GeneralException(ErrorStatus.PROJECT_NOT_FOUND));

        validateProjectMember(userKey, projectKey);

        Version previousVersion = findPreviousVersion(projectKey, currentVersion);
        VersionResponse currentResponse = VersionResponse.from(currentVersion);
        VersionResponse previousResponse = previousVersion != null ? VersionResponse.from(previousVersion) : null;

        // 이전 버전이 없으면 diff 없이 반환
        if (previousVersion == null) {
            return new VersionCompareResponse(null, currentResponse, null);
        }

        SchemaDiff schemaDiff = calculateSchemaDiff(previousVersion.getSchemaData(), currentVersion.getSchemaData());

        return new VersionCompareResponse(previousResponse, currentResponse, schemaDiff);
    }

    private Version findPreviousVersion(Long projectKey, Version currentVersion) {
        List<Version> versions = versionRepository.findByProjectKeyOrderByCreatedAtDesc(projectKey);

        return versions.stream()
                .filter(v -> v.getCreatedAt().isBefore(currentVersion.getCreatedAt()))
                .findFirst()
                .orElse(null);
    }

    private SchemaDiff calculateSchemaDiff(Map<String, Object> previousSchema, Map<String, Object> currentSchema) {
        List<Map<String, Object>> previousTables = extractTables(previousSchema);
        List<Map<String, Object>> currentTables = extractTables(currentSchema);

        List<Map<String, Object>> previousRelations = extractRelations(previousSchema);
        List<Map<String, Object>> currentRelations = extractRelations(currentSchema);

        List<TableDiff> tableDiffs = calculateTableDiffs(previousTables, currentTables);
        List<RelationDiff> relationDiffs = calculateRelationDiffs(previousRelations, currentRelations);

        DiffSummary summary = calculateSummary(tableDiffs, relationDiffs);

        return new SchemaDiff(tableDiffs, relationDiffs, summary);
    }

    private List<Map<String, Object>> extractTables(Map<String, Object> schema) {
        if (schema == null) return Collections.emptyList();
        Object tables = schema.get("tables");
        if (tables instanceof List) return (List<Map<String, Object>>) tables;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRelations(Map<String, Object> schema) {
        if (schema == null) return Collections.emptyList();
        Object relations = schema.get("relations");
        if (relations instanceof List) {
            return (List<Map<String, Object>>) relations;
        }
        return Collections.emptyList();
    }

    private List<TableDiff> calculateTableDiffs(List<Map<String, Object>> previousTables, List<Map<String, Object>> currentTables) {
        List<TableDiff> tableDiffs = new ArrayList<>();

        Map<String, Map<String, Object>> previousTableMap = previousTables.stream()
                .collect(Collectors.toMap(t -> (String) t.get("physicalName"), t -> t));

        Map<String, Map<String, Object>> currentTableMap = currentTables.stream()
                .collect(Collectors.toMap(t -> (String) t.get("physicalName"), t -> t));

        // 현재 테이블 처리 (ADDED, MODIFIED, UNCHANGED)
        for (Map<String, Object> currentTable : currentTables) {
            String physicalName = (String) currentTable.get("physicalName");
            Map<String, Object> previousTable = previousTableMap.get(physicalName);

            if (previousTable == null) {
                // 새로 추가된 테이블
                tableDiffs.add(createTableDiff(currentTable, null, ChangeType.ADDED));
            } else {
                // 존재하는 테이블 - 변경 여부 확인
                tableDiffs.add(createTableDiff(currentTable, previousTable, null));
            }
        }

        // 삭제된 테이블 처리
        for (Map<String, Object> previousTable : previousTables) {
            String physicalName = (String) previousTable.get("physicalName");
            if (!currentTableMap.containsKey(physicalName)) {
                tableDiffs.add(createTableDiff(previousTable, null, ChangeType.DELETED));
            }
        }

        return tableDiffs;
    }

    @SuppressWarnings("unchecked")
    private TableDiff createTableDiff(Map<String, Object> currentTable, Map<String, Object> previousTable, ChangeType forceChangeType) {
        Long tableKey = getLongValue(currentTable != null ? currentTable : previousTable, "tableKey");
        String physicalName = (String) (currentTable != null ? currentTable : previousTable).get("physicalName");
        String logicalName = (String) (currentTable != null ? currentTable : previousTable).get("logicalName");

        List<Map<String, Object>> currentColumns = currentTable != null ?
                (List<Map<String, Object>>) currentTable.getOrDefault("columns", Collections.emptyList()) :
                Collections.emptyList();
        List<Map<String, Object>> previousColumns = previousTable != null ?
                (List<Map<String, Object>>) previousTable.getOrDefault("columns", Collections.emptyList()) :
                Collections.emptyList();

        List<ColumnDiff> columnDiffs = calculateColumnDiffs(previousColumns, currentColumns);

        if (forceChangeType != null) {
            return new TableDiff(forceChangeType, tableKey, physicalName, logicalName, columnDiffs, null, null);
        }

        // 테이블 자체의 변경 확인
        List<String> changedFields = new ArrayList<>();
        Map<String, Object> previousValues = new HashMap<>();

        if (!Objects.equals(currentTable.get("logicalName"), previousTable.get("logicalName"))) {
            changedFields.add("logicalName");
            previousValues.put("logicalName", previousTable.get("logicalName"));
        }

        // 컬럼에 변경이 있거나 테이블 필드에 변경이 있으면 MODIFIED
        boolean hasColumnChanges = columnDiffs.stream().anyMatch(cd -> cd.changeType() != ChangeType.UNCHANGED);
        ChangeType changeType = (!changedFields.isEmpty() || hasColumnChanges) ? ChangeType.MODIFIED : ChangeType.UNCHANGED;

        return new TableDiff(
                changeType,
                tableKey,
                physicalName,
                logicalName,
                columnDiffs,
                changedFields.isEmpty() ? null : changedFields,
                previousValues.isEmpty() ? null : previousValues
        );
    }

    private List<ColumnDiff> calculateColumnDiffs(List<Map<String, Object>> previousColumns, List<Map<String, Object>> currentColumns) {
        List<ColumnDiff> columnDiffs = new ArrayList<>();

        Map<String, Map<String, Object>> previousColumnMap = previousColumns.stream()
                .collect(Collectors.toMap(c -> (String) c.get("physicalName"), c -> c));

        Map<String, Map<String, Object>> currentColumnMap = currentColumns.stream()
                .collect(Collectors.toMap(c -> (String) c.get("physicalName"), c -> c));

        // 현재 컬럼 처리
        for (Map<String, Object> currentColumn : currentColumns) {
            String physicalName = (String) currentColumn.get("physicalName");
            Map<String, Object> previousColumn = previousColumnMap.get(physicalName);

            if (previousColumn == null) {
                columnDiffs.add(createColumnDiff(currentColumn, null, ChangeType.ADDED));
            } else {
                columnDiffs.add(createColumnDiff(currentColumn, previousColumn, null));
            }
        }

        // 삭제된 컬럼 처리
        for (Map<String, Object> previousColumn : previousColumns) {
            String physicalName = (String) previousColumn.get("physicalName");
            if (!currentColumnMap.containsKey(physicalName)) {
                columnDiffs.add(createColumnDiff(previousColumn, null, ChangeType.DELETED));
            }
        }

        return columnDiffs;
    }

    @SuppressWarnings("unchecked")
    private ColumnDiff createColumnDiff(Map<String, Object> currentColumn, Map<String, Object> previousColumn, ChangeType forceChangeType) {
        Map<String, Object> column = currentColumn != null ? currentColumn : previousColumn;

        Long columnKey = getLongValue(column, "columnKey");
        String physicalName = (String) column.get("physicalName");
        String logicalName = (String) column.get("logicalName");
        String dataType = (String) column.get("dataType");
        List<Object> dataDetail = (List<Object>) column.getOrDefault("dataDetail", Collections.emptyList());
        Boolean isPrimaryKey = (Boolean) column.get("isPrimaryKey");
        Boolean isNullable = (Boolean) column.get("isNullable");
        Boolean isUnique = (Boolean) column.get("isUnique");
        Boolean isForeignKey = (Boolean) column.get("isForeignKey");
        Boolean isIncremental = (Boolean) column.get("isIncremental");
        String defaultValue = (String) column.get("defaultValue");

        if (forceChangeType != null) {
            return new ColumnDiff(forceChangeType, columnKey, physicalName, logicalName, dataType, dataDetail,
                    isPrimaryKey, isNullable, isUnique, isForeignKey, isIncremental, defaultValue, null, null);
        }

        // 변경 필드 확인
        List<String> changedFields = new ArrayList<>();
        Map<String, Object> previousValues = new HashMap<>();

        String[] fieldsToCheck = {"logicalName", "dataType", "dataDetail", "isPrimaryKey", "isNullable",
                "isUnique", "isForeignKey", "isIncremental", "defaultValue"};

        for (String field : fieldsToCheck) {
            Object currentValue = currentColumn.get(field);
            Object previousValue = previousColumn.get(field);
            if (!Objects.equals(currentValue, previousValue)) {
                changedFields.add(field);
                previousValues.put(field, previousValue);
            }
        }

        ChangeType changeType = changedFields.isEmpty() ? ChangeType.UNCHANGED : ChangeType.MODIFIED;

        return new ColumnDiff(changeType, columnKey, physicalName, logicalName, dataType, dataDetail,
                isPrimaryKey, isNullable, isUnique, isForeignKey, isIncremental, defaultValue,
                changedFields.isEmpty() ? null : changedFields,
                previousValues.isEmpty() ? null : previousValues);
    }

    private List<RelationDiff> calculateRelationDiffs(List<Map<String, Object>> previousRelations, List<Map<String, Object>> currentRelations) {
        List<RelationDiff> relationDiffs = new ArrayList<>();

        // 관계는 (fromTableKey, toTableKey) 조합으로 식별
        Map<String, Map<String, Object>> previousRelationMap = previousRelations.stream()
                .collect(Collectors.toMap(this::getRelationKey, r -> r));

        Map<String, Map<String, Object>> currentRelationMap = currentRelations.stream()
                .collect(Collectors.toMap(this::getRelationKey, r -> r));

        // 현재 관계 처리
        for (Map<String, Object> currentRelation : currentRelations) {
            String key = getRelationKey(currentRelation);
            Map<String, Object> previousRelation = previousRelationMap.get(key);

            if (previousRelation == null) {
                relationDiffs.add(createRelationDiff(currentRelation, null, ChangeType.ADDED));
            } else {
                relationDiffs.add(createRelationDiff(currentRelation, previousRelation, null));
            }
        }

        // 삭제된 관계 처리
        for (Map<String, Object> previousRelation : previousRelations) {
            String key = getRelationKey(previousRelation);
            if (!currentRelationMap.containsKey(key)) {
                relationDiffs.add(createRelationDiff(previousRelation, null, ChangeType.DELETED));
            }
        }

        return relationDiffs;
    }

    private String getRelationKey(Map<String, Object> relation) {
        Long fromTableKey = getLongValue(relation, "fromTableKey");
        Long toTableKey = getLongValue(relation, "toTableKey");
        return fromTableKey + "-" + toTableKey;
    }

    private RelationDiff createRelationDiff(Map<String, Object> currentRelation, Map<String, Object> previousRelation, ChangeType forceChangeType) {
        Map<String, Object> relation = currentRelation != null ? currentRelation : previousRelation;

        Long fromTableKey = getLongValue(relation, "fromTableKey");
        Long toTableKey = getLongValue(relation, "toTableKey");
        String relationType = (String) relation.get("relationType");
        String constraintName = (String) relation.get("constraintName");
        String onDeleteAction = (String) relation.get("onDeleteAction");
        String onUpdateAction = (String) relation.get("onUpdateAction");

        if (forceChangeType != null) {
            return new RelationDiff(forceChangeType, fromTableKey, toTableKey, relationType, constraintName,
                    onDeleteAction, onUpdateAction, null, null);
        }

        // 변경 필드 확인
        List<String> changedFields = new ArrayList<>();
        Map<String, Object> previousValues = new HashMap<>();

        String[] fieldsToCheck = {"relationType", "constraintName", "onDeleteAction", "onUpdateAction"};

        for (String field : fieldsToCheck) {
            Object currentValue = currentRelation.get(field);
            Object previousValue = previousRelation.get(field);
            if (!Objects.equals(currentValue, previousValue)) {
                changedFields.add(field);
                previousValues.put(field, previousValue);
            }
        }

        ChangeType changeType = changedFields.isEmpty() ? ChangeType.UNCHANGED : ChangeType.MODIFIED;

        return new RelationDiff(changeType, fromTableKey, toTableKey, relationType, constraintName,
                onDeleteAction, onUpdateAction,
                changedFields.isEmpty() ? null : changedFields,
                previousValues.isEmpty() ? null : previousValues);
    }

    private DiffSummary calculateSummary(List<TableDiff> tableDiffs, List<RelationDiff> relationDiffs) {
        int addedTables = (int) tableDiffs.stream().filter(t -> t.changeType() == ChangeType.ADDED).count();
        int modifiedTables = (int) tableDiffs.stream().filter(t -> t.changeType() == ChangeType.MODIFIED).count();
        int deletedTables = (int) tableDiffs.stream().filter(t -> t.changeType() == ChangeType.DELETED).count();

        int addedColumns = tableDiffs.stream()
                .flatMap(t -> t.columnDiffs().stream())
                .filter(c -> c.changeType() == ChangeType.ADDED)
                .mapToInt(c -> 1).sum();

        int modifiedColumns = tableDiffs.stream()
                .flatMap(t -> t.columnDiffs().stream())
                .filter(c -> c.changeType() == ChangeType.MODIFIED)
                .mapToInt(c -> 1).sum();

        int deletedColumns = tableDiffs.stream()
                .flatMap(t -> t.columnDiffs().stream())
                .filter(c -> c.changeType() == ChangeType.DELETED)
                .mapToInt(c -> 1).sum();

        int addedRelations = (int) relationDiffs.stream().filter(r -> r.changeType() == ChangeType.ADDED).count();
        int modifiedRelations = (int) relationDiffs.stream().filter(r -> r.changeType() == ChangeType.MODIFIED).count();
        int deletedRelations = (int) relationDiffs.stream().filter(r -> r.changeType() == ChangeType.DELETED).count();

        boolean hasChanges = addedTables > 0 || modifiedTables > 0 || deletedTables > 0 ||
                addedColumns > 0 || modifiedColumns > 0 || deletedColumns > 0 ||
                addedRelations > 0 || modifiedRelations > 0 || deletedRelations > 0;

        return new DiffSummary(
                addedTables, modifiedTables, deletedTables,
                addedColumns, modifiedColumns, deletedColumns,
                addedRelations, modifiedRelations, deletedRelations,
                hasChanges
        );
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return (Long) value;
    }

    private void validateProjectMember(Integer userKey, Long projectKey) {
        if (!projectMemberRelationRepository.existsByProjectKeyAndMemberKey(projectKey, userKey)) {
            throw new GeneralException(ErrorStatus.PROJECT_FORBIDDEN);
        }
    }
}
