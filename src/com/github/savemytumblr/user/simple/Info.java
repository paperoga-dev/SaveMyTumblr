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

package com.github.savemytumblr.user.simple;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public interface Info {
    enum PostFormat {
        Html, MarkDown, Raw
    }

    class Data {
        private String name; // String - The user's tumblr short name
        private int likes; // Number - The total count of the user's likes
        private int following; // Number - The number of blogs the user is following
        private PostFormat defaultPostFormat; // String - The default posting format - html, markdown, or raw
        private List<com.github.savemytumblr.blog.simple.Info.Data> blogs; // Array - Each item is a blog the user has
                                                                           // permissions to post to

        Data(JSONObject userObject) throws JSONException {
            this.name = userObject.getString("name");
            this.likes = userObject.getInt("likes");
            this.following = userObject.getInt("following");

            String postFormat = userObject.getString("default_post_format");
            if (postFormat.equalsIgnoreCase("html")) {
                this.defaultPostFormat = PostFormat.Html;
            } else if (postFormat.equalsIgnoreCase("markdown")) {
                this.defaultPostFormat = PostFormat.MarkDown;
            } else {
                this.defaultPostFormat = PostFormat.Raw;
            }

            JSONArray jsonBlogs = userObject.getJSONArray("blogs");
            this.blogs = new ArrayList<>();
            for (int i = 0; i < jsonBlogs.length(); ++i) {
                this.blogs.add(new com.github.savemytumblr.blog.simple.Info.Data(jsonBlogs.getJSONObject(i)));
            }
        }

        public String getName() {
            return this.name;
        }

        public int getLikes() {
            return this.likes;
        }

        public int getFollowing() {
            return this.following;
        }

        public PostFormat getDefaultPostFormat() {
            return this.defaultPostFormat;
        }

        public List<com.github.savemytumblr.blog.simple.Info.Data> getBlogs() {
            return this.blogs;
        }
    }

    class Api extends com.github.savemytumblr.api.simple.Api<Data> {

        /*
         * "response": { "user": { "name": "paperogacoibentato", "likes": 8300,
         * "following": 175, "default_post_format": "html", "blogs": [ => View
         * BlogInfo.Data ] } }
         */

        @Override
        protected String getPath() {
            return "/user/info";
        }

        @Override
        protected boolean requiresApiKey() {
            return false;
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            return new Data(jsonObject.getJSONObject("user"));
        }
    }
}
