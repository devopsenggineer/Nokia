package com.fxlabs.fxt.bot.processor;

import com.fxlabs.fxt.dto.project.Auth;
import com.fxlabs.fxt.dto.project.AuthType;
import com.fxlabs.fxt.dto.project.GrantType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.client.token.grant.redirect.AbstractRedirectResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @author Intesar Shannan Mohammed
 */
@Component
public class RestTemplateUtil {

    final Logger logger = LoggerFactory.getLogger(getClass());
    final String UNICODE_REPLACEMENT_CHARACTER = "\uFFFD";

    @Autowired
    private Auth0Service auth0Service;

    public ResponseEntity<String> execRequest(String url, HttpMethod method, HttpHeaders httpHeaders, String req, Auth auth) {

        // TODO - handle other auth types
        if (auth == null || auth.getAuthType() == null || auth.getAuthType() == AuthType.No_Authentication) {
            return execBasicRequest(url, method, httpHeaders, req, auth);
        } else if (auth.getAuthType() == AuthType.Basic || auth.getAuthType() == AuthType.BASIC || auth.getAuthType() == AuthType.BasicAuth) {
            httpHeaders.set("Authorization", AuthBuilder.createBasicAuth(auth.getUsername(), auth.getPassword()));
            return execBasicRequest(url, method, httpHeaders, req, auth);
        } else if (auth.getAuthType() == AuthType.OAuth_2_0) {
            return execOAuth2Request(url, method, httpHeaders, req, auth);
        } else if (auth.getAuthType() == AuthType.Token) {
            return execTokenRequest(url, method, httpHeaders, req, auth);
        } else if (auth.getAuthType() == AuthType.Auth0) {
            return execAuth0Request(url, method, httpHeaders, req, auth);
        } else {
            logger.warn("Invalid auth-type [{}]", auth.getAuthType());
        }

        return null;

    }

    private ResponseEntity<String> execBasicRequest(String url, HttpMethod method, HttpHeaders httpHeaders, String req, Auth auth) {
        // execute request
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpClientFactoryUtil.setRequestFactory(restTemplate);
        } catch (KeyStoreException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (KeyManagementException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (auth != null && (auth.getAuthType() == AuthType.Basic || auth.getAuthType() == AuthType.BasicAuth || auth.getAuthType() == AuthType.BASIC)) {
            httpHeaders.set("Authorization", AuthBuilder.createBasicAuth(auth.getUsername(), auth.getPassword()));
        }

        //logger.info("Request: [{}]", req);
        HttpEntity<String> request = new HttpEntity<>(req, httpHeaders);

        ResponseEntity<String> response = null;
        int statusCode = -1;
        String responseBody = null;
        HttpHeaders headers = null;
        try {
            response = restTemplate.exchange(url, method, request, String.class);
            //statusCode = response.getStatusCodeValue();
            //responseBody = response.getBody();
            //headers = response.getHeaders();
        } catch (HttpStatusCodeException statusCodeException) {
            response = new ResponseEntity<String>(statusCodeException.getResponseBodyAsString(), statusCodeException.getResponseHeaders(), statusCodeException.getStatusCode());
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private ResponseEntity<String> execTokenRequest(String url, HttpMethod method, HttpHeaders httpHeaders, String req, Auth auth) {
        // execute request
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpClientFactoryUtil.setRequestFactory(restTemplate);
        } catch (KeyStoreException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (KeyManagementException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //logger.info("Request: [{}]", req);
        HttpEntity<String> request = new HttpEntity<>(req, httpHeaders);

        ResponseEntity<String> response = null;
        int statusCode = -1;
        String responseBody = null;
        HttpHeaders headers = null;
        try {
            response = restTemplate.exchange(url, method, request, String.class);

            if ( response.getHeaders() != null &&
                    response.getHeaders().getContentType() != null &&
                    response.getHeaders().getContentType().getCharset() != null &&
                    response.getHeaders().getContentType().getCharset().contains(Charset.forName("UTF-16"))) {

                String resp16 = new String(response.getBody().getBytes("UTF-16"), "UTF-8");
                resp16 = resp16.replaceAll(UNICODE_REPLACEMENT_CHARACTER, StringUtils.EMPTY);
                resp16 = resp16.replaceAll(Character.toString(Character.MIN_VALUE), StringUtils.EMPTY);
                response = new ResponseEntity<String>(resp16, response.getHeaders(), response.getStatusCode());

            }

            //statusCode = response.getStatusCodeValue();
            //responseBody = response.getBody();
            //headers = response.getHeaders();
        } catch (HttpStatusCodeException statusCodeException) {
            response = new ResponseEntity<String>(statusCodeException.getResponseBodyAsString(), statusCodeException.getResponseHeaders(), statusCodeException.getStatusCode());
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private ResponseEntity<String> execAuth0Request(String url, HttpMethod method, HttpHeaders httpHeaders, String req, Auth auth) {
        // execute request
        RestTemplate restTemplate = new RestTemplate();

        try {
            HttpClientFactoryUtil.setRequestFactory(restTemplate);
        } catch (KeyStoreException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (KeyManagementException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Get Access Token
        String accessToken = null;
        try {
            if (auth.getGrantType() == GrantType.client_credentials) {
                accessToken = auth0Service.getAccessTokenForClientCredentials(auth.getAccessTokenUri(), auth.getClientId(), auth.getClientSecret(), auth.getId());
            } else {
                accessToken = auth0Service.getAccessTokenForPassword(auth.getAccessTokenUri(), auth.getClientId(), auth.getId(), auth.getUsername(), auth.getPassword(), auth.getScope());
            }
        } catch (RuntimeException ex) {
            return new ResponseEntity<String>(ex.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (StringUtils.isEmpty(accessToken)) {
            return new ResponseEntity<String>("Error retrieving access token from Auth0", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Set Bearer
        httpHeaders.set("authorization", String.format("Bearer %s", accessToken));

        //logger.info("Request: [{}]", req);
        HttpEntity<String> request = new HttpEntity<>(req, httpHeaders);

        ResponseEntity<String> response = null;
        int statusCode = -1;
        String responseBody = null;
        HttpHeaders headers = null;
        try {
            response = restTemplate.exchange(url, method, request, String.class);
            //statusCode = response.getStatusCodeValue();
            //responseBody = response.getBody();
            //headers = response.getHeaders();
        } catch (HttpStatusCodeException statusCodeException) {
            response = new ResponseEntity<String>(statusCodeException.getResponseBodyAsString(), statusCodeException.getResponseHeaders(), statusCodeException.getStatusCode());
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private ResponseEntity<String> execOAuth2Request(String url, HttpMethod method, HttpHeaders httpHeaders, String req, Auth auth) {
        // execute request
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        DefaultOAuth2ClientContext clientContext = new DefaultOAuth2ClientContext(atr);

        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails(auth), clientContext);

        try {
            HttpClientFactoryUtil.setRequestFactory(restTemplate);
        } catch (KeyStoreException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NoSuchAlgorithmException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (KeyManagementException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //logger.info("Request: [{}]", req);
        HttpEntity<String> request = new HttpEntity<>(req, httpHeaders);

        ResponseEntity<String> response = null;
        int statusCode = -1;
        String responseBody = null;
        HttpHeaders headers = null;
        try {
            response = restTemplate.exchange(url, method, request, String.class);
            //statusCode = response.getStatusCodeValue();
            //responseBody = response.getBody();
            //headers = response.getHeaders();
        } catch (HttpStatusCodeException statusCodeException) {
            response = new ResponseEntity<String>(statusCodeException.getResponseBodyAsString(), statusCodeException.getResponseHeaders(), statusCodeException.getStatusCode());
        } catch (OAuth2AccessDeniedException e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage(), e);
            return new ResponseEntity<String>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response;
    }

    private OAuth2ProtectedResourceDetails resourceDetails(Auth auth) {

        BaseOAuth2ProtectedResourceDetails details1 = null;
        if (auth.getGrantType() == GrantType.authorization_code) {
            return getAbstractRedirectResourceDetails(auth);
        } else if (auth.getGrantType() == GrantType.implicit) {
            return getAbstractRedirectResourceDetails(auth);
        } else if (auth.getGrantType() == GrantType.client_credentials) {
            return getBaseOAuth2ProtectedResourceDetails(auth);
        } else if (auth.getGrantType() == GrantType.password) {
            return getResourceOwnerPasswordResourceDetails(auth);
        } else {
            logger.warn("Invalid grant-type [{}]", auth.getGrantType());
        }

        return null;
    }

    private OAuth2ProtectedResourceDetails getResourceOwnerPasswordResourceDetails(Auth auth) {

        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();


        details.setId(auth.getId());
        details.setClientId(auth.getClientId());
        details.setClientSecret(auth.getClientSecret());

        details.setUsername(auth.getUsername());
        details.setPassword(auth.getPassword());

        details.setAccessTokenUri(auth.getAccessTokenUri());

        details.setTokenName(auth.getTokenName());
        if (StringUtils.isNotEmpty(auth.getScope())) {
            details.setScope(Arrays.asList(auth.getScope().split(",")));
        }

        if (auth.getClientAuthenticationScheme() != null) {
            details.setClientAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getClientAuthenticationScheme().toString()));
        }

        if (auth.getAuthorizationScheme() != null) {
            details.setAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getAuthorizationScheme().toString()));
        }

        return details;
    }

    private OAuth2ProtectedResourceDetails getBaseOAuth2ProtectedResourceDetails(Auth auth) {

        BaseOAuth2ProtectedResourceDetails details = new BaseOAuth2ProtectedResourceDetails();


        details.setId(auth.getId());
        details.setClientId(auth.getClientId());
        details.setClientSecret(auth.getClientSecret());

        details.setAccessTokenUri(auth.getAccessTokenUri());

        details.setTokenName(auth.getTokenName());
        if (StringUtils.isNotEmpty(auth.getScope())) {
            details.setScope(Arrays.asList(auth.getScope().split(",")));
        }

        if (auth.getClientAuthenticationScheme() != null) {
            details.setClientAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getClientAuthenticationScheme().toString()));
        }

        if (auth.getAuthorizationScheme() != null) {
            details.setAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getAuthorizationScheme().toString()));
        }

        return details;
    }

    private OAuth2ProtectedResourceDetails getAbstractRedirectResourceDetails(Auth auth) {

        AbstractRedirectResourceDetails details = new ImplicitResourceDetails();


        details.setId(auth.getId());
        details.setClientId(auth.getClientId());
        details.setClientSecret(auth.getClientSecret());

        details.setAccessTokenUri(auth.getAccessTokenUri());

        details.setTokenName(auth.getTokenName());
        if (StringUtils.isNotEmpty(auth.getScope())) {
            details.setScope(Arrays.asList(auth.getScope().split(",")));
        }

        if (auth.getClientAuthenticationScheme() != null) {
            details.setClientAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getClientAuthenticationScheme().toString()));
        }

        if (auth.getAuthorizationScheme() != null) {
            details.setAuthenticationScheme(org.springframework.security.oauth2.common.AuthenticationScheme.valueOf(auth.getAuthorizationScheme().toString()));
        }

        details.setUserAuthorizationUri(auth.getUserAuthorizationUri());
        details.setPreEstablishedRedirectUri(auth.getPreEstablishedRedirectUri());

        details.setUseCurrentUri(false);

        return details;
    }

}
