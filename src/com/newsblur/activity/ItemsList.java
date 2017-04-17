package com.newsblur.activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.newsblur.R;
import com.newsblur.fragment.DefaultFeedViewDialogFragment;
import com.newsblur.fragment.ItemListFragment;
import com.newsblur.fragment.ReadFilterDialogFragment;
import com.newsblur.fragment.StoryOrderDialogFragment;
import com.newsblur.service.NBSyncService;
import com.newsblur.util.AppConstants;
import com.newsblur.util.DefaultFeedView;
import com.newsblur.util.DefaultFeedViewChangedListener;
import com.newsblur.util.FeedSet;
import com.newsblur.util.FeedUtils;
import com.newsblur.util.ReadFilter;
import com.newsblur.util.ReadFilterChangedListener;
import com.newsblur.util.StateFilter;
import com.newsblur.util.StoryOrder;
import com.newsblur.util.StoryOrderChangedListener;
import com.newsblur.view.StateToggleButton.StateChangedListener;

public abstract class ItemsList extends NbActivity implements StateChangedListener, StoryOrderChangedListener, ReadFilterChangedListener, DefaultFeedViewChangedListener {

	public static final String EXTRA_STATE = "currentIntelligenceState";
	private static final String STORY_ORDER = "storyOrder";
	private static final String READ_FILTER = "readFilter";
    private static final String DEFAULT_FEED_VIEW = "defaultFeedView";
    public static final String BUNDLE_FEED_IDS = "feedIds";

	protected ItemListFragment itemListFragment;
	protected FragmentManager fragmentManager;
    private TextView overlayStatusText;
	protected StateFilter currentState;

    private FeedSet fs;
	
	protected boolean stopLoading = false;

	@Override
    protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);

        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);

        // our intel state is entirely determined by the state of the Main view
		currentState = (StateFilter) getIntent().getSerializableExtra(EXTRA_STATE);
        this.fs = createFeedSet();

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		setContentView(R.layout.activity_itemslist);
		fragmentManager = getFragmentManager();

		getActionBar().setDisplayHomeAsUpEnabled(true);

        this.overlayStatusText = (TextView) findViewById(R.id.itemlist_sync_status);
	}

    protected abstract FeedSet createFeedSet();

    public FeedSet getFeedSet() {
        return this.fs;
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusIndicators();
        stopLoading = false;
        // this view shows stories, it is not safe to perform cleanup
        NBSyncService.holdStories(true);
        // Reading activities almost certainly changed the read/unread state of some stories. Ensure
        // we reflect those changes promptly.
        itemListFragment.hasUpdated();
    }

    private void getFirstStories() {
        stopLoading = false;
        triggerRefresh(AppConstants.READING_STORY_PRELOAD, 0);
    }

    @Override
    protected void onPause() {
        stopLoading = true;
        NBSyncService.holdStories(false);
        super.onPause();
    }

    public void triggerRefresh(int desiredStoryCount, int totalSeen) {
        if (!stopLoading) {
            boolean gotSome = NBSyncService.requestMoreForFeed(fs, desiredStoryCount, totalSeen);
            if (gotSome) triggerSync();
            updateStatusIndicators();
        }
    }

	public void markItemListAsRead() {
        FeedUtils.markFeedsRead(fs, null, null, this);
        Toast.makeText(this, R.string.toast_marked_stories_as_read, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.menu_mark_all_as_read) {
			markItemListAsRead();
			return true;
		} else if (item.getItemId() == R.id.menu_story_order) {
            StoryOrder currentValue = getStoryOrder();
            StoryOrderDialogFragment storyOrder = StoryOrderDialogFragment.newInstance(currentValue);
            storyOrder.show(getFragmentManager(), STORY_ORDER);
            return true;
        } else if (item.getItemId() == R.id.menu_read_filter) {
            ReadFilter currentValue = getReadFilter();
            ReadFilterDialogFragment readFilter = ReadFilterDialogFragment.newInstance(currentValue);
            readFilter.show(getFragmentManager(), READ_FILTER);
            return true;
        } else if (item.getItemId() == R.id.menu_default_view) {
            DefaultFeedView currentValue = getDefaultFeedView();
            DefaultFeedViewDialogFragment readFilter = DefaultFeedViewDialogFragment.newInstance(currentValue);
            readFilter.show(getFragmentManager(), DEFAULT_FEED_VIEW);
            return true;
        }
	
		return false;
	}
	
    // TODO: can all of these be replaced with PrefsUtils queries via FeedSet?
	protected abstract StoryOrder getStoryOrder();
	
	protected abstract ReadFilter getReadFilter();

    protected abstract DefaultFeedView getDefaultFeedView();
	
    @Override
	public void handleUpdate() {
        updateStatusIndicators();
		if (itemListFragment != null) {
			itemListFragment.hasUpdated();
        }
    }

    private void updateStatusIndicators() {
        boolean isLoading = NBSyncService.isFeedSetSyncing(this.fs);
        setProgressBarIndeterminateVisibility(isLoading);
		if (itemListFragment != null) {
			itemListFragment.setLoading(isLoading);
        }

        if (overlayStatusText != null) {
            String syncStatus = NBSyncService.getSyncStatusMessage();
            if (syncStatus != null)  {
                overlayStatusText.setText(syncStatus);
                overlayStatusText.setVisibility(View.VISIBLE);
            } else {
                overlayStatusText.setVisibility(View.GONE);
            }
        }
    }

	@Override
	public void changedState(StateFilter state) {
		itemListFragment.changeState(state);
	}
	
	@Override
    public void storyOrderChanged(StoryOrder newValue) {
        updateStoryOrderPreference(newValue);
        FeedUtils.clearReadingSession(); 
        itemListFragment.resetEmptyState();
        itemListFragment.hasUpdated();
        itemListFragment.scrollToTop();
        getFirstStories();
    }
	
	public abstract void updateStoryOrderPreference(StoryOrder newValue);

    @Override
    public void readFilterChanged(ReadFilter newValue) {
        updateReadFilterPreference(newValue);
        FeedUtils.clearReadingSession(); 
        itemListFragment.resetEmptyState();
        itemListFragment.hasUpdated();
        itemListFragment.scrollToTop();
        getFirstStories();
    }

    protected abstract void updateReadFilterPreference(ReadFilter newValue);

    @Override
    public void finish() {
        super.finish();
        /*
         * Animate out the list by sliding it to the right and the Main activity in from
         * the left.  Do this when going back to Main as a subtle hint to the swipe gesture,
         * to make the gesture feel more natural, and to override the really ugly transition
         * used in some of the newer platforms.
         */
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }
}
