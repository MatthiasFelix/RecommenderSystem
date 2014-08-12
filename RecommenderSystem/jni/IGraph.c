/*
 * GraphGenerator.c
 *
 *  Created on: 21.05.2014
 *      Author: matthiasfelix
 */

#include <stdio.h>
#include <igraph.h>
#include <gsl/gsl_rng.h>
#include <gsl/gsl_randist.h>
#include <jni.h>

JNIEXPORT jdoubleArray JNICALL Java_cwrapper_CWrapper_generateGraph(
		JNIEnv * env, jobject jobj, jstring fileName, jint k,
		jint maxCliqueSize, jdouble expFactor, jdouble expMult,
		jint openNodesEnd) {

	gsl_rng *r = gsl_rng_alloc(gsl_rng_default);
	gsl_rng_set(r, time(NULL));

	// Parameters to set
	int minSize = 1;

	// First, create k cliques of different sizes
	igraph_t graph;
	igraph_empty(&graph, 0, IGRAPH_UNDIRECTED);

	for (int i = 0; i < k; i++) {
		int c = 0;
		while (c < minSize || c > maxCliqueSize) {
			c = (int) floor(gsl_ran_exponential(r, 2.0) * 1.3) + 2;
		}
		igraph_t newPart;
		igraph_full(&newPart, c, IGRAPH_UNDIRECTED, IGRAPH_NO_LOOPS);
		igraph_disjoint_union(&graph, &graph, &newPart);
	}

	int v = igraph_vcount(&graph);

	// Array with node attribute OC (Open Connections). This determines how many connections
	// a node can make. The distribution of OC's over all nodes follows a power law.
	int initialOpenConnections[v];

	igraph_vector_t mapping;
	igraph_vector_init(&mapping, v);

	for (int i = 0; i < v; i++) {
		initialOpenConnections[i] = gsl_ran_exponential(r, expFactor) * expMult;
		VECTOR(mapping)[i] = i;
	}

	long int node1, node2, chosenNode1, chosenNode2;

	long int numberOfOpenNodes = 10000000000000;

	// Random nodes are merged (according to their OC attribute)
	while (numberOfOpenNodes > openNodesEnd) {

		v = igraph_vcount(&graph);

		int openConnections[v];
		for (int i = 0; i < v; i++) {
			openConnections[i] = initialOpenConnections[i];
		}

		numberOfOpenNodes = 0;
		for (int i = 0; i < v; i++) {
			if (openConnections[i] > 0) {
				numberOfOpenNodes++;
			}
		}

		node1 = -1;
		node2 = -1;

		while (node1 == node2) {
			node1 = gsl_rng_uniform_int(r, numberOfOpenNodes);
			node2 = gsl_rng_uniform_int(r, numberOfOpenNodes);
		}

		chosenNode1 = 0, chosenNode2 = 0;

		// Set chosenNode1 to the first available node
		while (openConnections[chosenNode1] < 1) {
			chosenNode1++;
		}

		for (int i = 0; i < node1; i++) {
			chosenNode1++;
			while (openConnections[chosenNode1] < 1) {
				chosenNode1++;
			}
		}

		// Set chosenNode2 to the first available node
		while (openConnections[chosenNode2] < 1) {
			chosenNode2++;
		}

		for (int i = 0; i < node2; i++) {
			chosenNode2++;
			while (openConnections[chosenNode2] < 1) {
				chosenNode2++;
			}
		}

		igraph_bool_t res;
		igraph_are_connected(&graph, (int) chosenNode1, (int) chosenNode2,
				&res);

		if (res == 1) {
			continue;
		}

		openConnections[chosenNode1]--;
		openConnections[chosenNode2]--;

		igraph_vector_init(&mapping, v);

		if (chosenNode1 > chosenNode2) {
			// Swap the numbers so that node1 < node2
			long int temp = chosenNode1;
			chosenNode1 = chosenNode2;
			chosenNode2 = temp;
		}

		for (long int j = 0; j < (chosenNode2); j++) {
			VECTOR(mapping)[j] = j;
		}
		VECTOR(mapping)[chosenNode2] = chosenNode1;
		for (long int k = chosenNode2; k < (v - 1); k++) {
			VECTOR(mapping)[k + 1] = k;
		}

		igraph_contract_vertices(&graph, &mapping, NULL);

		v = igraph_vcount(&graph);

		for (int i = 0; i < chosenNode1; i++) {
			initialOpenConnections[i] = openConnections[i];
		}
		for (long int i = chosenNode1 + 1; i < chosenNode2; i++) {
			initialOpenConnections[i] = openConnections[i];
		}
		for (long int i = chosenNode2; i < v; i++) {
			initialOpenConnections[i] = openConnections[i + 1];
		}
		long int random = gsl_rng_uniform_int(r, 2);
		if (random == 1) {
			initialOpenConnections[chosenNode1] = openConnections[chosenNode1];
		} else {
			initialOpenConnections[chosenNode1] = openConnections[chosenNode2];
		}

		// Recount number of open nodes

		numberOfOpenNodes = 0;
		for (int i = 0; i < v; i++) {
			if (initialOpenConnections[i] > 0) {
				numberOfOpenNodes++;
			}
		}

	}

	// Calculate and print some measures of the resulting graph
	igraph_real_t transitivity, averagePathLength;

	igraph_simplify(&graph, 1, 1, NULL);

	igraph_transitivity_undirected(&graph, &transitivity,
			IGRAPH_TRANSITIVITY_NAN);
	igraph_average_path_length(&graph, &averagePathLength, IGRAPH_UNDIRECTED,
			1);

	// Write the edgelist to a file
	FILE* file;

	const char *friendsFile = (*env)->GetStringUTFChars(env, fileName, NULL);
	if (NULL == friendsFile) {
		return NULL;
	}

	file = fopen(friendsFile, "w+");

	if (file == NULL) {
		fprintf(stderr, "Can't open the file!\n");
		exit(1);
	}

	igraph_write_graph_edgelist(&graph, file);

	// The nodes with degree 0 also need to be written to the file
	igraph_vs_t allNodes;
	igraph_vs_all(&allNodes);

	igraph_vector_t noFriendNodes;
	igraph_vector_init(&noFriendNodes, 1);

	igraph_degree(&graph, &noFriendNodes, allNodes, IGRAPH_ALL, 0);

	long s = igraph_vector_size(&noFriendNodes);
	int c = 0;
	for (int i = 0; i < s; i++) {
		if (VECTOR(noFriendNodes)[i] == 0) {
			fprintf(file, "%i %i\n", i, i);
			c++;
		}
	}

	fclose(file);

	// Community structure part
	igraph_vector_t modularity, membership;
	igraph_matrix_t merges;
	igraph_vector_init(&modularity, 0);
	igraph_vector_init(&membership, 0);
	igraph_matrix_init(&merges, 0, 0);

	igraph_community_walktrap(&graph, 0, 4, &merges, &modularity, &membership);
	//igraph_community_fastgreedy(&graph, 0, &merges, &modularity, &membership);

	jdouble membArray[igraph_vector_size(&membership)];

	igraph_vector_copy_to(&membership, (igraph_real_t*) membArray);

	jdoubleArray outJNIArray = (*env)->NewDoubleArray(env,
			igraph_vector_size(&membership));  // allocate
	if (NULL == outJNIArray) {
		return NULL;
	}
	(*env)->SetDoubleArrayRegion(env, outJNIArray, 0,
			igraph_vector_size(&membership), membArray);  // copy
	return outJNIArray;

}

JNIEXPORT jdoubleArray JNICALL Java_cwrapper_CWrapper_getCentrality(
		JNIEnv * env, jobject jobi, jstring fileName, jint centralityMode) {

	igraph_t graph;

	const char *graphfile = (*env)->GetStringUTFChars(env, fileName, NULL);
	if (NULL == graphfile) {
		return NULL;
	}

	FILE* file;
	file = fopen(graphfile, "r");

	igraph_read_graph_edgelist(&graph, file, 0, 0);

	igraph_vector_t res;
	igraph_vector_init(&res, 0);

	if ((int) centralityMode == 0) {
		igraph_degree(&graph, &res, igraph_vss_all(), IGRAPH_ALL, 0);
	} else if ((int) centralityMode == 1) {
		igraph_closeness(&graph, &res, igraph_vss_all(), IGRAPH_ALL, 0, 1);
	} else if ((int) centralityMode == 2) {
		igraph_betweenness(&graph, &res, igraph_vss_all(), 0, 0, 0);
	}

	int v = igraph_vcount(&graph);
	int resSize = igraph_vector_size(&res);

	jdouble resArray[igraph_vector_size(&res)];

	igraph_vector_copy_to(&res, (igraph_real_t*) resArray);

	jdoubleArray outJNIArray = (*env)->NewDoubleArray(env,
			igraph_vector_size(&res));  // allocate
	if (NULL == outJNIArray) {
		return NULL;
	}
	(*env)->SetDoubleArrayRegion(env, outJNIArray, 0, igraph_vector_size(&res),
			resArray);  // copy
	return outJNIArray;

}

JNIEXPORT jdoubleArray JNICALL Java_cwrapper_CWrapper_getCommunities(
		JNIEnv * env, jobject jobi, jstring fileName, jint community) {

	igraph_t graph;

	const char *graphfile = (*env)->GetStringUTFChars(env, fileName, NULL);
	if (NULL == graphfile) {
		return NULL;
	}

	FILE* file;
	file = fopen(graphfile, "r");

	igraph_read_graph_edgelist(&graph, file, 0, 0);

	igraph_vector_t res;
	igraph_vector_init(&res, 0);

	// Community structure part
	igraph_vector_t modularity, membership;
	igraph_matrix_t merges;
	igraph_vector_init(&modularity, 0);
	igraph_vector_init(&membership, 0);
	igraph_matrix_init(&merges, 0, 0);

	if ((int) community == 0) {
		igraph_community_walktrap(&graph, 0, 4, &merges, &modularity,
				&membership);
	} else if ((int) community == 1) {
		igraph_community_fastgreedy(&graph, 0, &merges, &modularity,
				&membership);
	}

	jdouble membArray[igraph_vector_size(&membership)];

	igraph_vector_copy_to(&membership, (igraph_real_t*) membArray);

	jdoubleArray outJNIArray = (*env)->NewDoubleArray(env,
			igraph_vector_size(&membership));  // allocate
	if (NULL == outJNIArray) {
		return NULL;
	}
	(*env)->SetDoubleArrayRegion(env, outJNIArray, 0,
			igraph_vector_size(&membership), membArray);  // copy
	return outJNIArray;

}
