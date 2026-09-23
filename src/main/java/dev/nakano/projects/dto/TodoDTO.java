package dev.nakano.projects.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class TodoDTO {

    private String title;

    @JsonProperty("isCompleted")
    private boolean isCompleted;

    public TodoDTO() {
    }

    public TodoDTO(String title, boolean isCompleted) {
        this.title = title;
        this.isCompleted = isCompleted;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @JsonGetter("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @JsonSetter("isCompleted")
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
