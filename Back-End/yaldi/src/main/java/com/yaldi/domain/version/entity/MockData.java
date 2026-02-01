package com.yaldi.domain.version.entity;

import com.yaldi.global.common.BaseSoftDeleteEntity;
import com.yaldi.global.asyncjob.entity.AsyncJob;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * MockData 엔티티
 */
@Entity
@Table(
    name = "mock_data",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_version_mock_name",
            columnNames = {"version_key", "file_name"}
        )
    }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockData extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_data_key")
    private Long mockDataKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private AsyncJob asyncJob;

    @Column(name = "version_key", nullable = false)
    private Long versionKey;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "row_counts", nullable = false)
    private Short rowCounts;

    public void complete(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
}
