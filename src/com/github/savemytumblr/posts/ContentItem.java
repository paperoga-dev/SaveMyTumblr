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

package com.github.savemytumblr.posts;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ContentItem {

    private static final Map<String, Class<? extends ContentItem>> typesMap = new HashMap<String, Class<? extends ContentItem>>() {
        private static final long serialVersionUID = 1L;
        {
            put("text", com.github.savemytumblr.posts.text.Base.class);
            put("image", com.github.savemytumblr.posts.media.Base.class);
            put("link", com.github.savemytumblr.posts.link.Base.class);
            put("audio", com.github.savemytumblr.posts.audio.Base.class);
            put("video", com.github.savemytumblr.posts.video.Base.class);
        }
    };

    static public <T> T allocateOrNothing(Class<T> clazz, JSONObject jsonObject, String key)
            throws com.github.savemytumblr.exception.RuntimeException {
        try {
            JSONObject object = jsonObject.optJSONObject(key);
            return (object != null) ? clazz.getDeclaredConstructor(JSONObject.class).newInstance(object) : null;
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException
                | InstantiationException e) {
            e.printStackTrace();

            throw new com.github.savemytumblr.exception.RuntimeException(
                    clazz.getName() + "has no construction with a JSONObject argument");
        }
    }

    static public ContentItem create(JSONObject contentItem)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        String type = contentItem.getString("type");
        try {
            return (ContentItem) typesMap.get(type).getMethod("doCreate", JSONObject.class).invoke(null, contentItem);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();

            throw new com.github.savemytumblr.exception.RuntimeException("Add missing type: " + type);
        }
    }

    public abstract String toHTML(String newRoot, String id);
}
