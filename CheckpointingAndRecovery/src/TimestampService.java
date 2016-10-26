
public class TimestampService
{
	private int[] currTimestamp;
	private int N;
	private int nodeId;

	public TimestampService(int N, int nodeId)
	{
		this.N = N;
		this.currTimestamp = new int[N];
		this.nodeId = nodeId;
	}

	public void updateVectorClock(Message message)
	{
		if(message != null)
		{
			if(message.getType().equals(Constants.REB) || message.getType().equals(Constants.LOST))
			{
				REBMessage rebMessage = (REBMessage) message;

				if(rebMessage.getSourceId() == nodeId)
				{
					currTimestamp[nodeId]++;
				}
				else if(rebMessage.getDestId() == nodeId)
				{
					int[] vectorClock = rebMessage.getVectorClock();

					for(int i = 0; i < N; i++)
					{
						if(i != nodeId)
						{
							currTimestamp[i] = Math.max(vectorClock[i], currTimestamp[i]);
						}
					}
				}
			}
		}
	}

	public int[] getCurrTimestamp()
	{
		return currTimestamp;
	}
}
