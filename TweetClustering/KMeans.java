import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

class Centroid
{
	private double x;
	private double y;
	private ArrayList<Integer> points=new ArrayList<Integer>();
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public ArrayList<Integer> getPoints() {
		return points;
	}
	public void setPoints(ArrayList<Integer> points) {
		this.points = points;
	}
	
}
public class KMeans {

	private static double dataset[][] = new double[100][4];
	
	private static int readingFile(String filePath)
	{
		
		String csvLine;
		String [] attributes;
		int count=0, i=0;
		 
		try {
			//D:/Workspace/MLAssign5/src/data.txt
			BufferedReader reader= new BufferedReader(new FileReader(filePath));
			//Reading from file
			while((csvLine=reader.readLine())!=null)
			{
				attributes=csvLine.split("\t");
				while(count<attributes.length)
				{
					try{
						
					dataset[i][count]=Double.parseDouble(attributes[count]);
					System.out.print(dataset[i][count]);
					System.out.print("\t");
					}
					catch (java.lang.NumberFormatException e)
					{
						System.out.println("Exception");
						i--;
						break;
					}
					count++;
				}
				System.out.println();
				count=0;
				i++;
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return i;	
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int i, k=4, minPosition=0;
		double minValue;
		int j=0,updateCentroid; double x=0, y=0;
		ArrayList<Centroid> points=new ArrayList<Centroid>();
		ArrayList<Centroid> centroid=new ArrayList<Centroid>();
		try
		{
			k=Integer.parseInt(args[0]);
		}
		catch(Exception e)
		{
			System.out.println("Invalid output");
			return;
		}
		i=readingFile(args[1]);
			//K-Means
			//Centroid initialization
			Random random = new Random();
			int cent=0;
			//int cent[]={4,45,67,87};
			ArrayList<Integer> cluster=new ArrayList<Integer>();
			for(int m=0; m<k; m++)
			{
				centroid.add(new Centroid());
				cent=random.nextInt(99);
				while(cluster.contains(cent))
				{
					cent=random.nextInt(99);
				}
				cluster.add(m, cent);
				centroid.get(m).setX(dataset[cluster.get(m)][1]);
				centroid.get(m).setY(dataset[cluster.get(m)][2]);
				//centroid.get(m).setX(dataset[cent][1]);
				//centroid.get(m).setY(dataset[cent][2]);
				
			}
			//System.out.println(cluster);
			for(int m=0;m<k; m++)
			{
			System.out.println(centroid.get(m).getX()+"\t"+centroid.get(m).getY());
			}
	
			//Calculate distance for each point
			for(int itr=0; itr<25;itr++)
			{
			for(int m=0; m<i; m++)
			{
				minValue=10;
				for(int l=0; l<k; l++)
				{	
				if(minValue>EuclidianDistance(m,centroid.get(l)))
				{
					//Assign the corresponding cluster
					minValue=EuclidianDistance(m,centroid.get(l));
					dataset[m][3]=(double) l;
					minPosition=l;
				}
				}
				centroid.get(minPosition).getPoints().add(m);
			}
			for(int m=0; m<k; m++)
			{
				while(j<centroid.get(m).getPoints().size())
				{
					updateCentroid=centroid.get(m).getPoints().get(j);
					x=x+dataset[updateCentroid][1];
					y=y+dataset[updateCentroid][2];
					j++;
				}
				x=x/centroid.get(m).getPoints().size();
				y=y/centroid.get(m).getPoints().size();
				centroid.get(m).setX(x);
				centroid.get(m).setY(y);		
				/*for(int l=0; l<centroid.get(m).getPoints().size(); l++)
				{
					sum=sum+Math.pow(EuclidianDistance(centroid.get(m).getPoints().get(l),centroid.get(m)),2);
				}
				*/
				points.add(m, new Centroid());
				points.get(m).setX(x);
				points.get(m).setY(y);
				points.get(m).setPoints(centroid.get(m).getPoints());
				centroid.get(m).setPoints(new ArrayList<Integer>());
				x=0;
				y=0;
				j=0;			
			}
			}
			/*for(int m=0; m<k; m++)
			{
			System.out.print((m+1)+"\t ");
			for(int l=0; l<points.get(m).getPoints().size(); l++)
			{
				System.out.print(points.get(m).getPoints().get(l)+1+", ");
			}
			System.out.println();
			}
			*/
			writeOutputFile(points, k, args[2]);
			System.out.println("SSE "+validateSSE(points, k));
	}
	private static void writeOutputFile(ArrayList<Centroid> points,  int k, String outputFile)
	{
		PrintWriter writer;
		try {
			writer = new PrintWriter(outputFile);
		System.out.println(k);
		
		for(int m=0; m<k; m++)
		{
		writer.print((m+1)+"\t ");
		for(int l=0; l<points.get(m).getPoints().size(); l++)
		{
			writer.print(points.get(m).getPoints().get(l)+1+", ");
		}
		
		writer.print(System.getProperty("line.separator"));
		}
		writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}

		
	}
	private static double validateSSE(ArrayList<Centroid> point, int k)
	{
		double sum=0;
		for(int m=0; m<k; m++)
		{
		for(int l=0; l<point.get(m).getPoints().size(); l++)
		{
			sum=sum+Math.pow(EuclidianDistance(point.get(m).getPoints().get(l),point.get(m)),2);
		}
		}
		return sum;
	}
	private static double EuclidianDistance(int a, Centroid centroid )
	{
		double x1=dataset[a][1];
		double y1=dataset[a][2];
		double x2=centroid.getX();
		double y2=centroid.getY();
		return (double) Math.abs(Math.sqrt(Math.pow(x1-x2, 2)+Math.pow(y1-y2, 2)));
	}
	
}
