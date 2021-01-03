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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.posts.layout.Ask;
import com.github.savemytumblr.posts.layout.Rows;

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
            return id;
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
            JSONArray content = postObject.getJSONArray("content");
            for (int i = 0; i < content.length(); ++i) {
                this.content.add(ContentItem.create(content.getJSONObject(i)));
            }

            this.layout = new ArrayList<>();
            JSONArray layout = postObject.optJSONArray("layout");
            if (layout != null) {
                for (int i = 0; i < layout.length(); ++i) {
                    this.layout.add(LayoutItem.create(layout.getJSONObject(i)));
                }
            }

            this.trail = new ArrayList<>();
            JSONArray trail = postObject.optJSONArray("trail");
            if (trail != null) {
                for (int i = 0; i < trail.length(); ++i) {
                    String brokenBlogName = trail.getJSONObject(i).optString("broken_blog_name", null);
                    if (brokenBlogName != null) {
                        this.trail.add(new Trail(trail.getJSONObject(i), brokenBlogName));
                    } else {
                        this.trail.add(new Trail(trail.getJSONObject(i), trail.getJSONObject(i).getJSONObject("post")));
                    }
                }
            }

            this.askingName = postObject.optString("asking_name");
            this.askingUrl = postObject.optString("asking_url");
            this.isPinned = postObject.optBoolean("is_pinned", false);
        }

        public Info.Base getBlog() {
            return blog;
        }

        public List<ContentItem> getContent() {
            return content;
        }

        public List<LayoutItem> getLayout() {
            return layout;
        }

        public List<Trail> getTrail() {
            return trail;
        }

        public String getAskingName() {
            return askingName;
        }

        public String getAskingUrl() {
            return askingUrl;
        }

        public boolean isPinned() {
            return isPinned;
        }

        public List<List<Integer>> getBlocksLayout() {
            SortedSet<Integer> indexes = new TreeSet<>();

            for (int i = 0; i < getContent().size(); ++i)
                indexes.add(i);

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
    }

    class Item extends Trail {
        private Date timestamp;
        private List<String> tags;
        private String url;
        private String shortUrl;
        private String json;

        public Item(JSONObject postObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super(postObject, postObject);

            json = postObject.toString(2);
            this.timestamp = new Date(postObject.getLong("timestamp") * 1000L);
            this.url = postObject.getString("post_url");
            this.shortUrl = postObject.getString("short_url");

            this.tags = new ArrayList<>();
            JSONArray tags = postObject.optJSONArray("tags");
            if (tags != null) {
                for (int i = 0; i < tags.length(); ++i) {
                    this.tags.add(tags.getString(i));
                }
            }
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public List<String> getTags() {
            return tags;
        }

        public String getUrl() {
            return url;
        }

        public String getShortUrl() {
            return shortUrl;
        }

        public String getJSON() {
            return json;
        }
    }

    class Data implements ContentInterface<Item> {
        private int totalPosts;
        private List<Item> posts;

        public Data(JSONObject postsObject) throws JSONException, com.github.savemytumblr.exception.RuntimeException {
            super();

            this.totalPosts = postsObject.getInt("total_posts");
            this.posts = new ArrayList<>();

            JSONArray posts = postsObject.getJSONArray("posts");
            for (int i = 0; i < posts.length(); ++i) {
                this.posts.add(new Item(posts.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return totalPosts;
        }

        @Override
        public List<Item> getItems() {
            return posts;
        }
    }
}
