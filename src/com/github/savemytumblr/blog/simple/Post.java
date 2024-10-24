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

package com.github.savemytumblr.blog.simple;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.exception.RuntimeException;

public interface Post {
    class Api extends Id<com.github.savemytumblr.posts.Post.Item> {
        private final String postId;

        public Api(String sBlogId, String sPostId) {
            super(sBlogId);

            this.postId = sPostId;
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/posts/" + this.postId;
        }

        @Override
        protected Map<String, String> defaultParams() {
            Map<String, String> m = super.defaultParams();

            m.put("npf", "true");

            return m;
        }

        @Override
        protected com.github.savemytumblr.posts.Post.Item readData(JSONObject jsonObject)
                throws JSONException, RuntimeException {
            return new com.github.savemytumblr.posts.Post.Item(jsonObject);
        }
    }
}
