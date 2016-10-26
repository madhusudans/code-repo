import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

public class App
{
	private long interRequestDelayMean;
	private DMEService dmeService;
	private long meanCsExecutionTime;
	private int appId;
	private ExponentialDistribution expInterReqDelay;
	private ExponentialDistribution expCsExecTime;
	private InetSocketAddress testerAddress;
	private SctpChannel testerChannel = null;
	private static final String CS_EVENT = "C";
	private static final String CS_EVENT_DONE = "L";
	private static final String ACK = "A";
	private static final int MESSAGE_SIZE = 200;
	private boolean testing;
	private static final String END_TAG = "!END";


	public App(long ird, long cset, DMEService dmeServ, int appId)
	{
		interRequestDelayMean = ird;
		dmeService = dmeServ;
		meanCsExecutionTime = cset;
		this.appId = appId;
		testing = false;

		expInterReqDelay = new ExponentialDistribution(interRequestDelayMean);
		expCsExecTime = new ExponentialDistribution(meanCsExecutionTime);
	}

	public void setTesting()
	{
		testing = true;
	}

	public void setUpNetworking(int testerPort, String testerHost)
	{
		boolean connected = false;
		testerAddress = new InetSocketAddress(testerHost, testerPort);
		SocketAddress socketAddress = testerAddress;
		//Open a channel. NOT SERVER CHANNEL
		
		connected = false;
		testerChannel = null;

		//waiting till server is up
		while(!connected)
		{
			try
			{
				testerChannel = SctpChannel.open();
				//Bind the channel's socket to a local port. Again this is not a server bind
				//sctpChannel.bind(new InetSocketAddress(neighbor.getNeighborAddress().getPort()));
				try
				{
					testerChannel.bind(null);
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}

				//Connect the channel's socket to  the remote server
				try
				{
					testerChannel.connect(socketAddress);
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
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	}

	public void start(int numberOfReq)
	{	
		ByteBuffer byteBuffer = ByteBuffer.allocate(MESSAGE_SIZE);

		int requestNo = 0;

		long interRequestDelay, csExecutionTime;

		Message msg;
		MessageInfo messageInfo;
		int[] vectorClock;

		for(int i = 0; i < numberOfReq; i++)
		{
			boolean res = dmeService.csEnter(appId, requestNo);

			
			if(res)
			{
				/*
				 * 
				 * Testing
				 */
				if(testing)
				{
					dmeService.sendVectorClock(requestNo);
					vectorClock = dmeService.getVectorClock();
					String vectorClockStr = Arrays.toString(vectorClock);
					String str;

					msg = new Message(appId, CS_EVENT, i, Integer.MIN_VALUE);
					str = msg.toString();
					str = msg.stripMessage(str);
					str = str + "~" + vectorClockStr + END_TAG;
					/*
					 * 
					 * Testing
					 */
					
					messageInfo = MessageInfo.createOutgoing(null,0);
					byteBuffer.clear();

					byteBuffer.put(str.getBytes());				

					byteBuffer.flip();

					try
					{
						testerChannel.send(byteBuffer,messageInfo);
						
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					try
					{
						byteBuffer.clear();
						messageInfo = testerChannel.receive(byteBuffer,null,null);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					try
					{
						csExecutionTime = Math.round(expCsExecTime.sample());
						//doing some work
						Thread.sleep(csExecutionTime);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}

			dmeService.csLeave(appId, requestNo++);

			interRequestDelay =  Math.round(expInterReqDelay.sample());
			
			try
			{
				Thread.sleep(interRequestDelay);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println("DONE");
	}

	public static void main(String[] args)
	{
		
		int N = Integer.parseInt(args[0]);
		long interReqDelay = Long.parseLong(args[1]);
		long csExecTime = Long.parseLong(args[2]);
		int numberOfReq = Integer.parseInt(args[3]);
		int nodeId = Integer.parseInt(args[6]);
		int testerPort = Integer.parseInt(args[7]);
		String testerHost = args[8];
		String testing = args[9];

		String nodeLocations = args[4];
		String[] locationsArr = nodeLocations.split("#");
		HashMap<Integer, String[]> nodeDetails = new HashMap<Integer, String[]>();
		int[] nodeIds = new int[N];

		for(int i = 0; i < locationsArr.length; i++)
		{
			int tempNodeId = Integer.parseInt(locationsArr[i].split(" ")[0]);
			nodeDetails.put(tempNodeId, locationsArr[i].split(" "));
			nodeIds[i] = tempNodeId;
		}


		String quorumLocations = args[5];
		String[] quorumLocationsArr = quorumLocations.split("#");
		HashMap<Integer, ArrayList<Integer>> quorumAllSet = new HashMap<Integer, ArrayList<Integer>>();

		for(int i = 0; i < quorumLocationsArr.length; i++)
		{
			String[] splitQuorumLocs = quorumLocationsArr[i].split(" ");
			ArrayList<Integer> temp = new ArrayList<Integer>();

			for(int j = 0; j < splitQuorumLocs.length; j++)
			{
				temp.add(Integer.parseInt(splitQuorumLocs[j]));
			}

			quorumAllSet.put(nodeIds[i], temp);
		}

		ArrayList<Integer> dualQuorumSet = new ArrayList<Integer>();

		Iterator it = quorumAllSet.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry pair = (Map.Entry)it.next();
			ArrayList<Integer> tempArr = (ArrayList<Integer>)pair.getValue();

			for(Integer t : tempArr)
			{
				if(t == nodeId)
				{
					dualQuorumSet.add((Integer)pair.getKey());
					break;
				}
			}
		}

		DMEService dmeService = new DMEService(nodeId, nodeDetails, quorumAllSet.get(nodeId), dualQuorumSet, testerPort, N);

		if(!quorumAllSet.get(nodeId).isEmpty())
		{
			App app = new App(interReqDelay, csExecTime, dmeService, nodeId);

			if(testing.equalsIgnoreCase("yes"))
			{
				app.setUpNetworking(testerPort, testerHost);
				app.setTesting();
			}

			app.start(numberOfReq);
		}
	}
}
