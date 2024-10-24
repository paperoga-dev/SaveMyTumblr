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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.blog.simple.Info;

public interface Following {
    class Data implements ContentInterface<Info.Base> {
        private List<Info.Base> blogs;
        private int totalBlogs;

        Data(JSONObject followingObject) throws JSONException {
            super();

            this.totalBlogs = followingObject.getInt("total_blogs");

            this.blogs = new ArrayList<>();
            JSONArray jsonBlogs = followingObject.getJSONArray("blogs");
            for (int i = 0; i < jsonBlogs.length(); ++i) {
                this.blogs.add(new Info.Base(jsonBlogs.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return this.totalBlogs;
        }

        @Override
        public List<Info.Base> getItems() {
            return this.blogs;
        }
    }

    class Api extends Id<Info.Base, Data> {

        public Api(Integer offset, Integer limit, String blogId) {
            super(offset, limit, blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/following";
        }

        @Override
        protected boolean requiresApiKey() {
            return false;
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            return new Data(jsonObject);
        }
    }
}
