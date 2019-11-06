package com.fxlabs.fxt.bot.processor;

public interface Auth0Service {

    public String getAccessTokenForClientCredentials(String tokenURI, String clientId, String clientSecret, String audience);

    public String getAccessTokenForPassword(String tokenURI, String clientId, String audience, String username, String password, String scope);
}
