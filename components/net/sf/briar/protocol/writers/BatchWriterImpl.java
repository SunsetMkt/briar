package net.sf.briar.protocol.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import net.sf.briar.api.protocol.BatchId;
import net.sf.briar.api.protocol.ProtocolConstants;
import net.sf.briar.api.protocol.Types;
import net.sf.briar.api.protocol.writers.BatchWriter;
import net.sf.briar.api.serial.Writer;
import net.sf.briar.api.serial.WriterFactory;

class BatchWriterImpl implements BatchWriter {

	private final DigestOutputStream out;
	private final Writer w;
	private final MessageDigest messageDigest;

	private boolean started = false;

	BatchWriterImpl(OutputStream out, WriterFactory writerFactory,
			MessageDigest messageDigest) {
		this.out = new DigestOutputStream(out, messageDigest);
		w = writerFactory.createWriter(this.out);
		this.messageDigest = messageDigest;
	}

	public boolean writeMessage(byte[] message) throws IOException {
		if(!started) {
			messageDigest.reset();
			w.writeUserDefinedTag(Types.BATCH);
			w.writeListStart();
			started = true;
		}
		// Allow one byte for the list end tag
		int capacity = ProtocolConstants.MAX_PACKET_LENGTH
		- (int) w.getBytesWritten() - 1;
		if(capacity < message.length) return false;
		// Bypass the writer and write each raw message directly
		out.write(message);
		return true;
	}

	public BatchId finish() throws IOException {
		if(!started) {
			messageDigest.reset();
			w.writeUserDefinedTag(Types.BATCH);
			w.writeListStart();
			started = true;
		}
		w.writeListEnd();
		out.flush();
		started = false;
		return new BatchId(messageDigest.digest());
	}
}
