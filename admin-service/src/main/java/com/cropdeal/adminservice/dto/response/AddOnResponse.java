package com.cropdeal.adminservice.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class AddOnResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
}
