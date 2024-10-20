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

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.Constants;

import io.github.cdimascio.dotenv.Dotenv;
import io.mikael.urlbuilder.UrlBuilder;

public abstract class Api<T> {

    protected boolean requiresApiKey() {
        return true;
    }

    protected abstract String getPath();

    protected Map<String, String> defaultParams() {
        Map<String, String> m = new HashMap<>();

        if (requiresApiKey()) {
            Dotenv dotenv = Dotenv.load();
            m.put("api_key", dotenv.get("CONSUMER_KEY"));
        }

        return m;
    }

    protected abstract T readData(JSONObject jsonObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException;

    protected Builder setupCall(Map<String, ?> queryParams) {
        UrlBuilder url = UrlBuilder.fromString(Constants.API_ENDPOINT + getPath());

        for (Map.Entry<String, ?> entry : defaultParams().entrySet()) {
            url = url.addParameter(entry.getKey(), entry.getValue().toString());
        }

        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            url = url.addParameter(entry.getKey(), entry.getValue().toString());
        }

        String urlString = url.toString();

        return HttpRequest.newBuilder(URI.create(urlString));
    }
}
