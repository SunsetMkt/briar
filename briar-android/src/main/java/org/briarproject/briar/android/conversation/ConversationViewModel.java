package org.briarproject.briar.android.conversation;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.crypto.CryptoExecutor;
import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchContactException;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.sync.GroupId;
import org.briarproject.bramble.api.sync.Message;
import org.briarproject.briar.android.util.UiUtils;
import org.briarproject.briar.api.messaging.Attachment;
import org.briarproject.briar.api.messaging.AttachmentHeader;
import org.briarproject.briar.api.messaging.MessagingManager;
import org.briarproject.briar.api.messaging.PrivateMessage;
import org.briarproject.briar.api.messaging.PrivateMessageFactory;
import org.briarproject.briar.api.messaging.PrivateMessageHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import static java.util.Objects.requireNonNull;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.IoUtils.tryToClose;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;
import static org.briarproject.briar.android.util.UiUtils.observeForeverOnce;

@NotNullByDefault
public class ConversationViewModel extends AndroidViewModel {

	private static Logger LOG =
			getLogger(ConversationViewModel.class.getName());

	@DatabaseExecutor
	private final Executor dbExecutor;
	@CryptoExecutor
	private final Executor cryptoExecutor;
	private final MessagingManager messagingManager;
	private final ContactManager contactManager;
	private final PrivateMessageFactory privateMessageFactory;
	private final AttachmentController attachmentController;

	@Nullable
	private ContactId contactId = null;
	private final MutableLiveData<Contact> contact = new MutableLiveData<>();
	private final LiveData<AuthorId> contactAuthorId =
			Transformations.map(contact, c -> c.getAuthor().getId());
	private final LiveData<String> contactName =
			Transformations.map(contact, UiUtils::getContactDisplayName);
	private final MutableLiveData<Boolean> contactDeleted =
			new MutableLiveData<>();
	private final MutableLiveData<GroupId> messagingGroupId =
			new MutableLiveData<>();
	private final MutableLiveData<PrivateMessageHeader> addedHeader =
			new MutableLiveData<>();

	@Inject
	ConversationViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			@CryptoExecutor Executor cryptoExecutor,
			MessagingManager messagingManager,
			ContactManager contactManager,
			PrivateMessageFactory privateMessageFactory) {
		super(application);
		this.dbExecutor = dbExecutor;
		this.cryptoExecutor = cryptoExecutor;
		this.messagingManager = messagingManager;
		this.contactManager = contactManager;
		this.privateMessageFactory = privateMessageFactory;
		this.attachmentController = new AttachmentController(messagingManager,
				application.getResources());
		contactDeleted.setValue(false);
	}

	void setContactId(ContactId contactId) {
		if (this.contactId == null) {
			this.contactId = contactId;
			loadContact();
		} else if (!contactId.equals(this.contactId)) {
			throw new IllegalStateException();
		}
	}

	private void loadContact() {
		dbExecutor.execute(() -> {
			try {
				long start = now();
				Contact c =
						contactManager.getContact(requireNonNull(contactId));
				contact.postValue(c);
				logDuration(LOG, "Loading contact", start);
			} catch (NoSuchContactException e) {
				contactDeleted.postValue(true);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	void setContactAlias(String alias) {
		dbExecutor.execute(() -> {
			try {
				contactManager.setContactAlias(requireNonNull(contactId),
						alias.isEmpty() ? null : alias);
				loadContact();
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	void sendMessage(@Nullable String text, List<Uri> uris, long timestamp) {
		if (messagingGroupId.getValue() == null) loadGroupId();
		observeForeverOnce(messagingGroupId, groupId -> {
			if (groupId == null) return;
			// calls through to creating and storing the message
			storeAttachments(groupId, text, uris, timestamp);
		});
	}

	private void loadGroupId() {
		if (contactId == null) throw new IllegalStateException();
		dbExecutor.execute(() -> {
			try {
				messagingGroupId.postValue(
						messagingManager.getConversationId(contactId));
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	private void storeAttachments(GroupId groupId, @Nullable String text,
			List<Uri> uris, long timestamp) {
		dbExecutor.execute(() -> {
			long start = now();
			List<AttachmentHeader> attachments = new ArrayList<>();
			List<AttachmentItem> items = new ArrayList<>();
			for (Uri uri : uris) {
				Pair<AttachmentHeader, AttachmentItem> pair =
						createAttachmentHeader(groupId, uri, timestamp);
				if (pair == null) continue;
				attachments.add(pair.getFirst());
				items.add(pair.getSecond());
			}
			logDuration(LOG, "Storing attachments", start);
			createMessage(groupId, text, attachments, items, timestamp);
		});
	}

	@Nullable
	@DatabaseExecutor
	private Pair<AttachmentHeader, AttachmentItem> createAttachmentHeader(
			GroupId groupId, Uri uri, long timestamp) {
		InputStream is = null;
		try {
			ContentResolver contentResolver =
					getApplication().getContentResolver();
			is = contentResolver.openInputStream(uri);
			if (is == null) throw new IOException();
			String contentType = contentResolver.getType(uri);
			if (contentType == null) throw new IOException("null content type");
			AttachmentHeader h = messagingManager
					.addLocalAttachment(groupId, timestamp, contentType, is);
			is.close();
			// re-open stream to get AttachmentItem
			is = contentResolver.openInputStream(uri);
			if (is == null) throw new IOException();
			AttachmentItem item = attachmentController
					.getAttachmentItem(h, new Attachment(is));
			return new Pair<>(h, item);
		} catch (DbException | IOException e) {
			logException(LOG, WARNING, e);
			return null;
		} finally {
			if (is != null) tryToClose(is, LOG, WARNING);
		}
	}

	private void createMessage(GroupId groupId, @Nullable String text,
			List<AttachmentHeader> attachments, List<AttachmentItem> aItems,
			long timestamp) {
		cryptoExecutor.execute(() -> {
			try {
				// TODO remove when text can be null in the backend
				String msgText = text == null ? "null" : text;
				PrivateMessage pm = privateMessageFactory
						.createPrivateMessage(groupId, timestamp, msgText,
								attachments);
				attachmentController.put(pm.getMessage().getId(), aItems);
				storeMessage(pm, msgText, attachments);
			} catch (FormatException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void storeMessage(PrivateMessage m, @Nullable String text,
			List<AttachmentHeader> attachments) {
		dbExecutor.execute(() -> {
			try {
				long start = now();
				messagingManager.addLocalMessage(m);
				logDuration(LOG, "Storing message", start);
				Message message = m.getMessage();
				PrivateMessageHeader h = new PrivateMessageHeader(
						message.getId(), message.getGroupId(),
						message.getTimestamp(), true, true, false, false,
						text != null, attachments);
				// TODO add text to cache when available here
				addedHeader.postValue(h);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	@UiThread
	void onAddedPrivateMessageSeen() {
		addedHeader.setValue(null);
	}

	AttachmentController getAttachmentController() {
		return attachmentController;
	}

	LiveData<Contact> getContact() {
		return contact;
	}

	LiveData<AuthorId> getContactAuthorId() {
		return contactAuthorId;
	}

	LiveData<String> getContactDisplayName() {
		return contactName;
	}

	LiveData<Boolean> isContactDeleted() {
		return contactDeleted;
	}

	LiveData<PrivateMessageHeader> getAddedPrivateMessage() {
		return addedHeader;
	}

}
