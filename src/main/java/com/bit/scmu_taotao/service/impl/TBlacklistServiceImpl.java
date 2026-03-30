package com.bit.scmu_taotao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bit.scmu_taotao.entity.TBlacklist;
import com.bit.scmu_taotao.entity.TUser;
import com.bit.scmu_taotao.mapper.TUserMapper;
import com.bit.scmu_taotao.service.TBlacklistService;
import com.bit.scmu_taotao.mapper.TBlacklistMapper;
import com.bit.scmu_taotao.util.UserContext;
import com.bit.scmu_taotao.util.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 35314
 * @description 针对表【t_blacklist(黑名单表)】的数据库操作Service实现
 * @createDate 2026-03-14 18:49:37
 */
@Slf4j
@Service
public class TBlacklistServiceImpl extends ServiceImpl<TBlacklistMapper, TBlacklist>
        implements TBlacklistService{
    @Autowired
    private TUserMapper userMapper;

    @Override
    public Result getBlacklistPage(Integer page, Integer size) {
        try {
            String userId = UserContext.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail(401, "用户未登录或登录已过期");
            }

            log.info("获取黑名单列表：userId={}, page={}, size={}", userId, page, size);

            Page<TBlacklist> p = new Page<>(page, size);
            LambdaQueryWrapper<TBlacklist> qw = new LambdaQueryWrapper<>();
            qw.eq(TBlacklist::getUserId, userId)
                    .eq(TBlacklist::getIsDelete, 0)
                    .orderByDesc(TBlacklist::getCreateTime);

            Page<TBlacklist> resultPage = this.page(p, qw);
            List<TBlacklist> records = resultPage.getRecords();
            if (records.isEmpty()) {
                return Result.ok("请求成功", Map.of("total", 0, "pages", 0, "list", List.of()));
            }

            List<String> blackUserIds = records.stream()
                    .map(TBlacklist::getBlackUserId).distinct().collect(Collectors.toList());

            Map<String, String> nameMap = userMapper.selectList(
                            new LambdaQueryWrapper<TUser>()
                                    .in(TUser::getUserId, blackUserIds)
                                    .eq(TUser::getIsDelete, 0))
                    .stream()
                    .collect(Collectors.toMap(TUser::getUserId, TUser::getUserName, (a, b) -> a));

            List<Map<String, Object>> list = new ArrayList<>();
            for (TBlacklist item : records) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", item.getBlackUserId());
                row.put("name", nameMap.getOrDefault(item.getBlackUserId(), ""));
                row.put("addTime", item.getCreateTime() == null ? "" : item.getCreateTime());
                list.add(row);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("total", resultPage.getTotal());
            data.put("pages", resultPage.getPages());
            data.put("list", list);

            return Result.ok("请求成功", data);
        } catch (Exception e) {
            log.error("获取黑名单列表失败", e);
            return Result.fail("获取黑名单列表失败，请稍后重试");
        }
    }

    @Override
    public Result removeBlacklist(String userId) {
        try {
            String currentUserId = UserContext.getUserId();
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                return Result.fail(401, "用户未登录或登录已过期");
            }

            if (userId == null || userId.trim().isEmpty()) {
                return Result.fail(400, "被拉黑用户ID不能为空");
            }

            log.info("解除黑名单：currentUserId={}, blackUserId={}", currentUserId, userId);

            // 查询黑名单记录
            LambdaQueryWrapper<TBlacklist> qw = new LambdaQueryWrapper<>();
            qw.eq(TBlacklist::getUserId, currentUserId)
                    .eq(TBlacklist::getBlackUserId, userId)
                    .eq(TBlacklist::getIsDelete, 0);

            TBlacklist blacklist = this.getOne(qw);
            if (blacklist == null) {
                return Result.fail(404, "黑名单记录不存在");
            }

            // 更新isDelete字段为1（逻辑删除）
            blacklist.setIsDelete(1);
            boolean updated = this.updateById(blacklist);

            if (updated) {
                log.info("解除黑名单成功：currentUserId={}, blackUserId={}", currentUserId, userId);
                return Result.ok("解除黑名单成功",null);
            } else {
                log.warn("解除黑名单失败（更新返回false）：currentUserId={}, blackUserId={}", currentUserId, userId);
                return Result.fail("解除黑名单失败，请稍后重试");
            }

        } catch (Exception e) {
            log.error("解除黑名单失败：{}", e.getMessage(), e);
            return Result.fail("解除黑名单失败，请稍后重试");
        }
    }
}




