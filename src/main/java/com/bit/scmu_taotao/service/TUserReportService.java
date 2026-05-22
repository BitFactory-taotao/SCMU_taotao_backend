package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.UserReportRequest;
import com.bit.scmu_taotao.entity.TUserReport;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;

/**
* @author 35314
* @description 针对表【t_user_report(用户举报记录表)】的数据库操作Service
* @createDate 2026-05-20 18:48:56
*/
public interface TUserReportService extends IService<TUserReport> {

	Result reportUser(String targetId, UserReportRequest request);

}
