package com.bit.scmu_taotao.dto.admin;

import lombok.Data;

@Data
public class SolvedUserItemDTO {
    private String userId;
    private String userName;
    private String avatar;
    private String reason;
    private String registerTime;
    private String handleStatus;
}
