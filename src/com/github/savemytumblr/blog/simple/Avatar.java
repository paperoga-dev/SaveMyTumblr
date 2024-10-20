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

import org.json.JSONException;
import org.json.JSONObject;

public interface Avatar {
    class Data {
        /*
         * { "width": 512, "height": 512 "url":
         * "https://66.media.tumblr.com/avatar_ed354109bd89_512.png" }
         */

        private int width; // Number - Avatar width
        private int height; // Number - Avatar height
        private String url; // String - Avatar url

        Data(int size, String url) {
            this.width = size;
            this.height = size;
            this.url = url;
        }

        public Data(JSONObject avatarObject) throws JSONException {
            this.width = avatarObject.getInt("width");
            this.height = avatarObject.getInt("height");
            this.url = avatarObject.getString("url");
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getUrl() {
            return url;
        }
    }

    class Api extends Id<Data> {

        public Api(String blogId) {
            super(blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/avatar";
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            return new Data(64, jsonObject.getString("avatar_url"));
        }
    }
}
