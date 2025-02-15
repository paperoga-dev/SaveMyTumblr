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

package com.github.savemytumblr.api.actions;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Map;

import com.github.savemytumblr.Constants;
import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.Url;
import com.github.savemytumblr.api.AuthInterface;

import io.github.cdimascio.dotenv.Dotenv;

public abstract class Api implements ApiInterface {

    public enum Method {
        POST, DELETE
    }

    @SuppressWarnings("static-method")
    protected boolean requiresApiKey() {
        return true;
    }

    protected abstract String getPath();

    protected abstract Method getMethod();

    protected Map<String, String> defaultParams() {
        Map<String, String> m = new HashMap<>();

        if (requiresApiKey()) {
            Dotenv dotenv = Dotenv.load();
            m.put("api_key", dotenv.get("CONSUMER_KEY"));
        }

        return m;
    }

    protected HttpRequest.Builder setupCall(Map<String, ?> queryParams) {
        Url url = new Url(Constants.API_ENDPOINT, getPath());

        Url queryUrl = new Url("", "");
        for (Map.Entry<String, ?> entry : defaultParams().entrySet()) {
            queryUrl.addParameter(entry.getKey(), entry.getValue().toString());
        }

        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            queryUrl.addParameter(entry.getKey(), entry.getValue().toString());
        }

        return HttpRequest.newBuilder().uri(URI.create(url.toString()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method(this.getMethod().toString(), BodyPublishers.ofString(queryUrl.toString()));
    }

    @Override
    public Runnable call(Executor executor, Logger logger, Map<String, String> queryParams, AuthInterface authInterface,
            CompletionInterface onCompletion) {
        return new TumblrCall(executor, logger, this, queryParams, authInterface, onCompletion);
    }
}
