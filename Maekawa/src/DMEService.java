import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class DMEService
{
	private int nodeId;
	private int timeStamp;
	private InetSocketAddress address; //my address
	private CustomLatch requestLatch;
	private CustomLatch testingLatch;
	private Object serverReceiveLock = new Object();
	private Object clientReceiveLock = new Object();
	private int quorumSize;
	private ArrayList<Neighbor> quorumNeighbors;
	private ArrayList<Neighbor> dualNeighbors;
	private static final int MESSAGE_SIZE = 200;
	private static final String REQUEST = "R";
	private static final String GRANT = "G";
	private static final String INQUIRE = "I";
	private static final String YIELD = "Y";
	private static final String DONE = "D";
	private static final String FAILED = "F";
	private static final String TEST = "T";
	private static final String TEST_FORWARD = "X";
	private static final String ACK_FORWARD = "Z";
	private static final String ACK = "A";
	private volatile PriorityQueue<Message> requestQueue;
	private volatile boolean csActive;
	private Message grantedForMsg;
	private ConcurrentHashMap<Integer, SctpChannel> serverClientChannel;
	private boolean inquireSent;
	private boolean failed;
	private SctpServerChannel sctpServerChannel = null;
	
	private SctpChannel testChannel;
	
	private int[] vectorClock;
	private static final String END_TAG = "!END";
	private volatile int forwardAckCount;
	private volatile int forwardSentCount;


	//node inner class
	class Neighbor
	{
		public int id;
		public InetSocketAddress address;
		public SctpChannel sctpChannel;

		public Neighbor(int id, InetSocketAddress address)
		{
			this.id = id;
			this.address = address;
		}

		public InetSocketAddress getAddress()
		{
			return address;
		}

		public void setSctpChannel(SctpChannel sctpChannel)
		{
			this.sctpChannel = sctpChannel;
		}

		public SctpChannel getSctpChannel()
		{
			return sctpChannel;
		}

		public int getId()
		{
			return id;
		}
	}

	public DMEService(int id, HashMap<Integer, String[]> nodeDetails, ArrayList<Integer> quorumSet, ArrayList<Integer> dualQuorumSet, int testerPort, int N)
	{
		
		timeStamp = 0;
		serverClientChannel = new ConcurrentHashMap<Integer, SctpChannel>();
		nodeId = id;
		csActive = false;
		inquireSent = false;

		grantedForMsg = null;
		failed = false;
		vectorClock = new int[N];
		
		forwardAckCount = 0;

		
		requestQueue = new PriorityQueue<Message>(11, new Comparator<Message>() {
			@Override
			public int compare(Message msg1, Message msg2)
			{
				if(msg1.getTimeStamp() == msg2.getTimeStamp())
				{
					return msg1.getNodeId() - msg2.getNodeId();
				}

				return msg1.getTimeStamp() - msg2.getTimeStamp();
			}
		}); 

		quorumNeighbors = new ArrayList<Neighbor>();
		dualNeighbors = new ArrayList<Neighbor>();

		Neighbor node;

		//server who are my quorum set...to send request message to their servers
		if(!quorumSet.isEmpty())
		{
			for(int i = 0; i < quorumSet.size(); i++)
			{
				int quorumId = quorumSet.get(i);

				int quorumPort = Integer.parseInt(nodeDetails.get(quorumId)[2]);
				try {
					node = new Neighbor(quorumId, new InetSocketAddress(InetAddress.getByName(nodeDetails.get(quorumId)[1]), quorumPort));
					quorumNeighbors.add(node);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}

			quorumSize = quorumSet.size();
		}

		//servers to whose quorum set I belong...to send inquire message to their servers
		if(!dualQuorumSet.isEmpty())
		{
			for(int i = 0; i < dualQuorumSet.size(); i++)
			{
				int memberId = dualQuorumSet.get(i);

				int memberPort = Integer.parseInt(nodeDetails.get(memberId)[2]);
				try {
					node = new Neighbor(memberId, new InetSocketAddress(InetAddress.getByName(nodeDetails.get(memberId)[1]), memberPort));
					dualNeighbors.add(node);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}

		//getting my address
		int myPort = Integer.parseInt(nodeDetails.get(nodeId)[2]);
		try
		{
			address = new InetSocketAddress(InetAddress.getByName(nodeDetails.get(nodeId)[1]), myPort);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}

		try
		{
			//Open a server channel
			sctpServerChannel = SctpServerChannel.open();
			//Create a socket addess in the current machine at port 5000
			InetSocketAddress serverAddr = address;
			//Bind the channel's socket to the server in the current machine at port 5000
			sctpServerChannel.bind(serverAddr);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}

		Thread server = new Thread(new Runnable() {
			public void run()
			{
				//start servers
				goServer();
			}
		});

		server.start();

		//start client and create channels to quorum servers
		boolean connected = false;
		try
		{
			Iterator<Neighbor> it = quorumNeighbors.iterator();
			while(it.hasNext())
			{
				//Create a socket address for  server at net01 at port 5000
				Neighbor neighbor = it.next();
				SocketAddress socketAddress = neighbor.getAddress();
				//Open a channel. NOT SERVER CHANNEL
		
				connected = false;
				SctpChannel sctpChannel = null;

				//waiting till server is up
				while(!connected)
				{
					sctpChannel = SctpChannel.open();
					//Bind the channel's socket to a local port. Again this is not a server bind
					try
					{
						sctpChannel.bind(null);
					}
					catch(IOException ioe)
					{
						ioe.printStackTrace();
					}

					//Connect the channel's socket to  the remote server
					try
					{
						sctpChannel.connect(socketAddress);
						connected = true;
					}
					catch(ConnectException ce)
					{
						connected = false;
						try
						{ 
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				neighbor.setSctpChannel(sctpChannel);
			}
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

		connected = false;
		try
		{
			Iterator<Neighbor> it = dualNeighbors.iterator();
			while(it.hasNext())
			{
				//Create a socket address for  server at net01 at port 5000
				Neighbor neighbor = it.next();
				SocketAddress socketAddress = neighbor.getAddress();
				//Open a channel. NOT SERVER CHANNEL

				connected = false;
				SctpChannel sctpChannel = null;

				//waiting till server is up
				while(!connected)
				{
					sctpChannel = SctpChannel.open();
					//Bind the channel's socket to a local port. Again this is not a server bind
					try
					{
						sctpChannel.bind(null);
					}
					catch(IOException ioe)
					{
						ioe.printStackTrace();
					}

					//Connect the channel's socket to  the remote server
					try
					{
						sctpChannel.connect(socketAddress);
						connected = true;
					}
					catch(ConnectException ce)
					{
						connected = false;
						try
						{ 
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				neighbor.setSctpChannel(sctpChannel);
			}
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

		Thread clientReceive = new Thread(new Runnable() {
			public void run()
			{
				clientReceiveMessages();
			}
		});

		clientReceive.start();
	}

	public synchronized void setCriticalSection()
	{
		csActive = true;
	}

	class IncomingReader implements Runnable
	{
		SctpChannel sctpChannel;
		public static final int MESSAGE_SIZE = 200;
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		String message;
		int serverId = -1;

		public IncomingReader(SctpChannel sctpChannel)
		{
			this.sctpChannel = sctpChannel;
		}

		@Override
		public void run() 
		{
			MessageInfo messageInfo = null;
			while(true)
			{
				//TODO synchronized block
				try
				{
					byteBuffer.clear();
					messageInfo = sctpChannel.receive(byteBuffer,null,null);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				if(messageInfo != null)
				{
					message = byteToString(byteBuffer);
				}

				Message msg = new Message(message);

				if(serverId == -1)
				{
					serverId = Integer.parseInt(msg.getFromMessage(0).trim());
				}
				
				incrementTimeStamp(timeStamp, msg.getTimeStamp());

				synchronized (clientReceiveLock)
				{
					if(msg.getType().equals(GRANT))
					{
						//TODO put in synchorized block
						//count down
						int rno = msg.getReqNo();
						requestLatch.countDown();

						if(requestLatch.getCount() == 0)
						{
							failed = false;
							csActive = true;
						}
					}
					else if(msg.getType().equals(INQUIRE))
					{
						
						if(!csActive && (requestLatch != null && requestLatch.getRequestNo() == msg.getReqNo()) && failed)
						{
							//count up
							int rno = msg.getReqNo();
						
							incrementTimeStamp(timeStamp, Integer.MIN_VALUE);

							msg = new Message(msg.getNodeId(), YIELD, msg.getReqNo(), timeStamp);
							try
							{
								sendMessage(msg.toString(), sctpChannel);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}

							requestLatch.countUp();
						}
					}
					else if(msg.getType().equals(FAILED))
					{
						failed = true;
					}
					else if(msg.getType().equals(TEST_FORWARD))
					{
						message = msg.stripMessage(message);
						String[] testParts = message.split("~");
						
						if(testParts.length == 5)
						{
							int[] incomingVectorClock = getIntArr(testParts[4]);
							updateVectorClock(incomingVectorClock);
						}
						
						
						msg = new Message(nodeId, ACK_FORWARD, Integer.MIN_VALUE, Integer.MIN_VALUE);
						
						try
						{
							sendMessage(msg.toString(), sctpChannel);
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					else if(msg.getType().equals(ACK))
					{
						
						testingLatch.countDown();
					}
				}
			}
		}
	}

	public void clientReceiveMessages()
	{
		Iterator<Neighbor> it = quorumNeighbors.iterator();
		Thread t = null;

		while(it.hasNext())
		{
			t = new Thread(new IncomingReader(it.next().getSctpChannel()));
			t.start();
		}

		it = dualNeighbors.iterator();
		t = null;

		while(it.hasNext())
		{
			t = new Thread(new IncomingReader(it.next().getSctpChannel()));
			t.start();
		}
	}

	private void goServer()
	{
		Thread t;
		try
		{			
			while(true)
			{
				SctpChannel channel = sctpServerChannel.accept();
				t = new Thread(new ServerIncomingReader(channel));
				t.start();
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	private String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
	}

	private synchronized void incrementTimeStamp(int ts1, int ts2)
	{
		timeStamp = Integer.max(ts1, ts2) + 1;
	}

	class ServerIncomingReader implements Runnable
	{
		SctpChannel channel;
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		String message;
		int clientId = -1;

		public ServerIncomingReader(SctpChannel channel)
		{
			this.channel = channel;
		}

		@Override
		public void run()
		{
			MessageInfo messageInfo = null;

			while(true)
			{				
				byteBuffer.clear();
				try {
					messageInfo = channel.receive(byteBuffer,null,null);
				} catch (IOException e) {
					e.printStackTrace();
				}

				if(messageInfo != null)
				{
					message = byteToString(byteBuffer);
				}

				Message msg = new Message(message);
			
				incrementTimeStamp(timeStamp, Integer.parseInt(msg.getFromMessage(3)));

				synchronized (serverReceiveLock)
				{
					if(msg.getType().equals(REQUEST))
					{
						requestQueue.add(msg);
			
						if(clientId == -1)
						{
							clientId =  Integer.parseInt(msg.getFromMessage(0).trim());
							serverClientChannel.put(clientId, channel);
						}

			
			
						if(grantedForMsg != null)
						{
			
							if(msg.getTimeStamp() < grantedForMsg.getTimeStamp() || 
									((msg.getTimeStamp() == grantedForMsg.getTimeStamp()) && msg.getNodeId() < grantedForMsg.getNodeId()))
							{
								if(!inquireSent)
								{
			
									SctpChannel inquireChannel = serverClientChannel.get(grantedForMsg.getNodeId());

									incrementTimeStamp(timeStamp, Integer.MIN_VALUE);

									Message inquireMsg = new Message(grantedForMsg.getNodeId(), INQUIRE, grantedForMsg.getReqNo(), timeStamp);
									try
									{
										sendMessage(inquireMsg.toString(), inquireChannel);
										inquireSent = true;
									}
									catch (IOException e)
									{
										e.printStackTrace();
									}
								}
							}
							else
							{
								incrementTimeStamp(timeStamp, Integer.MIN_VALUE);

								Message inquireMsg = new Message(msg.getNodeId(), FAILED, msg.getReqNo(), timeStamp);
								try
								{
									sendMessage(inquireMsg.toString(), channel);
								}
								catch (IOException e)
								{
									e.printStackTrace();
								}

							}
						}
						else
						{
							grantedForMsg = requestQueue.remove();
			
							try
							{
								incrementTimeStamp(timeStamp, Integer.MIN_VALUE);
								msg = new Message(grantedForMsg.getNodeId(), GRANT, grantedForMsg.getReqNo(), timeStamp);
								sendMessage(msg.toString(), channel);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
					else if(msg.getType().equals(YIELD))
					{
						inquireSent = false;

						requestQueue.add(grantedForMsg);

						grantedForMsg = requestQueue.remove();
						try
						{
							incrementTimeStamp(timeStamp, Integer.MIN_VALUE);
							msg = new Message(grantedForMsg.getNodeId(), GRANT, grantedForMsg.getReqNo(), timeStamp);

							sendMessage(msg.toString(), serverClientChannel.get(msg.getNodeId()));
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					else if(msg.getType().equals(DONE))
					{
						inquireSent = false;
						if(!requestQueue.isEmpty())
						{
							grantedForMsg = requestQueue.remove();
							if(!requestQueue.isEmpty())



							try
							{
								incrementTimeStamp(timeStamp, Integer.MIN_VALUE);
								msg = new Message(grantedForMsg.getNodeId(), GRANT, grantedForMsg.getReqNo(), timeStamp);

								sendMessage(msg.toString(), serverClientChannel.get(msg.getNodeId()));


							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						else
						{
							grantedForMsg = null;
						}

					}
					else if(msg.getType().equals(TEST))
					{
						testChannel = channel;
						
						message = msg.stripMessage(message);
						String[] testParts = message.split("~");
						if(testParts.length == 5)
						{
							int[] incomingVectorClock = getIntArr(testParts[4]);
							updateVectorClock(incomingVectorClock);
						}
						
						String vectorClockStr = Arrays.toString(vectorClock);
						
						
						forwardAckCount = 0;
						forwardSentCount = 0;
						
						Iterator it = serverClientChannel.entrySet().iterator();
						while (it.hasNext())
						{
							Map.Entry pair = (Map.Entry)it.next();
							int id = (int) pair.getKey();
							
							Message msgForward = new Message(nodeId, TEST_FORWARD, Integer.MIN_VALUE, Integer.MIN_VALUE);
							String temp = msgForward.toString();
							temp = msgForward.stripMessage(temp);
							temp = temp + "~" + vectorClockStr + END_TAG;
							
							try
							{
								
								sendMessage(temp, serverClientChannel.get(id));
								forwardSentCount++;
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
						
						
					}
					else if(msg.getType().equals(ACK_FORWARD))
					{
						forwardAckCount++;
						
						if(forwardAckCount == forwardSentCount)
						{
							
							Message msgACK = new Message(nodeId, ACK, Integer.MIN_VALUE, Integer.MIN_VALUE);
							
							try
							{
								sendMessage(msgACK.toString(), testChannel);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
	
	public void updateVectorClock(int[] incomingVectorClock)
	{
		for(int i = 0; i < vectorClock.length; i++)
		{
			vectorClock[i] = Math.max(vectorClock[i], incomingVectorClock[i]);
		}
	}
	
	private int[] getIntArr(String arr)
	{
		String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
		int[] results = new int[items.length];
		for (int i = 0; i < items.length; i++) {
		    try {
		        results[i] = Integer.parseInt(items[i].trim());
		    } catch (NumberFormatException nfe) {};
		}
		
		return results;
	}

	private void sendMessage(String message, SctpChannel channel) throws IOException
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
		byteBuffer.clear();
		byteBuffer.put(message.getBytes());
		byteBuffer.flip();
		channel.send(byteBuffer,messageInfo);
	}

	private void sendToQuorumSet(int appId, int requestNo, String msgType)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

		incrementTimeStamp(timeStamp, Integer.MIN_VALUE);

		Message msg = new Message(appId, msgType, requestNo, timeStamp);

		String message = msg.toString();
	
		Iterator<Neighbor> it = this.quorumNeighbors.iterator();
		while(it.hasNext())
		{
			Neighbor neighbor = it.next();
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			byteBuffer.clear();

			byteBuffer.put(message.getBytes());				

			byteBuffer.flip();

			try
			{
				neighbor.getSctpChannel().send(byteBuffer,messageInfo);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}	
		}
	}

	public boolean csEnter(int appId, int requestNo)
	{
		requestLatch = new CustomLatch(quorumSize, requestNo);
		sendToQuorumSet(appId, requestNo, REQUEST);

		try
		{
			requestLatch.await();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		return true;
	}

	public void csLeave(int appId, int requestNo)
	{
		requestLatch = null;
		sendToQuorumSet(appId, requestNo, DONE);
		csActive = false;
	}
	
	private void incrementVectorClock()
	{
		vectorClock[nodeId]++;
	}
	
	public void sendTestToQuorum(String msg)
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

	
		Iterator<Neighbor> it = this.quorumNeighbors.iterator();
		while(it.hasNext())
		{
			Neighbor neighbor = it.next();
			MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			byteBuffer.clear();

			byteBuffer.put(msg.getBytes());				

			byteBuffer.flip();

			try
			{
				neighbor.getSctpChannel().send(byteBuffer,messageInfo);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}	
		}

	}
	
	public void sendVectorClock(int requestNo)
	{
		incrementVectorClock();
		
		String vectorClockStr = Arrays.toString(vectorClock);
		String str;
		
		Message msg = new Message(nodeId, TEST, Integer.MIN_VALUE, Integer.MIN_VALUE);
		str = msg.toString();
		str = msg.stripMessage(str);
		str = str + "~" + vectorClockStr + END_TAG;
		
		
		testingLatch = new CustomLatch(quorumSize, Integer.MIN_VALUE);
		
		sendTestToQuorum(str);

		try
		{
			testingLatch.await();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}
	
	public int[] getVectorClock()
	{
		return vectorClock;
	}
}
