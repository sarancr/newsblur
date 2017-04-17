package com.newsblur.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.newsblur.R;
import com.newsblur.activity.Profile;
import com.newsblur.domain.Classifier;
import com.newsblur.fragment.ClassifierDialogFragment;
import com.newsblur.view.FlowLayout;

public class ViewUtils {

    private static Drawable tag_green_background, tag_red_background;
    private static int tag_green_text, tag_red_text;

	public static void setupShareCount(Context context, View storyView, int sharedUserCount) {
		String sharedBy = context.getResources().getString(R.string.reading_shared_count);
		TextView sharesText = (TextView) storyView.findViewById(R.id.shared_by);
		if (sharedUserCount > 0) {
			sharedBy = String.format(sharedBy, sharedUserCount);
			sharesText.setText(sharedUserCount > 1 ? sharedBy : sharedBy.substring(0, sharedBy.length() - 1));
		} else {
			sharesText.setVisibility(View.INVISIBLE);
		}

	}
	
	public static void setupCommentCount(Context context, View storyView, int sharedCommentCount) {
        if (context == null || context.getResources() == null) return;
		String commentsBy = context.getResources().getString(R.string.reading_comment_count);
		TextView sharesText = (TextView) storyView.findViewById(R.id.comment_by);
		if (sharedCommentCount > 0) {
			commentsBy = String.format(commentsBy, sharedCommentCount);
			sharesText.setText(sharedCommentCount > 1 ? commentsBy : commentsBy.substring(0, commentsBy.length() - 1));
		} else {
			sharesText.setVisibility(View.INVISIBLE);
		}
	}

	public static ImageView createSharebarImage(final Context context, final ImageLoader imageLoader, final String photoUrl, final String userId) {
		ImageView image = new ImageView(context);
		int imageLength = UIUtils.convertDPsToPixels(context, 15);
		image.setMaxHeight(imageLength);
		image.setMaxWidth(imageLength);
		
		FlowLayout.LayoutParams imageParameters = new FlowLayout.LayoutParams(5, 5);
		
		imageParameters.height = imageLength;
		imageParameters.width = imageLength;
		
		image.setMaxHeight(imageLength);
		image.setMaxWidth(imageLength);
		
		image.setLayoutParams(imageParameters);
		imageLoader.displayImage(photoUrl, image, 10f);
		image.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent i = new Intent(context, Profile.class);
				i.putExtra(Profile.USER_ID, userId);
				context.startActivity(i);
			}
		});
		return image;
	}

    public static void setupTags(Context context) {
        tag_green_text = context.getResources().getColor(R.color.tag_green_text);
        tag_red_text = context.getResources().getColor(R.color.tag_red_text);
        tag_green_background = context.getResources().getDrawable(R.drawable.tag_background_positive);
        tag_red_background = context.getResources().getDrawable(R.drawable.tag_background_negative);
    }

	public static View createTagView(final LayoutInflater inflater, final FragmentManager fragmentManager, final String tag, final Classifier classifier, final ClassifierDialogFragment.TagUpdateCallback callback, final String feedId) {
		
		View v = inflater.inflate(R.layout.tag_view, null);

		TextView tagText = (TextView) v.findViewById(R.id.tag_text);
        
		tagText.setText(tag);

		if (classifier != null && classifier.tags.containsKey(tag)) {
			switch (classifier.tags.get(tag)) {
			case Classifier.LIKE:
                setViewBackground(tagText, R.drawable.tag_background_positive);
                tagText.setTextColor(tag_green_text);
                break;
			case Classifier.DISLIKE:
                setViewBackground(tagText, R.drawable.tag_background_negative);
                tagText.setTextColor(tag_red_text);
                break;
			}
		}

		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				ClassifierDialogFragment classifierFragment = ClassifierDialogFragment.newInstance(callback, feedId, classifier, tag, Classifier.TAG);
				classifierFragment.show(fragmentManager, "dialog");
			}
		});

		return v;
	}

    /**
     * Sets the background resource of a view, working around a platform bug that causes the declared
     * padding to get reset.
     */
    public static void setViewBackground(View v, int resId) {
        // due to a framework bug, the below modification of background resource also resets the declared
        // padding on the view.  save a copy of said padding so it can be re-applied after the change.
        int oldPadL = v.getPaddingLeft();
        int oldPadT = v.getPaddingTop();
        int oldPadR = v.getPaddingRight();
        int oldPadB = v.getPaddingBottom();

        v.setBackgroundResource(resId);

        v.setPadding(oldPadL, oldPadT, oldPadR, oldPadB);
    }

    public static void showSystemUI(View view) {
        // Some layout/drawing artifacts as we don't use the FLAG_LAYOUT flags but otherwise the overlays wouldn't appear
        // and the action bar would overlap the content
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    public static void hideSystemUI(View view) {
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public static boolean isSystemUIHidden(View view) {
        return (view.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_IMMERSIVE) != 0;
    }

    public static boolean immersiveViewExitedViaSystemGesture(View view) {
        return view.getSystemUiVisibility() == (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
}
