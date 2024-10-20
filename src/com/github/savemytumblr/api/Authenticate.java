package com.github.savemytumblr.api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.json.JSONObject;

import com.github.savemytumblr.AccessToken;
import com.github.savemytumblr.Constants;
import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.Url;

import io.github.cdimascio.dotenv.Dotenv;

public class Authenticate {
    public interface OnAuthenticationListener {
        void onAuthenticationRequest(String authenticationUrl, String state);

        void onAuthenticationGranted(AccessToken accessToken);

        void onFailure(Exception e);
    }

    private static class AuthorizationRequestTask implements Runnable {
        private final Executor executor;
        private final OnAuthenticationListener onAuthenticationListener;

        AuthorizationRequestTask(Executor executor, OnAuthenticationListener onAuthenticationListener) {
            super();

            this.executor = executor;
            this.onAuthenticationListener = onAuthenticationListener;
        }

        @Override
        public void run() {
            if (onAuthenticationListener == null) {
                return;
            }

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    final String state = UUID.randomUUID().toString();
                    Dotenv dotenv = Dotenv.load();
                    Url url = new Url("https://www.tumblr.com", "/oauth2/authorize");
                    url.addParameter("client_id", dotenv.get("CONSUMER_KEY"));
                    url.addParameter("response_type", "code");
                    url.addParameter("scope", "write offline_access");
                    url.addParameter("state", state);
                    url.addParameter("redirect_uri", Constants.CALLBACK_URL);
                    onAuthenticationListener.onAuthenticationRequest(url.toString(), state);
                }
            });
        }
    }

    private static class GetAccessTokenTask implements Runnable {
        private final Executor executor;
        private final String authCode;
        private final OnAuthenticationListener onAuthenticationListener;

        GetAccessTokenTask(Executor executor, String authCode, OnAuthenticationListener onAuthenticationListener) {
            super();

            this.executor = executor;
            this.authCode = authCode;
            this.onAuthenticationListener = onAuthenticationListener;
        }

        @Override
        public void run() {
            if (onAuthenticationListener == null) {
                return;
            }

            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Dotenv dotenv = Dotenv.load();
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(Constants.API_ENDPOINT + "/oauth2/token"))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(BodyPublishers.ofString("grant_type="
                                            + URLEncoder.encode("authorization_code", StandardCharsets.UTF_8) + "&code="
                                            + URLEncoder.encode(authCode, StandardCharsets.UTF_8) + "&client_id="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_KEY"), StandardCharsets.UTF_8)
                                            + "&client_secret="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_SECRET"), StandardCharsets.UTF_8)
                                            + "&redirect_uri="
                                            + URLEncoder.encode(Constants.CALLBACK_URL, StandardCharsets.UTF_8)))
                                    .build();

                            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                                    HttpResponse.BodyHandlers.ofString());
                            AccessToken accessToken = null;
                            if (response.statusCode() == 200) {
                                JSONObject rootObj = new JSONObject(response.body());
                                accessToken = new AccessToken(rootObj.getString("access_token"),
                                        rootObj.getString("refresh_token"));
                            }

                            if (accessToken != null) {
                                onAuthenticationListener.onAuthenticationGranted(accessToken);
                            } else {
                                onAuthenticationListener.onFailure(new Exception("Hey"));
                            }
                        } catch (Exception e) {
                            onAuthenticationListener.onFailure(new Exception("Hey"));
                            e.printStackTrace();
                        }
                    }
                });

            } catch (

            final Exception e) {
                e.printStackTrace();

                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        onAuthenticationListener.onFailure(e);
                    }
                });
            }
        }
    }

    public class LogOutputStream extends OutputStream {
        private String mem;

        public LogOutputStream(Logger logger) {
            super();

            mem = "";
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) (b & 0xff);
            if (c == '\n') {
                flush();
            } else {
                mem += c;
            }
        }

        @Override
        public void flush() {
            logger.info(mem);
            mem = "";
        }
    }

    private OnAuthenticationListener onAuthenticationListener;
    private final Executor executor;
    private final Logger logger;

    public Authenticate(Executor executor, Logger logger) {
        super();

        this.executor = executor;
        this.logger = logger;
        this.onAuthenticationListener = null;
    }

    public void request() {
        if (onAuthenticationListener == null) {
            return;
        }

        executor.execute(new AuthorizationRequestTask(executor, onAuthenticationListener));
    }

    public void getAccessToken(String authCode) {

        executor.execute(new GetAccessTokenTask(executor, authCode, onAuthenticationListener));
    }

    public void setOnAuthenticationListener(OnAuthenticationListener onAuthenticationListener) {
        this.onAuthenticationListener = onAuthenticationListener;
    }
}
