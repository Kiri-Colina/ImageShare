package com.example.imageshare.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.imageshare.server.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，继承 BaseMapper 获得单表 CRUD 能力。
 * 条件查询通过 LambdaQueryWrapper 在 Service 层拼装。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
