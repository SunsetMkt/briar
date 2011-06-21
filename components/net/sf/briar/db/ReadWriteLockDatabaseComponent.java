package net.sf.briar.db;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.briar.api.db.DbException;
import net.sf.briar.api.db.NeighbourId;
import net.sf.briar.api.db.Rating;
import net.sf.briar.api.db.Status;
import net.sf.briar.api.protocol.AuthorId;
import net.sf.briar.api.protocol.Batch;
import net.sf.briar.api.protocol.BatchId;
import net.sf.briar.api.protocol.Bundle;
import net.sf.briar.api.protocol.GroupId;
import net.sf.briar.api.protocol.Message;
import net.sf.briar.api.protocol.MessageId;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ReadWriteLockDatabaseComponent<Txn> extends DatabaseComponentImpl<Txn> {

	/*
	 * Locks must always be acquired in alphabetical order. See the Database
	 * interface to find out which calls require which locks. Note: this
	 * implementation can allow writers to starve.
	 */

	private final ReentrantReadWriteLock messageLock =
		new ReentrantReadWriteLock(true);
	private final ReentrantReadWriteLock neighbourLock =
		new ReentrantReadWriteLock(true);
	private final ReentrantReadWriteLock ratingLock =
		new ReentrantReadWriteLock(true);
	private final ReentrantReadWriteLock subscriptionLock =
		new ReentrantReadWriteLock(true);

	@Inject
	ReadWriteLockDatabaseComponent(Database<Txn> db,
			Provider<Batch> batchProvider) {
		super(db, batchProvider);
	}

	protected void expireMessages(long size) throws DbException {
		messageLock.writeLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				Txn txn = db.startTransaction("cleaner");
				try {
					for(MessageId m : db.getOldMessages(txn, size)) {
						removeMessage(txn, m);
					}
					db.commitTransaction(txn);
				} catch(DbException e) {
					db.abortTransaction(txn);
					throw e;
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.writeLock().unlock();
		}
	}

	public void close() throws DbException {
		messageLock.writeLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				ratingLock.writeLock().lock();
				try {
					subscriptionLock.writeLock().lock();
					try {
						db.close();
					} finally {
						subscriptionLock.writeLock().unlock();
					}
				} finally {
					ratingLock.writeLock().unlock();
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.writeLock().unlock();
		}
	}

	public void addNeighbour(NeighbourId n) throws DbException {
		System.out.println("Adding neighbour " + n);
		neighbourLock.writeLock().lock();
		try {
			Txn txn = db.startTransaction("addNeighbour");
			try {
				db.addNeighbour(txn, n);
				db.commitTransaction(txn);
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			neighbourLock.writeLock().unlock();
		}
	}

	public void addLocallyGeneratedMessage(Message m) throws DbException {
		waitForPermissionToWrite();
		messageLock.writeLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				subscriptionLock.readLock().lock();
				try {
					Txn txn = db.startTransaction("addLocallyGeneratedMessage");
					try {
						if(db.containsSubscription(txn, m.getGroup())) {
							boolean added = storeMessage(txn, m, null);
							assert added;
						} else {
							System.out.println("Not subscribed");
						}
						db.commitTransaction(txn);
					} catch(DbException e) {
						db.abortTransaction(txn);
						throw e;
					}
				} finally {
					subscriptionLock.readLock().unlock();
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.writeLock().unlock();
		}
	}

	public Rating getRating(AuthorId a) throws DbException {
		ratingLock.readLock().lock();
		try {
			Txn txn = db.startTransaction("getRating");
			try {
				Rating r = db.getRating(txn, a);
				db.commitTransaction(txn);
				return r;
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			ratingLock.readLock().unlock();
		}
	}

	public void setRating(AuthorId a, Rating r) throws DbException {
		messageLock.writeLock().lock();
		try {
			ratingLock.writeLock().lock();
			try {
				Txn txn = db.startTransaction("setRating");
				try {
					Rating old = db.setRating(txn, a, r);
					// Update the sendability of the author's messages
					if(r == Rating.GOOD && old != Rating.GOOD)
						updateAuthorSendability(txn, a, true);
					else if(r != Rating.GOOD && old == Rating.GOOD)
						updateAuthorSendability(txn, a, false);
					db.commitTransaction(txn);
				} catch(DbException e) {
					db.abortTransaction(txn);
					throw e;
				}
			} finally {
				ratingLock.writeLock().unlock();
			}
		} finally {
			messageLock.writeLock().unlock();
		}
	}

	public Set<GroupId> getSubscriptions() throws DbException {
		subscriptionLock.readLock().lock();
		try {
			Txn txn = db.startTransaction("getSubscriptions");
			try {
				HashSet<GroupId> subs = new HashSet<GroupId>();
				for(GroupId g : db.getSubscriptions(txn)) subs.add(g);
				db.commitTransaction(txn);
				return subs;
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			subscriptionLock.readLock().unlock();
		}
	}

	public void subscribe(GroupId g) throws DbException {
		System.out.println("Subscribing to " + g);
		subscriptionLock.writeLock().lock();
		try {
			Txn txn = db.startTransaction("subscribe");
			try {
				db.addSubscription(txn, g);
				db.commitTransaction(txn);
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			subscriptionLock.writeLock().unlock();
		}
	}

	public void unsubscribe(GroupId g) throws DbException {
		System.out.println("Unsubscribing from " + g);
		messageLock.writeLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				subscriptionLock.writeLock().lock();
				try {
					Txn txn = db.startTransaction("unsubscribe");
					try {
						db.removeSubscription(txn, g);
						db.commitTransaction(txn);
					} catch(DbException e) {
						db.abortTransaction(txn);
						throw e;
					}
				} finally {
					subscriptionLock.writeLock().unlock();
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.writeLock().unlock();
		}
	}

	public void generateBundle(NeighbourId n, Bundle b) throws DbException {
		System.out.println("Generating bundle for " + n);
		// Ack all batches received from the neighbour
		neighbourLock.writeLock().lock();
		try {
			Txn txn = db.startTransaction("generateBundle:acks");
			try {
				int numAcks = 0;
				for(BatchId ack : db.removeBatchesToAck(txn, n)) {
					b.addAck(ack);
					numAcks++;
				}
				System.out.println("Added " + numAcks + " acks");
				db.commitTransaction(txn);
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			neighbourLock.writeLock().unlock();
		}
		// Add a list of subscriptions
		subscriptionLock.readLock().lock();
		try {
			Txn txn = db.startTransaction("generateBundle:subscriptions");
			try {
				int numSubs = 0;
				for(GroupId g : db.getSubscriptions(txn)) {
					b.addSubscription(g);
					numSubs++;
				}
				System.out.println("Added " + numSubs + " subscriptions");
				db.commitTransaction(txn);
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			subscriptionLock.readLock().unlock();
		}
		// Add as many messages as possible to the bundle
		long capacity = b.getCapacity();
		while(true) {
			Batch batch = fillBatch(n, capacity);
			if(batch == null) break; // No more messages to send
			b.addBatch(batch);
			capacity -= batch.getSize();
			// If the batch is less than half full, stop trying - there may be
			// more messages trickling in but we can't wait forever
			if(batch.getSize() * 2 < Batch.CAPACITY) break;
		}
		b.seal();
		System.out.println("Bundle sent, " + b.getSize() + " bytes");
		System.gc();
	}

	private Batch fillBatch(NeighbourId n, long capacity) throws DbException {
		messageLock.readLock().lock();
		try {
			Set<MessageId> sent;
			Batch b;
			neighbourLock.readLock().lock();
			try {
				Txn txn = db.startTransaction("fillBatch:read");
				try {
					capacity = Math.min(capacity, Batch.CAPACITY);
					Iterator<MessageId> it =
						db.getSendableMessages(txn, n, capacity).iterator();
					if(!it.hasNext()) {
						db.commitTransaction(txn);
						return null; // No more messages to send
					}
					sent = new HashSet<MessageId>();
					b = batchProvider.get();
					while(it.hasNext()) {
						MessageId m = it.next();
						b.addMessage(db.getMessage(txn, m));
						sent.add(m);
					}
					b.seal();
					db.commitTransaction(txn);
				} catch(DbException e) {
					db.abortTransaction(txn);
					throw e;
				}
			} finally {
				neighbourLock.readLock().unlock();
			}
			// Record the contents of the batch
			neighbourLock.writeLock().lock();
			try {
				Txn txn = db.startTransaction("fillBatch:write");
				try {
					assert !sent.isEmpty();
					db.addOutstandingBatch(txn, n, b.getId(), sent);
					db.commitTransaction(txn);
					return b;
				} catch(DbException e) {
					db.abortTransaction(txn);
					throw e;
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.readLock().unlock();
		}
	}

	public void receiveBundle(NeighbourId n, Bundle b) throws DbException {
		System.out.println("Received bundle from " + n + ", "
				+ b.getSize() + " bytes");
		// Mark all messages in acked batches as seen
		messageLock.readLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				int acks = 0, expired = 0;
				for(BatchId ack : b.getAcks()) {
					acks++;
					Txn txn = db.startTransaction("receiveBundle:acks");
					try {
						Iterable<MessageId> batch =
							db.removeOutstandingBatch(txn, n, ack);
						// May be null if the batch was empty or has expired
						if(batch == null) {
							expired++;
						} else {
							for(MessageId m : batch) {
								// Don't re-create statuses for expired messages
								if(db.containsMessage(txn, m))
									db.setStatus(txn, n, m, Status.SEEN);
							}
						}
						db.commitTransaction(txn);
					} catch(DbException e) {
						db.abortTransaction(txn);
						throw e;
					}
				}
				System.out.println("Received " + acks + " acks, " + expired
						+ " expired");
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.readLock().unlock();
		}
		// Update the neighbour's subscriptions
		neighbourLock.writeLock().lock();
		try {
			Txn txn = db.startTransaction("receiveBundle:subscriptions");
			try {
				db.clearSubscriptions(txn, n);
				int subs = 0;
				for(GroupId g : b.getSubscriptions()) {
					subs++;
					db.addSubscription(txn, n, g);
				}
				System.out.println("Received " + subs + " subscriptions");
				db.commitTransaction(txn);
			} catch(DbException e) {
				db.abortTransaction(txn);
				throw e;
			}
		} finally {
			neighbourLock.writeLock().unlock();
		}
		// Store the messages
		int batches = 0;
		for(Batch batch : b.getBatches()) {
			batches++;
			waitForPermissionToWrite();
			messageLock.writeLock().lock();
			try {
				neighbourLock.writeLock().lock();
				try {
					subscriptionLock.readLock().lock();
					try {
						Txn txn = db.startTransaction("receiveBundle:batch");
						try {
							int received = 0, stored = 0;
							for(Message m : batch.getMessages()) {
								received++;
								if(db.containsSubscription(txn, m.getGroup())) {
									if(storeMessage(txn, m, n)) stored++;
								}
							}
							System.out.println("Received " + received
									+ " messages, stored " + stored);
							db.addBatchToAck(txn, n, batch.getId());
							db.commitTransaction(txn);
						} catch(DbException e) {
							db.abortTransaction(txn);
							throw e;
						}
					} finally {
						subscriptionLock.readLock().unlock();
					}
				} finally {
					neighbourLock.writeLock().unlock();
				}
			} finally {
				messageLock.writeLock().unlock();
			}
		}
		System.out.println("Received " + batches + " batches");
		// Find any lost batches that need to be retransmitted
		Set<BatchId> lost;
		messageLock.readLock().lock();
		try {
			neighbourLock.writeLock().lock();
			try {
				Txn txn = db.startTransaction("receiveBundle:findLost");
				try {
					lost = db.addReceivedBundle(txn, n, b.getId());
					db.commitTransaction(txn);
				} catch(DbException e) {
					db.abortTransaction(txn);
					throw e;
				}
			} finally {
				neighbourLock.writeLock().unlock();
			}
		} finally {
			messageLock.readLock().unlock();
		}
		for(BatchId batch : lost) {
			messageLock.readLock().lock();
			try {
				neighbourLock.writeLock().lock();
				try {
					Txn txn = db.startTransaction("receiveBundle:removeLost");
					try {
						System.out.println("Removing lost batch");
						db.removeLostBatch(txn, n, batch);
						db.commitTransaction(txn);
					} catch(DbException e) {
						db.abortTransaction(txn);
						throw e;
					}
				} finally {
					neighbourLock.writeLock().unlock();
				}
			} finally {
				messageLock.readLock().unlock();
			}
		}
		System.gc();
	}
}