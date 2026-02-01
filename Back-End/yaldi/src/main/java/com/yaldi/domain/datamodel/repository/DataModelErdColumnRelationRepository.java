package com.yaldi.domain.datamodel.repository;

import com.yaldi.domain.datamodel.entity.DataModelErdColumnRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataModelErdColumnRelationRepository extends JpaRepository<DataModelErdColumnRelation, Long> {

    /**
     * 데이터 모델의 컬럼 관계 목록 조회
     */
    List<DataModelErdColumnRelation> findByModelKey(Long modelKey);
}
