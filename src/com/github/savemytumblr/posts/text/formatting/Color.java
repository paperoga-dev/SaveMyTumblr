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

package com.github.savemytumblr.posts.text.formatting;

import org.json.JSONException;
import org.json.JSONObject;

public class Color extends Base {
    private int color;

    public Color(JSONObject formattingObject) throws JSONException {
        super(formattingObject);

        this.color = Integer.parseUnsignedInt(formattingObject.getString("hex").substring(1), 16);
    }

    public int getColor() {
        return this.color;
    }

    @Override
    public String getStartHTMLTag() {
        return "<font style=\"color: #" + Integer.toHexString(getColor()) + ";\">";
    }

    @Override
    public String getEndHTMLTag() {
        return "</font>";
    }
}
