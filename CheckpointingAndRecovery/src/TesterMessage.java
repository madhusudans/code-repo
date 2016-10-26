import java.util.Arrays;

public class TesterMessage extends Message 
{
	private int failureNumber;
	private boolean activeState;
	private int[] vectorClock;
	
	public TesterMessage(int src, int dest, String type, int failureNumber, boolean activeState, int[] vectorClock)
	{
		super(src, dest, type);
		this.failureNumber = failureNumber;
		this.activeState = activeState;
		this.vectorClock = vectorClock;
	}
	
	public TesterMessage(String msg)
	{
		super(msg);
		
		msg = stripMessage(msg);
		String[] parts = msg.trim().split("~");
		this.failureNumber = Integer.parseInt(parts[3].trim());
		this.activeState = Boolean.parseBoolean(parts[4].trim());
		
		String[] items = parts[5].trim().replaceAll("\\[", "").replaceAll("\\]", "").split(",");
		vectorClock = new int[items.length];
		
		for(int i = 0; i < items.length; i++)
		{
			vectorClock[i] = Integer.parseInt(items[i].trim());
		}
	}

	public int getFailureNumber()
	{
		return failureNumber;
	}

	public void setFailureNumber(int failureNumber)
	{
		this.failureNumber = failureNumber;
	}

	public boolean isActiveState()
	{
		return activeState;
	}

	public void setActiveState(boolean activeState)
	{
		this.activeState = activeState;
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
		return super.toString()+"~"+failureNumber+"~"+activeState+"~"+Arrays.toString(vectorClock)+Constants.END_TAG;
	}
}
