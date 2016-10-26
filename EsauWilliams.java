import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EsauWilliams {

	
	 int[][] weightedGraph = new int[][] { { 0, 5, 6, 9, 12, 15 }, { 5, 0, 4,
	  3, 8, 10 }, { 6, 4, 0, 8, 5, 12 }, { 9, 3, 8, 0, 6, 6 }, { 12, 8, 5, 6,
	  0, 7 }, { 15, 10, 12, 6, 7, 0 } };
	 

	int noOfNodes = 6;
	
	//int noOfNodes = 7;

	int[] visited = new int[noOfNodes];

//	int[][] weightedGraph = new int[][] { { 0, 5, 6, 9, 10, 11, 15 }, { 5, 0, 9, 6, 6, 8, 17 },
//			{ 6, 9, 0, 7, 9, 8, 12 }, { 9, 6, 7, 0, 10, 5, 11 }, { 10, 6, 9, 10, 0, 14, 9 }, { 11, 8, 8, 5, 14, 0, 8 },
//			{ 15, 17, 12, 11, 9, 8, 0 } };

	int[][] CMST = new int[noOfNodes][noOfNodes];

	int[] tradeoff = new int[noOfNodes];

	int[] tradeoffvertex = new int[noOfNodes];

	int[] disttoCenter = new int[noOfNodes];

	int[] neighborCount = new int[noOfNodes];

	int W = 3;

	int[] nodeWeights = new int[] { 1, 1, 1, 1, 1, 1, 1 };
	
	//int[] nodeWeights = new int[] { 1, 1, 1, 2, 1, 1, 1 };

	int[] marked = new int[noOfNodes];

	public static void main(String[] args) {

		EsauWilliams ew = new EsauWilliams();

		ew.updateD2C();
		ew.buildCMST();
	}

	public void updateNeighborCount() {
		for (int i = 1; i <= noOfNodes - 1; i++) {
			for (int j = 1; j <= noOfNodes - 1; j++) {
				if (weightedGraph[i][j] > 0) {
					neighborCount[i]++;
				}
			}
		}
	}

	public void updateD2C() {
		for (int i = 1; i <= noOfNodes - 1; i++) {
			disttoCenter[i] = weightedGraph[0][i];
			// System.out.println(disttoCenter[i]);
		}
	}

	public void computeTradeoff() {
		int a = Integer.MAX_VALUE;

		int k = 0;

		boolean nodesremain = false;

		for (int i = 1; i <= noOfNodes - 1; i++) {
			if (visited[i] == 0) {
				nodesremain = false;
				for (int j = 1; j <= noOfNodes - 1; j++) {
					if (i != j) {
						if (weightedGraph[i][j] < a && CMST[i][j] == 0 && visited[j] == 0
								&& nodeWeights[i] + nodeWeights[j] <= W) {
							a = weightedGraph[i][j];
							k = j;
							nodesremain = true;
						}
					}
				}
				if (!nodesremain) {
					System.out.println("No edges remain for node " + (i + 1));
					marked[i] = 1;
				}
			}
			tradeoff[i] = a - disttoCenter[i];
			tradeoffvertex[i] = k;
			a = Integer.MAX_VALUE;
		}
		for (int i = 1; i <= noOfNodes - 1; i++) {
			System.out.print(tradeoff[i] + " " + tradeoffvertex[i]);
			System.out.println();
		}
	}

	public void buildCMST() {

		boolean noUpdate = false;

		int cnt = 0;

		while (true) {
			// while(cnt <= 6){
			// noTradeOffLeft();
			computeTradeoff();
			if (!noTradeOffLeft()) {
				int min = 0, minval = tradeoff[1];

				for (int i = 1; i <= noOfNodes - 1; i++) {
					if (tradeoff[i] < minval) {
						minval = tradeoff[i];
						min = i;
					}
				}

				System.out.println("min is" + (min + 1));
				System.out.println("tradeoff neighbor of min is" + (tradeoffvertex[min] + 1));

				if (nodeWeights[min] + nodeWeights[tradeoffvertex[min]] <= W && visited[min] == 0) {
					disttoCenter[min] = disttoCenter[tradeoffvertex[min]];
					CMST[min][tradeoffvertex[min]] = weightedGraph[min][tradeoffvertex[min]];
					CMST[tradeoffvertex[min]][min] = weightedGraph[tradeoffvertex[min]][min];
					visited[min] = 1;
					System.out.println("visited of " + (min + 1) + " is set to :" + visited[min]);
					// nodeWeights[min]++;
					nodeWeights[tradeoffvertex[min]] += nodeWeights[min];
					if ((nodeWeights[tradeoffvertex[min]] + 1) > W) {
						marked[tradeoffvertex[min]] = 1;
					}
				}

				for (int i = 1; i <= noOfNodes - 1; i++) {
					System.out.println(disttoCenter[i]);
				}

				for (int i = 0; i <= noOfNodes - 1; i++) {
					for (int j = 0; j <= noOfNodes - 1; j++) {
						System.out.print(CMST[i][j]);
					}
					System.out.println();
				}

				for (int i = 1; i <= noOfNodes - 1; i++) {
					System.out.println(nodeWeights[i]);
				}

				cnt++;
			} else {
				break;
			}
		}

		for (int i = 1; i <= noOfNodes - 1; i++) {
			if (visited[i] == 0) {
				CMST[0][i] = weightedGraph[0][i];
				CMST[i][0] = weightedGraph[i][0];
			}
		}

		int totalCMSTCost = 0;
		
		for (int i = 0; i <= noOfNodes - 1; i++) {
			for (int j = 0; j <= noOfNodes - 1; j++) {
				System.out.print(CMST[i][j]);
//				if(CMST[i][j]>0 && i!=0){
//					totalCMSTCost+=CMST[i][j];
//				}
			}
			System.out.println();
		}
		
		for (int i = 0; i <= noOfNodes - 1; i++) {
			for (int j = i; j <= noOfNodes - 1; j++) {
				if(CMST[i][j]>0){
					totalCMSTCost+=CMST[i][j];
				}
			}
		}
		
		System.out.println("Total Cost of CMST is"+(totalCMSTCost));
	}

	public boolean noTradeOffLeft() {
		boolean noTradeOffLeft = false;

		int markedcount = 0, visitedcount = 0;

		for (int i = 1; i <= noOfNodes - 1; i++) {
			if (marked[i] == 1) {
				markedcount++;
			} else if (visited[i] == 1) {
				visitedcount++;
			}
		}

		System.out.println("Marked count " + markedcount);
		System.out.println("Visited count " + visitedcount);
		if (markedcount + visitedcount == noOfNodes - 1) {
			noTradeOffLeft = true;
		}
		return noTradeOffLeft;
	}
}
