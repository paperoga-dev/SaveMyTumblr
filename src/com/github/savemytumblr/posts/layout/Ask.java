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

package com.github.savemytumblr.posts.layout;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.posts.LayoutItem;
import com.github.savemytumblr.posts.attribution.Base;

public class Ask extends LayoutItem {
    private final List<Integer> blocks;
    private Base attribution;

    public Ask(JSONObject layoutObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super();

        this.blocks = new ArrayList<>();

        JSONArray jsonBlocks = layoutObject.getJSONArray("blocks");
        for (int i = 0; i < jsonBlocks.length(); ++i) {
            this.blocks.add(jsonBlocks.getInt(i));
        }

        JSONObject attributionObject = layoutObject.optJSONObject("attribution");
        this.attribution = null;
        if (attributionObject == null) {
            return;
        }

        this.attribution = Base.doCreate(attributionObject);
    }

    public List<Integer> getBlocks() {
        return this.blocks;
    }

    public Base getAttribution() {
        return this.attribution;
    }

    public static LayoutItem doCreate(JSONObject layoutObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        return new Ask(layoutObject);
    }
}
