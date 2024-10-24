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

        AuthorizationRequestTask(Executor cExecutor, OnAuthenticationListener iOnAuthenticationListener) {
            super();

            this.executor = cExecutor;
            this.onAuthenticationListener = iOnAuthenticationListener;
        }

        @Override
        public void run() {
            if (this.onAuthenticationListener == null) {
                return;
            }

            this.executor.execute(new Runnable() {
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
                    AuthorizationRequestTask.this.onAuthenticationListener.onAuthenticationRequest(url.toString(),
                            state);
                }
            });
        }
    }

    private static class GetAccessTokenTask implements Runnable {
        private final Executor executor;
        private final String authCode;
        private final OnAuthenticationListener onAuthenticationListener;

        GetAccessTokenTask(Executor cExecutor, String sAuthCode, OnAuthenticationListener iOnAuthenticationListener) {
            super();

            this.executor = cExecutor;
            this.authCode = sAuthCode;
            this.onAuthenticationListener = iOnAuthenticationListener;
        }

        @Override
        public void run() {
            if (this.onAuthenticationListener == null) {
                return;
            }

            try {
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Dotenv dotenv = Dotenv.load();
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(Constants.API_ENDPOINT + "/oauth2/token"))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .POST(BodyPublishers.ofString("grant_type="
                                            + URLEncoder.encode("authorization_code", StandardCharsets.UTF_8) + "&code="
                                            + URLEncoder.encode(GetAccessTokenTask.this.authCode,
                                                    StandardCharsets.UTF_8)
                                            + "&client_id="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_KEY"), StandardCharsets.UTF_8)
                                            + "&client_secret="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_SECRET"), StandardCharsets.UTF_8)
                                            + "&redirect_uri="
                                            + URLEncoder.encode(Constants.CALLBACK_URL, StandardCharsets.UTF_8)))
                                    .build();

                            try (HttpClient client = HttpClient.newHttpClient()) {
                                HttpResponse<String> response = client.send(request,
                                        HttpResponse.BodyHandlers.ofString());
                                AccessToken accessToken = null;
                                if (response.statusCode() == 200) {
                                    JSONObject rootObj = new JSONObject(response.body());
                                    accessToken = new AccessToken(rootObj.getString("access_token"),
                                            rootObj.getString("refresh_token"));
                                }

                                if (accessToken != null) {
                                    GetAccessTokenTask.this.onAuthenticationListener
                                            .onAuthenticationGranted(accessToken);
                                } else {
                                    GetAccessTokenTask.this.onAuthenticationListener.onFailure(new Exception("Hey"));
                                }
                            }
                        } catch (Exception e) {
                            GetAccessTokenTask.this.onAuthenticationListener.onFailure(new Exception("Hey"));
                            e.printStackTrace();
                        }
                    }
                });

            } catch (

            final Exception e) {
                e.printStackTrace();

                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        GetAccessTokenTask.this.onAuthenticationListener.onFailure(e);
                    }
                });
            }
        }
    }

    public class LogOutputStream extends OutputStream {
        private String mem;

        public LogOutputStream() {
            super();

            this.mem = "";
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) (b & 0xff);
            if (c == '\n') {
                flush();
            } else {
                this.mem += c;
            }
        }

        @Override
        public void flush() {
            Authenticate.this.logger.info(this.mem);
            this.mem = "";
        }
    }

    private OnAuthenticationListener onAuthenticationListener;
    private final Executor executor;
    private final Logger logger;

    public Authenticate(Executor cExecutor, Logger iLogger) {
        super();

        this.executor = cExecutor;
        this.logger = iLogger;
        this.onAuthenticationListener = null;
    }

    public void request() {
        if (this.onAuthenticationListener == null) {
            return;
        }

        this.executor.execute(new AuthorizationRequestTask(this.executor, this.onAuthenticationListener));
    }

    public void getAccessToken(String authCode) {

        this.executor.execute(new GetAccessTokenTask(this.executor, authCode, this.onAuthenticationListener));
    }

    public void setOnAuthenticationListener(OnAuthenticationListener iOnAuthenticationListener) {
        this.onAuthenticationListener = iOnAuthenticationListener;
    }
}
