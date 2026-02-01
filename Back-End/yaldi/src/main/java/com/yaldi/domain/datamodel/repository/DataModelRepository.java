package com.yaldi.domain.datamodel.repository;

import com.yaldi.domain.datamodel.entity.DataModelType;
import com.yaldi.domain.datamodel.entity.DataModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataModelRepository extends JpaRepository<DataModel, Long> {

    /**
     * 프로젝트의 데이터 모델 목록 조회 (페이지네이션)
     */
    Page<DataModel> findByProjectKey(Long projectKey, Pageable pageable);

    /**
     * 프로젝트의 특정 타입 데이터 모델 목록 조회 (페이지네이션)
     */
    Page<DataModel> findByProjectKeyAndType(Long projectKey, DataModelType type, Pageable pageable);

    /**
     * 프로젝트의 특정 이름 데이터 모델 존재 여부
     */
    boolean existsByProjectKeyAndName(Long projectKey, String name);
}
