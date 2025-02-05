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

package com.github.savemytumblr.posts.link;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.ContentItem;
import com.github.savemytumblr.posts.media.Media;

public class Base extends ContentItem {
    private String url;
    private String title;
    private String description;
    private String author;
    private String siteName;
    private String displayUrl;
    private Media poster;

    public Base(JSONObject linkObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.url = linkObject.getString("url");
        this.title = linkObject.optString("title", "");
        this.description = linkObject.optString("description", "");
        this.author = linkObject.optString("author", "");
        this.siteName = linkObject.optString("site_name", "");
        this.displayUrl = linkObject.optString("display_url", "");
        this.poster = allocateOrNothing(Media.class, linkObject, "poster");
    }

    public static ContentItem doCreate(JSONObject linkObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Base(linkObject);
    }

    public String getUrl() {
        return this.url;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public String getDisplayUrl() {
        return this.displayUrl;
    }

    public Media getPoster() {
        return this.poster;
    }

    @Override
    public String toHTML(String newRoot, String id) {
        return "<a href=\"" + getUrl() + "\" target=\"_blank\">" + getTitle() + "</a>";
    }
}
