package com.newsblur.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView.HitTestResult;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.newsblur.R;
import com.newsblur.activity.NewsBlurApplication;
import com.newsblur.activity.Reading;
import com.newsblur.domain.Classifier;
import com.newsblur.domain.Story;
import com.newsblur.domain.UserDetails;
import com.newsblur.network.APIManager;
import com.newsblur.network.SetupCommentSectionTask;
import com.newsblur.service.NBSyncService;
import com.newsblur.util.DefaultFeedView;
import com.newsblur.util.FeedUtils;
import com.newsblur.util.ImageCache;
import com.newsblur.util.ImageLoader;
import com.newsblur.util.PrefsUtils;
import com.newsblur.util.StoryUtils;
import com.newsblur.util.UIUtils;
import com.newsblur.util.ViewUtils;
import com.newsblur.view.FlowLayout;
import com.newsblur.view.NewsblurWebview;
import com.newsblur.view.NonfocusScrollview;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;

public class ReadingItemFragment extends NbFragment implements ClassifierDialogFragment.TagUpdateCallback, ShareDialogFragment.SharedCallbackDialog {

	public static final String TEXT_SIZE_CHANGED = "textSizeChanged";
	public static final String TEXT_SIZE_VALUE = "textSizeChangeValue";
	public Story story;
	private LayoutInflater inflater;
	private APIManager apiManager;
	private ImageLoader imageLoader;
	private String feedColor, feedTitle, feedFade, feedBorder, feedIconUrl, faviconText;
	private Classifier classifier;
	private ContentResolver resolver;
	private NewsblurWebview web;
	private BroadcastReceiver receiver;
	private TextView itemAuthors;
	private TextView itemFeed;
	private boolean displayFeedDetails;
	private FlowLayout tagContainer;
	private View view;
	private UserDetails user;
	public String previouslySavedShareText;
	private ImageView feedIcon;
    private Reading activity;
    private DefaultFeedView selectedFeedView;
    private String originalText;
    private HashMap<String,String> imageAltTexts;
    private HashMap<String,String> imageUrlRemaps;
    private String sourceUserId;
    private int contentHash;

    private final Object WEBVIEW_CONTENT_MUTEX = new Object();

	public static ReadingItemFragment newInstance(Story story, String feedTitle, String feedFaviconColor, String feedFaviconFade, String feedFaviconBorder, String faviconText, String faviconUrl, Classifier classifier, boolean displayFeedDetails, DefaultFeedView defaultFeedView, String sourceUserId) {
		ReadingItemFragment readingFragment = new ReadingItemFragment();

		Bundle args = new Bundle();
		args.putSerializable("story", story);
		args.putString("feedTitle", feedTitle);
		args.putString("feedColor", feedFaviconColor);
        args.putString("feedFade", feedFaviconFade);
        args.putString("feedBorder", feedFaviconBorder);
        args.putString("faviconText", faviconText);
		args.putString("faviconUrl", faviconUrl);
		args.putBoolean("displayFeedDetails", displayFeedDetails);
		args.putSerializable("classifier", classifier);
        args.putSerializable("defaultFeedView", defaultFeedView);
        args.putString("sourceUserId", sourceUserId);
		readingFragment.setArguments(args);

		return readingFragment;
	}

    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof Reading) {
            this.activity = (Reading) activity;
        }
        super.onAttach(activity);
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		imageLoader = ((NewsBlurApplication) getActivity().getApplicationContext()).getImageLoader();
		apiManager = new APIManager(getActivity());
		story = getArguments() != null ? (Story) getArguments().getSerializable("story") : null;

		resolver = getActivity().getContentResolver();
		inflater = getActivity().getLayoutInflater();
		
		displayFeedDetails = getArguments().getBoolean("displayFeedDetails");
		
		user = PrefsUtils.getUserDetails(getActivity());

		feedIconUrl = getArguments().getString("faviconUrl");
		feedTitle = getArguments().getString("feedTitle");
		feedColor = getArguments().getString("feedColor");
        feedFade = getArguments().getString("feedFade");
        feedBorder = getArguments().getString("feedBorder");
        faviconText = getArguments().getString("faviconText");

		classifier = (Classifier) getArguments().getSerializable("classifier");

        selectedFeedView = (DefaultFeedView)getArguments().getSerializable("defaultFeedView");

        sourceUserId = getArguments().getString("sourceUserId");

		receiver = new TextSizeReceiver();
		getActivity().registerReceiver(receiver, new IntentFilter(TEXT_SIZE_CHANGED));
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("story", story);
    }

	@Override
	public void onDestroy() {
		getActivity().unregisterReceiver(receiver);
        web.setOnTouchListener(null);
        view.setOnTouchListener(null);
        getActivity().getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
		super.onDestroy();
	}

    // WebViews don't automatically pause content like audio and video when they lose focus.  Chain our own
    // state into the webview so it behaves.
    @Override
    public void onPause() {
        if (this.web != null ) { this.web.onPause(); }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadStoryContent();
        if (this.web != null ) { this.web.onResume(); }
    }

	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_readingitem, null);

		web = (NewsblurWebview) view.findViewById(R.id.reading_webview);
        registerForContextMenu(web);

		setupItemMetadata();
		setupShareButton();
		setupSaveButton();

		if (story.sharedUserIds.length > 0 || story.commentCount > 0 ) {
			view.findViewById(R.id.reading_share_bar).setVisibility(View.VISIBLE);
			view.findViewById(R.id.share_bar_underline).setVisibility(View.VISIBLE);
			setupItemCommentsAndShares(view);
		}

        NonfocusScrollview scrollView = (NonfocusScrollview) view.findViewById(R.id.reading_scrollview);
        scrollView.registerScrollChangeListener(this.activity);

        setupImmersiveViewGestureDetector();

		return view;
	}

    private void setupImmersiveViewGestureDetector() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Change the system visibility on the decorview from the activity so that the state is maintained as we page through
            // fragments
            ImmersiveViewHandler immersiveViewHandler = new ImmersiveViewHandler(getActivity().getWindow().getDecorView());
            final GestureDetector gestureDetector = new GestureDetector(getActivity(), immersiveViewHandler);
            View.OnTouchListener touchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return gestureDetector.onTouchEvent(motionEvent);
                }
            };
            web.setOnTouchListener(touchListener);
            view.setOnTouchListener(touchListener);

            getActivity().getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(immersiveViewHandler);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        HitTestResult result = web.getHitTestResult();
        if (result.getType() == HitTestResult.IMAGE_TYPE ||
            result.getType() == HitTestResult.SRC_ANCHOR_TYPE ||
            result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE ) {
            // if the long-pressed item was an image, see if we can pop up a little dialogue
            // that presents the alt text.  Note that images wrapped in links tend to get detected
            // as anchors, not images, and may not point to the corresponding image URL.
            String imageURL = result.getExtra();
            imageURL = imageURL.replace("file://", "");
            String mappedURL = imageUrlRemaps.get(imageURL);
            final String finalURL = mappedURL == null ? imageURL : mappedURL;
            final String altText = imageAltTexts.get(finalURL);
            if (altText != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(finalURL);
                builder.setMessage(altText);
                builder.setPositiveButton(R.string.alert_dialog_openimage, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(finalURL));
                        startActivity(i);
                    }
                });
                builder.setNegativeButton(R.string.alert_dialog_done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ; // do nothing
                    }
                });
                builder.show();
            }
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }
	
	private void setupSaveButton() {
		final Button saveButton = (Button) view.findViewById(R.id.save_story_button);
        saveButton.setText(story.starred ? R.string.unsave_this : R.string.save_this);

        saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (story.starred) {
                    FeedUtils.setStorySaved(story, false, activity);
                } else {
                    FeedUtils.setStorySaved(story,true, activity);
                }
			}
		});
	}

    private void updateSaveButton() {
        if (view == null) { return; }
		Button saveButton = (Button) view.findViewById(R.id.save_story_button);
        if (saveButton == null) { return; }
        saveButton.setText(story.starred ? R.string.unsave_this : R.string.save_this);
    }

	private void setupShareButton() {
		Button shareButton = (Button) view.findViewById(R.id.share_story_button);

		for (String userId : story.sharedUserIds) {
			if (TextUtils.equals(userId, user.id)) {
				shareButton.setText(R.string.edit);
				break;
			}
		}

		shareButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment newFragment = ShareDialogFragment.newInstance(ReadingItemFragment.this, story, previouslySavedShareText, sourceUserId);
				newFragment.show(getFragmentManager(), "dialog");
			}
		});
	}

	private void setupItemCommentsAndShares(final View view) {
		new SetupCommentSectionTask(getActivity(), view, getFragmentManager(), inflater, resolver, apiManager, story, imageLoader).execute();
	}

	private void setupItemMetadata() {
        View feedHeader = view.findViewById(R.id.row_item_feed_header);
        View feedHeaderBorder = view.findViewById(R.id.item_feed_border);
        TextView itemTitle = (TextView) view.findViewById(R.id.reading_item_title);
        TextView itemDate = (TextView) view.findViewById(R.id.reading_item_date);
        itemAuthors = (TextView) view.findViewById(R.id.reading_item_authors);
        itemFeed = (TextView) view.findViewById(R.id.reading_feed_title);
        feedIcon = (ImageView) view.findViewById(R.id.reading_feed_icon);

		if (TextUtils.equals(feedColor, "#null") || TextUtils.equals(feedFade, "#null")) {
            feedColor = "#303030";
            feedFade = "#505050";
            feedBorder = "#202020";
        }

        int[] colors = {
            Color.parseColor(feedColor),
            Color.parseColor(feedFade)
        };
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                colors);
        feedHeader.setBackgroundDrawable(gradient);
        feedHeaderBorder.setBackgroundColor(Color.parseColor(feedBorder));

        if (TextUtils.equals(faviconText, "black")) {
            itemFeed.setTextColor(getActivity().getResources().getColor(R.color.darkgray));
            itemFeed.setShadowLayer(1, 0, 1, getActivity().getResources().getColor(R.color.half_white));
        } else {
            itemFeed.setTextColor(getActivity().getResources().getColor(R.color.white));
            itemFeed.setShadowLayer(1, 0, 1, getActivity().getResources().getColor(R.color.half_black));
        }

		if (!displayFeedDetails) {
			itemFeed.setVisibility(View.GONE);
			feedIcon.setVisibility(View.GONE);
		} else {
			imageLoader.displayImage(feedIconUrl, feedIcon, false);
			itemFeed.setText(feedTitle);
		}

        itemTitle.setText(Html.fromHtml(story.title));
        itemDate.setText(StoryUtils.formatLongDate(getActivity(), new Date(story.timestamp)));

        if (!TextUtils.isEmpty(story.authors)) {
            itemAuthors.setText("•   " + story.authors);
            if (classifier != null && classifier.authors.containsKey(story.authors)) {
                updateTagView(story.authors, Classifier.AUTHOR, classifier.authors.get(story.authors));
            }
        }

        if (story.tags.length <= 0) {
            tagContainer = (FlowLayout) view.findViewById(R.id.reading_item_tags);
            tagContainer.setVisibility(View.GONE);
        }

		itemAuthors.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ClassifierDialogFragment classifierFragment = ClassifierDialogFragment.newInstance(ReadingItemFragment.this, story.feedId, classifier, story.authors, Classifier.AUTHOR);
				classifierFragment.show(getFragmentManager(), "dialog");		
			}	
		});

		itemFeed.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ClassifierDialogFragment classifierFragment = ClassifierDialogFragment.newInstance(ReadingItemFragment.this, story.feedId, classifier, feedTitle, Classifier.FEED);
				classifierFragment.show(getFragmentManager(), "dialog");
			}
		});

		itemTitle.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(story.permalink));
				startActivity(i);
			}
		});

		setupTags();
	}

	private void setupTags() {
		tagContainer = (FlowLayout) view.findViewById(R.id.reading_item_tags);
        ViewUtils.setupTags(getActivity());
		for (String tag : story.tags) {
			View v = ViewUtils.createTagView(inflater, getFragmentManager(), tag, classifier, this, story.feedId);
			tagContainer.addView(v);
		}
	}

    public void switchSelectedFeedView() {
        synchronized (selectedFeedView) {
            // if we were already in text mode, switch back to story mode
            if (selectedFeedView == DefaultFeedView.TEXT) {
                selectedFeedView = DefaultFeedView.STORY;
            } else {
                selectedFeedView = DefaultFeedView.TEXT;
            }
            reloadStoryContent();
        }
    }

    public DefaultFeedView getSelectedFeedView() {
        return selectedFeedView;
    }

    private void reloadStoryContent() {
        if (selectedFeedView == DefaultFeedView.STORY) {
            setupWebview(story.content);
            enableProgress(false);
        } else {
            if (originalText == null) {
                enableProgress(true);
                loadOriginalText();
            } else {
                setupWebview(originalText);
                enableProgress(false);
            }
        }
    }

    private void enableProgress(boolean loading) {
        Activity parent = getActivity();
        if (parent == null) return;
        ((Reading) parent).enableLeftProgressCircle(loading);
    }

    /** 
     * Lets the pager offer us an updated version of our story when a new cursor is
     * cycled in. This class takes the responsibility of ensureing that the cursor
     * index has not shifted, though, by checking story IDs.
     */
    public void offerStoryUpdate(Story story) {
        if (story == null) return;
        if (! TextUtils.equals(story.storyHash, this.story.storyHash)) return;
        this.story = story;
    }

    public void handleUpdate() {
        updateSaveButton();
        reloadStoryContent();
    }

    private void loadOriginalText() {
        if (story != null) {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... arg) {
                    return FeedUtils.getStoryText(story.storyHash);
                }
                @Override
                protected void onPostExecute(String result) {
                    if (result != null) {
                        ReadingItemFragment.this.originalText = result;
                        reloadStoryContent();
                    } else {
                        if (getActivity() != null) setupWebview(getActivity().getResources().getString(R.string.orig_text_loading));
                        NBSyncService.getOriginalText(story.storyHash);
                        triggerSync();
                    }
                }
            }.execute();
        }
    }

	private void setupWebview(String storyText) {
        if (getActivity() == null) {
            // this method gets called by async UI bits that might hold stale fragment references with no assigned
            // activity.  If this happens, just abort the call.
            return;
        }

        synchronized (WEBVIEW_CONTENT_MUTEX) {
            // this method might get called repeatedly despite no content change, which is expensive
            int contentHash = storyText.hashCode();
            if (this.contentHash == contentHash) return;
            this.contentHash = contentHash;
            
            sniffAltTexts(storyText);

            if (PrefsUtils.isImagePrefetchEnabled(getActivity())) {
                storyText = swapInOfflineImages(storyText);
            } 

            float currentSize = PrefsUtils.getTextSize(getActivity());

            StringBuilder builder = new StringBuilder();
            builder.append("<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1, minimum-scale=1, user-scalable=0\" />");
            builder.append("<style style=\"text/css\">");
            builder.append(String.format("body { font-size: %sem; } ", Float.toString(currentSize)));
            builder.append("</style>");
            builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"reading.css\" />");
            if (PrefsUtils.isLightThemeSelected(getActivity())) {
                builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"light_reading.css\" />");
            } else {
                builder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"dark_reading.css\" />");
            }
            builder.append("</head><body><div class=\"NB-story\">");
            builder.append(storyText);
            builder.append("</div></body></html>");
            web.loadDataWithBaseURL("file:///android_asset/", builder.toString(), "text/html", "UTF-8", null);
        }
	}

    private void sniffAltTexts(String html) {
        // Find images with alt tags and cache the text for use on long-press
        //   NOTE: if doing this via regex has a smell, you have a good nose!  This method is far from perfect
        //   and may miss valid cases or trucate tags, but it works for popular feeds (read: XKCD) and doesn't
        //   require us to import a proper parser lib of hundreds of kilobytes just for this one feature.
        imageAltTexts = new HashMap<String,String>();
        imageUrlRemaps = new HashMap<String,String>();
        Matcher imgTagMatcher1 = Pattern.compile("<img[^>]*src=\"([^\"]*)\"[^>]*alt=\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (imgTagMatcher1.find()) {
            imageAltTexts.put(imgTagMatcher1.group(1), imgTagMatcher1.group(2));
        }
        Matcher imgTagMatcher2 = Pattern.compile("<img[^>]*alt=\"([^\"]*)\"[^>]*src=\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (imgTagMatcher2.find()) {
            imageAltTexts.put(imgTagMatcher2.group(2), imgTagMatcher2.group(1));
        }
    }

    private String swapInOfflineImages(String html) {
        ImageCache cache = new ImageCache(getActivity());

        Matcher imageTagMatcher = Pattern.compile("<img[^>]*(src\\s*=\\s*)\"([^\"]*)\"[^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        while (imageTagMatcher.find()) {
            String url = imageTagMatcher.group(2);
            String localPath = cache.getCachedLocation(url);
            if (localPath == null) continue;
            html = html.replace(imageTagMatcher.group(1)+"\""+url+"\"", "src=\""+localPath+"\"");
            imageUrlRemaps.put(localPath, url);
        }

        return html;
    }

	private class TextSizeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			web.setTextSize(intent.getFloatExtra(TEXT_SIZE_VALUE, 1.0f));
		}   
	}

	@Override
	public void updateTagView(String key, int classifierType, int classifierAction) {
		switch (classifierType) {
		case Classifier.AUTHOR:
			switch (classifierAction) {
			case Classifier.LIKE:
				itemAuthors.setTextColor(getActivity().getResources().getColor(R.color.positive));
				break;
			case Classifier.DISLIKE:
				itemAuthors.setTextColor(getActivity().getResources().getColor(R.color.negative));
				break;
			case Classifier.CLEAR_DISLIKE:
				itemAuthors.setTextColor(getActivity().getResources().getColor(R.color.half_darkgray));
				break;
			case Classifier.CLEAR_LIKE:
				itemAuthors.setTextColor(getActivity().getResources().getColor(R.color.half_darkgray));
				break;	
			}
			break;
		case Classifier.FEED:
			switch (classifierAction) {
			case Classifier.LIKE:
				itemFeed.setTextColor(getActivity().getResources().getColor(R.color.positive));
				break;
			case Classifier.DISLIKE:
				itemFeed.setTextColor(getActivity().getResources().getColor(R.color.negative));
				break;
			case Classifier.CLEAR_DISLIKE:
				itemFeed.setTextColor(getActivity().getResources().getColor(R.color.darkgray));
				break;
			case Classifier.CLEAR_LIKE:
				itemFeed.setTextColor(getActivity().getResources().getColor(R.color.darkgray));
				break;
			}
			break;
		case Classifier.TAG:
			classifier.tags.put(key, classifierAction);
			tagContainer.removeAllViews();
			setupTags();
			break;	
		}
	}

	@Override
	public void sharedCallback(String sharedText, boolean hasAlreadyBeenShared) {
		view.findViewById(R.id.reading_share_bar).setVisibility(View.VISIBLE);
		view.findViewById(R.id.share_bar_underline).setVisibility(View.VISIBLE);
		
		if (!hasAlreadyBeenShared) {
			
			if (!TextUtils.isEmpty(sharedText)) {
				View commentView = inflater.inflate(R.layout.include_comment, null);
				commentView.setTag(SetupCommentSectionTask.COMMENT_VIEW_BY + user.id);

                TextView commentText = (TextView) commentView.findViewById(R.id.comment_text);
                commentText.setTag("commentBy" + user.id);
                commentText.setText(sharedText);

                TextView commentLocation = (TextView) commentView.findViewById(R.id.comment_location);
                if (!TextUtils.isEmpty(user.location)) {
                    commentLocation.setText(user.location.toUpperCase());
                } else {
                    commentLocation.setVisibility(View.GONE);
                }

                if (PrefsUtils.getUserImage(getActivity()) != null) {
					ImageView commentImage = (ImageView) commentView.findViewById(R.id.comment_user_image);
					commentImage.setImageBitmap(UIUtils.roundCorners(PrefsUtils.getUserImage(getActivity()), 10f));
				}

				TextView commentSharedDate = (TextView) commentView.findViewById(R.id.comment_shareddate);
				commentSharedDate.setText(R.string.now);

				TextView commentUsername = (TextView) commentView.findViewById(R.id.comment_username);
				commentUsername.setText(user.username);

				((LinearLayout) view.findViewById(R.id.reading_friend_comment_container)).addView(commentView);

				ViewUtils.setupCommentCount(getActivity(), view, story.commentCount + 1);
				
				final ImageView image = ViewUtils.createSharebarImage(getActivity(), imageLoader, user.photoUrl, user.id);
				((FlowLayout) view.findViewById(R.id.reading_social_commentimages)).addView(image);
				
			} else {
				ViewUtils.setupShareCount(getActivity(), view, story.sharedUserIds.length + 1);
				final ImageView image = ViewUtils.createSharebarImage(getActivity(), imageLoader, user.photoUrl, user.id);
				((FlowLayout) view.findViewById(R.id.reading_social_shareimages)).addView(image);
			}
		} else {
			View commentViewForUser = view.findViewWithTag(SetupCommentSectionTask.COMMENT_VIEW_BY + user.id);
			TextView commentText = (TextView) view.findViewWithTag(SetupCommentSectionTask.COMMENT_BY + user.id);
			commentText.setText(sharedText);

			TextView commentDateText = (TextView) view.findViewWithTag(SetupCommentSectionTask.COMMENT_DATE_BY + user.id);
			commentDateText.setText(R.string.now);
		}
	}


	@Override
	public void setPreviouslySavedShareText(String previouslySavedShareText) {
		this.previouslySavedShareText = previouslySavedShareText;
	}

    private class ImmersiveViewHandler extends GestureDetector.SimpleOnGestureListener implements View.OnSystemUiVisibilityChangeListener {
        private View view;

        public ImmersiveViewHandler(View view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (web.wasLinkClicked()) {
                // Clicked a link so ignore immersive view
                return super.onSingleTapUp(e);
            }

            if (ViewUtils.isSystemUIHidden(view)) {
                ViewUtils.showSystemUI(view);
            } else if (PrefsUtils.enterImmersiveReadingModeOnSingleTap(getActivity())) {
                ViewUtils.hideSystemUI(view);
            }

            return super.onSingleTapUp(e);
        }

        @Override
        public void onSystemUiVisibilityChange(int i) {
            // If immersive view has been exited via a system gesture we want to ensure that it gets resized
            // in the same way as using tap to exit.
            if (ViewUtils.immersiveViewExitedViaSystemGesture(view)) {
                ViewUtils.showSystemUI(view);
            }
        }
    }
}
