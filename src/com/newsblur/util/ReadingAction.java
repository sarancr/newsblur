package com.newsblur.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.newsblur.database.BlurDatabaseHelper;
import com.newsblur.database.DatabaseConstants;
import com.newsblur.network.domain.NewsBlurResponse;
import com.newsblur.network.APIManager;

import java.util.HashSet;
import java.util.Set;

public class ReadingAction {

    private enum ActionType {
        MARK_READ,
        MARK_UNREAD,
        SAVE,
        UNSAVE
    };

    private ActionType type;
    private String storyHash;
    private FeedSet feedSet;
    private Long olderThan;
    private Long newerThan;

    private ReadingAction() {
        ; // must use helpers
    }

    public static ReadingAction markStoryRead(String hash) {
        ReadingAction ra = new ReadingAction();
        ra.type = ActionType.MARK_READ;
        ra.storyHash = hash;
        return ra;
    }

    public static ReadingAction markStoryUnread(String hash) {
        ReadingAction ra = new ReadingAction();
        ra.type = ActionType.MARK_UNREAD;
        ra.storyHash = hash;
        return ra;
    }

    public static ReadingAction saveStory(String hash) {
        ReadingAction ra = new ReadingAction();
        ra.type = ActionType.SAVE;
        ra.storyHash = hash;
        return ra;
    }

    public static ReadingAction unsaveStory(String hash) {
        ReadingAction ra = new ReadingAction();
        ra.type = ActionType.UNSAVE;
        ra.storyHash = hash;
        return ra;
    }

    public static ReadingAction markFeedRead(FeedSet fs, Long olderThan, Long newerThan) {
        ReadingAction ra = new ReadingAction();
        ra.type = ActionType.MARK_READ;
        ra.feedSet = fs;
        ra.olderThan = olderThan;
        ra.newerThan = newerThan;
        return ra;
    }

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
        values.put(DatabaseConstants.ACTION_TIME, System.currentTimeMillis());
        switch (type) {

            case MARK_READ:
                values.put(DatabaseConstants.ACTION_MARK_READ, 1);
                if (storyHash != null) {
                    values.put(DatabaseConstants.ACTION_STORY_HASH, storyHash);
                } else if (feedSet != null) {
                    values.put(DatabaseConstants.ACTION_FEED_ID, feedSet.toCompactSerial());
                    if (olderThan != null) values.put(DatabaseConstants.ACTION_INCLUDE_OLDER, olderThan);
                    if (newerThan != null) values.put(DatabaseConstants.ACTION_INCLUDE_NEWER, newerThan);
                }
                break;
                
            case MARK_UNREAD:
                values.put(DatabaseConstants.ACTION_MARK_UNREAD, 1);
                if (storyHash != null) {
                    values.put(DatabaseConstants.ACTION_STORY_HASH, storyHash);
                }
                break;

            case SAVE:
                values.put(DatabaseConstants.ACTION_SAVE, 1);
                values.put(DatabaseConstants.ACTION_STORY_HASH, storyHash);
                break;

            case UNSAVE:
                values.put(DatabaseConstants.ACTION_UNSAVE, 1);
                values.put(DatabaseConstants.ACTION_STORY_HASH, storyHash);
                break;

            default:
                throw new IllegalStateException("cannot serialise uknown type of action.");

        }

		return values;
	}

	public static ReadingAction fromCursor(Cursor c) {
		ReadingAction ra = new ReadingAction();
        if (c.getInt(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_MARK_READ)) == 1) {
            ra.type = ActionType.MARK_READ;
            String hash = c.getString(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_STORY_HASH));
            String feedIds = c.getString(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_FEED_ID));
            Long includeOlder = DatabaseConstants.nullIfZero(c.getLong(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_INCLUDE_OLDER)));
            Long includeNewer = DatabaseConstants.nullIfZero(c.getLong(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_INCLUDE_NEWER)));
            if (hash != null) {
                ra.storyHash = hash;
            } else if (feedIds != null) {
                ra.feedSet = FeedSet.fromCompactSerial(feedIds);
                ra.olderThan = includeOlder;
                ra.newerThan = includeNewer;
            } else {
                throw new IllegalStateException("cannot deserialise uknown type of action.");
            }
        } else if (c.getInt(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_MARK_UNREAD)) == 1) {
            ra.type = ActionType.MARK_UNREAD;
            ra.storyHash = c.getString(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_STORY_HASH));
        } else if (c.getInt(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_SAVE)) == 1) {
            ra.type = ActionType.SAVE;
            ra.storyHash = c.getString(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_STORY_HASH));
        } else if (c.getInt(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_UNSAVE)) == 1) {
            ra.type = ActionType.UNSAVE;
            ra.storyHash = c.getString(c.getColumnIndexOrThrow(DatabaseConstants.ACTION_STORY_HASH));
        } else {
            throw new IllegalStateException("cannot deserialise uknown type of action.");
        }
		return ra;
	}

    /**
     * Execute this action remotely via the API.
     */
    public NewsBlurResponse doRemote(APIManager apiManager) {
        switch (type) {

            case MARK_READ:
                if (storyHash != null) {
                    return apiManager.markStoryAsRead(storyHash);
                } else if (feedSet != null) {
                    return apiManager.markFeedsAsRead(feedSet, olderThan, newerThan);
                }
                break;
                
            case MARK_UNREAD:
                return apiManager.markStoryHashUnread(storyHash);

            case SAVE:
                return apiManager.markStoryAsStarred(storyHash);

            case UNSAVE:
                return apiManager.markStoryAsUnstarred(storyHash);

            default:

        }

        throw new IllegalStateException("cannot execute uknown type of action.");
    }

    /**
     * Excecute this action on the local DB. These must be idempotent.
     */
    public void doLocal(BlurDatabaseHelper dbHelper) {
        switch (type) {

            case MARK_READ:
                if (storyHash != null) {
                    dbHelper.setStoryReadState(storyHash, true);
                } else if (feedSet != null) {
                    dbHelper.markStoriesRead(feedSet, olderThan, newerThan);
                }
                break;
                
            case MARK_UNREAD:
                dbHelper.setStoryReadState(storyHash, false);
                break;

            case SAVE:
                dbHelper.setStoryStarred(storyHash, true);
                break;

            case UNSAVE:
                dbHelper.setStoryStarred(storyHash, false);
                break;

            default:
                // not all actions have these, which is fine
        }
    }

}
