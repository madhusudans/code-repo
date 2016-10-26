import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class KruskalAlgorithm {
	private List<Edge> edges;
	private int numberOfVertices;
	public static final int MAX_VALUE = 999;
	private int visited[];
	private int spanning_tree[][];

	int W = 3;

	public KruskalAlgorithm(int numberOfVertices) {
		this.numberOfVertices = numberOfVertices;
		edges = new LinkedList<Edge>();
		visited = new int[this.numberOfVertices + 1];
		spanning_tree = new int[numberOfVertices + 1][numberOfVertices + 1];
	}

	public void kruskalAlgorithm(int adjacencyMatrix[][],int[][] adj) {
		boolean finished = false;
		for (int source = 1; source <= numberOfVertices; source++) {
			for (int destination = 1; destination <= numberOfVertices; destination++) {
				if (adjacencyMatrix[source][destination] != MAX_VALUE
						&& source != destination) {
					Edge edge = new Edge();
					edge.sourcevertex = source;
					edge.destinationvertex = destination;
					edge.weight = adjacencyMatrix[source][destination];
					adjacencyMatrix[destination][source] = MAX_VALUE;
					edges.add(edge);
				}
			}
		}
		
		System.out.println("Adj is");
		for (int source = 1; source <= numberOfVertices; source++) {
			for (int destination = 1; destination <= numberOfVertices; destination++) {
				System.out.print(adj[source][destination]);
			}
			System.out.println();
		}
		
		Collections.sort(edges, new EdgeComparator());
		CheckCycle checkCycle = new CheckCycle();
		Cluster cluster;
		List<Cluster> clusterList = null;
		for (int j = 0; j <= edges.size() - 1; j++) {
			System.out.println("j is now: "+j);
			Edge edge = edges.get(j);
			System.out.println("Edge is: "+(edge.sourcevertex-1)+"-->"+(edge.destinationvertex-1));
			if (edge.sourcevertex != 1 && edge.destinationvertex != 1) {

				spanning_tree[edge.sourcevertex][edge.destinationvertex] = edge.weight;
				spanning_tree[edge.destinationvertex][edge.sourcevertex] = edge.weight;

				if (checkCycle.checkCycle(spanning_tree, edge.sourcevertex)) {
					spanning_tree[edge.sourcevertex][edge.destinationvertex] = 0;
					spanning_tree[edge.destinationvertex][edge.sourcevertex] = 0;
					edge.weight = -1;
					continue;
				}
				
				if (edge.weight != -1) {
					if (clusterList==null || clusterList.isEmpty()) {
						System.out.println("Creating the first cluster");
						cluster = new Cluster();
						clusterList = new ArrayList<Cluster>();
						cluster.clusterWeight += 2;
						cluster.vertices = new ArrayList<Integer>();
						System.out.println("Adding source vertex "+(edge.sourcevertex-1));
						cluster.vertices.add(edge.sourcevertex);
						System.out.println("Adding destination vertex "+(edge.destinationvertex-1));
						cluster.vertices.add(edge.destinationvertex);
						cluster.isConnectedToRoot = false;
						clusterList.add(cluster);
						System.out.println("Cluster List size is increased to: "+clusterList.size());
						continue;
					} else {
						System.out.println("Cluster List size is: "+clusterList.size());
						for (int k = 0; k <= clusterList.size() - 1; k++) {
							Cluster aCluster = clusterList.get(k);
							if (aCluster.vertices.contains(edge.sourcevertex)){
								if (aCluster.clusterWeight + 1 <= W) {
									System.out.println("Adding destination vertex "+(edge.destinationvertex-1));
								  	aCluster.vertices.add(edge.destinationvertex);
								  	aCluster.clusterWeight++;
								  	break;
								}  else{
									System.out.println("Weight constraint violated");
									spanning_tree[edge.sourcevertex][edge.destinationvertex] = 0;
									spanning_tree[edge.destinationvertex][edge.sourcevertex] = 0;
									edge.weight = -1;
									break;
								}
							} else if (aCluster.vertices.contains(edge.destinationvertex)){
								if (aCluster.clusterWeight + 1 <= W) {
									System.out.println("Adding source vertex "+(edge.sourcevertex-1));
									aCluster.vertices.add(edge.sourcevertex);
								  	aCluster.clusterWeight++;
								  	break;
								}  else{
									System.out.println("Weight constraint violated");
									spanning_tree[edge.sourcevertex][edge.destinationvertex] = 0;
									spanning_tree[edge.destinationvertex][edge.sourcevertex] = 0;
									edge.weight = -1;
									break;
								}
							} else{
								System.out.println("Creating a new cluster");
								cluster = new Cluster();
								cluster.clusterWeight += 2;
								cluster.vertices = new ArrayList<Integer>();
								System.out.println("Adding source vertex "+(edge.sourcevertex-1));
								cluster.vertices.add(edge.sourcevertex);
								System.out.println("Adding destination vertex "+(edge.destinationvertex-1));
								cluster.vertices.add(edge.destinationvertex);
								cluster.isConnectedToRoot = false;
								clusterList.add(cluster);
								break;
							}
						}
					}

					visited[edge.sourcevertex] = 1;
					visited[edge.destinationvertex] = 1;
					for (int i = 0; i < visited.length; i++) {
						if (visited[i] == 0) {
							finished = false;
							break;
						} else {
							finished = true;
						}
					}
					if (finished)
						break;
				}
			} else{
				System.out.println("Edge is: "+(edge.sourcevertex-1)+"-->"+(edge.destinationvertex-1)+" so it is skipped");
			}
		}
		
		int max = Integer.MAX_VALUE;
		int clusterRoot = 0;
		
		int c=0;
		
		for(Cluster someCluster : clusterList){
			System.out.println("Cluster "+(++c));
			for(Integer i :someCluster.vertices){
				System.out.print((i-1)+" ");
			}
			System.out.println();
		}
		
		for(Cluster someCluster : clusterList){
			if(!someCluster.isConnectedToRoot){
				for(int l=1;l<=numberOfVertices;l++){
					if(someCluster.vertices.contains(l)){
						if(adj[1][l]<=max){
							max = adj[1][l];
							clusterRoot = l;
						}
					}
				}
				System.out.println("clusterRoot is: "+clusterRoot);
				System.out.println("adj value of [1,clusterRoot] is: "+adj[1][clusterRoot]);
				System.out.println("adj value of [clusterRoot,1] is: "+adj[clusterRoot][1]);
				spanning_tree[1][clusterRoot] = adj[1][clusterRoot];
				spanning_tree[clusterRoot][1] = adj[clusterRoot][1];
				clusterRoot = 0;
				max = Integer.MAX_VALUE;
			}
		}
		
		System.out.println("The spanning tree is ");
		for (int i = 1; i <= numberOfVertices; i++)
			System.out.print("\t" + i);
		System.out.println();
		for (int source = 1; source <= numberOfVertices; source++) {
			System.out.print(source + "\t");
			for (int destination = 1; destination <= numberOfVertices; destination++) {
				System.out.print(spanning_tree[source][destination] + "\t");
			}
			System.out.println();
		}
	}

	public static void main(String... arg) {
		int adjacency_matrix[][];
		int number_of_vertices;

		Scanner scan = new Scanner(System.in);
		System.out.println("Enter the number of vertices");
		number_of_vertices = scan.nextInt();
		adjacency_matrix = new int[number_of_vertices + 1][number_of_vertices + 1];

		System.out.println("Enter the Weighted Matrix for the graph");
		
		for (int i = 1; i <= number_of_vertices; i++) {
			for (int j = 1; j <= number_of_vertices; j++) {
				adjacency_matrix[i][j] = scan.nextInt();
			}
		}
		
		int[][] adj = adjacency_matrix.clone();
		
		
		for (int i = 1; i <= number_of_vertices; i++) {
			for (int j = 1; j <= number_of_vertices; j++) {
				if (i == j) {
					adjacency_matrix[i][j] = 0;
					continue;
				}
				if (adjacency_matrix[i][j] == 0) {
					adjacency_matrix[i][j] = MAX_VALUE;
				}
			}
		}
		
		for (int i = 1; i <= number_of_vertices; i++) {
			for (int j = 1; j <= number_of_vertices; j++) {
				System.out.print(adj[i][j]);
			}
			System.out.println();
		}
		
		KruskalAlgorithm kruskalAlgorithm = new KruskalAlgorithm(
				number_of_vertices);
		kruskalAlgorithm.kruskalAlgorithm(adjacency_matrix,adj);
		scan.close();
	}
}

class Edge {
	int sourcevertex;
	int destinationvertex;
	int weight;
}

class EdgeComparator implements Comparator<Edge> {
	public int compare(Edge edge1, Edge edge2) {
		if (edge1.weight < edge2.weight)
			return -1;
		if (edge1.weight > edge2.weight)
			return 1;
		return 0;
	}
}

class Cluster {
	ArrayList<Integer> vertices;
	int clusterWeight;
	boolean isConnectedToRoot;
}

class CheckCycle {
	private Stack<Integer> stack;
	private int adjacencyMatrix[][];

	public CheckCycle() {
		stack = new Stack<Integer>();
	}

	public boolean checkCycle(int adjacency_matrix[][], int source) {
		boolean cyclepresent = false;
		int number_of_nodes = adjacency_matrix[source].length - 1;

		adjacencyMatrix = new int[number_of_nodes + 1][number_of_nodes + 1];
		for (int sourcevertex = 1; sourcevertex <= number_of_nodes; sourcevertex++) {
			for (int destinationvertex = 1; destinationvertex <= number_of_nodes; destinationvertex++) {
				adjacencyMatrix[sourcevertex][destinationvertex] = adjacency_matrix[sourcevertex][destinationvertex];
			}
		}

		int visited[] = new int[number_of_nodes + 1];
		int element = source;
		int i = source;
		visited[source] = 1;
		stack.push(source);

		while (!stack.isEmpty()) {
			element = stack.peek();
			i = element;
			while (i <= number_of_nodes) {
				if (adjacencyMatrix[element][i] >= 1 && visited[i] == 1) {
					if (stack.contains(i)) {
						cyclepresent = true;
						return cyclepresent;
					}
				}
				if (adjacencyMatrix[element][i] >= 1 && visited[i] == 0) {
					stack.push(i);
					visited[i] = 1;
					adjacencyMatrix[element][i] = 0;// mark as labelled;
					adjacencyMatrix[i][element] = 0;
					element = i;
					i = 1;
					continue;
				}
				i++;
			}
			stack.pop();
		}
		return cyclepresent;
	}
}