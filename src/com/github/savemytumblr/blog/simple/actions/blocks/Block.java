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

package com.github.savemytumblr.blog.simple.actions.blocks;

import java.util.Map;

import com.github.savemytumblr.TumblrClient.Executor;
import com.github.savemytumblr.TumblrClient.Logger;
import com.github.savemytumblr.api.AuthInterface;
import com.github.savemytumblr.api.actions.CompletionInterface;
import com.github.savemytumblr.blog.simple.actions.Action;

public abstract class Block extends Action {

    public Block(String blogId) {
        super(blogId);
    }

    @Override
    protected String getPath() {
        return super.getPath() + "/blocks";
    }

    @Override
    public Runnable call(Executor executor, Logger logger, Map<String, String> queryParams, AuthInterface authInterface,
            CompletionInterface onCompletion) {
        // TODO Auto-generated method stub
        return null;
    }
}
