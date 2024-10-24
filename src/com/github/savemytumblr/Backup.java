package com.github.savemytumblr;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.blog.array.Posts;
import com.github.savemytumblr.blog.simple.Info;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.posts.ContentItem;
import com.github.savemytumblr.posts.Post;
import com.github.savemytumblr.posts.Post.Item;
import com.github.savemytumblr.posts.Post.Trail;

public class Backup {
    public interface Progress {
        void progress(int current, int total);

        void log(String msg);

        void onCompleted(boolean ok);
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

        public TimePoint(int iIndex, Date cTimestamp, long lId) {
            this.index = iIndex;
            this.timestamp = cTimestamp;
            this.id = lId;
        }

        public TimePoint(JSONObject jsonObject) {
            try {
                this.index = jsonObject.getInt("index");
                this.timestamp = new Date(jsonObject.getLong("timestamp") * 1000);
                this.id = jsonObject.getLong("id");
            } catch (@SuppressWarnings("unused") JSONException e) {
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
            return this.index;
        }

        public Date getTimestamp() {
            return this.timestamp;
        }

        public long getId() {
            return this.id;
        }

        public boolean isNull() {
            return (this.index == 0) && (this.timestamp == null) && (this.id == 0);
        }
    }

    private class BackupState {
        private TimePoint tStart;
        private TimePoint tEnd;

        public BackupState() {
            this.tStart = new TimePoint();
            this.tEnd = new TimePoint();
        }

        public BackupState(TimePoint tpStart, TimePoint tpEnd) {
            this.tStart = tpStart;
            this.tEnd = tpEnd;
        }

        public BackupState(JSONObject jsonObject) throws JSONException {
            this.tStart = new TimePoint(jsonObject.getJSONObject("tStart"));
            this.tEnd = new TimePoint(jsonObject.getJSONObject("tEnd"));
        }

        public TimePoint getStart() {
            return this.tStart;
        }

        public TimePoint getEnd() {
            return this.tEnd;
        }

        public JSONObject toJSON() {
            JSONObject res = new JSONObject();

            res.put("tStart", getStart().toJSON());
            res.put("tEnd", getEnd().toJSON());

            return res;
        }
    }

    final private TumblrClient tumblrClient;
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

    public Backup(Path cBackupRootPath, TumblrClient cTumblrClient, String sBlogName, Progress cProgress) {
        this.tumblrClient = cTumblrClient;
        this.blogName = sBlogName;
        this.isRunning = new AtomicBoolean(false);
        this.terminate = new AtomicBoolean(false);
        this.esTC = null;
        this.currentOffset = 0;
        this.totalPosts = 0;
        this.savedFromHead = 0;
        this.blogFirstPost = null;
        this.tailFound = false;
        this.progress = cProgress;

        try {
            this.blogPath = cBackupRootPath.resolve(this.blogName);
            Files.createDirectories(this.blogPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMedia(Path mediaPath, Post.Trail item) throws URISyntaxException, IOException {
        for (ContentItem ci : item.getContent()) {
            String sUrl = null;
            String req = null;

            if (ci instanceof com.github.savemytumblr.posts.media.Base) {
                com.github.savemytumblr.posts.media.Base mediaContent = (com.github.savemytumblr.posts.media.Base) ci;

                List<com.github.savemytumblr.posts.media.Media> mediaList = mediaContent.getMedia();

                if (!mediaList.isEmpty()) {
                    int maxWidth = mediaList.get(0).getWidth();
                    sUrl = mediaList.get(0).getUrl();

                    for (int i = 1; i < mediaList.size(); ++i) {
                        com.github.savemytumblr.posts.media.Media mediaItem = mediaList.get(i);

                        if (mediaItem.getWidth() > maxWidth) {
                            maxWidth = mediaItem.getWidth();
                            sUrl = mediaItem.getUrl();
                        }
                    }

                    req = "image/*";
                }
            } else if (ci instanceof com.github.savemytumblr.posts.video.Base) {
                com.github.savemytumblr.posts.video.Base mediaContent = (com.github.savemytumblr.posts.video.Base) ci;

                if (mediaContent.getMedia() != null) {
                    sUrl = mediaContent.getMedia().getUrl();
                    req = "video/*";
                }

            } else if (ci instanceof com.github.savemytumblr.posts.audio.Base) {
                com.github.savemytumblr.posts.audio.Base mediaContent = (com.github.savemytumblr.posts.audio.Base) ci;

                if (mediaContent.getMedia() != null) {
                    sUrl = mediaContent.getMedia().getUrl();
                    req = "audio/*";
                }
            }

            if (sUrl == null) {
                continue;
            }

            sUrl = sUrl.replace(" ", "%20");

            Files.createDirectories(mediaPath);

            int tries = 4;
            while (--tries != 0) {
                try {
                    Path fPath = mediaPath.resolve(Paths.get(new URI(sUrl).getPath()).getFileName());

                    URL url = URI.create(sUrl).toURL();

                    this.progress.log("=> Save media " + sUrl);

                    HttpsURLConnection httpConn = (HttpsURLConnection) url.openConnection();
                    httpConn.addRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
                    httpConn.addRequestProperty("Accept", req);

                    if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (InputStream inputStream = httpConn.getInputStream()) {
                            Files.copy(inputStream, fPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (@SuppressWarnings("unused") SSLException | URISyntaxException | UnknownHostException e) {
                    // Not used
                } catch (@SuppressWarnings("unused") ConnectException e) {
                    continue;
                }

                break;
            }
        }

        for (Trail trail : item.getTrail()) {
            saveMedia(mediaPath, trail);
        }
    }

    private void savePost(Post.Item item) throws URISyntaxException, IOException {
        LocalDate lDate = item.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        Path postPath = this.blogPath.resolve(String.valueOf(lDate.getYear()))
                .resolve(String.format("%02d", lDate.getMonthValue()))
                .resolve(String.format("%02d", lDate.getDayOfMonth()));
        Files.createDirectories(postPath);

        this.progress.log("Save post " + String.valueOf(item.getId()));

        try (BufferedWriter writer = Files.newBufferedWriter(postPath.resolve(String.valueOf(item.getId()) + ".json"),
                StandardCharsets.UTF_8)) {
            writer.write(item.getJSON());
        }

        saveMedia(postPath.resolve(String.valueOf(item.getId())), item);

        this.progress.log("");
    }

    private void updateState(TimePoint start, TimePoint end) {
        TimePoint newStart = (start == null) ? this.backupState.getStart() : start;
        TimePoint newEnd = (end == null) ? this.backupState.getEnd() : end;

        this.backupState = new BackupState(newStart, newEnd);

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(this.blogPath.resolve("data.json"),
                    StandardCharsets.UTF_8)) {
                writer.write(this.backupState.toJSON().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void joinThread(Thread t) {
        if (t != null) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadHead() {
        Thread prevT = this.esTC;

        this.esTC = new Thread(new Runnable() {

            @Override
            public void run() {
                Backup.this.tumblrClient.call(Posts.Api.class, Backup.this.blogName, Backup.this.currentOffset, 20,
                        new com.github.savemytumblr.api.array.CompletionInterface<Post.Item, Post.Data>() {
                            @Override
                            public void onSuccess(List<Post.Item> result, Integer offset, Integer limit, int count) {
                                result.sort(new Comparator<Post.Item>() {
                                    @Override
                                    public int compare(Item o1, Item o2) {
                                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                                    }
                                });

                                boolean headFound = false;
                                boolean emptyBackup = Backup.this.backupState.getStart().isNull();
                                int pinnedItems = 0;

                                for (Post.Item item : result) {
                                    if (item.isPinned()) {
                                        ++pinnedItems;
                                        continue;
                                    }

                                    if (Backup.this.blogFirstPost == null) {
                                        Backup.this.blogFirstPost = new TimePoint(0, item.getTimestamp(), item.getId());
                                    }

                                    if (emptyBackup || item.getTimestamp()
                                            .after(Backup.this.backupState.getStart().getTimestamp())) {
                                        try {
                                            savePost(item);
                                        } catch (Exception e) {
                                            Backup.this.isRunning.set(false);
                                            Backup.this.progress.onCompleted(false);
                                            e.printStackTrace();
                                            return;
                                        }
                                        ++Backup.this.savedFromHead;

                                        if (emptyBackup) {
                                            headFound = true;
                                        }
                                    } else if (!item.getTimestamp()
                                            .after(Backup.this.backupState.getStart().getTimestamp())) {
                                        headFound = true;
                                        break;
                                    }

                                    if (Backup.this.terminate.get()) {
                                        Backup.this.isRunning.set(false);
                                        return;
                                    }
                                }

                                Backup.this.progress.progress(Backup.this.currentOffset + result.size(),
                                        Backup.this.totalPosts);

                                if (!headFound) {
                                    Backup.this.currentOffset += result.size();
                                    downloadHead();
                                } else {
                                    if (emptyBackup) {
                                        Backup.this.tailFound = true;
                                        updateState(Backup.this.blogFirstPost,
                                                new TimePoint(Backup.this.currentOffset + result.size() - 1,
                                                        result.get(result.size() - 1).getTimestamp(),
                                                        result.get(result.size() - 1).getId()));
                                    } else {
                                        updateState(Backup.this.blogFirstPost, null);
                                    }

                                    Backup.this.currentOffset = (emptyBackup) ? (result.size() - pinnedItems)
                                            : Math.min(Backup.this.totalPosts - 1,
                                                    (Backup.this.backupState.getEnd().getIndex()
                                                            + Backup.this.savedFromHead));

                                    if (Backup.this.currentOffset < Backup.this.totalPosts) {
                                        downloadTail();
                                    } else {
                                        Backup.this.isRunning.set(false);
                                        Backup.this.progress.onCompleted(true);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(BaseException e) {
                                Backup.this.isRunning.set(false);
                                Backup.this.progress.onCompleted(false);

                                e.printStackTrace();
                            }
                        });
            }
        });

        this.esTC.start();
        joinThread(prevT);
    }

    private void downloadTail() {
        Thread prevT = this.esTC;

        this.esTC = new Thread(new Runnable() {

            @Override
            public void run() {
                Backup.this.tumblrClient.call(Posts.Api.class, Backup.this.blogName, Backup.this.currentOffset, 20,
                        new com.github.savemytumblr.api.array.CompletionInterface<Post.Item, Post.Data>() {
                            @Override
                            public void onSuccess(List<Post.Item> result, Integer offset, Integer limit, int count) {
                                if (Backup.this.terminate.get()) {
                                    Backup.this.isRunning.set(false);
                                    return;
                                }

                                result.sort(new Comparator<Post.Item>() {
                                    @Override
                                    public int compare(Item o1, Item o2) {
                                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                                    }
                                });

                                int index = 0;

                                if (!Backup.this.tailFound) {
                                    if (result.get(index).getTimestamp()
                                            .equals(Backup.this.backupState.getEnd().getTimestamp())) {
                                        ++index;
                                        Backup.this.tailFound = true;
                                    } else if (result.get(index).getTimestamp()
                                            .before(Backup.this.backupState.getEnd().getTimestamp())) {
                                        Backup.this.currentOffset -= 20;
                                        downloadTail();
                                        return;
                                    } else {
                                        while ((index < result.size()) && result.get(index).getTimestamp()
                                                .after(Backup.this.backupState.getEnd().getTimestamp())) {
                                            ++index;
                                        }

                                        if (index == result.size()) {
                                            int newOffset = Backup.this.currentOffset + 20;

                                            if (newOffset > Backup.this.totalPosts) {
                                                Post.Item lastItem = result.get(result.size() - 1);

                                                updateState(null,
                                                        new TimePoint(Backup.this.currentOffset + result.size() - 1,
                                                                lastItem.getTimestamp(), lastItem.getId()));
                                                Backup.this.isRunning.set(false);
                                                Backup.this.progress.onCompleted(false);
                                                return;
                                            }

                                            Backup.this.currentOffset = newOffset;
                                            downloadTail();
                                            return;
                                        }

                                        Backup.this.tailFound = true;
                                    }
                                }

                                for (int i = index; i < result.size(); ++i) {
                                    Post.Item item = result.get(i);

                                    if (item.isPinned()) {
                                        continue;
                                    }

                                    try {
                                        savePost(item);
                                    } catch (Exception e) {
                                        Backup.this.isRunning.set(false);
                                        Backup.this.progress.onCompleted(false);
                                        e.printStackTrace();
                                        return;
                                    }
                                    updateState(null, new TimePoint(Backup.this.currentOffset + index,
                                            item.getTimestamp(), item.getId()));

                                    if (Backup.this.terminate.get()) {
                                        Backup.this.isRunning.set(false);
                                        return;
                                    }
                                }

                                Backup.this.currentOffset += result.size();
                                Backup.this.progress.progress(Backup.this.currentOffset, Backup.this.totalPosts);

                                if (Backup.this.currentOffset < Backup.this.totalPosts) {
                                    downloadTail();
                                } else {
                                    Backup.this.isRunning.set(false);
                                    Backup.this.progress.onCompleted(true);
                                }
                            }

                            @Override
                            public void onFailure(BaseException e) {
                                Backup.this.isRunning.set(false);
                                Backup.this.progress.onCompleted(false);

                                e.printStackTrace();
                            }
                        });
            }
        });

        this.esTC.start();
        joinThread(prevT);
    }

    public void start() {
        if (isRunning()) {
            return;
        }

        try {
            JSONObject jsonRoot = new JSONObject(
                    new String(Files.readAllBytes(this.blogPath.resolve("data.json")), StandardCharsets.UTF_8));

            this.backupState = new BackupState(jsonRoot);
        } catch (@SuppressWarnings("unused") NoSuchFileException e) {
            this.backupState = new BackupState();
        } catch (Exception e) {
            e.printStackTrace();
            this.backupState = new BackupState();
        }

        this.currentOffset = 0;
        this.totalPosts = 0;
        this.savedFromHead = 0;
        this.blogFirstPost = null;
        this.tailFound = false;

        this.terminate.set(false);
        this.isRunning.set(true);

        this.tumblrClient.call(Info.Api.class, this.blogName,
                new com.github.savemytumblr.api.simple.CompletionInterface<Info.Data>() {
                    @Override
                    public void onSuccess(Info.Data result) {
                        Backup.this.totalPosts = result.getTotalPosts();
                        downloadHead();
                    }

                    @Override
                    public void onFailure(BaseException e) {
                        Backup.this.progress.onCompleted(false);
                        Backup.this.isRunning.set(false);
                    }
                });
    }

    public void stop() {
        if (!isRunning()) {
            return;
        }

        this.terminate.set(true);
        joinThread(this.esTC);
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }
}
