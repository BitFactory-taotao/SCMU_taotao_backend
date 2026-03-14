package com.bit.scmu_taotao.service;

import com.bit.scmu_taotao.service.impl.EasyCookieSpec;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.cookie.*;
import org.apache.http.protocol.HttpContext;


public class EasyCookieSpecProvider implements CookieSpecProvider {
    static final String[] DATE_PATTERNS = new String[]{"EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy"};
    private volatile CookieSpec cookieSpec;

    public EasyCookieSpecProvider() {
    }

    @Override
    public CookieSpec create(HttpContext httpContext) {
        if(this.cookieSpec == null) {
            synchronized (this) {
                if(this.cookieSpec == null) {
                    this.cookieSpec = new EasyCookieSpec(new CommonCookieAttributeHandler[]{new BasicPathHandler(), new EasyCookieDomainHandler(), new BasicMaxAgeHandler(), new BasicSecureHandler(), new BasicExpiresHandler(DATE_PATTERNS)});
                    return this.cookieSpec;
                }
            }
        }
        return this.cookieSpec;
    }
}
