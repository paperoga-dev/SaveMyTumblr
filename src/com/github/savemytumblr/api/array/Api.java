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

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.api.AuthInterface;

public abstract class Api<T extends Uuidable, W extends ContentInterface<T>> extends com.github.savemytumblr.api.Api<W>
        implements ApiInterface<T, W> {
    private final Integer offset;
    private final Integer limit;
    private Integer tumblrNextOffset;

    protected Api(Integer iOffset, Integer iLimit) {
        this.offset = iOffset;
        this.limit = iLimit;
        this.tumblrNextOffset = -1;
    }

    @Override
    public Integer getLimit() {
        return this.limit;
    }

    @Override
    public Integer getOffset() {
        return this.offset;
    }

    @Override
    public Integer getTumblrNextOffset() {
        return this.tumblrNextOffset;
    }

    protected void readLinks(JSONObject jsonObject) {
        try {
            this.tumblrNextOffset = jsonObject.getJSONObject("_links").getJSONObject("next")
                    .getJSONObject("query_params").getInt("offset");
        } catch (@SuppressWarnings("unused") JSONException e) {
            this.tumblrNextOffset = -1;
        }
    }

    @Override
    public Runnable call(Executor executor, Logger logger, List<T> container, Map<String, String> queryParams,
            AuthInterface authInterface, Integer iOffset, Integer iLimit, CompletionInterface<T, W> onCompletion) {
        /*
         * limit Number The number of results to return: 1–20, inclusive default: 20
         * offset Number Result to start at default: 0
         */

        queryParams.put("limit", String.valueOf(iLimit));
        queryParams.put("offset", String.valueOf(iOffset));

        return new TumblrCall<>(executor, logger, this, queryParams, authInterface, onCompletion);
    }
}
