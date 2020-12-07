package com.github.savemytumblr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.savemytumblr.api.array.CompletionInterface;
import com.github.savemytumblr.blog.array.Posts;
import com.github.savemytumblr.exception.BaseException;
import com.github.savemytumblr.posts.Post;

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
				res.put("index", index);
				res.put("timestamp", timestamp.getTime() / 1000);
				res.put("id", id);
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
		
		public int savedPosts() {
			return savedPosts;
		}

		public JSONObject toJSON() {
			JSONObject res = new JSONObject();
			
			res.put("tStart", tStart.toJSON());
			res.put("tEnd", tEnd.toJSON());
			res.put("savedPosts", savedPosts);
			
			return res;
		}
	}

	final private TumblrClient tc;
	private String blogName;
	private Path backupRootPath;
	private Path blogPath;
	private BackupState backupState;
	private Thread thread;
	
	private AtomicBoolean isRunning;
	private AtomicBoolean terminate;
	
	public Backup(String backupRootPath, TumblrClient tc, String blogName) {
		this.tc = tc;
		this.blogName = blogName;
		this.isRunning = new AtomicBoolean(false);
		this.terminate = new AtomicBoolean(false);
		this.thread = null;
		
		this.backupRootPath = Path.of(backupRootPath);
		
		try {
			this.blogPath = this.backupRootPath.resolve(this.blogName);
			Files.createDirectories(this.blogPath);
		} catch (IOException e) {
			
		}
		
        try {
			JSONObject jsonRoot = new JSONObject(new String(Files.readAllBytes(this.blogPath.resolve("data.json")), StandardCharsets.UTF_8));
			
			backupState = new BackupState(jsonRoot);
		} catch (IOException | JSONException e) {
			backupState = new BackupState();
		}
	}
	
	public void start() {
		if (isRunning())
			return;
		
		terminate.set(false);
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				isRunning.set(true);

				while (!terminate.get()) {
					tc.call(
						Posts.Api.class,
						blogName,
						0,
						20,
						new CompletionInterface<Post.Item, Post.Data>() {
							@Override
							public void onSuccess(List<Post.Item> result, int offset, int limit, int count) {
								
							}
							
							@Override
							public void onFailure(BaseException e) {
								terminate.set(true);
							}
						});
				}
				
				isRunning.set(false);
			}
		});
	}
	
	public void stop() {
		if (!isRunning())
			return;

		terminate.set(true);
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isRunning() {
		return isRunning.get();
	}
}
