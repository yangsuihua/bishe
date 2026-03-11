package com.videoplatform.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.videoplatform.common.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
