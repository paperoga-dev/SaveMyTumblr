package com.github.savemytumblr.blog.array;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.ContentInterface;
import com.github.savemytumblr.blog.simple.Info;

public interface Blocks {
    class Data implements ContentInterface<Info.Base> {
        private List<Info.Base> blogs;

        Data(JSONObject blocksObject) throws JSONException {
            super();

            this.blogs = new ArrayList<>();
            JSONArray jsonBlogs = blocksObject.getJSONArray("blocked_tumblelogs");
            for (int i = 0; i < jsonBlogs.length(); ++i) {
                this.blogs.add(new Info.Base(jsonBlogs.getJSONObject(i)));
            }
        }

        @Override
        public int getCount() {
            return -2;
        }

        @Override
        public List<Info.Base> getItems() {
            return this.blogs;
        }
    }

    class Api extends Id<Info.Base, Data> {

        public Api(Integer offset, Integer limit, String blogId) {
            super(offset, limit, blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/blocks";
        }

        @Override
        protected boolean requiresApiKey() {
            return false;
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            readLinks(jsonObject);
            return new Data(jsonObject);
        }
    }
}
