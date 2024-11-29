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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.api.array.Uuidable;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.posts.layout.Ask;
import com.github.savemytumblr.posts.layout.Rows;
import com.github.savemytumblr.posts.text.OrderedListItem;
import com.github.savemytumblr.posts.text.UnorderedListItem;

public interface Post {

    class Base {
        private long id;

        public Base() {
            super();

            this.id = 0;
        }

        public Base(JSONObject idObject) throws JSONException {
            super();

            this.id = idObject.getLong("id");
        }

        public long getId() {
            return this.id;
        }
    }

    class Trail extends Base {
        private Info.Base blog;
        private List<ContentItem> content;
        private List<LayoutItem> layout;
        private List<Trail> trail;
        private String askingName;
        private String askingUrl;
        private boolean isPinned;

        public Trail(JSONObject postObject, String brokenBlogName)
                throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super();

            this.blog = new Info.Base(brokenBlogName);

            loadContent(postObject);
        }

        public Trail(JSONObject postObject, JSONObject idObject)
                throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super(idObject);

            this.blog = new Info.Base(postObject.getJSONObject("blog"));

            loadContent(postObject);
        }

        private void loadContent(JSONObject postObject)
                throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            this.content = new ArrayList<>();
            JSONArray jsonContent = postObject.getJSONArray("content");
            for (int i = 0; i < jsonContent.length(); ++i) {
                ContentItem newItem = ContentItem.create(jsonContent.getJSONObject(i));
                if (newItem != null) {
                    this.content.add(newItem);
                }
            }

            this.layout = new ArrayList<>();
            JSONArray jsonLayout = postObject.optJSONArray("layout");
            if (jsonLayout != null) {
                for (int i = 0; i < jsonLayout.length(); ++i) {
                    this.layout.add(LayoutItem.create(jsonLayout.getJSONObject(i)));
                }
            }

            this.trail = new ArrayList<>();
            JSONArray jsonTrail = postObject.optJSONArray("trail");
            if (jsonTrail != null) {
                for (int i = 0; i < jsonTrail.length(); ++i) {
                    String brokenBlogName = jsonTrail.getJSONObject(i).optString("broken_blog_name", null);
                    if (brokenBlogName != null) {
                        this.trail.add(new Trail(jsonTrail.getJSONObject(i), brokenBlogName));
                    } else {
                        this.trail.add(new Trail(jsonTrail.getJSONObject(i),
                                jsonTrail.getJSONObject(i).getJSONObject("post")));
                    }
                }
            }

            this.askingName = postObject.optString("asking_name");
            this.askingUrl = postObject.optString("asking_url");
            this.isPinned = postObject.optBoolean("is_pinned", false);
        }

        public Info.Base getBlog() {
            return this.blog;
        }

        public List<ContentItem> getContent() {
            return this.content;
        }

        public List<LayoutItem> getLayout() {
            return this.layout;
        }

        public List<Trail> getTrail() {
            return this.trail;
        }

        public String getAskingName() {
            return this.askingName;
        }

        public String getAskingUrl() {
            return this.askingUrl;
        }

        public boolean isPinned() {
            return this.isPinned;
        }

        public List<List<Integer>> getBlocksLayout() {
            SortedSet<Integer> indexes = new TreeSet<>();

            for (int i = 0; i < getContent().size(); ++i) {
                indexes.add(i);
            }

            ArrayList<List<Integer>> list = new ArrayList<>();

            for (int i = 0; i < getLayout().size(); ++i) {
                if (getLayout().get(i) instanceof Rows) {
                    Rows rows = (Rows) getLayout().get(i);

                    for (Rows.Blocks blocks : rows.getBlocksList()) {
                        ArrayList<Integer> innerBlock = new ArrayList<>();

                        for (Integer index : blocks.getIndexes()) {
                            innerBlock.add(index);
                            indexes.remove(index);
                        }

                        list.add(innerBlock);
                    }
                } else if (getLayout().get(i) instanceof Ask) {
                    ArrayList<Integer> askBlock = new ArrayList<>();
                    askBlock.add(-1);
                    list.add(askBlock);
                    indexes.remove(i);
                }
            }

            for (Integer index : indexes) {
                ArrayList<Integer> innerBlock = new ArrayList<>();
                innerBlock.add(index);
                list.add(innerBlock);
            }

            return list;
        }

        public String toHTML(String newRoot, String id) {
            boolean isOrdered = false;
            boolean isUnordered = false;
            String res = "";

            for (Trail parentPost : getTrail()) {
                res += parentPost.toHTML(newRoot, id) + "<hr>";
            }

            res += "<b>" + getBlog().getName() + "</b><br><br>";

            Set<Integer> askPos = new HashSet<>();
            for (LayoutItem element : getLayout()) {
                if (element instanceof Ask) {
                    askPos.addAll(((Ask) element).getBlocks());
                }
            }

            boolean inAsk = false;
            boolean askNameWritten = false;

            for (int i = 0; i < getContent().size(); ++i) {
                ContentItem item = getContent().get(i);

                if (askPos.contains(i)) {
                    if (!inAsk) {
                        res += "<div style=\"background-color: lightgray;\">";
                        inAsk = true;
                    }
                    if (!askNameWritten) {
                        res += "<b>" + getAskingName() + "</b> chiede:";
                        askNameWritten = true;
                    }
                } else {
                    if (inAsk) {
                        res += "</div>";
                        inAsk = false;
                    }
                }

                if (item instanceof OrderedListItem) {
                    if (!isOrdered) {
                        res += "<ol>";
                        isOrdered = true;
                    }
                } else if (item instanceof UnorderedListItem) {
                    if (!isUnordered) {
                        res += "<ul>";
                        isUnordered = true;
                    }
                } else {
                    if (isOrdered) {
                        res += "</ol>";
                        isOrdered = false;
                    }

                    if (isUnordered) {
                        res += "</ul>";
                        isUnordered = false;
                    }
                }

                if (!isOrdered && !isUnordered) {
                    res += "<p>";
                }

                res += item.toHTML(newRoot, id);

                if (!isOrdered && !isUnordered) {
                    res += "</p>";
                }
            }

            return res;
        }
    }

    class Item extends Trail implements Uuidable {
        private Date timestamp;
        private List<String> tags;
        private String url;
        private String shortUrl;
        private String summary;
        private String json;
        private String uuid;

        public Item(JSONObject postObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super(postObject, postObject);

            this.json = postObject.toString(2);
            this.timestamp = new Date(postObject.getLong("timestamp") * 1000L);
            this.url = postObject.getString("post_url");
            this.shortUrl = postObject.getString("short_url");
            this.summary = postObject.optString("summary", "");
            this.uuid = UUID.randomUUID().toString();

            this.tags = new ArrayList<>();
            JSONArray jsonTags = postObject.optJSONArray("tags");
            if (jsonTags != null) {
                for (int i = 0; i < jsonTags.length(); ++i) {
                    this.tags.add(jsonTags.getString(i));
                }
            }
        }

        public Date getTimestamp() {
            return this.timestamp;
        }

        public List<String> getTags() {
            return this.tags;
        }

        public String getUrl() {
            return this.url;
        }

        public String getShortUrl() {
            return this.shortUrl;
        }

        public String getSummary() {
            return this.summary;
        }

        public String getJSON() {
            return this.json;
        }

        @Override
        public String toHTML(String newRoot, String id) {
            String res = "<html><head><meta charset=\"UTF-8\"><title>" + getSummary()
                    + "</title></head><body style=\"font-family: Arial, sans-serif;\">" + super.toHTML(newRoot, id)
                    + "<small><p>" + getTimestamp().toString() + "</p>";

            if (!getTags().isEmpty()) {
                res += "<p># " + String.join("<br># ", getTags()) + "</p>";
            }

            res += "</small><br><a href=\"" + getUrl() + "\" target=\"_blank\">Tumblr link</a></body></html>";

            return res;
        }

        @Override
        public String getUuid() {
            return this.uuid;
        }
    }

    class Data implements ContentInterface<Item> {
        private int totalPosts;
        private List<Item> posts;

        public Data(JSONObject postsObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super();

            this.totalPosts = postsObject.getInt("total_posts");
            this.posts = new ArrayList<>();

            JSONArray jsonPosts = postsObject.getJSONArray("posts");
            for (int i = 0; i < jsonPosts.length(); ++i) {
                this.posts.add(new Item(jsonPosts.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return this.totalPosts;
        }

        @Override
        public List<Item> getItems() {
            return this.posts;
        }
    }
}
