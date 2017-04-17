package com.newsblur.activity;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.newsblur.R;
import com.newsblur.database.DatabaseConstants;
import com.newsblur.fragment.SavedStoriesItemListFragment;
import com.newsblur.fragment.FeedItemListFragment;
import com.newsblur.util.DefaultFeedView;
import com.newsblur.util.FeedSet;
import com.newsblur.util.FeedUtils;
import com.newsblur.util.PrefConstants;
import com.newsblur.util.PrefsUtils;
import com.newsblur.util.ReadFilter;
import com.newsblur.util.StoryOrder;

public class SavedStoriesItemsList extends ItemsList {

	private ContentResolver resolver;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		setTitle(getResources().getString(R.string.saved_stories_title));

		resolver = getContentResolver();
		
		itemListFragment = (SavedStoriesItemListFragment) fragmentManager.findFragmentByTag(SavedStoriesItemListFragment.class.getName());
		if (itemListFragment == null) {
			itemListFragment = SavedStoriesItemListFragment.newInstance(getDefaultFeedView());
			itemListFragment.setRetainInstance(true);
			FragmentTransaction listTransaction = fragmentManager.beginTransaction();
			listTransaction.add(R.id.activity_itemlist_container, itemListFragment, SavedStoriesItemListFragment.class.getName());
			listTransaction.commit();
		}
	}

    @Override
    protected FeedSet createFeedSet() {
        return FeedSet.allSaved();
    }

	@Override
	public void markItemListAsRead() {
        ; // This activity has no mark-as-read action
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.savedstories_itemslist, menu);
        return true;
	}

    @Override
    protected DefaultFeedView getDefaultFeedView() {
        return PrefsUtils.getDefaultFeedViewForFolder(this, PrefConstants.SAVED_STORIES_FOLDER_NAME);
    }

    @Override
    public void defaultFeedViewChanged(DefaultFeedView value) {
        PrefsUtils.setDefaultFeedViewForFolder(this, PrefConstants.SAVED_STORIES_FOLDER_NAME, value);
        if (itemListFragment != null) {
            itemListFragment.setDefaultFeedView(value);
        }
    }

    // Note: the following four methods are required by our parent spec but are not
    // relevant since saved stories have no read/unread status nor ordering.

    @Override
    protected StoryOrder getStoryOrder() {
        return PrefsUtils.getStoryOrderForFolder(this, PrefConstants.ALL_STORIES_FOLDER_NAME);
    }

    @Override
    public void updateStoryOrderPreference(StoryOrder newValue) {
        PrefsUtils.setStoryOrderForFolder(this, PrefConstants.ALL_STORIES_FOLDER_NAME, newValue);
    }
    
    @Override
    protected void updateReadFilterPreference(ReadFilter newValue) {
        PrefsUtils.setReadFilterForFolder(this, PrefConstants.ALL_STORIES_FOLDER_NAME, newValue);
    }
    
    @Override
    protected ReadFilter getReadFilter() {
        return PrefsUtils.getReadFilterForFolder(this, PrefConstants.ALL_STORIES_FOLDER_NAME);
    }


}
