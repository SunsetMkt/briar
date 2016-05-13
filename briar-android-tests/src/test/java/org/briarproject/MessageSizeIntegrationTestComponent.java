package org.briarproject;

import org.briarproject.clients.ClientsModule;
import org.briarproject.crypto.CryptoModule;
import org.briarproject.data.DataModule;
import org.briarproject.db.DatabaseModule;
import org.briarproject.event.EventModule;
import org.briarproject.identity.IdentityModule;
import org.briarproject.messaging.MessagingModule;
import org.briarproject.sync.SyncModule;
import org.briarproject.system.SystemModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		TestDatabaseModule.class,
		TestLifecycleModule.class,
		TestSeedProviderModule.class,
		ClientsModule.class,
		CryptoModule.class,
		DataModule.class,
		DatabaseModule.class,
		EventModule.class,
		IdentityModule.class,
		MessagingModule.class,
		SyncModule.class,
		SystemModule.class
})
public interface MessageSizeIntegrationTestComponent {

	void inject(MessageSizeIntegrationTest testCase);

	void inject(SystemModule.EagerSingletons init);
}
