package com.bit.scmu_taotao.service;

import org.apache.http.cookie.*;
import org.apache.http.util.Args;
import org.apache.http.util.TextUtils;

import java.util.Locale;

public class EasyCookieDomainHandler implements CommonCookieAttributeHandler {
    @Override
    public String getAttributeName() {
        return "domain";
    }

    @Override
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        Args.notNull(cookie, "Cookie");
        if (TextUtils.isBlank(value)) {
            throw new MalformedCookieException("Blank or null value for domain attribute");
        } else if (!value.endsWith(".")) {
            String domain = value;
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }

            domain = domain.toLowerCase(Locale.ROOT);
            cookie.setDomain(domain);
        }
    }

    @Override
    public void validate(Cookie cookie, CookieOrigin origin) {
        return;
    }

    @Override
    public boolean match(Cookie cookie, CookieOrigin cookieOrigin) {
        return true;
    }
}
