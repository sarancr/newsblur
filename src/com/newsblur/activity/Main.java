package com.newsblur.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.net.Uri;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.TextView;

import com.newsblur.R;
import com.newsblur.fragment.FeedIntelligenceSelectorFragment;
import com.newsblur.fragment.FolderListFragment;
import com.newsblur.fragment.LogoutDialogFragment;
import com.newsblur.service.BootReceiver;
import com.newsblur.service.NBSyncService;
import com.newsblur.util.AppConstants;
import com.newsblur.util.FeedUtils;
import com.newsblur.util.PrefsUtils;
import com.newsblur.util.StateFilter;
import com.newsblur.util.UIUtils;
import com.newsblur.view.StateToggleButton.StateChangedListener;

public class Main extends NbActivity implements StateChangedListener, SwipeRefreshLayout.OnRefreshListener, AbsListView.OnScrollListener {

	private FolderListFragment folderFeedList;
	private FragmentManager fragmentManager;
    private TextView overlayStatusText;
    private boolean isLightTheme;
    private SwipeRefreshLayout swipeLayout;
    private boolean wasSwipeEnabled = false;

    @Override
	public void onCreate(Bundle savedInstanceState) {
        PreferenceManager.setDefaultValues(this, R.layout.activity_settings, false);

        isLightTheme = PrefsUtils.isLightThemeSelected(this);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

		setContentView(R.layout.activity_main);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        swipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(R.color.refresh_1, R.color.refresh_2, R.color.refresh_3, R.color.refresh_4);
        swipeLayout.setOnRefreshListener(this);

		fragmentManager = getFragmentManager();
		folderFeedList = (FolderListFragment) fragmentManager.findFragmentByTag("folderFeedListFragment");
		folderFeedList.setRetainInstance(true);
        ((FeedIntelligenceSelectorFragment) fragmentManager.findFragmentByTag("feedIntelligenceSelector")).setState(folderFeedList.currentState);

        this.overlayStatusText = (TextView) findViewById(R.id.main_sync_status);

        // make sure the interval sync is scheduled, since we are the root Activity
        BootReceiver.scheduleSyncService(this);
	}

    @Override
    protected void onResume() {
        super.onResume();

        // clear the read-this-session flag from stories so they don't show up in the wrong place
        FeedUtils.clearReadingSession();

        updateStatusIndicators();
        // this view doesn't show stories, it is safe to perform cleanup
        NBSyncService.holdStories(false);
        triggerSync();

        if (PrefsUtils.isLightThemeSelected(this) != isLightTheme) {
            UIUtils.restartActivity(this);
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

        MenuItem feedbackItem = menu.findItem(R.id.menu_feedback);
        if (AppConstants.ENABLE_FEEDBACK) {
            feedbackItem.setTitle(feedbackItem.getTitle() + " (v" + PrefsUtils.getVersion(this) + ")");
        } else {
            feedbackItem.setVisible(false);
        }

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_profile) {
			Intent profileIntent = new Intent(this, Profile.class);
			startActivity(profileIntent);
			return true;
		} else if (item.getItemId() == R.id.menu_refresh) {
            NBSyncService.forceFeedsFolders();
			triggerSync();
			return true;
		} else if (item.getItemId() == R.id.menu_add_feed) {
			Intent intent = new Intent(this, SearchForFeeds.class);
			startActivityForResult(intent, 0);
			return true;
		} else if (item.getItemId() == R.id.menu_logout) {
			DialogFragment newFragment = new LogoutDialogFragment();
			newFragment.show(getFragmentManager(), "dialog");
		} else if (item.getItemId() == R.id.menu_settings) {
            Intent settingsIntent = new Intent(this, Settings.class);
            startActivity(settingsIntent);
            return true;
        } else if (item.getItemId() == R.id.menu_feedback) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(PrefsUtils.createFeedbackLink(this)));
                startActivity(i);
            } catch (Exception e) {
                Log.wtf(this.getClass().getName(), "device cannot even open URLs to report feedback");
            }
            return true;
        }
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void changedState(StateFilter state) {
		folderFeedList.changeState(state);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			folderFeedList.hasUpdated();
		}
	}

    @Override
	public void handleUpdate() {
		folderFeedList.hasUpdated();
        updateStatusIndicators();
	}

    private void updateStatusIndicators() {
        if (NBSyncService.isFeedFolderSyncRunning()) {
            swipeLayout.setRefreshing(true);
        } else {
            swipeLayout.setRefreshing(false);
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
    public void onRefresh() {
        NBSyncService.forceFeedsFolders();
        triggerSync();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        // not required
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (swipeLayout != null) {
            boolean enable = (firstVisibleItem == 0);
            if (wasSwipeEnabled != enable) {
                swipeLayout.setEnabled(enable);
                wasSwipeEnabled = enable;
            }
        }
    }

}
