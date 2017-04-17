package com.newsblur.util;

public class AppConstants {

    // Enables high-volume logging that may be useful for debugging. This should
    // never be enabled for releases, as it not only slows down the app considerably,
    // it will log sensitive info such as passwords!
    public static final boolean VERBOSE_LOG = false;
    public static final boolean VERBOSE_LOG_DB = false;
    public static final boolean VERBOSE_LOG_NET = false;
	
	public static final int REGISTRATION_DEFAULT = 0;
	public static final int REGISTRATION_STARTED = 1;
	public static final int REGISTRATION_COMPLETED = 1;
	
	public static final String FOLDER_PRE = "folder_collapsed";

    // reading view font sizes, in em
	public static final float[] READING_FONT_SIZE = {0.75f, 0.9f, 1.0f, 1.2f, 1.5f, 2.0f};
	
    // the name to give the "root" folder in the local DB since the API does not assign it one.
    // this name should be unique and such that it will sort to the beginning of a list, ideally.
    public static final String ROOT_FOLDER = "0000_TOP_LEVEL_";

    public static final String LAST_APP_VERSION = "LAST_APP_VERSION";

    // a pref for the time we completed the last full sync of the feed/fodler list
    public static final String LAST_SYNC_TIME = "LAST_SYNC_TIME";

    // how long to wait before auto-syncing the feed/folder list
    public static final long AUTO_SYNC_TIME_MILLIS = 15L * 60L * 1000L;

    // how often to trigger the BG service. slightly longer than how often we will find new stories,
    // to account for the fact that it is approximate, and missing a cycle is bad.
    public static final long BG_SERVICE_CYCLE_MILLIS = AUTO_SYNC_TIME_MILLIS + 30L * 1000L;

    // how many total attemtps to make at a single API call
    public static final int MAX_API_TRIES = 3;

    // the base amount for how long to sleep during exponential API failure backoff
    public static final long API_BACKOFF_BASE_MILLIS = 500L;

    // when generating a request for multiple feeds, limit the total number requested to prevent
    // unworkably long URLs
    public static final int MAX_FEED_LIST_SIZE = 250;

    // when reading stories, how many stories worth of buffer to keep loaded ahead of the user
    public static final int READING_STORY_PRELOAD = 5;

    // max old stories to keep in the DB per feed before fetching new unreads
    public static final int MAX_READ_STORIES_STORED = 500;

    // how many unread stories to fetch via hash at a time
    public static final int UNREAD_FETCH_BATCH_SIZE = 50;

    // how many images to prefetch before updating the countdown UI
    public static final int IMAGE_PREFETCH_BATCH_SIZE = 10;

    // should the feedback link be enabled (read: is this a beta?)
    public static final boolean ENABLE_FEEDBACK = true;

    // link to app feedback page
    public static final String FEEDBACK_URL = "https://getsatisfaction.com/newsblur/topics/new?topic[style]=question&from=company&product=NewsBlur+Android+App&topic[additional_detail]=";

}
