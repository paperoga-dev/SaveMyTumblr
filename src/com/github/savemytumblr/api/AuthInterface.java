package com.github.savemytumblr.api;

import com.github.savemytumblr.AccessToken;

public interface AuthInterface {
    AccessToken getAccessToken();

    String getUserAgent();

    void onUpdateToken(AccessToken accessToken);

    void onClearToken();
}
