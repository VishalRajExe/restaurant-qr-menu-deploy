package com.restaurantqr.platform.modules.media.dto;

import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetDto {

    private Long id;
    private Long restaurantId;
    private String url;
    private String publicId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private MediaAsset.StorageProviderType provider;
    private String cropData;
    private LocalDateTime createdAt;
}
