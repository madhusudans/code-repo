
public class JuangMessage extends Message
{
	private int val;
	private int roundNo;
	
	public JuangMessage(int src, int dest, String type, int val, int roundNo)
	{
		super(src, dest, type);
		this.val = val;
		this.roundNo = roundNo;
	}
	
	public JuangMessage(String msg)
	{
		super(msg);
		
		msg = stripMessage(msg);
		String[] parts = msg.trim().split("~");
		this.val = Integer.parseInt(parts[3].trim());
		this.roundNo = Integer.parseInt(parts[4].trim());
	}

	public int getVal()
	{
		return val;
	}

	public void setVal(int val)
	{
		this.val = val;
	}

	public int getRoundNo()
	{
		return roundNo;
	}

	public void setRoundNo(int roundNo)
	{
		this.roundNo = roundNo;
	}
	
	public String toString()
	{
		return super.toString()+"~"+val+"~"+roundNo+Constants.END_TAG;
	}
}
