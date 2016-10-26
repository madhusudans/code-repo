import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

class Cluster
{
	private  String clusterId;
	private  ArrayList<String> tweets=new ArrayList<String>();
	public String getClusterId() {
		return clusterId;
	}
	public  void setClusterId(String clusterId) {
		this.clusterId = clusterId;
	}
	public  ArrayList<String> getTweets() {
		return tweets;
	}
	public void setTweets(ArrayList<String> tweets) {
		this.tweets = tweets;
	}
	
}
public class Tweet {

	private static BufferedReader reader;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String [] words;
		String [] centroidArray;
		int noCluster=Integer.parseInt(args[0]);
		ArrayList<String> id=new ArrayList<String>();
		ArrayList<Cluster> cluster=new ArrayList<Cluster>();
		ArrayList<String> selectedLines=new ArrayList<String>(); 
		ArrayList<Cluster> output=new ArrayList<Cluster>();
		int j=0;
		try {
			reader = new BufferedReader(new FileReader(args[2]));
			//Reading from file
			String csvLine;
			int count=0;
			while((csvLine=reader.readLine())!=null)
			{
				
				words=csvLine.split(", \"profile_image_url\":");
				centroidArray=csvLine.split("\"id\": ");
				centroidArray=centroidArray[1].split(", \"iso_language_code\":");
				id.add(centroidArray[0]);
				selectedLines.add(words[0].substring(9));
				count++;
			}
			int index=0;
			int i=0;
			ArrayList<ArrayList<String>> selectedWords = new ArrayList<ArrayList<String>>(count);
			while(index<selectedLines.size())
			{
			selectedLines.add(index, selectedLines.get(index).replace(",", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("!", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("-", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("_", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("|", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace(".", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("/", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("\n", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace(":", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace(";", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("#", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("@", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("'", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("\\", " "));
			selectedLines.remove(index+1);
			selectedLines.add(index, selectedLines.get(index).replace("\"", " "));
			selectedLines.remove(index+1);
			words=selectedLines.get(index).split(" ");
			selectedWords.add(new ArrayList<String>());
			while(i<words.length)
			{
				selectedWords.get(index).add(words[i]);
				i++;
			}
			selectedWords.get(index).remove(0);
			selectedWords.get(index).remove(0);
			i=0;
			index++;
			}
			index=0;
			//K-Means
			//Centroid initialization
			reader = new BufferedReader(new FileReader(args[1]));
			int k=0;
			Cluster c;
			while((csvLine=reader.readLine())!=null&&k<noCluster)
			{
				c=new Cluster();
				c.setClusterId(csvLine.substring(0,18));
				cluster.add(k, c);
				k++;
			}
			//k-means
			double minValue=10;
			int  minPosition=0,  t;
			ArrayList<Double> valArray=new ArrayList<Double>();
			for(int itr=0; itr<1; itr++)
			{
			for(int m=0; m<selectedWords.size(); m++)
			{
				minValue=10;
				for(int l=0; l<noCluster; l++)
				{	
					//finding index
					for( t=0; t<251; t++)
					{
						if(id.get(t).equalsIgnoreCase(cluster.get(l).getClusterId()))
						{
							break;
						}
					}
				if(minValue>jaccardDistance(selectedWords.get(m),selectedWords.get(t)))
				{
					//Assign the corresponding cluster
					minValue=jaccardDistance(selectedWords.get(m),selectedWords.get(t));
					minPosition=l;
				}
				}
				cluster.get(minPosition).getTweets().add(id.get(m));
			}
			for(int m=0; m<noCluster; m++)
			{
				j=0;
				while(j<cluster.get(m).getTweets().size())
				{
					for(int s=0; s<cluster.get(m).getTweets().size(); s++)
					{
						if(s!=j)
						{
							minValue=minValue+jaccardDistance(selectedWords.get(idPosition(m, s, id, cluster)),selectedWords.get(idPosition(m, j, id, cluster)));
						}					
					}
					minValue=minValue/cluster.get(m).getTweets().size();
					valArray.add(minValue);
					j++;
				}
				for(int r=0; r<cluster.get(m).getTweets().size(); r++)
				{
					if(minValue>=valArray.get(r))
					{
						minValue=valArray.get(r);
						minPosition=r;
					}
				}
				output.add(m, new Cluster());
				output.get(m).setClusterId(cluster.get(m).getTweets().get(minPosition));
				output.get(m).setTweets(cluster.get(m).getTweets());
				cluster.get(m).setClusterId(cluster.get(m).getTweets().get(minPosition));
				cluster.get(m).setTweets(new ArrayList<String>());
				valArray=new ArrayList<Double>();		
				j=0;			
			}
			count=0;
			while(count<noCluster)
			{
			System.out.println(output.get(count).getClusterId()+" \t"+output.get(count).getTweets());
			count++;
			}
			}
			System.out.println("SSE  "+SSE(output, noCluster, selectedWords, id));
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private static int idPosition(int m, int j, ArrayList<String> id, ArrayList<Cluster> cluster  )
	{
		int t=0;
		for( t=0; t<251; t++)
		{
		//	System.out.println(" t "+t);
			if(id.get(t).equalsIgnoreCase(cluster.get(m).getTweets().get(j)))
			{
				break;
			}
		}
	return t;
	}
	
	private static double jaccardDistance(ArrayList<String> a, ArrayList<String> b)
	{
		int index=0, i=0, icount=0, ucount=1;
		while(index<a.size())
		{
			while(i<b.size())
			{
			if(a.get(index).equalsIgnoreCase(b.get(i)))
			{
				icount++;
				break;
			}
			i++;
			}
			i=0;
			index++;
		}
		ucount=a.size()+b.size()-icount;
		return (1-((double)icount/ucount));
	}
	private static double SSE(ArrayList<Cluster> output, int k, ArrayList<ArrayList<String>> selectedWords, ArrayList<String> id)
	{
				double sum=0.0;
				int t=0;
					for(int m=0; m<k; m++)
					{
						for( t=0; t<251; t++)
						{
							if(id.get(t).equalsIgnoreCase(output.get(m).getClusterId()))
							{
								break;
							}
						}	
						for(int i=0; i<output.get(m).getTweets().size(); i++)
						{					
							//System.out.println(jaccardDistance(selectedWords.get(idPosition(m, i, id, output)), selectedWords.get(t)));
							sum=sum+Math.pow(jaccardDistance(selectedWords.get(idPosition(m, i, id, output)), selectedWords.get(t)),2);
						}
					}
				return sum;
	}
}
