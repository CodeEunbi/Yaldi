package com.yaldi.domain.search.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.OffsetDateTime;

/**
 * Document: 이 클래스가 ES 인덱스의 도큐먼트임을 알려주는 애노테이션.
 * 어떤 인덱스(versions)에 저장되는지 지정
 *  Spring Data Elasticsearch가 이 클래스를 기반으로
 * → 인덱스 생성 / 매핑 관리 / CRUD 리포지토리 생성 등을 자동화
 *
 * Field, FieldType:
 * 이 클래스의 필드를 ES 인덱스의 프로퍼티 매핑으로 바꿔주는 애노테이션.
 *  ES에서 어떤 타입으로 저장할지 결정
 *  text, keyword, date, boolean 등 지정
 *  분석기/토크나이저도 설정 가능 (Analyzer)
 *  검색 자동완성/정확 매칭/정렬 여부 등을 제어
 */

@Document(indexName = "versions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionDocument {

    @Id
    private Long versionKey;

    @Field(type = FieldType.Long)
    private Long projectKey;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String versionName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String versionDescription;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String projectName;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String projectDescription;

    @Field(type = FieldType.Keyword)
    private String projectImageUrl;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] vector;

    @Field(type = FieldType.Boolean)
    private Boolean isPublic;

    @Field(type = FieldType.Keyword)
    private String designVerificationStatus;

    @Field(type = FieldType.Date)
    private OffsetDateTime  createdAt;
}
