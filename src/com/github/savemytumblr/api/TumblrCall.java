/*
 * SaveMyTumblr
 * Copyright (C) 2020

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.savemytumblr.api;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.AccessToken;
import com.github.savemytumblr.Constants;
import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.exception.JsonException;
import com.github.savemytumblr.exception.NetworkException;
import com.github.savemytumblr.exception.ResponseException;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class TumblrCall<T> implements Runnable {
    private final AbstractCompletionInterface<T> onCompletion;
    private final AuthInterface authInterface;
    private final Api<T> api;
    private final Map<String, String> queryParams;
    private final Executor executor;
    private final Logger logger;

    protected TumblrCall(Executor executor, Logger logger, Api<T> api, Map<String, String> queryParams,
            AuthInterface authInterface, AbstractCompletionInterface<T> onCompletion) {
        super();

        this.api = api;
        this.onCompletion = onCompletion;
        this.queryParams = queryParams;
        this.executor = executor;
        this.logger = logger;
        this.authInterface = authInterface;
    }

    protected abstract void process(final T output);

    protected Executor getExecutor() {
        return executor;
    }

    @Override
    public void run() {
        if (onCompletion == null) {
            return;
        }

        String jsonResponse = null;
        final TumblrCall<T> me = this;

        try {
            final HttpRequest.Builder requestBuilder = this.api.setupCall(queryParams);
            requestBuilder.setHeader("User-Agent", authInterface.getUserAgent());
            requestBuilder.setHeader("Authorization", "Bearer " + authInterface.getAccessToken().token);

            final HttpRequest request = requestBuilder.build();

            logger.info("Request: " + request.toString());

            final HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());

            logger.info("Response: " + String.valueOf(response.statusCode()));

            switch (response.statusCode()) {
                case 200:
                case 201:
                    final JSONObject rootObj = new JSONObject(response.body());

                    logger.info("Response body: " + rootObj.toString(2));

                    process(api.readData(rootObj.getJSONObject("response")));
                    break;

                case 401:
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Dotenv dotenv = Dotenv.load();
                            final HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(Constants.API_ENDPOINT + "/oauth2/token"))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("User-Agent", authInterface.getUserAgent())
                                    .POST(BodyPublishers.ofString("grant_type="
                                            + URLEncoder.encode("refresh_token", StandardCharsets.UTF_8) + "&client_id="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_KEY"), StandardCharsets.UTF_8)
                                            + "&client_secret="
                                            + URLEncoder.encode(dotenv.get("CONSUMER_SECRET"), StandardCharsets.UTF_8)
                                            + "&refresh_token="
                                            + URLEncoder.encode(authInterface.getAccessToken().refreshToken,
                                                    StandardCharsets.UTF_8)))
                                    .build();

                            HttpResponse<String> response;
                            try {
                                response = HttpClient.newHttpClient().send(request,
                                        HttpResponse.BodyHandlers.ofString());
                                switch (response.statusCode()) {
                                    case 200:
                                        JSONObject rootObj = new JSONObject(response.body());
                                        authInterface.onUpdateToken(new AccessToken(rootObj.getString("access_token"),
                                                rootObj.getString("refresh_token")));
                                        executor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                me.run();
                                            }
                                        });
                                        break;

                                    default:
                                        authInterface.onClearToken();
                                        onCompletion.onFailure(new NetworkException(new Exception("Unauthenticated")));
                                        break;
                                }

                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });
                    break;

                default:
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            onCompletion.onFailure(new ResponseException(response.statusCode(), "Hey"));
                        }
                    });
                    break;
            }
        } catch (final JSONException e) {
            e.printStackTrace();

            final String jsonData = jsonResponse;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onFailure(new JsonException(e, jsonData));
                }
            });
        } catch (final com.github.savemytumblr.exception.RuntimeException e) {
            e.printStackTrace();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onFailure(e);
                }
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
