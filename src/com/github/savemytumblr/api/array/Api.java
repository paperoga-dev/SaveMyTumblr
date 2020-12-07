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

package com.github.savemytumblr.api.array;

import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;

import java.util.List;
import java.util.Map;

public abstract class Api<T, W extends ContentInterface<T>> extends com.github.savemytumblr.api.Api<W> implements ApiInterface<T, W> {
    private final Integer offset;
    private final Integer limit;

    protected Api(OAuthService service,
                  Token authToken,
                  String appId,
                  String appVersion,
                  Integer offset,
                  Integer limit) {
        super(service, authToken, appId, appVersion);

        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public Runnable call(
    		Executor executor,
    		Logger logger, 
            List<T> container,
            Map<String, String> queryParams,
            int offset,
            int limit,
            CompletionInterface<T, W> onCompletion) {
        /*
        limit            Number  The number of results to return: 1–20, inclusive  default: 20
        offset           Number  Result to start at                                default: 0
        */

        queryParams.put("limit", String.valueOf(limit));
        queryParams.put("offset", String.valueOf(offset));

        return new TumblrCall<>(executor, logger, this, setupCall(queryParams), onCompletion);
    }
}
