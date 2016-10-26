
public class CustomLatch 
{
	private int requestNo;
	private Object lockObj = new Object();
	private int count;

	public CustomLatch(int count, int requestNo)
	{
		synchronized (lockObj)
		{
			this.count = count;
			this.requestNo = requestNo;
		}
	}

	public void countUp()
	{
		synchronized (lockObj)
		{
			count++;
		}
	}

	public void countDown()
	{
		synchronized (lockObj)
		{
			if (--count <= 0)
			{
				lockObj.notify();
			}
		}
	}

	public void await() throws InterruptedException
	{
		synchronized (lockObj)
		{
			while (count > 0)
			{
				lockObj.wait();
			}
		}
	}
	
	public int getRequestNo()
	{
		return requestNo;
	}
	
	public int getCount()
	{
		return count;
	}
}
