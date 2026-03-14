package com.bit.scmu_taotao.util.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer code;
    private String msg;
    private Object data;
    private Long total;

    public static Result ok(){
        return new Result(200, "操作成功", null, null);
    }
    
    public static Result ok(Object data){
        return new Result(200, "操作成功", data, null);
    }
    
    public static Result ok(String msg, Object data){
        return new Result(200, msg, data, null);
    }
    
    public static Result ok(List<?> data, Long total){
        return new Result(200, "操作成功", data, total);
    }
    
    public static Result fail(String errorMsg){
        return new Result(500, errorMsg, null, null);
    }
    
    public static Result fail(Integer code, String errorMsg){
        return new Result(code, errorMsg, null, null);
    }
}
