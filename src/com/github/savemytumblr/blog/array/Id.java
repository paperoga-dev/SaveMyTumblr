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

package com.github.savemytumblr.blog.array;

import com.github.savemytumblr.api.array.Api;
import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.api.array.Uuidable;

public abstract class Id<T extends Uuidable, W extends ContentInterface<T>> extends Api<T, W>
        implements ApiInterface<T, W> {
    private final String blogId;

    protected Id(Integer offset, Integer limit, String sBlogId) {
        super(offset, limit);

        this.blogId = sBlogId;
    }

    @Override
    protected String getPath() {
        /*
         * blog-identifier String Any blog identifier
         */

        return "/blog/" + getBlogId();
    }

    @Override
    public String getBlogId() {
        return this.blogId;
    }
}
