package com.bit.scmu_taotao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bit.scmu_taotao.entity.TAccountAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TAccountAuditLogMapper extends BaseMapper<TAccountAuditLog> {
    IPage<TAccountAuditLog> selectLatestPerUser(Page<?> page,
                                                @Param("action") String action,
                                                @Param("keyword") String keyword);
}
