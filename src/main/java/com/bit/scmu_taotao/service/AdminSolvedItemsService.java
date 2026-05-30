package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.dto.admin.*;
import com.bit.scmu_taotao.util.common.Result;

public interface AdminSolvedItemsService {
    Result getSolvedItemList(SolvedItemListRequest request);
    Result getSolvedItemDetail(String type, String id);
    Result getSolvedItemCount();
    Result revokeSolvedItem(SolvedItemRevokeRequest request);
}
