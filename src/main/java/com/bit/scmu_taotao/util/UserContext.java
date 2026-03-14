package com.bit.scmu_taotao.util;

import lombok.Data;
import org.apache.http.client.CookieStore;

public class UserContext {
    //静态内部类，用于存储用户信息，让user的信息通过ThreadLocal来传递
    //这样可以避免在每个方法中都传递用户信息，而是通过ThreadLocal来传递用户信息
    @Data
    static
    class UserInfo{
        private String openid;
        private CookieStore cookieStore;
        private Integer id;
        private String username;
        private String password;
        //构造方法
        public UserInfo(String openid, CookieStore cookieStore, Integer id,String username, String password){
            this.openid = openid;
            this.cookieStore = cookieStore;
            this.id = id;
            this.username = username;
            this.password = password;
        }
    }

    //ThreadLocal用于存储用户信息
    private final static ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();
    //ThreadLocal.set
    public static void set(String openid, CookieStore cookieStore,
                           Integer id,String username, String password) {
        UserInfo userInfo = new UserInfo(openid, cookieStore,id, username, password);
        CONTEXT.set(userInfo);
    }
    //ThreadLocal.get
    public static String getOpenid() {
        return CONTEXT.get().getOpenid();
    }
    public static CookieStore getCookieStore() {
        return CONTEXT.get().getCookieStore();
    }
    public static String getUsername() {
        return CONTEXT.get().getUsername();
    }
    public static String getPassword() {
        return CONTEXT.get().getPassword();
    }
    public static Integer getId() {
        return CONTEXT.get().getId();
    }
    //ThreadLocal.remove
    public static void remove() {
        CONTEXT.remove();
    }
}
