package com.newsblur.activity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;
import android.util.Log;

import com.newsblur.R;
import com.newsblur.database.DatabaseConstants;
import com.newsblur.fragment.FolderItemListFragment;
import com.newsblur.fragment.MarkAllReadDialogFragment;
import com.newsblur.fragment.MarkAllReadDialogFragment.MarkAllReadDialogListener;
import com.newsblur.util.AppConstants;
import com.newsblur.util.DefaultFeedView;
import com.newsblur.util.FeedSet;
import com.newsblur.util.FeedUtils;
import com.newsblur.util.PrefsUtils;
import com.newsblur.util.ReadFilter;
import com.newsblur.util.StateFilter;
import com.newsblur.util.StoryOrder;

public class FolderItemsList extends ItemsList implements MarkAllReadDialogListener {

	public static final String EXTRA_FOLDER_NAME = "folderName";
	private String folderName;

	@Override
	protected void onCreate(Bundle bundle) {
		folderName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);

        // note: onCreate triggers createFeedSet() so it has to wait until we have the folder name
		super.onCreate(bundle);

		setTitle(folderName);

		itemListFragment = (FolderItemListFragment) fragmentManager.findFragmentByTag(FolderItemListFragment.class.getName());
		if (itemListFragment == null) {
			itemListFragment = FolderItemListFragment.newInstance(folderName, currentState, getDefaultFeedView());
			itemListFragment.setRetainInstance(true);
			FragmentTransaction listTransaction = fragmentManager.beginTransaction();
			listTransaction.add(R.id.activity_itemlist_container, itemListFragment, FolderItemListFragment.class.getName());
			listTransaction.commit();
		}
	}

    @Override
    protected FeedSet createFeedSet() {
        return FeedSet.folder(this.folderName, dbHelper.getFeedsForFolder(folderName));
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.itemslist, menu);
		return true;
	}

	@Override
	public void markItemListAsRead() {
	    MarkAllReadDialogFragment dialog = MarkAllReadDialogFragment.newInstance(folderName);
	    dialog.show(fragmentManager, "dialog");
	}

    @Override
    public void onMarkAllRead() {
        super.markItemListAsRead();
    }

    @Override
    protected StoryOrder getStoryOrder() {
        return PrefsUtils.getStoryOrderForFolder(this, folderName);
    }

    @Override
    public void updateStoryOrderPreference(StoryOrder newValue) {
        PrefsUtils.setStoryOrderForFolder(this, folderName, newValue);
    }
    
    @Override
    protected void updateReadFilterPreference(ReadFilter newValue) {
        PrefsUtils.setReadFilterForFolder(this, folderName, newValue);
    }

    @Override
    protected ReadFilter getReadFilter() {
        return PrefsUtils.getReadFilterForFolder(this, folderName);
    }

    @Override
    protected DefaultFeedView getDefaultFeedView() {
        return PrefsUtils.getDefaultFeedViewForFolder(this, folderName);
    }

    @Override
    public void defaultFeedViewChanged(DefaultFeedView value) {
        PrefsUtils.setDefaultFeedViewForFolder(this, folderName, value);
        if (itemListFragment != null) {
            itemListFragment.setDefaultFeedView(value);
        }
    }

    @Override
    public void onCancel() {
        // do nothing
    }

}
