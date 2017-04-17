package com.newsblur.activity;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.newsblur.R;
import com.newsblur.domain.FeedResult;
import com.newsblur.fragment.AddFeedFragment;
import com.newsblur.network.SearchAsyncTaskLoader;
import com.newsblur.network.SearchLoaderResponse;

public class SearchForFeeds extends NbActivity implements LoaderCallbacks<SearchLoaderResponse>, OnItemClickListener {
    
    private static String SUPPORTED_URL_PROTOCOL = "http";

	private ListView resultsList;
	private Loader<SearchLoaderResponse> searchLoader;
	private FeedSearchResultAdapter adapter;

	@Override
	protected void onCreate(Bundle arg0) {
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(arg0);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setTitle(R.string.title_feed_search);
		setContentView(R.layout.activity_feed_search);
		
		TextView emptyView = (TextView) findViewById(R.id.empty_view);
		resultsList = (ListView) findViewById(R.id.feed_result_list);
		resultsList.setEmptyView(emptyView);
		resultsList.setOnItemClickListener(this);
		resultsList.setItemsCanFocus(false);
		searchLoader = getLoaderManager().initLoader(0, new Bundle(), this);
		
		onSearchRequested();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search, menu);
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);

            // test to see if a feed URL was passed rather than a search term
            if (tryAddByURL(query)) { return; }

			setProgressBarIndeterminateVisibility(true);
			
			Bundle bundle = new Bundle();
			bundle.putString(SearchAsyncTaskLoader.SEARCH_TERM, query);
			searchLoader = getLoaderManager().restartLoader(0, bundle, this);
			
			searchLoader.forceLoad();
		}
	}

    /**
     * See if the text entered in the query field was actually a URL so we can skip the
     * search step and just let users who know feed URLs directly subscribe.
     */
    private boolean tryAddByURL(String s) {
        URL u = null;
        try {
            u = new URL(s);
        } catch (MalformedURLException mue) {
            ; // this just signals that the string wasn't a URL, we will return
        }
        if (u == null) { return false; }
        if (!u.getProtocol().equals(SUPPORTED_URL_PROTOCOL)) { return false; };
        if ((u.getHost() == null) || (u.getHost().trim().isEmpty())) { return false; }

		DialogFragment addFeedFragment = AddFeedFragment.newInstance(s, s);
		addFeedFragment.show(getFragmentManager(), "dialog");
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_search) {
			onSearchRequested();
			return true;
		} else if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<SearchLoaderResponse> onCreateLoader(int loaderId, Bundle bundle) {
		String searchTerm = bundle.getString(SearchAsyncTaskLoader.SEARCH_TERM);
		return new SearchAsyncTaskLoader(this, searchTerm);
	}

	@Override
	public void onLoadFinished(Loader<SearchLoaderResponse> loader, SearchLoaderResponse results) {
		setProgressBarIndeterminateVisibility(false);
		if(!results.hasError()) {
			adapter = new FeedSearchResultAdapter(this, 0, 0, results.getResults());
			resultsList.setAdapter(adapter);
		} else {
			String message = results.getErrorMessage() == null ? "Error" : results.getErrorMessage();
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onLoaderReset(Loader<SearchLoaderResponse> loader) {
		
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		FeedResult result = adapter.getItem(position);
		DialogFragment addFeedFragment = AddFeedFragment.newInstance(result.url, result.label);
		addFeedFragment.show(getFragmentManager(), "dialog");
	}

}
