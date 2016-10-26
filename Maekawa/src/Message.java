
public class Message implements Comparable<Message>
{
	private int nodeId;
	private String type;
	private int reqNo;
	private int timeStamp;
	private static final String END_TAG = "!END";
	
	public Message(int nodeId, String type, int reqNo, int timeStamp)
	{
		this.nodeId = nodeId;
		this.type = type;
		this.reqNo = reqNo;
		this.timeStamp = timeStamp;
	}
	
	public Message(String msg)
	{
		msg = stripMessage(msg);
		String[] parts = msg.trim().split("~");
		this.nodeId = Integer.parseInt(parts[0].trim());
		this.type = parts[1].trim();
		this.reqNo = Integer.parseInt(parts[2].trim());
		this.timeStamp = Integer.parseInt(parts[3].trim());
	}
	
	public String stripMessage(String msg)
	{
		int index = msg.indexOf(END_TAG);
		return msg.substring(0, index);
	}
	
	public int getNodeId()
	{
		return nodeId;
	}
	
	public void setNodeId(int nodeId)
	{
		this.nodeId = nodeId;
	}
	
	public String getType()
	{
		return type;
	}
	
	public void setType(String type)
	{
		this.type = type;
	}
	
	public int getReqNo()
	{
		return reqNo;
	}
	
	public void setReqNo(int reqNo)
	{
		this.reqNo = reqNo;
	}
	
	public int getTimeStamp()
	{
		return timeStamp;
	}
	
	public void setTimeStamp(int timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	
	public String getFromMessage(int part)
	{
		String str = this.toString();
		str = stripMessage(str);
		String[] parts = str.split("~");
		return parts[part].trim();
	}
	
	public String toString()
	{
		return nodeId+"~"+type+"~"+reqNo+"~"+timeStamp+END_TAG;
	}

	@Override
	public int compareTo(Message m) {
		// TODO Auto-generated method stub
		return m.getTimeStamp() == this.getTimeStamp() ? this.getNodeId() - m.getNodeId() : this.getTimeStamp() - m.getTimeStamp();
	}
	
}
