package com.lms.system.dto.admin;

import com.lms.system.model.CourseCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminCategoryForm {

    @NotBlank(message = "Category name is required")
    @Size(max = 120)
    private String name;

    @Size(max = 10000)
    private String description;

    private Long parentId;

    public AdminCategoryForm() {
    }

    public AdminCategoryForm(CourseCategory category) {
        this.name = category.getName();
        this.description = category.getDescription();
        this.parentId = category.getParentId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
