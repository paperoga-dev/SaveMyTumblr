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

package com.github.savemytumblr.user.array;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.posts.Post;

public interface Dashboard {

    class Data implements ContentInterface<Post.Item> {
        private final List<Post.Item> posts;

        Data(JSONObject postsObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super();

            this.posts = new ArrayList<>();

            JSONArray jsonPosts = postsObject.getJSONArray("posts");
            for (int i = 0; i < jsonPosts.length(); ++i) {
                this.posts.add(new Post.Item(jsonPosts.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return -1;
        }

        @Override
        public List<Post.Item> getItems() {
            return this.posts;
        }
    }

    class Api extends com.github.savemytumblr.api.array.Api<Post.Item, Data> {

        public Api(Integer offset, Integer limit) {
            super(offset, limit);
        }

        @Override
        protected String getPath() {
            return "/user/dashboard";
        }

        @Override
        protected Map<String, String> defaultParams() {
            Map<String, String> m = super.defaultParams();

            m.put("npf", "true");
            m.put("reblog_info", "true");
            m.put("notes_info", "true");

            return m;
        }

        @Override
        protected boolean requiresApiKey() {
            return false;
        }

        @Override
        protected Data readData(JSONObject jsonObject)
                throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            return new Data(jsonObject);
        }
    }
}