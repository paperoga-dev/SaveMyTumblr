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

public class Link extends Base {
    private String url;

    public Link(JSONObject formattingObject) throws JSONException {
        super(formattingObject);

        this.url = formattingObject.getString("url");
    }

    public String getUrl() {
        return this.url;
    }

    @Override
    public String getStartHTMLTag() {
        return "<a href=\"" + getUrl() + "\" target=\"_blank\">";
    }

    @Override
    public String getEndHTMLTag() {
        return "</a>";
    }
}
