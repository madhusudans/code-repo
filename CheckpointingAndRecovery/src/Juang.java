import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class Juang implements Runnable, Observer
{
	private int checkpointCount;
	private ArrayList<Checkpoint> checkpointArr;
	private Set<Integer> neighborids;
	private Checkpoint currCheckpoint;
	LinkedList<FailurePoint> failureEvents;
	private Logger logger;
	private int nodeId;
	private int noOfCheckPointsAfterRecovery;
	private Network network;
	private int recoverySentCount;
	private int recoveryRcvdCount;
	private int roundNo;
	private int failuresEncountered;
	private int N;
	private int failures;
	private LinkedBlockingQueue<Message> messageQueue;
	private PriorityBlockingQueue<JuangMessage> sentMsgQueue;
	private int[] sentSentCount;
	private int[] sentRcvdCount;

	private int receiveSentCount;
	private int receiveRcvdCount;

	private int failureDoneSentCount;
	private int failureDoneRcvdCount;

	private LinkedBlockingQueue<JuangMessage> receiveQueue;
	
	private TimestampService timestampService;

	public Juang(Logger logger, LinkedList<FailurePoint> failureEvents, int nodeId, Network network, int N, int failures)
	{
		checkpointCount = 0;
		checkpointArr = new ArrayList<Checkpoint>();
		this.logger = logger;
		this.failureEvents = failureEvents;
		this.nodeId = nodeId;
		noOfCheckPointsAfterRecovery = 0;
		this.network = network;
		recoverySentCount = 0;
		roundNo = 0;
		this.N = N;
		failuresEncountered = 0;
		this.failures = failures;
		sentSentCount = new int[N];
		sentRcvdCount = new int[N];

		receiveSentCount = 0;
		receiveRcvdCount = 0;

		failureDoneSentCount = 0;
		failureDoneRcvdCount = 0;

		sentMsgQueue = new PriorityBlockingQueue<JuangMessage>(11, new Comparator<JuangMessage>() {
			@Override
			public int compare(JuangMessage msg1, JuangMessage msg2)
			{
				if(msg1.getRoundNo() == msg2.getRoundNo())
				{
					return msg1.getSourceId() - msg2.getSourceId();
				}

				return msg1.getRoundNo() - msg2.getRoundNo();
			}
		});

		messageQueue = new LinkedBlockingQueue<Message>();
		receiveQueue = new LinkedBlockingQueue<JuangMessage>();
	}

	public void setNeighborIds(String[] neighborIds)
	{
		this.neighborids = new HashSet<Integer>();

		for(int i = 0; i < neighborIds.length; i++)
		{
			this.neighborids.add(Integer.parseInt(neighborIds[i]));
		}

		//logger.info("Neighbors hashset: "+ this.neighborids.toString());

		Globals.neighborCount = neighborids.size();
	}

	public void update(Message message)
	{
		if(message != null)
		{
			messageQueue.add(message);
		}
	}

	class MessageProcessor implements Runnable
	{
		public MessageProcessor()
		{
			//logger.info("Taking first checkpoint number: "+checkpointCount);
			currCheckpoint = new Checkpoint(neighborids, timestampService.getCurrTimestamp());
		}

		public void floodRecovery()
		{
			Message recoveryMsg;
			//call network's send to all
			for(Integer i : neighborids)
			{
				recoveryMsg = new JuangMessage(nodeId, i, Constants.RECOVERY, Constants.BOTTOM, Constants.BOTTOM);
				network.sendToNeighbor(i, recoveryMsg);
			}
		}

		public void floodSent()
		{
			roundNo++;

			if(roundNo < N)
			{
				Message sentMsg;
				//call network's send to all
				for(Integer i : neighborids)
				{
					sentMsg = new JuangMessage(nodeId, i, Constants.SENT, currCheckpoint.getSent().get(i), roundNo);
					network.sendToNeighbor(i, sentMsg);
				}
			}
			else
			{
				int recVal;
				//logger.info("Round Number is N");
				//logger.info("Sending receive count to neighbors");
				Message recVectorMsg;

				for(Integer neighborId : neighborids)
				{
					recVal = currCheckpoint.getRcvd().get(neighborId);

					recVectorMsg = new JuangMessage(nodeId, neighborId, Constants.RECEIVE, recVal, Constants.BOTTOM);

					network.sendToNeighbor(neighborId, recVectorMsg);
				}
			}
		}

		public void floodFailureDone()
		{
			Message failureDoneMsg;
			//call network's send to all
			for(Integer i : neighborids)
			{
				failureDoneMsg = new JuangMessage(nodeId, i, Constants.FAILURE_DONE, Constants.BOTTOM, Constants.BOTTOM);
				network.sendToNeighbor(i, failureDoneMsg);
			}
		}

		public void recover()
		{
			JuangMessage msg = null;
			while(!sentMsgQueue.isEmpty() && sentMsgQueue.peek().getRoundNo() == roundNo)
			{
				try
				{
					msg = (JuangMessage) sentMsgQueue.take();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				//logger.info("Message for recovery: "+msg);

				int sentVal = msg.getVal();

				for(int i = checkpointArr.size() - 1; i > 0; i--)
				{
					//logger.info("Received vector count for "+ msg.getSourceId() + " is "+ checkpointArr.get(i).getRcvd().get(msg.getSourceId()));
					//logger.info("Sent val count: "+ sentVal);

					if(checkpointArr.get(i).getRcvd().get(msg.getSourceId()) <= sentVal)
					{
						currCheckpoint = checkpointArr.get(i);
						checkpointCount = checkpointArr.size() - 1;
						break;
					}
					else
					{
						checkpointArr.remove(i);
					}
				}

				//logger.info("checkpoint arr size: "+ checkpointArr.size());
				//logger.info("Recoverd for checkpoint: "+ currCheckpoint.getCheckpointNum());
				//logger.info("Recoverd checkpoint: " + currCheckpoint);
			}
		}

		private void fail()
		{
			Random random = new Random();
			int rollbackTo = random.nextInt(checkpointArr.size());

			//logger.info("Rolling back to checkpoint :"+rollbackTo);

			Globals.juangInRecovery = true;
			Globals.rebActive = false;

			currCheckpoint = checkpointArr.get(rollbackTo);

			//logger.info("Rolled back to checkpoint state is : " + currCheckpoint.toString());

			for(int i = checkpointArr.size() - 1; i > rollbackTo; i--)
			{
				checkpointArr.remove(i);
			}

			floodRecovery();
		}

		private boolean isTimeToFail()
		{
			if(!failureEvents.isEmpty())
			{
				if(failureEvents.peek().getNoOfCheckpoints() == noOfCheckPointsAfterRecovery && failureEvents.peek().getNodeId() == nodeId)
				{
					noOfCheckPointsAfterRecovery = 0;
					return true;
				}
			}

			return false;
		}

		public synchronized void takeCheckpoint(REBMessage rebMessage)
		{	
			if(checkpointCount == 0)
			{
				//logger.info("Taking first checkpoint number: "+checkpointCount);
				currCheckpoint = new Checkpoint(neighborids, timestampService.getCurrTimestamp());
			}
			else
			{
				timestampService.updateVectorClock(rebMessage);
				currCheckpoint = new Checkpoint(currCheckpoint.getSent(), currCheckpoint.getRcvd(), currCheckpoint.getCheckpointNum(), Globals.rebActive, timestampService.getCurrTimestamp());
			}

			checkpointArr.add(currCheckpoint);
			
			if(rebMessage.getSourceId() == nodeId)
			{
				//logger.info("Comm event is of OUTGOING type");
				currCheckpoint.incrementSentVector(rebMessage.getDestId());
			}
			else if(rebMessage.getDestId() == nodeId)
			{
				//logger.info("Comm event is of INCOMING | LOST type");
				currCheckpoint.incrementRcvdVector(rebMessage.getSourceId());
			}

			//logger.info("Checkpoint state: "+ currCheckpoint.toString());

			checkpointCount++;

			if(!rebMessage.getType().equals(Constants.LOST))
			{
				noOfCheckPointsAfterRecovery++;
			}

			if(isTimeToFail())
			{
				//logger.info("Time to fail");
				fail();
			}
		}

		public boolean isRecoveryFloodDone()
		{
			if((recoverySentCount == neighborids.size()) && (recoveryRcvdCount == neighborids.size()))
			{
				//logger.info("Counts matched");
				return true;
			}

			return false;
		}

		public boolean isSentFloodDone()
		{
			if((sentSentCount[roundNo] == neighborids.size()) && (sentRcvdCount[roundNo] == neighborids.size()))
			{
				//logger.info("Sent Counts matched");
				return true;
			}

			return false;
		}

		public boolean isReceiveDone()
		{
			if((receiveSentCount == neighborids.size()) && (receiveRcvdCount == neighborids.size()))
			{
				//logger.info("Receive Counts matched");
				return true;
			}

			return false;
		}

		public boolean isFailureDoneSent()
		{
			if((failureDoneSentCount == neighborids.size()) && (failureDoneRcvdCount == neighborids.size()))
			{
				//logger.info("Failure Done Counts matched");
				return true;
			}

			return false;
		}

		public void clearVariables()
		{
			noOfCheckPointsAfterRecovery = 0;

			failuresEncountered++;
			failureEvents.removeFirst();

			recoverySentCount = 0;
			recoveryRcvdCount = 0;

			roundNo = 0;

			sentSentCount = new int[N];
			sentRcvdCount = new int[N];

			receiveSentCount = 0;
			receiveRcvdCount = 0;

			failureDoneSentCount = 0;
			failureDoneRcvdCount = 0;

			Globals.juangInRecovery = false;
			Globals.rebActive = currCheckpoint.getActiveState();

			synchronized (Globals.rebLock)
			{
				Globals.rebLock.notify();
			}
		}

		private void transmitLostMessages()
		{
			int diff, source;
			int recVal;
			REBMessage lostMessage = null;
			JuangMessage receiveMessage = null;

			while(!receiveQueue.isEmpty())
			{
				try
				{
					receiveMessage = (JuangMessage) receiveQueue.take();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}

				source = receiveMessage.getSourceId();
				recVal = receiveMessage.getVal();

				if(currCheckpoint.getSent().get(source) > recVal)
				{
					diff = currCheckpoint.getSent().get(source) - recVal;

					//logger.info("Diff: " + diff);

					for(int i = 0; i < diff; i++)
					{
						lostMessage = new REBMessage(nodeId, source, Constants.LOST, timestampService.getCurrTimestamp());

						network.sendToNeighbor(source, lostMessage);
					}
				}
				else
				{
					//logger.info("No lost message for : " + source);
				}
			}
		}

		@Override
		public void run()
		{
			Message m;
			JuangMessage juangMessage;
			REBMessage rebMessage;
			while(true)
			{
				try
				{
					m = messageQueue.take();

					//logger.info("REBProcessor processing : " + m);

					if(m.getType().equals(Constants.REB) && !Globals.juangInRecovery)
					{
						rebMessage = (REBMessage) m;
						
						takeCheckpoint(rebMessage);
					}
					else if(m.getType().equals(Constants.RECOVERY) && m.getSourceId() == nodeId)
					{
						recoverySentCount++;

						if(isRecoveryFloodDone())
						{
							floodSent();
						}
					}
					else if(m.getType().equals(Constants.RECOVERY) && m.getDestId() == nodeId)
					{
						if(!Globals.juangInRecovery)
						{
							Globals.juangInRecovery = true;
							Globals.rebActive = false;

							floodRecovery();
						}

						recoveryRcvdCount++;

						if(isRecoveryFloodDone())
						{
							floodSent();
						}
					}
					else if(m.getType().equals(Constants.SENT))
					{
						juangMessage = (JuangMessage) m;
						
						if(m.getSourceId() == nodeId)
						{
							sentSentCount[juangMessage.getRoundNo()]++;

							if(isSentFloodDone())
							{
								recover();

								floodSent();
							}
						}
						else if(m.getDestId() == nodeId)
						{
							sentRcvdCount[juangMessage.getRoundNo()]++;

							sentMsgQueue.add(juangMessage);

							if(isSentFloodDone())
							{
								recover();

								floodSent();
							}
						}
					}
					else if(m.getType().equals(Constants.RECEIVE))
					{
						juangMessage = (JuangMessage) m;
						
						if(m.getSourceId() == nodeId)
						{
							receiveSentCount++;

							if(isReceiveDone())
							{
								transmitLostMessages();

								floodFailureDone();
							}
						}
						else if(m.getDestId() == nodeId)
						{
							receiveRcvdCount++;

							receiveQueue.add(juangMessage);

							if(isReceiveDone())
							{
								transmitLostMessages();

								floodFailureDone();
							}
						}
					}
					else if(m.getType().equals(Constants.LOST))
					{
						rebMessage = (REBMessage) m;
						
						if(m.getDestId() == nodeId)
						{
							takeCheckpoint(rebMessage);
						}
					}
					else if(m.getType().equals(Constants.FAILURE_DONE))
					{
						if(m.getSourceId() == nodeId)
						{
							failureDoneSentCount++;

							if(isFailureDoneSent())
							{
								TesterMessage tstSentMsg = new TesterMessage(nodeId, Constants.BOTTOM, Constants.SENT, failuresEncountered, currCheckpoint.getActiveState(), timestampService.getCurrTimestamp());

								TesterMessage tstRcvdMsg = new TesterMessage(nodeId, Constants.BOTTOM, Constants.RECEIVE, failuresEncountered, currCheckpoint.getActiveState(), timestampService.getCurrTimestamp());

								//logger.info("Tester CP: " + currCheckpoint.toString());
								//logger.info("Tester Message: " + tstSentMsg.toString());

								network.sendToTester(tstSentMsg);
								network.sendToTester(tstRcvdMsg);

								clearVariables();
							}
						}
						else if(m.getDestId() == nodeId)
						{
							failureDoneRcvdCount++;

							if(isFailureDoneSent())
							{
								TesterMessage tstSentMsg = new TesterMessage(nodeId, Constants.BOTTOM, Constants.SENT, failuresEncountered, currCheckpoint.getActiveState(), timestampService.getCurrTimestamp());

								TesterMessage tstRcvdMsg = new TesterMessage(nodeId, Constants.BOTTOM, Constants.RECEIVE, failuresEncountered, currCheckpoint.getActiveState(), timestampService.getCurrTimestamp());

								//logger.info("Tester CP: " + currCheckpoint.toString());
								//logger.info("Tester Message: " + tstSentMsg.toString());

								network.sendToTester(tstSentMsg);
								network.sendToTester(tstRcvdMsg);

								clearVariables();
							}
						}
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void setTimestampService(TimestampService timestampService)
	{
		this.timestampService = timestampService;
	}

	@Override
	public void run()
	{
		Thread messageProcessor = new Thread(new MessageProcessor());
		messageProcessor.start();
	}
}
