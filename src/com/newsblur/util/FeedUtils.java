package com.newsblur.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.newsblur.R;
import com.newsblur.activity.NbActivity;
import com.newsblur.database.BlurDatabaseHelper;
import com.newsblur.database.DatabaseConstants;
import com.newsblur.database.FeedProvider;
import com.newsblur.domain.Classifier;
import com.newsblur.domain.Feed;
import com.newsblur.domain.SocialFeed;
import com.newsblur.domain.Story;
import com.newsblur.domain.ValueMultimap;
import com.newsblur.network.APIManager;
import com.newsblur.network.domain.NewsBlurResponse;
import com.newsblur.service.NBSyncService;
import com.newsblur.util.AppConstants;

public class FeedUtils {

    private static BlurDatabaseHelper dbHelper;

    public static void offerDB(BlurDatabaseHelper _dbHelper) {
        if (_dbHelper.isOpen()) {
            dbHelper = _dbHelper;
        }
    }

    private static void triggerSync(Context c) {
        Intent i = new Intent(c, NBSyncService.class);
        c.startService(i);
    }

    public static void dropAndRecreateTables() {
        dbHelper.dropAndRecreateTables();
    }

	public static void setStorySaved(final Story story, final boolean saved, final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                ReadingAction ra = (saved ? ReadingAction.saveStory(story.storyHash) : ReadingAction.unsaveStory(story.storyHash));
                ra.doLocal(dbHelper);
                NbActivity.updateAllActivities();
                dbHelper.enqueueAction(ra);
                triggerSync(context);
                return null;
            }
        }.execute();
    }

    public static void deleteFeed(final String feedId, final String folderName, final Context context, final APIManager apiManager) {
        new AsyncTask<Void, Void, NewsBlurResponse>() {
            @Override
            protected NewsBlurResponse doInBackground(Void... arg) {
                return apiManager.deleteFeed(feedId, folderName);
            }
            @Override
            protected void onPostExecute(NewsBlurResponse result) {
                // TODO: we can't check result.isError() because the delete call sets the .message property on all calls. find a better error check
                dbHelper.deleteFeed(feedId);
                NbActivity.updateAllActivities();
                Toast.makeText(context, R.string.toast_feed_deleted, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    public static void clearReadingSession() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                NBSyncService.resetFeeds();
                try {
                    dbHelper.clearReadingSession();
                } catch (Exception e) {
                    ; // this one call can evade the on-upgrade DB wipe and throw exceptions
                }
                return null;
            }
        }.execute();
    }

    public static void markStoryUnread(final Story story, final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                setStoryReadState(story, context, false);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void markStoryAsRead(final Story story, final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                setStoryReadState(story, context, true);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static void setStoryReadState(Story story, Context context, boolean read) {
        if (story.read == read) { return; }

        // update the local object to show as read before DB is touched
        story.read = read;
        
        // update unread state and unread counts in the local DB
        dbHelper.setStoryReadState(story, read);
        NbActivity.updateAllActivities();

        // tell the sync service we need to mark read
        ReadingAction ra = (read ? ReadingAction.markStoryRead(story.storyHash) : ReadingAction.markStoryUnread(story.storyHash));
        dbHelper.enqueueAction(ra);
        triggerSync(context);
    }

    public static void markFeedsRead(final FeedSet fs, final Long olderThan, final Long newerThan, final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... arg) {
                ReadingAction ra = ReadingAction.markFeedRead(fs, olderThan, newerThan);
                if (fs.isAllNormal() && (olderThan != null || newerThan != null)) {
                    // the mark-all-read API doesn't support range bounding, so we need to pass each and every
                    // feed ID to the API instead.
                    FeedSet newFeedSet = FeedSet.folder("all", dbHelper.getAllFeeds());
                    ra = ReadingAction.markFeedRead(newFeedSet, olderThan, newerThan);
                }
                dbHelper.markStoriesRead(fs, olderThan, newerThan);
                NbActivity.updateAllActivities();
                dbHelper.enqueueAction(ra);
                triggerSync(context);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void updateClassifier(final String feedId, final String key, final Classifier classifier, final int classifierType, final int classifierAction, final Context context) {

        // first, update the server
        new AsyncTask<Void, Void, NewsBlurResponse>() {
            @Override
            protected NewsBlurResponse doInBackground(Void... arg) {
                APIManager apiManager = new APIManager(context);
                return apiManager.trainClassifier(feedId, key, classifierType, classifierAction);
            }
            @Override
            protected void onPostExecute(NewsBlurResponse result) {
                if (result.isError()) {
                    Toast.makeText(context, result.getErrorMessage(context.getString(R.string.error_saving_classifier)), Toast.LENGTH_LONG).show();
                }
            }
        }.execute();

        // next, update the local DB
        classifier.getMapForType(classifierType).put(key, classifierAction);
        Uri classifierUri = FeedProvider.CLASSIFIER_URI.buildUpon().appendPath(feedId).build();
        try {
            // TODO: for feeds with many classifiers, this could be much faster by targeting just the row that changed
			context.getContentResolver().delete(classifierUri, null, null);
			for (ContentValues classifierValues : classifier.getContentValues()) {
                context.getContentResolver().insert(classifierUri, classifierValues);
            }
        } catch (Exception e) {
            Log.w(FeedUtils.class.getName(), "Could not update classifier in local storage.", e);
        }

    }

    public static void shareStory(Story story, Context context) {
        if (story == null ) { return; } 
        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_SUBJECT, Html.fromHtml(story.title));
        final String shareString = context.getResources().getString(R.string.share);
        intent.putExtra(Intent.EXTRA_TEXT, String.format(shareString, new Object[] { Html.fromHtml(story.title),
                                                                                       story.permalink }));
        context.startActivity(Intent.createChooser(intent, "Share using"));
    }

    public static FeedSet feedSetFromFolderName(String folderName, Context context) {
        Set<String> feedIds = dbHelper.getFeedsForFolder(folderName);
        return FeedSet.folder(folderName, feedIds);
    }

    public static String getStoryText(String hash) {
        return dbHelper.getStoryText(hash);
    }

    /**
     * Infer the feed ID for a story from the story's hash.  Useful for APIs
     * that takes a feed ID and story ID and only the story hash is known.
     *
     * TODO: this has a smell to it. can't all APIs just accept story hashes?
     */
    public static String inferFeedId(String storyHash) {
        String[] parts = TextUtils.split(storyHash, ":");
        if (parts.length != 2) return null;
        return parts[0];
    }

}
