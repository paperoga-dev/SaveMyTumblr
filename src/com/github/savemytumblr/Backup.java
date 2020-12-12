package com.github.savemytumblr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.CompletionInterface;
import com.github.savemytumblr.blog.array.Posts;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.posts.ContentItem;
import com.github.savemytumblr.posts.Post;
import com.github.savemytumblr.posts.media.Base;
import com.github.savemytumblr.posts.media.Media;

public class Backup {

    private class TimePoint {
        int index;
        Date timestamp;
        long id;

        public TimePoint() {
            this.index = 0;
            this.timestamp = null;
            this.id = 0;
        }

        public TimePoint(int index, Date timestamp, long id) {
            this.index = index;
            this.timestamp = timestamp;
            this.id = 0;
        }

        public TimePoint(JSONObject jsonObject) {
            try {
                this.index = jsonObject.getInt("index");
                this.timestamp = new Date(jsonObject.getLong("timestamp") * 1000);
                this.id = jsonObject.getLong("id");
            } catch (JSONException e) {
                this.index = 0;
                this.timestamp = new Date();
                this.id = 0;
            }
        }

        public JSONObject toJSON() {
            JSONObject res = new JSONObject();

            if (!isNull()) {
                res.put("index", getIndex());
                res.put("timestamp", getTimestamp().getTime() / 1000);
                res.put("id", getId());
            }

            return res;
        }

        public int getIndex() {
            return index;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public long getId() {
            return id;
        }

        public boolean isNull() {
            return (index == 0) || (timestamp == null) || (id == 0);
        }
    }

    private class BackupState {
        private TimePoint tStart;
        private TimePoint tEnd;
        private int savedPosts;

        public BackupState() {
            this.tStart = new TimePoint();
            this.tEnd = new TimePoint();
            this.savedPosts = 0;
        }

        public BackupState(TimePoint tStart, TimePoint tEnd, int savedPosts) {
            this.tStart = tStart;
            this.tEnd = tEnd;
            this.savedPosts = savedPosts;
        }

        public BackupState(JSONObject jsonObject) throws JSONException {
            this.tStart = new TimePoint(jsonObject.getJSONObject("tStart"));
            this.tEnd = new TimePoint(jsonObject.getJSONObject("tEnd"));
            this.savedPosts = jsonObject.getInt("savedPosts");
        }

        public TimePoint getStart() {
            return tStart;
        }

        public TimePoint getEnd() {
            return tEnd;
        }

        public int getSavedPosts() {
            return savedPosts;
        }

        public JSONObject toJSON() {
            JSONObject res = new JSONObject();

            res.put("tStart", getStart().toJSON());
            res.put("tEnd", getEnd().toJSON());
            res.put("savedPosts", getSavedPosts());

            return res;
        }
    }

    final private TumblrClient tc;
    private String blogName;
    private Path backupRootPath;
    private Path blogPath;
    private BackupState backupState;
    private ExecutorService esTC;
    private int currentOffset;
    private int savedPosts;
    private TimePoint newStart;
    private boolean tailFound;

    private AtomicBoolean isRunning;
    private AtomicBoolean terminate;

    public Backup(String backupRootPath, TumblrClient tc, String blogName) {
        this.tc = tc;
        this.blogName = blogName;
        this.isRunning = new AtomicBoolean(false);
        this.terminate = new AtomicBoolean(false);
        this.esTC = Executors.newSingleThreadExecutor();
        this.currentOffset = 0;
        this.savedPosts = 0;
        this.newStart = null;
        this.tailFound = false;

        this.backupRootPath = Path.of(backupRootPath);

        try {
            this.blogPath = this.backupRootPath.resolve(this.blogName);
            Files.createDirectories(this.blogPath);
        } catch (IOException e) {

        }
    }

    private void savePost(Post.Item item) throws IOException, URISyntaxException {
        LocalDate lDate = item.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        Path postPath = blogPath.resolve(String.valueOf(lDate.getYear()))
                .resolve(String.format("%02d", lDate.getMonth())).resolve(String.format("%02d", lDate.getDayOfMonth()));
        Files.createDirectories(postPath);

        BufferedWriter writer = Files.newBufferedWriter(postPath.resolve(String.valueOf(item.getId()) + ".json"),
                StandardCharsets.UTF_8);
        writer.write(item.getJSON());
        writer.close();

        for (ContentItem ci : item.getContent()) {
            if (ci instanceof Base) {
                Base mediaContent = (Base) ci;

                for (Media m : mediaContent.getMedia()) {
                    Path mediaPath = postPath.resolve(String.valueOf(item.getId()));
                    Files.createDirectories(postPath);

                    Path fPath = mediaPath.resolve(Paths.get(new URI(m.getUrl()).getPath()).getFileName());

                    InputStream in = new URL(m.getUrl()).openStream();
                    Files.copy(in, fPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void downloadHead() {
        esTC.execute(new Runnable() {

            @Override
            public void run() {
                tc.call(Posts.Api.class, blogName, currentOffset, 20, new CompletionInterface<Post.Item, Post.Data>() {
                    @Override
                    public void onSuccess(List<Post.Item> result, int offset, int limit, int count) {
                        if (count == backupState.getSavedPosts()) {
                            return;
                        }

                        if (newStart == null) {
                            newStart = new TimePoint(0, result.get(0).getTimestamp(), result.get(0).getId());
                        }

                        boolean headFound = false;
                        for (Post.Item item : result) {
                            if (item.getId() == backupState.getStart().getId()) {
                                headFound = true;

                                backupState = new BackupState(newStart, backupState.getEnd(),
                                        backupState.getSavedPosts() + savedPosts);
                                try {
                                    BufferedWriter writer = Files.newBufferedWriter(blogPath.resolve("data.json"),
                                            StandardCharsets.UTF_8);
                                    writer.write(backupState.toJSON().toString());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }

                            try {
                                savePost(item);
                            } catch (IOException | URISyntaxException e) {
                                e.printStackTrace();
                                isRunning.set(false);
                                return;
                            }

                            ++savedPosts;
                        }

                        if (terminate.get()) {
                            isRunning.set(false);
                            return;
                        }

                        if (backupState.getSavedPosts() < count) {
                            if (headFound) {
                                savedPosts = 0;
                                currentOffset = backupState.getEnd().getIndex();
                                downloadTail();
                            } else {
                                currentOffset += result.size();
                                downloadHead();
                            }
                        } else {
                            isRunning.set(false);
                        }
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        isRunning.set(false);
                    }
                });
            }
        });
    }

    private void downloadTail() {
        esTC.execute(new Runnable() {

            @Override
            public void run() {
                tc.call(Posts.Api.class, blogName, currentOffset, 20, new CompletionInterface<Post.Item, Post.Data>() {
                    @Override
                    public void onSuccess(List<Post.Item> result, int offset, int limit, int count) {
                        if (count == backupState.getSavedPosts()) {
                            return;
                        }

                        Post.Item lastPost = null;
                        Post.Item firstPost = null;
                        for (Post.Item item : result) {
                            if (firstPost == null) {
                                firstPost = item;
                            }

                            if (backupState.getStart().getId() == 0) {
                                tailFound = true;
                            }

                            if (tailFound) {
                                try {
                                    savePost(item);
                                } catch (IOException | URISyntaxException e) {
                                    e.printStackTrace();
                                    isRunning.set(false);
                                    return;
                                }

                                ++savedPosts;
                            } else {
                                if (item.getId() == backupState.getEnd().getId()) {
                                    tailFound = true;
                                }
                            }

                            lastPost = item;
                        }

                        if (tailFound) {
                            TimePoint start = backupState.getStart();
                            if (start.getId() == 0) {
                                start = new TimePoint(0, start.getTimestamp(), start.getId());
                            }

                            backupState = new BackupState(start, new TimePoint(currentOffset + result.size(),
                                    lastPost.getTimestamp(), lastPost.getId()),
                                    backupState.getSavedPosts() + savedPosts);
                            try {
                                BufferedWriter writer = Files.newBufferedWriter(blogPath.resolve("data.json"),
                                        StandardCharsets.UTF_8);
                                writer.write(backupState.toJSON().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        if (terminate.get()) {
                            isRunning.set(false);
                            return;
                        }

                        if (backupState.getSavedPosts() < count) {
                            currentOffset += result.size();
                            downloadTail();
                        } else {
                            isRunning.set(false);
                        }
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        isRunning.set(false);
                    }
                });
            }
        });
    }

    public void start() {
        if (isRunning())
            return;

        try {
            JSONObject jsonRoot = new JSONObject(
                    new String(Files.readAllBytes(this.blogPath.resolve("data.json")), StandardCharsets.UTF_8));

            backupState = new BackupState(jsonRoot);
        } catch (IOException | JSONException e) {
            backupState = new BackupState();
        }

        terminate.set(false);
        savedPosts = 0;
        currentOffset = 0;
        newStart = null;
        isRunning.set(true);
        tailFound = false;

        if (backupState.getStart().getId() == 0) {
            downloadTail();
        } else {
            downloadHead();
        }
    }

    public void stop() {
        if (!isRunning())
            return;

        terminate.set(true);
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
