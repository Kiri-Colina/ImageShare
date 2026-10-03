package com.example.imageshare.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.imageshare.server.entity.SharedImage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 共享图片 Mapper，继承 BaseMapper 获得单表 CRUD 能力。
 */
@Mapper
public interface SharedImageMapper extends BaseMapper<SharedImage> {
}
