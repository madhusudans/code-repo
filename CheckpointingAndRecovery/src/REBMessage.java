import java.util.Arrays;

public class REBMessage extends Message
{
	private int[] vectorClock;
	
	public REBMessage(int src, int dest, String type, int[] vectorClock)
	{
		super(src, dest, type);
		
		this.vectorClock = vectorClock;
	}
	
	public REBMessage(String msg)
	{
		super(msg);
		
		msg = stripMessage(msg);
		String[] parts = msg.trim().split("~");
		
		String[] items = parts[3].trim().replaceAll("\\[", "").replaceAll("\\]", "").split(",");
		vectorClock = new int[items.length];
		
		for(int i = 0; i < items.length; i++)
		{
			vectorClock[i] = Integer.parseInt(items[i].trim());
		}
		
	}
	
	public int[] getVectorClock()
	{
		return vectorClock;
	}
	
	public void setVectorClock(int[] vectorClock)
	{
		this.vectorClock = vectorClock;
	}
	
	public String toString()
	{
		return super.toString()+"~"+Arrays.toString(vectorClock)+Constants.END_TAG;
	}
}
