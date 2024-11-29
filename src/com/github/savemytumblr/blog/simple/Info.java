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

package com.github.savemytumblr.blog.simple;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.Uuidable;

public interface Info {

    class Reference implements Uuidable {
        private String name; // String - Blog name
        private String url; // String - Blog URL
        private String uuid; // String - Blog UUID

        public Reference(String brokenBlogName) {
            super();

            this.name = brokenBlogName;
            this.url = "";
            this.uuid = "";
        }

        public Reference(JSONObject blogObject) throws JSONException {
            super();

            this.name = blogObject.getString("name");
            this.url = blogObject.getString("url");
            this.uuid = blogObject.getString("uuid");
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }

        @Override
        public String getUuid() {
            return this.uuid;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj.getClass() != this.getClass()) {
                return false;
            }

            return this.getUuid().equals(((Reference) obj).getUuid());
        }

        @Override
        public int hashCode() {
            return this.getUuid().hashCode();
        }
    }

    class Base extends Reference {
        private String description; // String - Blog description
        private String title; // String - Blog title
        private Date updated; // Number - Last updated time (epoch)

        public Base(String brokenBlogName) {
            super(brokenBlogName);

            this.description = "";
            this.title = "";
            this.updated = new Date();
        }

        public Base(JSONObject blogObject) throws JSONException {
            super(blogObject);

            this.description = blogObject.getString("description");
            this.title = blogObject.getString("title");
            this.updated = new Date(blogObject.getInt("updated") * 1000L);
        }

        public String getDescription() {
            return this.description;
        }

        public String getTitle() {
            return this.title;
        }

        public Date getUpdated() {
            return this.updated;
        }
    }

    class SubmissionTerms {
        public enum AcceptedTypes {
            Text, Photo, Quote, Link, Video
        }

        private Set<AcceptedTypes> acceptedTypes;
        private Set<String> tags;
        private String title;
        private String guidelines;

        /*
         * "submission_terms": { "accepted_types": [ "text", "photo", "quote", "link",
         * "video" ], "tags":[ ], "title": "Ecco, bravo, damme na' mano", "guidelines":
         * "" },
         */

        SubmissionTerms(JSONObject jsonSubmissionTermsObject) throws JSONException {
            JSONArray jsonAcceptedTypes = jsonSubmissionTermsObject.getJSONArray("accepted_types");

            this.acceptedTypes = new HashSet<>();
            for (int i = 0; i < jsonAcceptedTypes.length(); ++i) {
                if (jsonAcceptedTypes.getString(i).equalsIgnoreCase("text")) {
                    this.acceptedTypes.add(AcceptedTypes.Text);
                } else if (jsonAcceptedTypes.getString(i).equalsIgnoreCase("photo")) {
                    this.acceptedTypes.add(AcceptedTypes.Photo);
                } else if (jsonAcceptedTypes.getString(i).equalsIgnoreCase("quote")) {
                    this.acceptedTypes.add(AcceptedTypes.Quote);
                } else if (jsonAcceptedTypes.getString(i).equalsIgnoreCase("link")) {
                    this.acceptedTypes.add(AcceptedTypes.Link);
                } else if (jsonAcceptedTypes.getString(i).equalsIgnoreCase("video")) {
                    this.acceptedTypes.add(AcceptedTypes.Video);
                }
            }

            JSONArray jsonTags = jsonSubmissionTermsObject.getJSONArray("tags");
            this.tags = new HashSet<>();
            for (int i = 0; i < jsonTags.length(); ++i) {
                this.tags.add(jsonTags.getString(i));
            }

            this.title = jsonSubmissionTermsObject.getString("title");
            this.guidelines = jsonSubmissionTermsObject.getString("guidelines");
        }

        public Set<AcceptedTypes> getAcceptedTypes() {
            return this.acceptedTypes;
        }

        public Set<String> getTags() {
            return this.tags;
        }

        public String getTitle() {
            return this.title;
        }

        public String getGuidelines() {
            return this.guidelines;
        }
    }

    class Data extends Base {
        enum Type {
            Public, Private
        }

        enum Tweet {
            Auto, Yes, No
        }

        private boolean admin; // Boolean - is admin
        private boolean ask; // Boolean - Indicates whether the blog allows questions
        private boolean askAnon; // Boolean - Indicates whether the blog allows anonymous questions; returned
                                 // only if ask is true
        private String askPageTitle; // String - Ask page title
        private List<Avatar.Data> avatars; // Array - List of available avatars
        private boolean canChat; // Boolean - Allows chat
        private boolean canSendFanMail; // Boolean - ????
        private boolean canSubmit; // Boolean - Allows submissions
        private boolean canSubscribe; // Boolean - ????
        private int drafts; // Number - Drafts count
        private boolean facebook; // Boolean - Is to Facebook linked
        private boolean facebookOpengraphEnabled; // Boolean String - ????? (Y/N)
        private boolean followed; // Boolean - ?????
        private int followers; // Number - Followers count
        private boolean isBlockedFromPrimary; // Boolean - ????
        private boolean isNSFW; // Boolean - NSFW blog
        private int messages; // Number - Messages count;
        private int posts; // Number - Posts count
        private boolean primary; // Boolean - Is a primary blog
        private int queue; // Number - Queued posts count
        private boolean shareLikes; // Boolean - ?????
        private String submissionPageTitle; // String - Submission page title
        private SubmissionTerms submissionTerms; // Submission Object => see above
        private boolean subscribed; // Boolean - ?????
        // There is a theme object here, we skip it, it's useless for our purposes
        private int totalPosts; // Number - Posts count
        private Tweet tweet; // String - indicate if posts are tweeted auto, Y, N
        private boolean twitterEnabled; // Boolean - ?????
        private boolean twitterSend; // Boolean - ????
        private Type type; // String - indicates whether a blog is public or private

        public Data(JSONObject blogObject) throws JSONException {
            super(blogObject);

            this.admin = blogObject.optBoolean("admin", false);
            this.ask = blogObject.getBoolean("ask");
            this.askAnon = blogObject.getBoolean("ask_anon");
            this.askPageTitle = blogObject.getString("ask_page_title");

            JSONArray jsonAvatars = blogObject.getJSONArray("avatar");

            this.avatars = new ArrayList<>();
            for (int i = 0; i < jsonAvatars.length(); ++i) {
                this.avatars.add(new Avatar.Data(jsonAvatars.getJSONObject(i)));
            }

            this.canChat = blogObject.optBoolean("can_chat", false);
            this.canSendFanMail = blogObject.optBoolean("can_send_fan_mail", false);
            this.canSubmit = blogObject.optBoolean("can_submit", false);
            this.canSubscribe = blogObject.optBoolean("can_subscribe", false);
            this.drafts = blogObject.optInt("drafts", -1);
            this.facebook = blogObject.optString("facebook", "").equalsIgnoreCase("Y");
            this.facebookOpengraphEnabled = blogObject.optString("facebook_opengraph_enabled", "")
                    .equalsIgnoreCase("Y");
            this.followed = blogObject.getBoolean("followed");
            this.followers = blogObject.optInt("followers", -1);
            this.isBlockedFromPrimary = blogObject.getBoolean("is_blocked_from_primary");
            this.isNSFW = blogObject.getBoolean("is_nsfw");
            this.messages = blogObject.optInt("messages", -1);
            this.posts = blogObject.getInt("posts");
            this.primary = blogObject.optBoolean("primary", false);
            this.queue = blogObject.optInt("queue", 0);
            this.shareLikes = blogObject.getBoolean("share_likes");
            this.submissionPageTitle = blogObject.optString("submission_page_title", "");
            JSONObject submissionJSONObject = blogObject.optJSONObject("submission_terms");
            this.submissionTerms = (submissionJSONObject != null) ? new SubmissionTerms(submissionJSONObject) : null;
            this.subscribed = blogObject.getBoolean("subscribed");
            this.totalPosts = blogObject.getInt("total_posts");

            String sTweet = blogObject.optString("tweet", "N");
            if (sTweet.equalsIgnoreCase("Auto")) {
                this.tweet = Tweet.Auto;
            } else if (sTweet.equalsIgnoreCase("Y")) {
                this.tweet = Tweet.Yes;
            } else {
                this.tweet = Tweet.No;
            }

            this.twitterEnabled = blogObject.optBoolean("twitter_enabled", false);
            this.twitterSend = blogObject.optBoolean("twitter_send", false);

            if (blogObject.optString("type", "public").equalsIgnoreCase("public")) {
                this.type = Type.Public;
            } else {
                this.type = Type.Private;
            }
        }

        public boolean isAdmin() {
            return this.admin;
        }

        public boolean isAsk() {
            return this.ask;
        }

        public boolean isAskAnon() {
            return this.askAnon;
        }

        public String getAskPageTitle() {
            return this.askPageTitle;
        }

        public List<Avatar.Data> getAvatars() {
            return this.avatars;
        }

        public boolean isCanChat() {
            return this.canChat;
        }

        public boolean isCanSendFanMail() {
            return this.canSendFanMail;
        }

        public boolean isCanSubmit() {
            return this.canSubmit;
        }

        public boolean isCanSubscribe() {
            return this.canSubscribe;
        }

        public int getDrafts() {
            return this.drafts;
        }

        public boolean isFacebook() {
            return this.facebook;
        }

        public boolean isFacebookOpengraphEnabled() {
            return this.facebookOpengraphEnabled;
        }

        public boolean isFollowed() {
            return this.followed;
        }

        public int getFollowers() {
            return this.followers;
        }

        public boolean isBlockedFromPrimary() {
            return this.isBlockedFromPrimary;
        }

        public boolean isNSFW() {
            return this.isNSFW;
        }

        public int getMessages() {
            return this.messages;
        }

        public int getPosts() {
            return this.posts;
        }

        public boolean isPrimary() {
            return this.primary;
        }

        public int getQueue() {
            return this.queue;
        }

        public boolean isShareLikes() {
            return this.shareLikes;
        }

        public String getSubmissionPageTitle() {
            return this.submissionPageTitle;
        }

        public SubmissionTerms getSubmissionTerms() {
            return this.submissionTerms;
        }

        public boolean isSubscribed() {
            return this.subscribed;
        }

        public int getTotalPosts() {
            return this.totalPosts;
        }

        public Tweet getTweet() {
            return this.tweet;
        }

        public boolean isTwitterEnabled() {
            return this.twitterEnabled;
        }

        public boolean isTwitterSend() {
            return this.twitterSend;
        }

        public Type getType() {
            return this.type;
        }
    }

    class Api extends Id<Data> {

        /*
         * "response": { "blog": { "admin": true, "ask": true, "ask_anon": true,
         * "ask_page_title": "Vediamo se la so", "avatar": [ => Array of Avatars Object,
         * see Avatar.Data ], "can_chat": true, "can_send_fan_mail": true, "can_submit":
         * true, "can_subscribe": false, "description":
         * "Anche un papero sa arrampicarsi su un albero se viene adulato", "drafts": 0,
         * "facebook": "N", "facebook_opengraph_enabled": "N", "followed": false,
         * "followers": 261, "is_blocked_from_primary": false, "is_nsfw": false,
         * "messages": 0, "name": "paperogacoibentato", "posts": 791, "primary": true,
         * "queue": 0, "share_likes": false, "submission_page_title":
         * "Ecco, bravo, damme na' mano", "submission_terms": { => SubmissionTerms
         * Object, see above }, "subscribed": false, "theme": { => Theme data, we don't
         * need it }, "title": "Paperoga Coibentato", "total_posts": 791, "tweet": "N",
         * "twitter_enabled": false, "twitter_send": false, "type": "public", "updated":
         * 1582480880, "url": "https:\/\/paperogacoibentato.tumblr.com\/", "uuid":
         * "t:4ZHKojAk25vVcuhziYcWLw", } } }
         */

        public Api(String blogId) {
            super(blogId);
        }

        @Override
        protected String getPath() {
            return super.getPath() + "/info";
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            return new Data(jsonObject.getJSONObject("blog"));
        }
    }
}
