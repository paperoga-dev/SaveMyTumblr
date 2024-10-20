package com.github.savemytumblr;

import org.json.JSONObject;

public class AccessToken {
    public final String token;
    public final String refreshToken;

    public AccessToken(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public JSONObject toJSON() {
        JSONObject root = new JSONObject();
        root.put("token", token);
        root.put("refreshToken", refreshToken);
        return root;
    }

    static public AccessToken fromJSON(JSONObject root) {
        return new AccessToken(root.getString("token"), root.getString("refreshToken"));
    }

    static public AccessToken fromJSON(String accessTokenJson) {
        return AccessToken.fromJSON(new JSONObject(accessTokenJson));
    }
}
