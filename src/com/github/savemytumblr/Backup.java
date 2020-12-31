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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.blog.array.Posts;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.posts.ContentItem;
import com.github.savemytumblr.posts.Post;
import com.github.savemytumblr.posts.media.Base;
import com.github.savemytumblr.posts.media.Media;

public class Backup {
    public interface Progress {
        void progress(int current, int total);

        void log(String msg);
    }

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
            this.id = id;
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
            return (index == 0) && (timestamp == null) && (id == 0);
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
    private Path blogPath;
    private BackupState backupState;
    private Thread esTC;
    private int currentOffset;
    private int totalPosts;
    private int savedFromHead;
    private TimePoint blogFirstPost;
    private boolean tailFound;
    private Progress progress;

    private AtomicBoolean isRunning;
    private AtomicBoolean terminate;

    public Backup(Path backupRootPath, TumblrClient tc, String blogName, Progress progress) {
        this.tc = tc;
        this.blogName = blogName;
        this.isRunning = new AtomicBoolean(false);
        this.terminate = new AtomicBoolean(false);
        this.esTC = null;
        this.currentOffset = 0;
        this.totalPosts = 0;
        this.savedFromHead = 0;
        this.blogFirstPost = null;
        this.tailFound = false;
        this.progress = progress;

        try {
            this.blogPath = backupRootPath.resolve(this.blogName);
            Files.createDirectories(this.blogPath);
        } catch (IOException e) {

        }
    }

    private void savePost(Post.Item item) {
        try {
            LocalDate lDate = item.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            Path postPath = blogPath.resolve(String.valueOf(lDate.getYear()))
                    .resolve(String.format("%02d", lDate.getMonthValue()))
                    .resolve(String.format("%02d", lDate.getDayOfMonth()));
            Files.createDirectories(postPath);

            progress.log("Save post " + String.valueOf(item.getId()));

            BufferedWriter writer = Files.newBufferedWriter(postPath.resolve(String.valueOf(item.getId()) + ".json"),
                    StandardCharsets.UTF_8);
            writer.write(item.getJSON());
            writer.close();

            for (ContentItem ci : item.getContent()) {
                if (ci instanceof Base) {
                    Base mediaContent = (Base) ci;

                    int maxWidth = 0;
                    String sUrl = null;

                    for (Media m : mediaContent.getMedia()) {
                        if (m.getWidth() > maxWidth) {
                            maxWidth = m.getWidth();
                            sUrl = m.getUrl();
                        }
                    }

                    Path mediaPath = postPath.resolve(String.valueOf(item.getId()));
                    Files.createDirectories(mediaPath);

                    try {
                        Path fPath = mediaPath.resolve(Paths.get(new URI(sUrl).getPath()).getFileName());

                        URL url = new URL(sUrl);

                        progress.log("=> Save media " + sUrl);

                        HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
                        httpConn.addRequestProperty("User-Agent",
                                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
                        httpConn.addRequestProperty("Accept", "image/*, video/*, audio/*");
                        int responseCode = httpConn.getResponseCode();

                        if (responseCode == HttpsURLConnection.HTTP_OK) {
                            InputStream inputStream = httpConn.getInputStream();
                            Files.copy(inputStream, fPath, StandardCopyOption.REPLACE_EXISTING);
                            inputStream.close();
                        }
                    } catch (URISyntaxException e) {

                    }
                }
            }
        } catch (IOException e) {

        }

        progress.log("");
    }

    private void updateState(TimePoint start, TimePoint end, int count) {
        TimePoint newStart = (start == null) ? backupState.getStart() : start;
        TimePoint newEnd = (end == null) ? backupState.getEnd() : end;

        backupState = new BackupState(newStart, newEnd, backupState.getSavedPosts() + count);

        try {
            BufferedWriter writer = Files.newBufferedWriter(blogPath.resolve("data.json"), StandardCharsets.UTF_8);
            writer.write(backupState.toJSON().toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateState(TimePoint start, TimePoint end) {
        updateState(start, end, 1);
    }

    private void downloadHead() {
        esTC = new Thread(new Runnable() {

            @Override
            public void run() {
                tc.call(Posts.Api.class, blogName, currentOffset, 20,
                        new com.github.savemytumblr.api.array.CompletionInterface<Post.Item, Post.Data>() {
                            @Override
                            public void onSuccess(List<Post.Item> result, int offset, int limit, int count) {
                                if (backupState.getSavedPosts() == totalPosts) {
                                    return;
                                }

                                boolean headFound = false;
                                boolean emptyBackup = backupState.getStart().isNull();

                                for (Post.Item item : result) {
                                    if (item.isPinned()) {
                                        continue;
                                    }

                                    if (blogFirstPost == null) {
                                        blogFirstPost = new TimePoint(0, item.getTimestamp(), item.getId());
                                    }

                                    if (emptyBackup
                                            || item.getTimestamp().after(backupState.getStart().getTimestamp())) {
                                        savePost(item);
                                        ++savedFromHead;

                                        if (emptyBackup) {
                                            headFound = true;
                                        }
                                    } else if (!item.getTimestamp().after(backupState.getStart().getTimestamp())) {
                                        headFound = true;
                                        break;
                                    }

                                    if (terminate.get()) {
                                        isRunning.set(false);
                                        return;
                                    }
                                }

                                progress.progress(currentOffset + result.size(), totalPosts);

                                if (!headFound) {
                                    currentOffset += result.size();
                                    downloadHead();
                                } else {
                                    if (emptyBackup) {
                                        tailFound = true;
                                        updateState(blogFirstPost,
                                                new TimePoint(currentOffset + result.size() - 1,
                                                        result.get(result.size() - 1).getTimestamp(),
                                                        result.get(result.size() - 1).getId()),
                                                savedFromHead);
                                    } else {
                                        updateState(blogFirstPost, null, savedFromHead);
                                    }

                                    currentOffset = (emptyBackup) ? result.size() : backupState.getEnd().getIndex();
                                    downloadTail();
                                }
                            }

                            @Override
                            public void onFailure(BaseException e) {
                                isRunning.set(false);
                            }
                        });
            }
        });

        esTC.setDaemon(true);
        esTC.start();
    }

    private void downloadTail() {
        esTC = new Thread(new Runnable() {

            @Override
            public void run() {
                tc.call(Posts.Api.class, blogName, currentOffset, 20,
                        new com.github.savemytumblr.api.array.CompletionInterface<Post.Item, Post.Data>() {
                            @Override
                            public void onSuccess(List<Post.Item> result, int offset, int limit, int count) {
                                if (terminate.get()) {
                                    isRunning.set(false);
                                    return;
                                }

                                int index = 0;

                                if (!tailFound) {
                                    if (result.get(index).getTimestamp().equals(backupState.getEnd().getTimestamp())) {
                                        tailFound = true;
                                    } else if (result.get(index).getTimestamp()
                                            .before(backupState.getEnd().getTimestamp())) {
                                        currentOffset -= 20;
                                        downloadTail();
                                        return;
                                    } else {
                                        while ((index < result.size()) && result.get(index).getTimestamp()
                                                .after(backupState.getEnd().getTimestamp())) {
                                            ++index;
                                        }

                                        if (index == result.size()) {
                                            currentOffset += 20;
                                            downloadTail();
                                            return;
                                        } else {
                                            tailFound = true;
                                        }
                                    }
                                }

                                for (int i = index; i < result.size(); ++i) {
                                    Post.Item item = result.get(i);

                                    if (item.isPinned()) {
                                        continue;
                                    }

                                    savePost(item);
                                    updateState(null,
                                            new TimePoint(currentOffset + index, item.getTimestamp(), item.getId()));

                                    if (terminate.get()) {
                                        isRunning.set(false);
                                        return;
                                    }
                                }

                                currentOffset += result.size();
                                progress.progress(currentOffset, totalPosts);

                                if (backupState.getSavedPosts() < totalPosts) {
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

        esTC.setDaemon(true);
        esTC.start();
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

        currentOffset = 0;
        totalPosts = 0;
        savedFromHead = 0;
        blogFirstPost = null;
        tailFound = false;

        terminate.set(false);
        isRunning.set(true);

        tc.call(Info.Api.class, blogName, new com.github.savemytumblr.api.simple.CompletionInterface<Info.Data>() {
            @Override
            public void onSuccess(Info.Data result) {
                totalPosts = result.getTotalPosts();
                downloadHead();
            }

            @Override
            public void onFailure(BaseException e) {
            }
        });
    }

    public void stop() {
        if (!isRunning())
            return;

        terminate.set(true);

        if (esTC != null) {
            try {
                esTC.join();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
