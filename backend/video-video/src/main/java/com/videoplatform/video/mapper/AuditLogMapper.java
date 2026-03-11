package com.videoplatform.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.videoplatform.video.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核日志Mapper
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
