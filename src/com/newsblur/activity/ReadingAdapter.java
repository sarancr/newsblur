package com.newsblur.activity;

import android.database.Cursor;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.newsblur.domain.Story;
import com.newsblur.fragment.LoadingFragment;
import com.newsblur.fragment.ReadingItemFragment;
import com.newsblur.util.DefaultFeedView;

import java.lang.ref.WeakReference;

public abstract class ReadingAdapter extends FragmentStatePagerAdapter {

	protected Cursor stories;
    protected DefaultFeedView defaultFeedView;
    protected String sourceUserId;
    private SparseArray<WeakReference<ReadingItemFragment>> cachedFragments;
	
	public ReadingAdapter(FragmentManager fm, DefaultFeedView defaultFeedView, String sourceUserId) {
		super(fm);
        this.cachedFragments = new SparseArray<WeakReference<ReadingItemFragment>>();
        this.defaultFeedView = defaultFeedView;
        this.sourceUserId = sourceUserId;
	}
	
	@Override
	public synchronized Fragment getItem(int position) {
		if (stories == null || stories.getCount() == 0 || position >= stories.getCount()) {
			return new LoadingFragment();
        } else {
            stories.moveToPosition(position);
            Story story = Story.fromCursor(stories);
            String tag = this.getClass().getName() + story.storyHash;
            ReadingItemFragment frag = getReadingItemFragment(story);
            return frag;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object o = super.instantiateItem(container, position);
        if (o instanceof ReadingItemFragment) {
            cachedFragments.put(position, new WeakReference((ReadingItemFragment) o));
        }
        return o;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        cachedFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public synchronized void swapCursor(Cursor cursor) {
        this.stories = cursor;
    }
        
	protected abstract ReadingItemFragment getReadingItemFragment(Story story);
	
	@Override
	public synchronized int getCount() {
		if (stories != null && stories.getCount() > 0) {
			return stories.getCount();
		} else {
			return 1;
		}
	}

	public synchronized Story getStory(int position) {
		if (stories == null || stories.isClosed() || stories.getColumnCount() == 0 || position >= stories.getCount() || position < 0) {
			return null;
		} else {
			stories.moveToPosition(position);
			return Story.fromCursor(stories);
		}
	}

    public synchronized int getPosition(Story story) {
        int pos = 0;
        while (pos < stories.getCount()) {
			stories.moveToPosition(pos);
            if (Story.fromCursor(stories).equals(story)) {
                return pos;
            }
            pos++;
        }
        return -1;
    }
	
	@Override
	public synchronized int getItemPosition(Object object) {
		if (object instanceof LoadingFragment) {
			return POSITION_NONE;
		} else {
			return POSITION_UNCHANGED;
		}
	}

    public String getSourceUserId() {
        return sourceUserId;
    }

    public synchronized ReadingItemFragment getExistingItem(int pos) {
        WeakReference<ReadingItemFragment> frag = cachedFragments.get(pos);
        if (frag == null) return null;
        return frag.get();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        // go one step further than the default pageradapter and also refresh the
        // story object inside each fragment we have active
        for (int i=0; i<stories.getCount(); i++) {
            WeakReference<ReadingItemFragment> frag = cachedFragments.get(i);
            if (frag == null) continue;
            ReadingItemFragment rif = frag.get();
            if (rif == null) continue;
            rif.offerStoryUpdate(getStory(i));
            rif.handleUpdate();
        }
    }
}
