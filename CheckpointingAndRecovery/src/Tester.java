import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class Tester
{
	private InetSocketAddress address; //my address
	private Selector serverSelector;
	private Logger logger;
	private static final int MESSAGE_SIZE = 100;
	private int N;
	private int failures;
	private int failuresEncountered;

	public Tester()
	{

		logger = Logger.getLogger("TesterLog");

		FileHandler fh = null; 
		try
		{
			fh = new FileHandler("/home/012/r/rx/rxr151330/AOS3_nologs/TesterLog.log");
			logger.addHandler(fh);
		}
		catch (SecurityException e1)
		{
			logger.log(Level.INFO, e1.getMessage(), e1);
		}
		catch (IOException e1)
		{
			logger.log(Level.INFO, e1.getMessage(), e1);
		}

		fh.setFormatter(new SimpleFormatter());
		
		failuresEncountered = 0;
	}

	public void parseInput(int port, String domain, int failures, int N)
	{
		//logger.info("Inside parseInput");
		
		this.failures = failures;
		this.N = N;

		try
		{
			this.address = new InetSocketAddress(InetAddress.getByName(domain), port);
			//logger.info("tester address: " + this.address);
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}

		try
		{
			serverSelector = Selector.open();
		}
		catch (Exception e)
		{
			logger.log(Level.INFO, e.getMessage(), e);
		}
	}
	
	public String processRead(SelectionKey key)
	{
		SctpChannel sChannel = (SctpChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_SIZE);
		MessageInfo msgInfo = null;

		try
		{
			msgInfo = sChannel.receive(buffer, null, null);
		}
		catch (Exception e)
		{
			logger.log(Level.INFO, e.getMessage(), e);
		}

		if(msgInfo != null)
		{
			int bytesCount = buffer.position();
			if (bytesCount > 0)
			{
				buffer.flip();
				return new String(buffer.array());
			}
		}

		return null;
	}
	
	public void processReadySet(Set readySet)
	{
		Iterator iterator = readySet.iterator();

		while (iterator.hasNext())
		{
			SelectionKey key = (SelectionKey) iterator.next();
			iterator.remove();

			if (key.isAcceptable())
			{
				SctpServerChannel ssChannel = (SctpServerChannel) key.channel();
				SctpChannel clientChannel;
				try
				{
					clientChannel = (SctpChannel) ssChannel.accept();
					clientChannel.configureBlocking(false);
					clientChannel.register(key.selector(), SelectionKey.OP_READ);
				}
				catch (Exception e)
				{
					logger.log(Level.INFO, e.getMessage(), e);
				}
			}

			if (key.isReadable())
			{
				String msg = processRead(key);
				//logger.info("Read message: " + msg);

				if(msg != null)
				{
					TesterMessage message = new TesterMessage(msg);
					logger.info(message.toString());
					
				}
			}
		}
	}
	
	public void startServer()
	{
		SctpServerChannel sctpServerChannel = null;
		try
		{
			sctpServerChannel = SctpServerChannel.open();
			sctpServerChannel.configureBlocking(false);
			//Create a socket addess in the current machine at port 5000
			InetSocketAddress serverAddr = address;
			
			//logger.info("tester address start: " + this.address);
			
			//Bind the channel's socket to the server in the current machine at port 5000
			sctpServerChannel.bind(serverAddr);

			sctpServerChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
			
			//logger.info("tester Server up!!1");
		}
		catch (Exception e)
		{
			logger.log(Level.INFO, e.getMessage(), e);
		}
		
		while (failuresEncountered < failures)
		{
			try
			{
				if (serverSelector.select() <= 0)
				{
					continue;
				}

				processReadySet(serverSelector.selectedKeys());
			}
			catch (Exception ioe)
			{
				logger.log(Level.INFO, ioe.getMessage(), ioe);
			}
		}
	}

	public static void main(String[] args)
	{
		int port = Integer.parseInt(args[0]);
		String domain = args[1];
		int N = Integer.parseInt(args[2]);
		String failures = args[3];

		Tester tester = new Tester();
		tester.parseInput(port, domain, Integer.parseInt(failures), N);
		
		tester.startServer();
	}
}
