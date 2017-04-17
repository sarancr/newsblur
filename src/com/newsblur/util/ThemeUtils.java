package com.newsblur.util;

import android.content.Context;

import com.newsblur.R;

/**
 * Created by mark on 20/05/2014.
 */
public class ThemeUtils {

    public static int getStoryTitleUnreadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_title_unread);
        } else {
            return getColor(context, R.color.dark_story_title_unread);
        }
    }

    private static int getColor(Context context, int id) {
        return context.getResources().getColor(id);
    }

    public static int getStoryTitleReadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_title_read);
        } else {
            return getColor(context, R.color.story_title_read);
        }
    }

    public static int getStoryContentUnreadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_content_unread);
        } else {
            return getColor(context, R.color.dark_story_content_unread);
        }
    }

    public static int getStoryContentReadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_content_read);
        } else {
            return getColor(context, R.color.story_content_read);
        }
    }

    public static int getStoryAuthorUnreadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_author_unread);
        } else {
            return getColor(context, R.color.story_author_unread);
        }
    }

    public static int getStoryAuthorReadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_author_read);
        } else {
            return getColor(context, R.color.story_author_read);
        }
    }

    public static int getStoryDateUnreadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_date_unread);
        } else {
            return getColor(context, R.color.dark_story_date_unread);
        }
    }

    public static int getStoryDateReadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_date_read);
        } else {
            return getColor(context, R.color.story_date_read);
        }
    }

    public static int getStoryFeedUnreadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_feed_unread);
        } else {
            return getColor(context, R.color.dark_story_feed_unread);
        }
    }

    public static int getStoryFeedReadColor(Context context) {
        if (PrefsUtils.isLightThemeSelected(context)) {
            return getColor(context, R.color.story_feed_read);
        } else {
            return getColor(context, R.color.story_feed_read);
        }
    }
}
