package org.briarproject.briar.android.blog;

import android.app.Application;

import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.TransactionManager;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.identity.LocalAuthor;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.MessageId;
import org.briarproject.bramble.api.sync.event.GroupRemovedEvent;
import org.briarproject.bramble.api.system.AndroidExecutor;
import org.briarproject.briar.android.viewmodel.LiveResult;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.blog.Blog;
import org.briarproject.briar.api.blog.BlogInvitationResponse;
import org.briarproject.briar.api.blog.BlogManager;
import org.briarproject.briar.api.blog.event.BlogInvitationResponseReceivedEvent;
import org.briarproject.briar.api.blog.event.BlogPostAddedEvent;
import org.briarproject.briar.api.sharing.event.ContactLeftShareableEvent;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;

@NotNullByDefault
class BlogViewModel extends BaseViewModel {

	private static final Logger LOG = getLogger(BlogViewModel.class.getName());

	// implicitly non-null
	private volatile GroupId groupId = null;

	private final MutableLiveData<BlogItem> blog = new MutableLiveData<>();
	private final MutableLiveData<Boolean> blogRemoved =
			new MutableLiveData<>();

	@Inject
	BlogViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager,
			TransactionManager db,
			AndroidExecutor androidExecutor,
			EventBus eventBus,
			IdentityManager identityManager,
			AndroidNotificationManager notificationManager,
			BlogManager blogManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor,
				eventBus, identityManager, notificationManager, blogManager);
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof BlogPostAddedEvent) {
			BlogPostAddedEvent b = (BlogPostAddedEvent) e;
			if (b.getGroupId().equals(groupId)) {
				LOG.info("Blog post added");
				onBlogPostAdded(b.getHeader(), b.isLocal());
			}
		} else if (e instanceof BlogInvitationResponseReceivedEvent) {
			BlogInvitationResponseReceivedEvent b =
					(BlogInvitationResponseReceivedEvent) e;
			BlogInvitationResponse r = b.getMessageHeader();
			if (r.getShareableId().equals(groupId) && r.wasAccepted()) {
				LOG.info("Blog invitation accepted");
				// TODO
//				onBlogInvitationAccepted(b.getContactId());
// 				sharingController.add(c);
			}
		} else if (e instanceof ContactLeftShareableEvent) {
			ContactLeftShareableEvent s = (ContactLeftShareableEvent) e;
			if (s.getGroupId().equals(groupId)) {
				LOG.info("Blog left by contact");
				// TODO
//				onBlogLeft(s.getContactId());
// 				sharingController.remove(c);
			}
		} else if (e instanceof GroupRemovedEvent) {
			GroupRemovedEvent g = (GroupRemovedEvent) e;
			if (g.getGroup().getId().equals(groupId)) {
				LOG.info("Blog removed");
				blogRemoved.setValue(true);
			}
		}
	}

	/**
	 * Set this before calling any other methods.
	 */
	public void setGroupId(GroupId groupId) {
		this.groupId = groupId;
		loadBlog(groupId);
		loadBlogPosts(groupId);
	}

	private void loadBlog(GroupId groupId) {
		runOnDbThread(() -> {
			try {
				long start = now();
				LocalAuthor a = identityManager.getLocalAuthor();
				Blog b = blogManager.getBlog(groupId);
				boolean ours = a.getId().equals(b.getAuthor().getId());
				boolean removable = blogManager.canBeRemoved(b);
				blog.postValue(new BlogItem(b, ours, removable));
				logDuration(LOG, "Loading blog", start);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	void blockNotifications() {
		notificationManager.blockNotification(groupId);
	}

	void clearBlogPostNotifications() {
		notificationManager.clearBlogPostNotification(groupId);
	}

	void unblockNotifications() {
		notificationManager.unblockNotification(groupId);
	}

	void loadBlogPosts(GroupId groupId) {
		loadList(txn -> loadBlogPosts(txn, groupId), this::updateBlogPosts);
	}

	void deleteBlog() {
		runOnDbThread(() -> {
			try {
				long start = now();
				Blog b = blogManager.getBlog(groupId);
				blogManager.removeBlog(b);
				logDuration(LOG, "Removing blog", start);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	LiveData<LiveResult<BlogPostItem>> loadBlogPost(MessageId m) {
		return loadBlogPost(groupId, m);
	}

	LiveData<BlogItem> getBlog() {
		return blog;
	}

	LiveData<Boolean> getBlogRemoved() {
		return blogRemoved;
	}

}