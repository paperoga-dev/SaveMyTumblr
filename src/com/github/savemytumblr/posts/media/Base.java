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

        JSONArray media = mediaObject.getJSONArray("media");
        this.media = new ArrayList<>();
        for (int i = 0; i < media.length(); ++i) {
            this.media.add(new Media(media.getJSONObject(i)));
        }

        this.colors = new ArrayList<>();
        JSONObject colors = mediaObject.optJSONObject("colors");
        if (colors != null) {
            Iterator<String> it = colors.keys();
            while (it.hasNext()) {
                this.colors.add(Integer.valueOf(colors.getString(it.next()), 16));
            }
        }

        this.feedbackToken = mediaObject.optString("feedback_token", "");
        this.attribution = com.github.savemytumblr.posts.attribution.Base
                .doCreate(mediaObject.optJSONObject("attribution"));
        this.altText = mediaObject.optString("alt_text", "");
    }

    public List<Media> getMedia() {
        return media;
    }

    public List<Integer> getColors() {
        return colors;
    }

    public String getFeedbackToken() {
        return feedbackToken;
    }

    public com.github.savemytumblr.posts.attribution.Base getAttribution() {
        return attribution;
    }

    public String getAltText() {
        return altText;
    }

    public static ContentItem doCreate(JSONObject mediaObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Base(mediaObject);
    }

    @Override
    public String toHTML(String newRoot) {
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
                res += Paths.get(newRoot, Paths.get(new URI(sUrl).getPath()).getFileName().toString());
            } catch (URISyntaxException e) {
                res += "dead_link";
            }
        }

        return res + "\">";
    }
}
