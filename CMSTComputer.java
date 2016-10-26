package com;

import java.util.Scanner;

public class CMSTComputer {
	public static void main(String[] args) {

		//declaring variables to get user input
		int number_of_vertices, mode, missingEdgeCount = 0, capacityConstraint;

		int[][] adjacency_matrix;

		int[] weights_matrix;

		int[] weights_matrix2;

		Scanner scan = new Scanner(System.in);

		System.out.println("Enter the number of vertices");
		number_of_vertices = scan.nextInt();

		//checking if number_of_vertices is >= 2 else error displayed to user 
		if (number_of_vertices >= 2) {
			//initialise the adjacency and weight matrices of the graph
			adjacency_matrix = new int[number_of_vertices][number_of_vertices];
			weights_matrix = new int[number_of_vertices];

			System.out.println("Enter the Weighted Matrix for the graph");

			//counting the missing edges to check if the graph is an empty one
			for (int i = 0; i < number_of_vertices; i++) {
				for (int j = 0; j < number_of_vertices; j++) {
					adjacency_matrix[i][j] = scan.nextInt();
					if (adjacency_matrix[i][j] == 0) {
						missingEdgeCount++;
					}
				}
			}
			
			//if graph is non empty proceed
			if (missingEdgeCount != (number_of_vertices * 2)) {
				System.out.println("Enter the weights of the vertices");
				for (int i = 0; i < number_of_vertices; i++) {
					weights_matrix[i] = scan.nextInt();
				}

				System.out.println("Enter Positive Integer Capacity Constraint");
				capacityConstraint = scan.nextInt();

				System.out.println("Enter mode \n 0 - Esau Williams Heuristic \n 1 - Kruskals Heuristic or \n 2 - both");
				mode = scan.nextInt();
				scan.close();

				//invoking corresponding classes based on selected mode
				if (mode == 0) {
					EsauWilliams esauWilliams = new EsauWilliams();
					esauWilliams.displayEsauWilliamsResult(number_of_vertices, adjacency_matrix, capacityConstraint,
							weights_matrix);
				} else if (mode == 1) {
					ModifiedKruskals mod = new ModifiedKruskals();
					mod.displayKruskalsResults(number_of_vertices, adjacency_matrix, capacityConstraint,
							weights_matrix);
				} else if (mode == 2) {
					weights_matrix2 = weights_matrix.clone();
					EsauWilliams esauWilliams = new EsauWilliams();
					esauWilliams.displayEsauWilliamsResult(number_of_vertices, adjacency_matrix, capacityConstraint,
							weights_matrix);
					ModifiedKruskals mod = new ModifiedKruskals();
					mod.displayKruskalsResults(number_of_vertices, adjacency_matrix, capacityConstraint,
							weights_matrix2);
				} 
				//display invalid mode error to user
				else {
					System.out.println("Input Error: Invalid Mode. Please run program again!");
				}
			} 
			//display invalid empty graph error to user
			else {
				System.out.println("Input Error: Inputted Graph has no edges. Please run program again!");
			}
		} else if (number_of_vertices == 1) {
			//display no edge error to user
			System.out.println("Input Error: Inputted Graph has only 1 node/vertex. Please run program again!");
		} else if (number_of_vertices == 0) {
			//display empty graph error to user
			System.out.println("Input Error: Empty Graph inputted. Please run program again!");
		} else {
			//display non positive number of vertices error to user
			System.out.println("Input Error: Incorrect number of vertices. Please run program again!");
		}
		scan.close();
	}
}
