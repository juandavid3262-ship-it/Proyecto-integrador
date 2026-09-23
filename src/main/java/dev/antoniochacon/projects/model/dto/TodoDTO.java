package dev.antoniochacon.projects.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TodoDTO(
        String title,
        @JsonProperty("isCompleted") boolean isCompleted
) {
}
