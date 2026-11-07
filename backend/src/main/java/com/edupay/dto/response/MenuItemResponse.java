package com.edupay.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private String code;
    private String name;
    private String icon;
    private String route;
    private Integer sortOrder;
    private String badge;
    private List<MenuItemResponse> children;
}

