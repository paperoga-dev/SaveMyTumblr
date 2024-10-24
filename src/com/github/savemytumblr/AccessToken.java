package com.github.savemytumblr;

import org.json.JSONObject;

public class AccessToken {
    private final String token;
    private final String refreshToken;

    public AccessToken(String sToken, String sRefreshToken) {
        this.token = sToken;
        this.refreshToken = sRefreshToken;
    }

    public JSONObject toJSON() {
        JSONObject root = new JSONObject();
        root.put("token", this.token);
        root.put("refreshToken", this.refreshToken);
        return root;
    }

    public String getToken() {
        return this.token;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    static public AccessToken fromJSON(JSONObject root) {
        return new AccessToken(root.getString("token"), root.getString("refreshToken"));
    }

    static public AccessToken fromJSON(String accessTokenJson) {
        return AccessToken.fromJSON(new JSONObject(accessTokenJson));
    }
}
