package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.entity.TBlacklist;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bit.scmu_taotao.util.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @author 35314
 * @description 针对表【t_blacklist(黑名单表)】的数据库操作Service
 * @createDate 2026-03-14 18:49:37
 */
public interface TBlacklistService extends IService<TBlacklist> {

    Result getBlacklistPage(@NotNull(message = "page不能为空") @Min(value = 1, message = "page必须大于等于1") Integer page, @NotNull(message = "size不能为空") @Min(value = 1, message = "size必须大于等于1") @Max(value = 100, message = "size不能超过100") Integer size);

    Result removeBlacklist(String userId);

    Result addBlacklist(String userId);
}
