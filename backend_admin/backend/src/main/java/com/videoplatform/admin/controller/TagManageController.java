package com.videoplatform.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.videoplatform.admin.dto.TagDTO;
import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.TagManageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/tags")
@RequiredArgsConstructor
public class TagManageController {
    
    private final TagManageService tagManageService;
    
    @GetMapping
    public Result<Page<TagDTO>> getTagList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<TagDTO> result = tagManageService.getTagList(page, size);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<TagDTO> getTagDetail(@PathVariable Integer id) {
        TagDTO result = tagManageService.getTagDetail(id);
        return Result.success(result);
    }
    
    @PostMapping
    public Result<String> createTag(@RequestBody TagDTO dto) {
        tagManageService.createTag(dto);
        return Result.success("创建成功");
    }
    
    @PutMapping("/{id}")
    public Result<String> updateTag(@PathVariable Integer id, @RequestBody TagDTO dto) {
        tagManageService.updateTag(id, dto);
        return Result.success("更新成功");
    }
    
    @DeleteMapping("/{id}")
    public Result<String> deleteTag(@PathVariable Integer id) {
        tagManageService.deleteTag(id);
        return Result.success("删除成功");
    }
}
