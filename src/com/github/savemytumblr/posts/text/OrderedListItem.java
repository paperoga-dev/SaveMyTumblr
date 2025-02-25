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

import org.json.JSONException;
import org.json.JSONObject;

public class OrderedListItem extends Base {
    private int orderNumber;

    public OrderedListItem(JSONObject textObject)
            throws JSONException, com.github.savemytumblr.exception.RuntimeException {
        super(textObject);

        this.orderNumber = Base.getOrderedListCounter();
    }

    public int getOrderNumber() {
        return this.orderNumber;
    }

    @Override
    public String toHTML(String newRoot, String id) {
        return "<li>" + getFormattedText() + "</li>";
    }
}
