package com.videoplatform.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoTagRelationMapper extends BaseMapper<Object> {
    int deleteByTagId(@Param("tagId") Integer tagId);
    int countByTagId(@Param("tagId") Integer tagId);
}
