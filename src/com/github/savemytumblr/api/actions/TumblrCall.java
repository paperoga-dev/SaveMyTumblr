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

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.api.AbstractCompletionInterface;
import com.github.savemytumblr.api.AuthInterface;

public class TumblrCall extends com.github.savemytumblr.TumblrCall {
    private final Api api;
    private final Map<String, String> queryParams;

    protected TumblrCall(Executor cExecutor, Logger iLogger, Api cApi, Map<String, String> mQueryParams,
            AuthInterface iAuthInterface, AbstractCompletionInterface iOnCompletion) {
        super(cExecutor, iLogger, iAuthInterface, iOnCompletion);

        this.api = cApi;
        this.queryParams = mQueryParams;
    }

    @Override
    protected HttpRequest.Builder doSetupCall() {
        return this.api.setupCall(this.queryParams);
    }

    @Override
    protected void doProcessResponse(HttpResponse<String> response) {
        // nothing to do here, HTTP status code is already processed
    }
}
