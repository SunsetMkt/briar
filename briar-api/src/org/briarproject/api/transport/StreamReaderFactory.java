package org.briarproject.api.transport;

import java.io.InputStream;

public interface StreamReaderFactory {

	/**
	 * Creates an {@link java.io.InputStream InputStream} for reading from a
	 * transport stream.
	 */
	InputStream createStreamReader(InputStream in, int maxFrameLength,
			StreamContext ctx);

	/**
	 * Creates an {@link java.io.InputStream InputStream} for reading from an
	 * invitation stream.
	 */
	InputStream createInvitationStreamReader(InputStream in,
			int maxFrameLength, byte[] secret, boolean alice);
}
