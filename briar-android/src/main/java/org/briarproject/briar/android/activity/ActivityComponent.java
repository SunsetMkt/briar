package org.briarproject.briar.android.activity;

import android.app.Activity;

import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.briar.android.AndroidComponent;
import org.briarproject.briar.android.StartupFailureActivity;
import org.briarproject.briar.android.account.AuthorNameFragment;
import org.briarproject.briar.android.account.DozeFragment;
import org.briarproject.briar.android.account.NewOrRecoverActivity;
import org.briarproject.briar.android.account.NewOrRecoverFragment;
import org.briarproject.briar.android.account.SetPasswordFragment;
import org.briarproject.briar.android.account.SetupActivity;
import org.briarproject.briar.android.account.UnlockActivity;
import org.briarproject.briar.android.blog.BlogActivity;
import org.briarproject.briar.android.blog.BlogFragment;
import org.briarproject.briar.android.blog.BlogModule;
import org.briarproject.briar.android.blog.BlogPostFragment;
import org.briarproject.briar.android.blog.FeedFragment;
import org.briarproject.briar.android.blog.FeedPostFragment;
import org.briarproject.briar.android.blog.ReblogActivity;
import org.briarproject.briar.android.blog.ReblogFragment;
import org.briarproject.briar.android.blog.RssFeedImportActivity;
import org.briarproject.briar.android.blog.RssFeedManageActivity;
import org.briarproject.briar.android.blog.WriteBlogPostActivity;
import org.briarproject.briar.android.contact.ContactListFragment;
import org.briarproject.briar.android.contact.add.nearby.AddNearbyContactActivity;
import org.briarproject.briar.android.contact.add.nearby.AddNearbyContactErrorFragment;
import org.briarproject.briar.android.contact.add.nearby.AddNearbyContactFragment;
import org.briarproject.briar.android.contact.add.nearby.AddNearbyContactIntroFragment;
import org.briarproject.briar.android.contact.add.remote.AddContactActivity;
import org.briarproject.briar.android.contact.add.remote.LinkExchangeFragment;
import org.briarproject.briar.android.contact.add.remote.NicknameFragment;
import org.briarproject.briar.android.contact.add.remote.PendingContactListActivity;
import org.briarproject.briar.android.conversation.AliasDialogFragment;
import org.briarproject.briar.android.conversation.ConversationActivity;
import org.briarproject.briar.android.conversation.ImageActivity;
import org.briarproject.briar.android.conversation.ImageFragment;
import org.briarproject.briar.android.forum.CreateForumActivity;
import org.briarproject.briar.android.forum.ForumActivity;
import org.briarproject.briar.android.forum.ForumListFragment;
import org.briarproject.briar.android.fragment.ScreenFilterDialogFragment;
import org.briarproject.briar.android.introduction.ContactChooserFragment;
import org.briarproject.briar.android.introduction.IntroductionActivity;
import org.briarproject.briar.android.introduction.IntroductionMessageFragment;
import org.briarproject.briar.android.login.ChangePasswordActivity;
import org.briarproject.briar.android.login.OpenDatabaseFragment;
import org.briarproject.briar.android.login.PasswordFragment;
import org.briarproject.briar.android.login.StartupActivity;
import org.briarproject.briar.android.navdrawer.NavDrawerActivity;
import org.briarproject.briar.android.navdrawer.TransportsActivity;
import org.briarproject.briar.android.panic.PanicPreferencesActivity;
import org.briarproject.briar.android.panic.PanicResponderActivity;
import org.briarproject.briar.android.privategroup.conversation.GroupActivity;
import org.briarproject.briar.android.privategroup.creation.CreateGroupActivity;
import org.briarproject.briar.android.privategroup.creation.CreateGroupFragment;
import org.briarproject.briar.android.privategroup.creation.CreateGroupModule;
import org.briarproject.briar.android.privategroup.creation.GroupInviteActivity;
import org.briarproject.briar.android.privategroup.creation.GroupInviteFragment;
import org.briarproject.briar.android.privategroup.invitation.GroupInvitationActivity;
import org.briarproject.briar.android.privategroup.invitation.GroupInvitationModule;
import org.briarproject.briar.android.privategroup.list.GroupListFragment;
import org.briarproject.briar.android.privategroup.memberlist.GroupMemberListActivity;
import org.briarproject.briar.android.privategroup.memberlist.GroupMemberModule;
import org.briarproject.briar.android.privategroup.reveal.GroupRevealModule;
import org.briarproject.briar.android.privategroup.reveal.RevealContactsActivity;
import org.briarproject.briar.android.privategroup.reveal.RevealContactsFragment;
import org.briarproject.briar.android.remotewipe.RemoteWipeActivatedActivity;
import org.briarproject.briar.android.remotewipe.RemoteWipeDisplayFragment;
import org.briarproject.briar.android.remotewipe.RemoteWipeSetupActivity;
import org.briarproject.briar.android.remotewipe.RemoteWipeSetupExplainerFragment;
import org.briarproject.briar.android.remotewipe.RemoteWipeSuccessFragment;
import org.briarproject.briar.android.remotewipe.WiperSelectorFragment;
import org.briarproject.briar.android.remotewipe.activate.ActivateRemoteWipeActivity;
import org.briarproject.briar.android.remotewipe.activate.ActivateRemoteWipeExplainerFragment;
import org.briarproject.briar.android.remotewipe.activate.ActivateRemoteWipeSuccessFragment;
import org.briarproject.briar.android.remotewipe.revoke.RevokeRemoteWipeActivity;
import org.briarproject.briar.android.remotewipe.revoke.RevokeRemoteWipeState;
import org.briarproject.briar.android.remotewipe.revoke.RevokeRemoteWipeSuccessFragment;
import org.briarproject.briar.android.reporting.CrashFragment;
import org.briarproject.briar.android.reporting.CrashReportActivity;
import org.briarproject.briar.android.reporting.ReportFormFragment;
import org.briarproject.briar.android.settings.ConfirmAvatarDialogFragment;
import org.briarproject.briar.android.settings.SettingsActivity;
import org.briarproject.briar.android.settings.SettingsFragment;
import org.briarproject.briar.android.sharing.BlogInvitationActivity;
import org.briarproject.briar.android.sharing.BlogSharingStatusActivity;
import org.briarproject.briar.android.sharing.ForumInvitationActivity;
import org.briarproject.briar.android.sharing.ForumSharingStatusActivity;
import org.briarproject.briar.android.sharing.ShareBlogActivity;
import org.briarproject.briar.android.sharing.ShareBlogFragment;
import org.briarproject.briar.android.sharing.ShareForumActivity;
import org.briarproject.briar.android.sharing.ShareForumFragment;
import org.briarproject.briar.android.sharing.SharingModule;
import org.briarproject.briar.android.socialbackup.recover.CustodianRecoveryModeExplainerFragment;
import org.briarproject.briar.android.socialbackup.CustodianSelectorFragment;
import org.briarproject.briar.android.socialbackup.DistributedBackupActivity;
import org.briarproject.briar.android.socialbackup.ExistingBackupFragment;
import org.briarproject.briar.android.socialbackup.recover.CustodianReturnShardActivity;
import org.briarproject.briar.android.socialbackup.recover.CustodianReturnShardErrorFragment;
import org.briarproject.briar.android.socialbackup.recover.CustodianReturnShardFragment;
import org.briarproject.briar.android.socialbackup.recover.CustodianReturnShardSuccessFragment;
import org.briarproject.briar.android.socialbackup.recover.OwnerRecoveryModeErrorFragment;
import org.briarproject.briar.android.socialbackup.recover.OwnerRecoveryModeExplainerFragment;
import org.briarproject.briar.android.socialbackup.recover.OwnerRecoveryModeMainFragment;
import org.briarproject.briar.android.socialbackup.recover.OwnerReturnShardActivity;
import org.briarproject.briar.android.socialbackup.recover.OwnerReturnShardFragment;
import org.briarproject.briar.android.socialbackup.ShardsSentFragment;
import org.briarproject.briar.android.socialbackup.ThresholdSelectorFragment;
import org.briarproject.briar.android.socialbackup.creation.CreateBackupModule;
import org.briarproject.briar.android.socialbackup.recover.OwnerReturnShardSuccessFragment;
import org.briarproject.briar.android.socialbackup.recover.RestoreAccountActivity;
import org.briarproject.briar.android.socialbackup.recover.RestoreAccountDozeFragment;
import org.briarproject.briar.android.socialbackup.recover.RestoreAccountSetPasswordFragment;
import org.briarproject.briar.android.splash.SplashScreenActivity;
import org.briarproject.briar.android.test.TestDataActivity;
import org.briarproject.briar.api.socialbackup.recovery.RestoreAccount;

import dagger.Component;

@ActivityScope
@Component(modules ={
		ActivityModule.class,
		BlogModule.class,
		CreateGroupModule.class,
		GroupInvitationModule.class,
		GroupMemberModule.class,
		GroupRevealModule.class,
		SharingModule.SharingLegacyModule.class,
		CreateBackupModule.class
}, dependencies = AndroidComponent.class)
public interface ActivityComponent {

	Activity activity();

	void inject(SplashScreenActivity activity);

	void inject(StartupActivity activity);

	void inject(SetupActivity activity);

	void inject(NavDrawerActivity activity);

	void inject(PanicResponderActivity activity);

	void inject(PanicPreferencesActivity activity);

	void inject(AddNearbyContactActivity activity);

	void inject(ConversationActivity activity);

	void inject(ImageActivity activity);

	void inject(ForumInvitationActivity activity);

	void inject(BlogInvitationActivity activity);

	void inject(CreateGroupActivity activity);

	void inject(GroupActivity activity);

	void inject(GroupInviteActivity activity);

	void inject(GroupInvitationActivity activity);

	void inject(GroupMemberListActivity activity);

	void inject(RevealContactsActivity activity);

	void inject(CreateForumActivity activity);

	void inject(ShareForumActivity activity);

	void inject(ShareBlogActivity activity);

	void inject(ForumSharingStatusActivity activity);

	void inject(BlogSharingStatusActivity activity);

	void inject(ForumActivity activity);

	void inject(BlogActivity activity);

	void inject(WriteBlogPostActivity activity);

	void inject(BlogFragment fragment);

	void inject(BlogPostFragment fragment);

	void inject(FeedPostFragment fragment);

	void inject(ReblogFragment fragment);

	void inject(ReblogActivity activity);

	void inject(SettingsActivity activity);

	void inject(TransportsActivity activity);

	void inject(TestDataActivity activity);

	void inject(ChangePasswordActivity activity);

	void inject(IntroductionActivity activity);

	void inject(RssFeedImportActivity activity);

	void inject(RssFeedManageActivity activity);

	void inject(StartupFailureActivity activity);

	void inject(UnlockActivity activity);

	void inject(AddContactActivity activity);

	void inject(PendingContactListActivity activity);

	void inject(CrashReportActivity crashReportActivity);

	void inject(NewOrRecoverActivity newOrRecoverActivity);

	void inject(CustodianReturnShardActivity custodianReturnShardActivity);

    void inject(OwnerReturnShardActivity ownerReturnShardActivity);

    void inject(OwnerRecoveryModeMainFragment ownerRecoveryModeMainFragment);

    void inject(RestoreAccountActivity restoreAccountActivity);

    void inject(RemoteWipeSetupActivity remoteWipeSetupActivity);

    void inject(ActivateRemoteWipeActivity activateRemoteWipeActivity);

    void inject(RemoteWipeActivatedActivity remoteWipeActivatedActivity);

    void inject(RevokeRemoteWipeActivity revokeRemoteWipeActivity);

	// Fragments

	void inject(AuthorNameFragment fragment);

	void inject(SetPasswordFragment fragment);

	void inject(DozeFragment fragment);

	void inject(PasswordFragment imageFragment);

	void inject(OpenDatabaseFragment activity);

	void inject(ContactListFragment fragment);

	void inject(CreateGroupFragment fragment);

	void inject(GroupListFragment fragment);

	void inject(GroupInviteFragment fragment);

	void inject(RevealContactsFragment activity);

	void inject(ForumListFragment fragment);

	void inject(FeedFragment fragment);

	void inject(AddNearbyContactIntroFragment fragment);

	void inject(AddNearbyContactFragment fragment);

	void inject(LinkExchangeFragment fragment);

	void inject(NicknameFragment fragment);

	void inject(ContactChooserFragment fragment);

	void inject(ShareForumFragment fragment);

	void inject(ShareBlogFragment fragment);

	void inject(IntroductionMessageFragment fragment);

	void inject(SettingsFragment fragment);

	void inject(ScreenFilterDialogFragment fragment);

	void inject(AddNearbyContactErrorFragment fragment);

	void inject(AliasDialogFragment aliasDialogFragment);

	void inject(ImageFragment imageFragment);

	void inject(ReportFormFragment reportFormFragment);

	void inject(CrashFragment crashFragment);

	void inject(ConfirmAvatarDialogFragment fragment);

	void inject(ThresholdSelectorFragment thresholdSelectorFragment);

	void inject(DistributedBackupActivity distributedBackupActivity);

	void inject(DatabaseComponent databaseComponent);

	void inject(CustodianSelectorFragment custodianSelectorFragment);

	void inject(ShardsSentFragment shardsSentFragment);

	void inject(OwnerRecoveryModeExplainerFragment ownerRecoveryModeExplainerFragment);

	void inject(ExistingBackupFragment existingBackupFragment);

	void inject(NewOrRecoverFragment newOrRecoverFragment);

	void inject(CustodianRecoveryModeExplainerFragment custodianRecoveryModeExplainerFragment);

	void inject(CustodianReturnShardFragment custodianReturnShardFragment);

	void inject(OwnerReturnShardFragment ownerReturnShardFragment);

	void inject(CustodianReturnShardSuccessFragment custodianReturnShardSuccessFragment);

	void inject(RestoreAccountSetPasswordFragment restoreAccountSetPasswordFragment);

	void inject(RestoreAccountDozeFragment restoreAccountDozeFragment);

	void inject(OwnerReturnShardSuccessFragment ownerReturnShardSuccessFragment);

	void inject(OwnerRecoveryModeErrorFragment ownerRecoveryModeErrorFragment);

	void inject(CustodianReturnShardErrorFragment custodianReturnShardErrorFragment);

	void inject(WiperSelectorFragment wiperSelectorFragment);

	void inject(RemoteWipeDisplayFragment remoteWipeDisplayFragment);

	void inject(RemoteWipeSuccessFragment remoteWipeSuccessFragment);

	void inject(ActivateRemoteWipeExplainerFragment activateRemoteWipeExplainerFragment);

	void inject(ActivateRemoteWipeSuccessFragment activateRemoteWipeSuccessFragment);

	void inject(RevokeRemoteWipeSuccessFragment revokeRemoteWipeSuccessFragment);

	void inject(RemoteWipeSetupExplainerFragment remoteWipeSetupExplainerFragment);
}
