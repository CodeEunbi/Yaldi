package com.yaldi.domain.search.repository;

import com.yaldi.domain.search.document.VersionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionSearchRepository extends
        ElasticsearchRepository<VersionDocument, Long> {
}
