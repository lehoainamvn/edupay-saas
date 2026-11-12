package com.edupay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "role_menus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleMenu {

    @EmbeddedId
    private RoleMenuId id;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class RoleMenuId implements Serializable {
        @Column(name = "role_id")
        private UUID roleId;

        @Column(name = "menu_id")
        private UUID menuId;
    }
}

