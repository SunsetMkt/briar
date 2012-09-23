package net.sf.briar.protocol.simplex;

import static net.sf.briar.api.protocol.ProtocolConstants.MAX_PACKET_LENGTH;
import static net.sf.briar.api.transport.TransportConstants.HEADER_LENGTH;
import static net.sf.briar.api.transport.TransportConstants.MAC_LENGTH;
import static net.sf.briar.api.transport.TransportConstants.MIN_CONNECTION_LENGTH;
import static net.sf.briar.api.transport.TransportConstants.TAG_LENGTH;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import net.sf.briar.BriarTestCase;
import net.sf.briar.TestUtils;
import net.sf.briar.api.ContactId;
import net.sf.briar.api.crypto.KeyManager;
import net.sf.briar.api.db.DatabaseComponent;
import net.sf.briar.api.db.DatabaseExecutor;
import net.sf.briar.api.protocol.Ack;
import net.sf.briar.api.protocol.BatchId;
import net.sf.briar.api.protocol.ProtocolWriterFactory;
import net.sf.briar.api.protocol.RawBatch;
import net.sf.briar.api.protocol.TransportId;
import net.sf.briar.api.protocol.UniqueId;
import net.sf.briar.api.transport.ConnectionContext;
import net.sf.briar.api.transport.ConnectionRecogniser;
import net.sf.briar.api.transport.ConnectionRegistry;
import net.sf.briar.api.transport.ConnectionWriterFactory;
import net.sf.briar.crypto.CryptoModule;
import net.sf.briar.protocol.ProtocolModule;
import net.sf.briar.protocol.duplex.DuplexProtocolModule;
import net.sf.briar.serial.SerialModule;
import net.sf.briar.transport.TransportModule;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class OutgoingSimplexConnectionTest extends BriarTestCase {

	// FIXME: This is an integration test, not a unit test

	private final Mockery context;
	private final DatabaseComponent db;
	private final KeyManager keyManager;
	private final ConnectionRecogniser connRecogniser;
	private final ConnectionRegistry connRegistry;
	private final ConnectionWriterFactory connFactory;
	private final ProtocolWriterFactory protoFactory;
	private final ContactId contactId;
	private final TransportId transportId;
	private final byte[] secret;

	public OutgoingSimplexConnectionTest() {
		super();
		context = new Mockery();
		db = context.mock(DatabaseComponent.class);
		keyManager = context.mock(KeyManager.class);
		connRecogniser = context.mock(ConnectionRecogniser.class);
		Module testModule = new AbstractModule() {
			@Override
			public void configure() {
				bind(DatabaseComponent.class).toInstance(db);
				bind(KeyManager.class).toInstance(keyManager);
				bind(ConnectionRecogniser.class).toInstance(connRecogniser);
				bind(Executor.class).annotatedWith(
						DatabaseExecutor.class).toInstance(
								Executors.newCachedThreadPool());
			}
		};
		Injector i = Guice.createInjector(testModule, new CryptoModule(),
				new SerialModule(), new TransportModule(),
				new SimplexProtocolModule(), new ProtocolModule(),
				new DuplexProtocolModule());
		connRegistry = i.getInstance(ConnectionRegistry.class);
		connFactory = i.getInstance(ConnectionWriterFactory.class);
		protoFactory = i.getInstance(ProtocolWriterFactory.class);
		contactId = new ContactId(234);
		transportId = new TransportId(TestUtils.getRandomId());
		secret = new byte[32];
	}

	@Test
	public void testConnectionTooShort() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TestSimplexTransportWriter transport = new TestSimplexTransportWriter(
				out, MAX_PACKET_LENGTH, true);
		byte[] tag = new byte[TAG_LENGTH];
		ConnectionContext ctx = new ConnectionContext(contactId, transportId,
				tag, secret, 0L, true);
		OutgoingSimplexConnection connection = new OutgoingSimplexConnection(db,
				connRegistry, connFactory, protoFactory, ctx, transport);
		connection.write();
		// Nothing should have been written
		assertEquals(0, out.size());
		// The transport should have been disposed with exception == true
		assertTrue(transport.getDisposed());
		assertTrue(transport.getException());
	}

	@Test
	public void testNothingToSend() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TestSimplexTransportWriter transport = new TestSimplexTransportWriter(
				out, MIN_CONNECTION_LENGTH, true);
		byte[] tag = new byte[TAG_LENGTH];
		ConnectionContext ctx = new ConnectionContext(contactId, transportId,
				tag, secret, 0L, true);
		OutgoingSimplexConnection connection = new OutgoingSimplexConnection(db,
				connRegistry, connFactory, protoFactory, ctx, transport);
		context.checking(new Expectations() {{
			// No transports to send
			oneOf(db).generateTransportUpdate(contactId);
			will(returnValue(null));
			// No subscriptions to send
			oneOf(db).generateSubscriptionUpdate(contactId);
			will(returnValue(null));
			// No acks to send
			oneOf(db).generateAck(with(contactId), with(any(int.class)));
			will(returnValue(null));
			// No batches to send
			oneOf(db).generateBatch(with(contactId), with(any(int.class)));
			will(returnValue(null));
		}});
		connection.write();
		// Nothing should have been written
		assertEquals(0, out.size());
		// The transport should have been disposed with exception == false
		assertTrue(transport.getDisposed());
		assertFalse(transport.getException());
		context.assertIsSatisfied();
	}

	@Test
	public void testSomethingToSend() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TestSimplexTransportWriter transport = new TestSimplexTransportWriter(
				out, MIN_CONNECTION_LENGTH, true);
		byte[] tag = new byte[TAG_LENGTH];
		ConnectionContext ctx = new ConnectionContext(contactId, transportId,
				tag, secret, 0L, true);
		OutgoingSimplexConnection connection = new OutgoingSimplexConnection(db,
				connRegistry, connFactory, protoFactory, ctx, transport);
		final Ack ack = context.mock(Ack.class);
		final BatchId batchId = new BatchId(TestUtils.getRandomId());
		final RawBatch batch = context.mock(RawBatch.class);
		final byte[] message = new byte[1234];
		context.checking(new Expectations() {{
			// No transports to send
			oneOf(db).generateTransportUpdate(contactId);
			will(returnValue(null));
			// No subscriptions to send
			oneOf(db).generateSubscriptionUpdate(contactId);
			will(returnValue(null));
			// One ack to send
			oneOf(db).generateAck(with(contactId), with(any(int.class)));
			will(returnValue(ack));
			oneOf(ack).getBatchIds();
			will(returnValue(Collections.singletonList(batchId)));
			// No more acks
			oneOf(db).generateAck(with(contactId), with(any(int.class)));
			will(returnValue(null));
			// One batch to send
			oneOf(db).generateBatch(with(contactId), with(any(int.class)));
			will(returnValue(batch));
			oneOf(batch).getMessages();
			will(returnValue(Collections.singletonList(message)));
			// No more batches
			oneOf(db).generateBatch(with(contactId), with(any(int.class)));
			will(returnValue(null));
		}});
		connection.write();
		// Something should have been written
		int overhead = TAG_LENGTH + HEADER_LENGTH + MAC_LENGTH;
		assertTrue(out.size() > overhead + UniqueId.LENGTH + message.length);
		// The transport should have been disposed with exception == false
		assertTrue(transport.getDisposed());
		assertFalse(transport.getException());
		context.assertIsSatisfied();
	}
}
