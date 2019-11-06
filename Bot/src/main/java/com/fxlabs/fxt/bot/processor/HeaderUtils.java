package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.bot.assertions.Context;
import com.fxlabs.fxt.dto.project.Auth;
import com.fxlabs.fxt.dto.project.AuthType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class HeaderUtils {

    @Autowired
    private DataResolver dataResolver;

    public void copyHeaders(HttpHeaders httpHeaders, List<String> headers, Context context, String suite) {
        for (String header : headers) {
            String[] tokens = StringUtils.split(header, ":", 2);
            if (ArrayUtils.isNotEmpty(tokens) && (tokens.length == 2) ) {
                String value = dataResolver.resolve(tokens[1].trim(), context, suite);
                httpHeaders.set(tokens[0].trim(), value);
            }
        }
    }

    public void copyAuth(Auth src, Auth dest, Context context, String suite) {

        if (src == null) {
            return;
        }

        if (src.getAuthType() == AuthType.Token) {
            if (StringUtils.isNotEmpty(src.getHeader_1())) {
                dest.setHeader_1(dataResolver.resolve(src.getHeader_1(), context, suite));
            }
            if (StringUtils.isNotEmpty(src.getHeader_2())) {
                dest.setHeader_2(dataResolver.resolve(src.getHeader_2(), context, suite));
            }
        } else if (src.getAuthType() == AuthType.BASIC || src.getAuthType() == AuthType.BasicAuth || src.getAuthType() == AuthType.Basic) {
            if (StringUtils.isNotEmpty(src.getPassword())) {
                dest.setPassword(dataResolver.resolve(src.getPassword(), context, suite));
            }
        }
        // TODO OAuth2.0 & Auth0 support

    }

    public Auth clone(Auth auth) {
        if (auth == null) {
            return auth;
        }

        Auth a = new Auth();
        a.setName(auth.getName());

        a.setAuthType(auth.getAuthType());
        a.setUsername(auth.getUsername());

        a.setPassword(auth.getPassword());

        a.setHeader_1(auth.getHeader_1());
        a.setHeader_2(auth.getHeader_2());

        a.setHeader_3(auth.getHeader_3());

        // OAuth 2.0 properties

        a.setClientId(auth.getClientId());
        a.setClientSecret(auth.getClientSecret());

        a.setId(auth.getId());
        a.setAccessTokenUri(auth.getAccessTokenUri());

        a.setAuthorizationScheme(auth.getAuthorizationScheme());  //[form,header,none,query]

        a.setClientAuthenticationScheme(auth.getClientAuthenticationScheme()); //[form,header,none,query]

        a.setTokenName(auth.getTokenName());
        a.setScope(auth.getScope());
        a.setGrantType(auth.getGrantType());

        a.setPreEstablishedRedirectUri(auth.getPreEstablishedRedirectUri());
        a.setUseCurrentUri(auth.getUseCurrentUri());
        a.setUserAuthorizationUri(auth.getUserAuthorizationUri());

        return a;
    }
}
