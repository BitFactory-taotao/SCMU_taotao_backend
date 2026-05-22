package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.dto.UserReportRequest;
import com.bit.scmu_taotao.entity.TUserReport;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.service.TUserService;
import com.bit.scmu_taotao.service.TUserReportService;
import com.bit.scmu_taotao.mapper.TUserReportMapper;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 35314
* @description 针对表【t_user_report(用户举报记录表)】的数据库操作Service实现
* @createDate 2026-05-20 18:48:56
*/
@Slf4j
@Service
public class TUserReportServiceImpl extends ServiceImpl<TUserReportMapper, TUserReport>
    implements TUserReportService{

    @Autowired
    private TUserService tUserService;

    @Override
    public Result reportUser(String targetId, UserReportRequest request) {
        String reporterId = UserContext.getUserId();
        if (reporterId == null || reporterId.trim().isEmpty()) {
            return Result.fail(401, "用户未登录或登录已过期");
        }
        if (targetId == null || targetId.trim().isEmpty()) {
            return Result.fail(400, "被举报用户ID不能为空");
        }
        if (reporterId.equals(targetId)) {
            return Result.fail(400, "不能举报自己");
        }

        TUser targetUser = tUserService.getById(targetId);
        if (targetUser == null) {
            return Result.fail(404, "被举报用户不存在");
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        long count = this.count(new LambdaQueryWrapper<TUserReport>()
                .eq(TUserReport::getReporterId, reporterId)
                .eq(TUserReport::getTargetId, targetId)
                .ge(TUserReport::getCreateTime, twentyFourHoursAgo));
        if (count > 0) {
            return Result.fail(400, "24小时内不能重复举报同一用户");
        }

        TUserReport report = new TUserReport();
        report.setReporterId(reporterId);
        report.setTargetId(targetId);
        report.setTag(request.getCategory());
        report.setContent(request.getContent());
        List<String> imgUrls = request.getImgUrls();
        List<String> validUrls = imgUrls == null ? List.of() : imgUrls.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .collect(Collectors.toList());
        report.setImgUrls(validUrls.isEmpty() ? null : String.join(",", validUrls));
        report.setStatus(0);

        boolean saved = this.save(report);
        if (!saved) {
            log.warn("举报提交失败：reporterId={}, targetId={}", reporterId, targetId);
            return Result.fail(500, "举报提交失败");
        }
        return Result.ok("举报提交成功");
    }

}




