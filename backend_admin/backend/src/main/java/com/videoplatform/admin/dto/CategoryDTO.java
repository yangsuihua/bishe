package com.videoplatform.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDTO {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sort;
    private String icon;
    private Integer status;
    private List<CategoryDTO> children;
}
