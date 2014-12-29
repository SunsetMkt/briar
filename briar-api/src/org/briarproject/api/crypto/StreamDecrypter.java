package org.briarproject.api.crypto;

import java.io.IOException;

public interface StreamDecrypter {

	/**
	 * Reads a frame, decrypts its payload into the given buffer and returns
	 * the payload length, or -1 if no more frames can be read from the stream.
	 * @throws java.io.IOException if an error occurs while reading the frame,
	 * or if authenticated decryption fails.
	 */
	int readFrame(byte[] payload) throws IOException;
}
