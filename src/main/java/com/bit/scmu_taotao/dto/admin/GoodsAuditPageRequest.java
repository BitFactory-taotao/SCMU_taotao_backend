package com.bit.scmu_taotao.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 商品巡检列表查询参数
 */
@Data
public class GoodsAuditPageRequest {

	/**
	 * 审核状态：0待巡检，1已巡检通过，2驳回下架
	 */
	@Min(value = 0, message = "auditStatus必须在0到2之间")
	@Max(value = 2, message = "auditStatus必须在0到2之间")
	private Integer auditStatus = 0;

	/**
	 * 类别筛选：dormitory / entertainment / study
	 */
	private String category;

	/**
	 * 关键词：商品名称/备注/学号
	 */
	private String keyword;

	/**
	 * 页码
	 */
	@Min(value = 1, message = "page必须大于等于1")
	private Integer page = 1;

	/**
	 * 每页条数
	 */
	@Min(value = 1, message = "size必须大于等于1")
	@Max(value = 50, message = "size不能超过50")
	private Integer size = 10;
}

