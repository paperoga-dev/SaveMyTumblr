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

package com.github.savemytumblr.posts.video;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.ContentItem;
import com.github.savemytumblr.posts.media.Media;

public class Base extends ContentItem {
    private final String url;
    private final Media media;
    private final String provider;
    private final String embedHtml;
    private final EmbedIframe embedIframe;
    private final String embedUrl;
    private final Media poster;

    // TODO: metadata

    private com.github.savemytumblr.posts.attribution.Base attribution;
    private boolean canAutoPlayOnCellular;

    public Base(JSONObject videoObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.url = videoObject.optString("url", "");
        this.media = allocateOrNothing(Media.class, videoObject, "media");
        this.provider = videoObject.optString("provider", "");
        this.embedHtml = videoObject.optString("embed_html", "");
        this.embedIframe = allocateOrNothing(EmbedIframe.class, videoObject, "embed_iframe");
        this.embedUrl = videoObject.optString("embed_url", "");
        this.poster = allocateOrNothing(Media.class, videoObject, "poster");
        this.attribution = com.github.savemytumblr.posts.attribution.Base
                .doCreate(videoObject.optJSONObject("attribution"));
    }

    public String getUrl() {
        return url;
    }

    public Media getMedia() {
        return media;
    }

    public String getProvider() {
        return provider;
    }

    public String getEmbedHtml() {
        return embedHtml;
    }

    public EmbedIframe getEmbedIframe() {
        return embedIframe;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public Media getPoster() {
        return poster;
    }

    public com.github.savemytumblr.posts.attribution.Base getAttribution() {
        return attribution;
    }

    public boolean canAutoPlayOnCellular() {
        return canAutoPlayOnCellular;
    }

    public static ContentItem doCreate(JSONObject videoObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Base(videoObject);
    }

    @Override
    public String toHTML(String newRoot, String id) {
        try {
            String sUrl = (getMedia() != null) ? getMedia().getUrl() : getUrl();
            String fName = "";
            Path videoPath = null;
            if (!sUrl.isEmpty()) {
                fName = Paths.get(new URI(sUrl).getPath().split("\\?")[0]).getFileName().toString();
                videoPath = Paths.get(newRoot, id, fName);
            }

            if ((videoPath != null) && videoPath.toFile().exists()) {
                return "<video controls><source src=\"" + Paths.get(id, fName)
                        + "\">Your browser does not support the video tag.</video>";
            } else if (!getEmbedHtml().isEmpty()) {
                return getEmbedHtml();
            } else {
                return "<a href=\"" + getUrl() + "\" target=\"_blank\">Video link</a>";
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();

            return "dead_link";
        }
    }
}
