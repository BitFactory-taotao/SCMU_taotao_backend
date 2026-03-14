package com.bit.scmu_taotao.service.impl;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cookie 序列化工具类
 * 用于将 CookieStore 序列化为字节数组，便于存储到 Redis
 */
public class CookieSerialization {

    /**
     * 序列化 CookieStore
     * @param cookieStore CookieStore 对象
     * @return 序列化后的字节数组
     * @throws IOException IO 异常
     */
    public static byte[] serialize(CookieStore cookieStore) throws IOException {
        // 获取所有 Cookie
        List<Cookie> cookies = cookieStore.getCookies();

        // 转换为可序列化的列表
        List<SerializableCookie> serializableCookies = new ArrayList<>();
        for (Cookie cookie : cookies) {
            serializableCookies.add(new SerializableCookie(cookie));
        }

        // 使用 ByteArrayOutputStream 和 ObjectOutputStream 进行序列化
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(serializableCookies);
            return baos.toByteArray();
        }
    }

    /**
     * 反序列化 CookieStore
     * @param data 序列化后的字节数组
     * @return 反序列化后的 CookieStore
     * @throws IOException IO 异常
     * @throws ClassNotFoundException 类未找到异常
     */
    public static CookieStore deserialize(byte[] data) throws IOException, ClassNotFoundException {
        CookieStore cookieStore = new org.apache.http.impl.client.BasicCookieStore();

        if (data == null || data.length == 0) {
            return cookieStore;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            List<SerializableCookie> serializableCookies = (List<SerializableCookie>) ois.readObject();

            for (SerializableCookie sc : serializableCookies) {
                cookieStore.addCookie(sc.getCookie());
            }
        }

        return cookieStore;
    }

    /**
     * 可序列化的 Cookie 包装类
     */
    private static class SerializableCookie implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String value;
        private String domain;
        private String path;
        private int version;
        private boolean secure;
        private long expiryDate;

        public SerializableCookie(Cookie cookie) {
            this.name = cookie.getName();
            this.value = cookie.getValue();
            this.domain = cookie.getDomain();
            this.path = cookie.getPath();
            this.version = cookie.getVersion();
            this.secure = cookie.isSecure();
            this.expiryDate = cookie.getExpiryDate() != null ? cookie.getExpiryDate().getTime() : -1;
        }

        public Cookie getCookie() {
            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path);
            cookie.setVersion(version);
            cookie.setSecure(secure);
            if (expiryDate > 0) {
                cookie.setExpiryDate(new java.util.Date(expiryDate));
            }

            return cookie;
        }
    }
}
