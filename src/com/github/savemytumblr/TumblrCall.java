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

package com.github.savemytumblr;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.api.AbstractCompletionInterface;
import com.github.savemytumblr.api.AuthInterface;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.exception.JsonException;
import com.github.savemytumblr.exception.NetworkException;
import com.github.savemytumblr.exception.ResponseException;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class TumblrCall implements Runnable {
    private final AbstractCompletionInterface onCompletion;
    private final AuthInterface authInterface;
    private final Executor executor;
    private final Logger logger;

    protected TumblrCall(Executor cExecutor, Logger iLogger, AuthInterface iAuthInterface,
            AbstractCompletionInterface iOnCompletion) {
        super();

        this.onCompletion = iOnCompletion;
        this.executor = cExecutor;
        this.logger = iLogger;
        this.authInterface = iAuthInterface;
    }

    protected Executor getExecutor() {
        return this.executor;
    }

    protected Logger getLogger() {
        return this.logger;
    }

    protected abstract HttpRequest.Builder doSetupCall();

    protected abstract void doProcessResponse(HttpResponse<String> response);

    protected void reportFailure(BaseException exception) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                TumblrCall.this.onCompletion.onFailure(exception);
            }
        });
    }

    @Override
    public void run() {
        if (this.onCompletion == null) {
            return;
        }

        String jsonResponse = null;
        final TumblrCall me = this;

        try (HttpClient client = HttpClient.newHttpClient()) {
            final HttpRequest.Builder requestBuilder = this.doSetupCall();
            requestBuilder.setHeader("User-Agent", this.authInterface.getUserAgent());
            requestBuilder.setHeader("Authorization", "Bearer " + this.authInterface.getAccessToken().getToken());

            final HttpRequest request = requestBuilder.build();

            this.logger.info("Request: " + request.toString());

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            this.logger.info("Response: " + String.valueOf(response.statusCode()));

            switch (response.statusCode()) {
                case 200:
                case 201:
                    Thread.sleep(Duration.ofMillis(1000));
                    doProcessResponse(response);
                    break;

                case 401:
                    this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Dotenv dotenv = Dotenv.load();

                            Url url = new Url("", "");
                            url.addParameter("grant_type", "refresh_token");
                            url.addParameter("client_id", dotenv.get("CONSUMER_KEY"));
                            url.addParameter("client_secret", dotenv.get("CONSUMER_SECRET"));
                            url.addParameter("refresh_token",
                                    TumblrCall.this.authInterface.getAccessToken().getRefreshToken());

                            final HttpRequest inRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(Constants.API_ENDPOINT + "/oauth2/token"))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("User-Agent", TumblrCall.this.authInterface.getUserAgent())
                                    .POST(BodyPublishers.ofString(url.toString())).build();

                            try (HttpClient inClient = HttpClient.newHttpClient()) {
                                HttpResponse<String> inResponse = inClient.send(inRequest,
                                        HttpResponse.BodyHandlers.ofString());
                                switch (inResponse.statusCode()) {
                                    case 200:
                                        JSONObject inRootObj = new JSONObject(inResponse.body());
                                        TumblrCall.this.authInterface
                                                .onUpdateToken(new AccessToken(inRootObj.getString("access_token"),
                                                        inRootObj.getString("refresh_token")));
                                        TumblrCall.this.executor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                me.run();
                                            }
                                        });
                                        break;

                                    default:
                                        TumblrCall.this.authInterface.onClearToken();
                                        TumblrCall.this.onCompletion
                                                .onFailure(new NetworkException(new Exception("Unauthenticated")));
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
                    this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            TumblrCall.this.onCompletion.onFailure(new ResponseException(response.statusCode(), "Hey"));
                        }
                    });
                    break;
            }
        } catch (final JSONException e) {
            e.printStackTrace();

            final String jsonData = jsonResponse;

            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    TumblrCall.this.onCompletion.onFailure(new JsonException(e, jsonData));
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
