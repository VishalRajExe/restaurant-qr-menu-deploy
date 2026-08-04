package com.restaurantqr.platform.modules.enterprise.entity;

import com.restaurantqr.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_backups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBackup extends BaseEntity {

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    @Builder.Default
    private Long sizeBytes = 0L;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "download_url", length = 500)
    private String downloadUrl;
}
