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

package com.github.savemytumblr.posts.media;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.ContentItem;

public class Media {
    private String url;
    private String mimeType;
    private boolean originalDimensionsMissing;
    private boolean cropped;
    private boolean hasOriginalDimensions;
    private int width;
    private int height;
    private Media poster;

    public Media(JSONObject mediaObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.url = mediaObject.getString("url");
        this.mimeType = mediaObject.optString("type", "");
        this.originalDimensionsMissing = mediaObject.optBoolean("original_dimensions_missing", false);
        this.cropped = mediaObject.optBoolean("cropped", false);
        this.hasOriginalDimensions = mediaObject.optBoolean("has_original_dimensions", false);
        this.width = mediaObject.optInt("width", 0);
        this.height = mediaObject.optInt("height", 0);
        this.poster = ContentItem.allocateOrNothing(Media.class, mediaObject, "poster");
    }

    public String getUrl() {
        return this.url;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public boolean areOriginalDimensionsMissing() {
        return this.originalDimensionsMissing;
    }

    public boolean isCropped() {
        return this.cropped;
    }

    public boolean hasOriginalDimensions() {
        return this.hasOriginalDimensions;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Media getPoster() {
        return this.poster;
    }
}
