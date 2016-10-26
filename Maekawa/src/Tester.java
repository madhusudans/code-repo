import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class Tester
{
	private InetSocketAddress address;
	private static final int MESSAGE_SIZE = 200;
	private static final String CS_EVENT = "C";
	private static final String ACK = "A";
	private Object csLock = new Object();
	private boolean violation;
	private int totalNumberOfNodes;
	private int totalNumberOfRequests;
	private int requestCount;
	private boolean yesWritten;
	private String configFileName;

	private int[] testerVectorClock;

	public Tester(InetSocketAddress address, int totalNumberOfNodes, int totalNumberOfRequests, String configFileName)
	{
		this.address = address;
		violation = false;
		this.totalNumberOfNodes = totalNumberOfNodes;
		this.totalNumberOfRequests = totalNumberOfRequests;
		requestCount = 0;
		testerVectorClock = new int[totalNumberOfNodes];
		yesWritten = false;
		this.configFileName = configFileName;
	}

	private String byteToString(ByteBuffer byteBuffer)
	{
		byteBuffer.position(0);
		byteBuffer.limit(MESSAGE_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
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

	private synchronized void incrementRequestCount()
	{
		requestCount++;
	}

	private int[] getIntArr(String arr)
	{

		int index = arr.indexOf("!END");
		arr = arr.substring(0,index);

		String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
		int[] results = new int[items.length];
		for (int i = 0; i < items.length; i++) {
			try {
				results[i] = Integer.parseInt(items[i].trim());
			} catch (NumberFormatException nfe) {}
		}

		return results;
	}

	public void updateVectorClock(int[] incomingVectorClock)
	{
		for(int i = 0; i < testerVectorClock.length; i++)
		{
			testerVectorClock[i] = Math.max(testerVectorClock[i], incomingVectorClock[i]);
		}
	}

	private boolean compareVectorClock(int[] incomingVectorClock)
	{
		boolean testerClockLesser = true;
		boolean changed = false;

		for(int i = 0; i < incomingVectorClock.length; i++)
		{
			if(testerVectorClock[i] < incomingVectorClock[i])
			{
				if(!testerClockLesser)
				{
					if(changed)
					{
						return false;
					}
				}
				testerClockLesser = true;
				changed = true;
			}
			else if(testerVectorClock[i] > incomingVectorClock[i])
			{
				if(testerClockLesser)
				{
					if(changed)
					{
						return false;
					}
				}

				testerClockLesser = false;
				changed = true;
			}
		}

		return true;
	}

	class TestServerReader implements Runnable
	{
		SctpChannel channel;
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);
		String message;
		int nodeId = -1;

		public TestServerReader(SctpChannel channel)
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


				if(msg.getType().equals(CS_EVENT))
				{
					synchronized (csLock)
					{
						if(nodeId == -1)
						{
							nodeId =  Integer.parseInt(msg.getFromMessage(0).trim());
						}

						String[] testParts = message.split("~");
						int[] incomingVectorClock = null;

						if(testParts.length == 5)
						{
							incomingVectorClock = getIntArr(testParts[4]);

						}

						boolean res = compareVectorClock(incomingVectorClock);
						if(!res)
						{
							if(!violation)
							{
								//put no in output file

								try {
									BufferedWriter bw = null;
									FileWriter fw = new FileWriter(new File(configFileName+"_result.txt"));
									bw = new BufferedWriter(fw);
									bw.write("No");
									bw.flush();
									bw.close();

									violation = true;
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							violation = true;
						}
						else
						{
							if(!violation && !yesWritten)
							{
								try {
									BufferedWriter bw = null;
									FileWriter fw = new FileWriter(new File(configFileName+"_result.txt"));
									bw = new BufferedWriter(fw);
									bw.write("Yes");
									bw.flush();
									bw.close();
									
									yesWritten = true;
									
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}


						updateVectorClock(incomingVectorClock);

						Message ackMsg = new Message(msg.getNodeId(), ACK, msg.getReqNo(), Integer.MIN_VALUE);
						try
						{
							sendMessage(ackMsg.toString(), channel);
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

	public void setUpNetworking()
	{
		Thread t;
		SctpServerChannel sctpServerChannel = null;
		try
		{
			//Open a server channel
			sctpServerChannel = SctpServerChannel.open();
			//Create a socket addess in the current machine at port 5000
			InetSocketAddress serverAddr = address;
			//Bind the channel's socket to the server in the current machine at port 5000
			sctpServerChannel.bind(serverAddr);
			//Server goes into a permanent loop accepting connections from clients			
			while(true)
			{
				SctpChannel channel = sctpServerChannel.accept();
				t = new Thread(new TestServerReader(channel));
				t.start();
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		String host = args[0];
		int portNo = Integer.parseInt(args[1]);
	
		int totalNumberOfNodes = Integer.parseInt(args[2]);
		int totalNumberOfRequests = Integer.parseInt(args[3]);

		Tester tester = new Tester(new InetSocketAddress(host, portNo), totalNumberOfNodes, totalNumberOfRequests, args[4]);
		tester.setUpNetworking();
	}
}
