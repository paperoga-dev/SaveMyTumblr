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

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.json.JSONObject;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;

public abstract class TumblrCall<T> extends com.github.savemytumblr.TumblrCall {
    private final Api<T> api;
    private final Map<String, String> queryParams;

    protected TumblrCall(Executor cExecutor, Logger iLogger, Api<T> cApi, Map<String, String> mQueryParams,
            AuthInterface iAuthInterface, AbstractCompletionInterface iOnCompletion) {
        super(cExecutor, iLogger, iAuthInterface, iOnCompletion);

        this.api = cApi;
        this.queryParams = mQueryParams;
    }

    protected abstract void process(final T output);

    @Override
    protected HttpRequest.Builder doSetupCall() {
        return this.api.setupCall(this.queryParams);
    }

    @Override
    protected void doProcessResponse(HttpResponse<String> response) {
        try {
            final JSONObject rootObj = new JSONObject(response.body());

            this.getLogger().info("Response body: " + rootObj.toString(2));

            process(this.api.readData(rootObj.getJSONObject("response")));
        } catch (final com.github.savemytumblr.exception.RuntimeException e) {
            e.printStackTrace();
            this.reportFailure(e);
        }
    }
}
