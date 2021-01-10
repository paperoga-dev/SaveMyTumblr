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

package com.github.savemytumblr.posts.text;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.ContentItem;

public abstract class Base extends ContentItem {
    private String text;
    private List<com.github.savemytumblr.posts.text.formatting.Base> formattingItems;
    private static int orderedListCounter = 0;

    private static final Map<String, Class<? extends com.github.savemytumblr.posts.text.formatting.Base>> formattingTypesMap = new HashMap<String, Class<? extends com.github.savemytumblr.posts.text.formatting.Base>>() {
        private static final long serialVersionUID = 1L;
        {
            put("bold", com.github.savemytumblr.posts.text.formatting.Bold.class);
            put("italic", com.github.savemytumblr.posts.text.formatting.Italic.class);
            put("strikethrough", com.github.savemytumblr.posts.text.formatting.Strikethrough.class);
            put("link", com.github.savemytumblr.posts.text.formatting.Link.class);
            put("mention", com.github.savemytumblr.posts.text.formatting.Mention.class);
            put("color", com.github.savemytumblr.posts.text.formatting.Color.class);
            put("small", com.github.savemytumblr.posts.text.formatting.Small.class);
        }
    };

    private static final Map<String, Class<? extends com.github.savemytumblr.posts.text.Base>> typesMap = new HashMap<String, Class<? extends com.github.savemytumblr.posts.text.Base>>() {
        private static final long serialVersionUID = 1L;
        {
            put("plain", com.github.savemytumblr.posts.text.Plain.class);
            put("heading1", com.github.savemytumblr.posts.text.Heading1.class);
            put("heading2", com.github.savemytumblr.posts.text.Heading2.class);
            put("quirky", com.github.savemytumblr.posts.text.Quirky.class);
            put("quote", com.github.savemytumblr.posts.text.Quote.class);
            put("indented", com.github.savemytumblr.posts.text.Indented.class);
            put("chat", com.github.savemytumblr.posts.text.Chat.class);
            put("ordered-list-item", com.github.savemytumblr.posts.text.OrderedListItem.class);
            put("unordered-list-item", com.github.savemytumblr.posts.text.UnorderedListItem.class);
        }
    };

    public Base(JSONObject textObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.text = textObject.getString("text");

        this.formattingItems = new ArrayList<>();
        JSONArray formattingItems = textObject.optJSONArray("formatting");
        if (formattingItems == null)
            return;

        for (int i = 0; i < formattingItems.length(); ++i) {
            JSONObject formattingItem = formattingItems.getJSONObject(i);
            String type = formattingItem.getString("type");
            try {
                this.formattingItems.add(formattingTypesMap.get(type).getDeclaredConstructor(JSONObject.class)
                        .newInstance(formattingItem));
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException
                    | IllegalAccessException e) {
                throw new com.github.savemytumblr.exception.RuntimeException("Add missing formatting type: " + type);
            }
        }
    }

    public static ContentItem doCreate(JSONObject textObject)
            throws com.github.savemytumblr.exception.RuntimeException {
        String subType = textObject.optString("subtype", "plain");

        try {
            if (subType.equalsIgnoreCase("ordered-list-item"))
                ++orderedListCounter;
            else
                orderedListCounter = 0;

            return typesMap.get(subType).getDeclaredConstructor(JSONObject.class).newInstance(textObject);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException e) {
            throw new com.github.savemytumblr.exception.RuntimeException("Add missing text subtype: " + subType);
        }
    }

    public List<com.github.savemytumblr.posts.text.formatting.Base> getFormattingItems() {
        return formattingItems;
    }

    public String getText() {
        return text;
    }

    public String getFormattedText() {
        String src = getText();
        String res = "";

        List<com.github.savemytumblr.posts.text.formatting.Base> pItems = new ArrayList<com.github.savemytumblr.posts.text.formatting.Base>();

        for (int i = 0; i < src.length(); ++i) {

            for (com.github.savemytumblr.posts.text.formatting.Base fItem : getFormattingItems()) {
                if (fItem.getStart() == i) {
                    pItems.add(fItem);
                    res += fItem.getStartHTMLTag();
                }

                if (fItem.getEnd() == i) {
                    pItems.remove(fItem);
                    res += fItem.getEndHTMLTag();
                }
            }

            res += src.charAt(i);
        }

        for (int i = pItems.size() - 1; i >= 0; --i) {
            res += pItems.get(i).getEndHTMLTag();
        }

        return res;
    }

    static protected int getOrderedListCounter() {
        return orderedListCounter;
    }
}
