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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.ContentItem;

public class Base extends ContentItem {
    private List<Media> media;
    private List<Integer> colors;
    private String feedbackToken;
    private com.github.savemytumblr.posts.attribution.Base attribution;
    private String altText;

    public Base(JSONObject mediaObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        JSONArray jsonMedia = mediaObject.getJSONArray("media");
        this.media = new ArrayList<>();
        for (int i = 0; i < jsonMedia.length(); ++i) {
            this.media.add(new Media(jsonMedia.getJSONObject(i)));
        }

        this.colors = new ArrayList<>();
        JSONObject jsonColors = mediaObject.optJSONObject("colors");
        if (jsonColors != null) {
            Iterator<String> it = jsonColors.keys();
            while (it.hasNext()) {
                this.colors.add(Integer.valueOf(jsonColors.getString(it.next()), 16));
            }
        }

        this.feedbackToken = mediaObject.optString("feedback_token", "");
        this.attribution = com.github.savemytumblr.posts.attribution.Base
                .doCreate(mediaObject.optJSONObject("attribution"));
        this.altText = mediaObject.optString("alt_text", "");
    }

    public List<Media> getMedia() {
        return this.media;
    }

    public List<Integer> getColors() {
        return this.colors;
    }

    public String getFeedbackToken() {
        return this.feedbackToken;
    }

    public com.github.savemytumblr.posts.attribution.Base getAttribution() {
        return this.attribution;
    }

    public String getAltText() {
        return this.altText;
    }

    public static ContentItem doCreate(JSONObject mediaObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Base(mediaObject);
    }

    @Override
    public String toHTML(String newRoot, String id) {
        int maxWidth = getMedia().get(0).getWidth();
        String sUrl = getMedia().get(0).getUrl();

        for (int i = 1; i < getMedia().size(); ++i) {
            com.github.savemytumblr.posts.media.Media mediaItem = getMedia().get(i);

            if (mediaItem.getWidth() > maxWidth) {
                maxWidth = mediaItem.getWidth();
                sUrl = mediaItem.getUrl();
            }
        }

        String res = "<img style=\"max-width: 100%; height: auto;\" src=\"";

        if (newRoot == null) {
            res += sUrl;
        } else {
            try {
                res += Paths.get(id, Paths.get(new URI(sUrl).getPath()).getFileName().toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();

                res += "dead_link";
            }
        }

        return res + "\">";
    }
}
