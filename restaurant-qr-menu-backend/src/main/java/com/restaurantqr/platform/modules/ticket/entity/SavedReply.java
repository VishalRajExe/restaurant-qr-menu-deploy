package com.restaurantqr.platform.modules.ticket.entity;

import com.restaurantqr.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_replies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedReply extends BaseEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
}
