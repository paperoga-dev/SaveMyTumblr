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

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.exception.JsonException;
import com.github.savemytumblr.exception.NetworkException;
import com.github.savemytumblr.exception.ResponseException;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;

public abstract class TumblrCall<T> implements Runnable {
    private final AbstractCompletionInterface<T> onCompletion;
    private final Api<T> api;
    private final OAuthRequest request;
    private final Executor executor;
    private final Logger logger;
    
    protected TumblrCall(Executor executor, Logger logger, Api<T> api, OAuthRequest request, AbstractCompletionInterface<T> onCompletion) {
        super();

        this.api = api;
        this.request = request;
        this.onCompletion = onCompletion;
        this.executor = executor;
        this.logger = logger;
    }

    protected abstract void process(final T output);
    
    protected Executor getExecutor() {
    	return executor;
    }

    @Override
    public void run() {
        if (onCompletion == null)
            return;

        logger.info("Request: " + request.toString());
        logger.info("Query Params: " + request.getQueryStringParams().asFormUrlEncodedString());
        logger.info("Headers: " + request.getHeaders().toString());

        String jsonResponse = null;

        try {
            jsonResponse = request.send().getBody();
            logger.info("JSON Response: " + jsonResponse);

            JSONObject rootObj = new JSONObject(jsonResponse);

            JSONObject metaObj = rootObj.getJSONObject("meta");
            final int responseCode = metaObj.getInt("status");
            final String responseMessage = metaObj.getString("msg");

            switch (responseCode) {
                case 200:
                case 201:
                    process(api.readData(rootObj.getJSONObject("response")));
                    break;

                default:
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            onCompletion.onFailure(
                                    new ResponseException(
                                            responseCode,
                                            responseMessage
                                    )
                            );
                        }
                    });
                    break;
            }
        } catch (final JSONException e) {
            final String jsonData = jsonResponse;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onFailure(new JsonException(e, jsonData));
                }
            });
        } catch (final OAuthException e) {
        	executor.execute(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onFailure(new NetworkException(e));
                }
            });
        } catch (final com.github.savemytumblr.exception.RuntimeException e) {
        	executor.execute(new Runnable() {
                @Override
                public void run() {
                    onCompletion.onFailure(e);
                }
            });
        }
    }
}
