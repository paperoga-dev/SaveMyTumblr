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

import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.api.array.Uuidable;

public interface Likes {
    class Post implements Uuidable {
        final String uuid;

        public Post() {
            this.uuid = UUID.randomUUID().toString();
        }

        @Override
        public String getUuid() {
            return this.uuid;
        }
    }

    class Data implements ContentInterface<Post> {
        private List<Post> likedPosts;
        private int likedCount;

        Data(JSONObject likesObject) throws JSONException {
            super();

            // TODO: Post
            // this.likedPosts = ...;
            this.likedCount = likesObject.getInt("liked_count");
        }

        @Override
        public List<Post> getItems() {
            return this.likedPosts;
        }

        @Override
        public int getCount() {
            return this.likedCount;
        }
    }

    class Api extends Id<Post, Data> {

        public Api(Integer offset, Integer limit, String blogId) {
            super(offset, limit, blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/likes";
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            return new Data(jsonObject);
        }
    }
}
