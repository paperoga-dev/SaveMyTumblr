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

package com.github.savemytumblr.posts.attribution;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.blog.simple.Info;

public class Post extends Base {
    private com.github.savemytumblr.posts.Post.Base post;
    private Info.Reference blog;

    public Post(JSONObject attributionObject) throws JSONException {
        super(attributionObject);

        this.post = new com.github.savemytumblr.posts.Post.Base(attributionObject.getJSONObject("post"));
        this.blog = new Info.Reference(attributionObject.getJSONObject("blog"));
    }

    public com.github.savemytumblr.posts.Post.Base getPost() {
        return this.post;
    }

    public Info.Reference getBlog() {
        return this.blog;
    }

    public static Base doCreate(JSONObject attributionObject) throws JSONException {
        return new Post(attributionObject);
    }
}
