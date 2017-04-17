package com.newsblur.network;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.newsblur.R;
import com.newsblur.activity.Profile;
import com.newsblur.database.DatabaseConstants;
import com.newsblur.database.FeedProvider;
import com.newsblur.domain.Comment;
import com.newsblur.domain.Reply;
import com.newsblur.domain.Story;
import com.newsblur.domain.UserDetails;
import com.newsblur.domain.UserProfile;
import com.newsblur.fragment.ReplyDialogFragment;
import com.newsblur.util.ImageLoader;
import com.newsblur.util.PrefsUtils;
import com.newsblur.util.ViewUtils;
import com.newsblur.view.FlowLayout;

public class SetupCommentSectionTask extends AsyncTask<Void, Void, Void> {
	public static final String COMMENT_BY = "commentBy";
	public static final String COMMENT_DATE_BY = "commentDateBy";
	public static final String COMMENT_VIEW_BY = "commentViewBy";

	private ArrayList<View> publicCommentViews;
	private ArrayList<View> friendCommentViews;
	private final ContentResolver resolver;
	private final APIManager apiManager;

	private final Story story;
	private final LayoutInflater inflater;
	private final ImageLoader imageLoader;
	private WeakReference<View> viewHolder;
	private final Context context;
	private UserDetails user;
	private final FragmentManager manager;
	private Cursor commentCursor;

	public SetupCommentSectionTask(final Context context, final View view, final FragmentManager manager, LayoutInflater inflater, final ContentResolver resolver, final APIManager apiManager, final Story story, final ImageLoader imageLoader) {
		this.context = context;
		this.manager = manager;
		this.inflater = inflater;
		this.resolver = resolver;
		this.apiManager = apiManager;
		this.story = story;
		this.imageLoader = imageLoader;
		viewHolder = new WeakReference<View>(view);
		user = PrefsUtils.getUserDetails(context);
	}

	@Override
	protected Void doInBackground(Void... arg0) {

		commentCursor = resolver.query(FeedProvider.COMMENTS_URI, null, null, new String[] { story.id }, null);

		publicCommentViews = new ArrayList<View>();
		friendCommentViews = new ArrayList<View>();

		while (commentCursor.moveToNext()) {
			final Comment comment = Comment.fromCursor(commentCursor);
			
			// skip public comments if they are disabled
			if (!comment.byFriend && !PrefsUtils.showPublicComments(context)) {
			    continue;
			}

			Cursor userCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { comment.userId }, null);
			UserProfile commentUser = UserProfile.fromCursor(userCursor);
            userCursor.close();
            // rarely, we get a comment but never got the user's profile, so we can't display it
            if (commentUser == null) {
                Log.w(this.getClass().getName(), "cannot display comment from missing user ID: " + comment.userId);
                continue;
            }
			
			View commentView = inflater.inflate(R.layout.include_comment, null);
			commentView.setTag(COMMENT_VIEW_BY + comment.userId);

			TextView commentText = (TextView) commentView.findViewById(R.id.comment_text);

			commentText.setText(Html.fromHtml(comment.commentText));
			commentText.setTag(COMMENT_BY + comment.userId);

			ImageView commentImage = (ImageView) commentView.findViewById(R.id.comment_user_image);

			TextView commentSharedDate = (TextView) commentView.findViewById(R.id.comment_shareddate);
			commentSharedDate.setText(comment.sharedDate + " ago");
			commentSharedDate.setTag(COMMENT_DATE_BY + comment.userId);

			final FlowLayout favouriteContainer = (FlowLayout) commentView.findViewById(R.id.comment_favourite_avatars);
			final ImageView favouriteIcon = (ImageView) commentView.findViewById(R.id.comment_favourite_icon);
			final ImageView replyIcon = (ImageView) commentView.findViewById(R.id.comment_reply_icon);

			if (comment.likingUsers != null) {
				if (Arrays.asList(comment.likingUsers).contains(user.id)) {
					favouriteIcon.setImageResource(R.drawable.have_favourite);
				}

				for (String id : comment.likingUsers) {
					ImageView favouriteImage = new ImageView(context);

					Cursor likingUserCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { id }, null);
					UserProfile user = UserProfile.fromCursor(likingUserCursor);
                    likingUserCursor.close();

					imageLoader.displayImage(user.photoUrl, favouriteImage, 10f);
					favouriteImage.setTag(id);
					
					favouriteContainer.addView(favouriteImage);
				}

				favouriteIcon.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (!Arrays.asList(comment.likingUsers).contains(user.id)) {
							new LikeCommentTask(context, apiManager, favouriteIcon, favouriteContainer, story.id, comment, story.feedId, user.id).execute();
						} else {
							new UnLikeCommentTask(context, apiManager, favouriteIcon, favouriteContainer, story.id, comment, story.feedId, user.id).execute();
						}
					}
				});
			}

			replyIcon.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (story != null) {
						Cursor userCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { comment.userId }, null);
						UserProfile user = UserProfile.fromCursor(userCursor);
                        userCursor.close();

						DialogFragment newFragment = ReplyDialogFragment.newInstance(story, comment.userId, user.username);
						newFragment.show(manager, "dialog");
					}
				}
			});

			Cursor replies = resolver.query(FeedProvider.REPLIES_URI, null, null, new String[] { comment.id }, DatabaseConstants.REPLY_DATE + " DESC");
			while (replies.moveToNext()) {
				Reply reply = Reply.fromCursor(replies);
				
				View replyView = inflater.inflate(R.layout.include_reply, null);
				TextView replyText = (TextView) replyView.findViewById(R.id.reply_text);
				replyText.setText(Html.fromHtml(reply.text));
				ImageView replyImage = (ImageView) replyView.findViewById(R.id.reply_user_image);

				// occasionally there was no reply user and this caused a force close 
				Cursor replyCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { reply.userId }, null);
				if (replyCursor.getCount() > 0) {
					final UserProfile replyUser = UserProfile.fromCursor(replyCursor);
					imageLoader.displayImage(replyUser.photoUrl, replyImage);
					replyImage.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent i = new Intent(context, Profile.class);
							i.putExtra(Profile.USER_ID, replyUser.userId);
							context.startActivity(i);
						}
					});
					
					TextView replyUsername = (TextView) replyView.findViewById(R.id.reply_username);
					replyUsername.setText(replyUser.username);
				} else {
					TextView replyUsername = (TextView) replyView.findViewById(R.id.reply_username);
					replyUsername.setText(R.string.unknown_user);
				}
                replyCursor.close();
				
				TextView replySharedDate = (TextView) replyView.findViewById(R.id.reply_shareddate);
				replySharedDate.setText(reply.shortDate + " ago");

				((LinearLayout) commentView.findViewById(R.id.comment_replies_container)).addView(replyView);
			}
            replies.close();

			TextView commentUsername = (TextView) commentView.findViewById(R.id.comment_username);
			commentUsername.setText(commentUser.username);
			String userPhoto = commentUser.photoUrl;

            TextView commentLocation = (TextView) commentView.findViewById(R.id.comment_location);
            if (!TextUtils.isEmpty(commentUser.location)) {
                commentLocation.setText(commentUser.location.toUpperCase());
            } else {
                commentLocation.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(comment.sourceUserId)) {
				commentImage.setVisibility(View.INVISIBLE);
				ImageView usershareImage = (ImageView) commentView.findViewById(R.id.comment_user_reshare_image);
				ImageView sourceUserImage = (ImageView) commentView.findViewById(R.id.comment_sharesource_image);
				sourceUserImage.setVisibility(View.VISIBLE);
				usershareImage.setVisibility(View.VISIBLE);
				commentImage.setVisibility(View.INVISIBLE);


				Cursor sourceUserCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { comment.sourceUserId }, null);
				if (sourceUserCursor.getCount() > 0) {
					UserProfile sourceUser = UserProfile.fromCursor(sourceUserCursor);
					sourceUserCursor.close();

					imageLoader.displayImage(sourceUser.photoUrl, sourceUserImage, 10f);
					imageLoader.displayImage(userPhoto, usershareImage, 10f);
				}
                sourceUserCursor.close();
			} else {
				imageLoader.displayImage(userPhoto, commentImage, 10f);
			}

			if (comment.byFriend) {
				friendCommentViews.add(commentView);
			} else {
				publicCommentViews.add(commentView);
			}

			commentImage.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent i = new Intent(context, Profile.class);
					i.putExtra(Profile.USER_ID, comment.userId);
					context.startActivity(i);
				}
			});
		}

		return null;
	}

	protected void onPostExecute(Void result) {
		if (viewHolder.get() != null) {
			FlowLayout sharedGrid = (FlowLayout) viewHolder.get().findViewById(R.id.reading_social_shareimages);
			FlowLayout commentGrid = (FlowLayout) viewHolder.get().findViewById(R.id.reading_social_commentimages);

			TextView friendCommentTotal = ((TextView) viewHolder.get().findViewById(R.id.reading_friend_comment_total));
			TextView publicCommentTotal = ((TextView) viewHolder.get().findViewById(R.id.reading_public_comment_total));
			
			ViewUtils.setupCommentCount(context, viewHolder.get(), commentCursor.getCount());
			ViewUtils.setupShareCount(context, viewHolder.get(), story.sharedUserIds.length);

			ArrayList<String> commentIds = new ArrayList<String>();
			commentCursor.moveToFirst();
			for (int i = 0; i < commentCursor.getCount(); i++) {
				commentIds.add(commentCursor.getString(commentCursor.getColumnIndex(DatabaseConstants.COMMENT_USERID)));
				commentCursor.moveToNext();
			}

            for (final String userId : story.sharedUserIds) {
                if (!commentIds.contains(userId)) {
                    Cursor userCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { userId }, null);
                    if (userCursor.getCount() > 0) {
                        UserProfile user = UserProfile.fromCursor(userCursor);
                        userCursor.close();

                        ImageView image = ViewUtils.createSharebarImage(context, imageLoader, user.photoUrl, user.userId);
                        sharedGrid.addView(image);
                    }
                    userCursor.close();
                }
            }

			commentCursor.moveToFirst();

			for (int i = 0; i < commentCursor.getCount(); i++) {
				final Comment comment = Comment.fromCursor(commentCursor);

				Cursor userCursor = resolver.query(FeedProvider.USERS_URI, null, DatabaseConstants.USER_USERID + " IN (?)", new String[] { comment.userId }, null);
				UserProfile user = UserProfile.fromCursor(userCursor);
				userCursor.close();

				ImageView image = ViewUtils.createSharebarImage(context, imageLoader, user.photoUrl, user.userId);
				commentGrid.addView(image);
				commentCursor.moveToNext();
			}
			
			if (publicCommentViews.size() > 0) {
				String commentCount = context.getString(R.string.public_comment_count);
				if (publicCommentViews.size() == 1) {
					commentCount = commentCount.substring(0, commentCount.length() - 1);
				}
				publicCommentTotal.setText(String.format(commentCount, publicCommentViews.size()));
                viewHolder.get().findViewById(R.id.reading_public_comment_header).setVisibility(View.VISIBLE);
            }
			
			if (friendCommentViews.size() > 0) {
				String commentCount = context.getString(R.string.friends_comments_count);
				if (friendCommentViews.size() == 1) {
					commentCount = commentCount.substring(0, commentCount.length() - 1);
				}
				friendCommentTotal.setText(String.format(commentCount, friendCommentViews.size()));
                viewHolder.get().findViewById(R.id.reading_friend_comment_header).setVisibility(View.VISIBLE);
            }

			for (int i = 0; i < publicCommentViews.size(); i++) {
				if (i == publicCommentViews.size() - 1) {
					publicCommentViews.get(i).findViewById(R.id.comment_divider).setVisibility(View.GONE);
				}
				((LinearLayout) viewHolder.get().findViewById(R.id.reading_public_comment_container)).addView(publicCommentViews.get(i));
			}
			
			for (int i = 0; i < friendCommentViews.size(); i++) {
				if (i == friendCommentViews.size() - 1) {
					friendCommentViews.get(i).findViewById(R.id.comment_divider).setVisibility(View.GONE);
				}
				((LinearLayout) viewHolder.get().findViewById(R.id.reading_friend_comment_container)).addView(friendCommentViews.get(i));
			}
			
		}
		commentCursor.close();
	}
}


