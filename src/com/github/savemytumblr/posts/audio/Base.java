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

package com.github.savemytumblr.posts.audio;

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
    private final String title;
    private final String artist;
    private final String album;
    private final Media poster;
    private final String embedHtml;
    private final String embedUrl;

    // TODO: metadata

    private com.github.savemytumblr.posts.attribution.Base attribution;

    public Base(JSONObject audioObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.url = audioObject.optString("url", "");
        this.media = allocateOrNothing(Media.class, audioObject, "media");
        this.provider = audioObject.optString("provider", "");
        this.title = audioObject.optString("title", "");
        this.artist = audioObject.optString("artist", "");
        this.album = audioObject.optString("album", "");
        this.poster = allocateOrNothing(Media.class, audioObject, "poster");
        this.embedHtml = audioObject.optString("embed_html", "");
        this.embedUrl = audioObject.optString("embed_url", "");
        this.attribution = com.github.savemytumblr.posts.attribution.Base
                .doCreate(audioObject.optJSONObject("attribution"));
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

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public Media getPoster() {
        return poster;
    }

    public String getEmbedHtml() {
        return embedHtml;
    }

    public String getEmbedUrl() {
        return embedUrl;
    }

    public com.github.savemytumblr.posts.attribution.Base getAttribution() {
        return attribution;
    }

    public static ContentItem doCreate(JSONObject audioObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Base(audioObject);
    }

    @Override
    public String toHTML(String newRoot, String id) {
        try {
            String sUrl = (getMedia() != null) ? getMedia().getUrl() : getUrl();
            String fName = "";
            Path audioPath = null;
            if (!sUrl.isEmpty()) {
                fName = Paths.get(new URI(sUrl).getPath().split("\\?")[0]).getFileName().toString();
                audioPath = Paths.get(newRoot, id, fName);
            }

            if ((audioPath != null) && audioPath.toFile().exists()) {
                return "<audio controls><source src=\"" + Paths.get(id, fName)
                        + "\">Your browser does not support the audio tag.</audio>";
            } else if (!getEmbedHtml().isEmpty()) {
                return getEmbedHtml();
            } else {
                return "<a href=\"" + getUrl() + "\" target=\"_blank\">Audio link</a>";
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();

            return "dead_link";
        }
    }
}
