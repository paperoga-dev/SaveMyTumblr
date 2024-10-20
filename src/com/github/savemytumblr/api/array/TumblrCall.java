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

import java.util.Map;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.api.AuthInterface;

public class TumblrCall<T, W extends ContentInterface<T>> extends com.github.savemytumblr.api.TumblrCall<W> {
    private final CompletionInterface<T, W> onCompletion;
    private final Api<T, W> api;

    protected TumblrCall(Executor executor, Logger logger, Api<T, W> api, Map<String, String> queryParams,
            AuthInterface authInterface, CompletionInterface<T, W> onCompletion) {
        super(executor, logger, api, queryParams, authInterface, onCompletion);

        this.api = api;
        this.onCompletion = onCompletion;
    }

    @Override
    protected void process(final W output) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                onCompletion.onSuccess(output.getItems(), api.getOffset(), api.getLimit(), output.getCount());
            }
        });
    }
}
