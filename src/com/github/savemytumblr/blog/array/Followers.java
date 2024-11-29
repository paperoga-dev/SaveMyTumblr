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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.api.array.Uuidable;

public interface Followers {
    class User implements Uuidable {
        private String name;
        private boolean following;
        private String url;
        private Date updated;
        private String uuid;

        User(JSONObject userObject) throws JSONException {
            super();

            this.name = userObject.getString("name");
            this.following = userObject.getBoolean("following");
            this.url = userObject.getString("url");
            this.updated = new Date(userObject.getInt("updated") * 1000L);
            this.uuid = UUID.randomUUID().toString();
        }

        public String getName() {
            return this.name;
        }

        public boolean isFollowing() {
            return this.following;
        }

        public String getUrl() {
            return this.url;
        }

        public Date getUpdated() {
            return this.updated;
        }

        @Override
        public String getUuid() {
            return this.uuid;
        }
    }

    class Data implements ContentInterface<User> {
        private int totalUsers;
        private List<User> users;

        Data(JSONObject followersObject) throws JSONException {
            super();

            this.totalUsers = followersObject.getInt("total_users");
            this.users = new ArrayList<>();

            JSONArray jsonUsers = followersObject.getJSONArray("users");
            for (int i = 0; i < jsonUsers.length(); ++i) {
                this.users.add(new User(jsonUsers.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return this.totalUsers;
        }

        @Override
        public List<User> getItems() {
            return this.users;
        }
    }

    class Api extends Id<User, Data> {

        public Api(Integer offset, Integer limit, String blogId) {
            super(offset, limit, blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/followers";
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
