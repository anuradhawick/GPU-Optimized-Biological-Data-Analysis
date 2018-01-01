#include <time.h>
#include <sys/time.h>
#include <iostream>
#include <string>
#include <vector>
#include <stack>

#include "../commons/matrixUtils.h"

using namespace std;

void executeCPU(string X, string Y);
void getAlignments(string X, string Y, Matrix matrix);
void executeGPU(string X, string Y);
__global__ void needlemanKernel(Matrix mat, string X, string Y);

typedef struct {
	char *content;
	int length;
} CUDAstring;

int GAP = -1, MISMATCH = -1, MATCH = 1;

int main() {
	// Size of vectors
	string X("GGTTGACTA");
	string Y("TGTTACGG");
	struct timeval start, end;

	gettimeofday(&start, NULL);
	executeCPU(X, Y);
	gettimeofday(&end, NULL);
	cout << "CPU calculation ended in "
			<< (end.tv_sec - start.tv_sec) * 1000
					+ (end.tv_usec - start.tv_usec) / 1000 << endl;

	gettimeofday(&start, NULL);
	executeGPU(X, Y);
	gettimeofday(&end, NULL);
	cout << "CPU calculation ended in "
			<< (end.tv_sec - start.tv_sec) * 1000
					+ (end.tv_usec - start.tv_usec) / 1000 << endl;

	return 0;
}

void executeCPU(string X, string Y) {
	int xs = X.size() + 1;
	int ys = Y.size() + 1;

	Matrix matrix = mallocMatrix(xs, ys);

	for (int i = 0; i < xs; i++) {
		matrix.elements[i] = GAP * i;
	}

	for (int j = 0; j < ys; j++) {
		matrix.elements[j * xs] = GAP * j;
	}

	for (int i = 1; i < ys; i++) {
		for (int j = 1; j < xs; j++) {
			int matchVal, yGapVal, xGapVal;
			// Match/mismatch
			if (Y[i - 1] == X[j - 1]) {
				matchVal = matrix.elements[(i - 1) * xs + j - 1] + MATCH;
			} else {
				matchVal = matrix.elements[(i - 1) * xs + j - 1] + MISMATCH;
			}
			// X Gap
			xGapVal = matrix.elements[(i - 1) * xs + j] + GAP;
			// Y Gap
			yGapVal = matrix.elements[(i) * xs + j - 1] + GAP;

			matrix.elements[i * xs + j] =
					(matchVal > xGapVal ? matchVal : xGapVal) > yGapVal ?
							(matchVal > xGapVal ? matchVal : xGapVal) : yGapVal;
		}
	}

	for (int i = 0; i < ys; i++) {
		for (int j = 0; j < xs; j++) {
			cout << matrix.elements[i * xs + j] << "\t";
		}
		cout << endl;
	}

	getAlignments(X, Y, matrix);
}

__global__ void needlemanKernel(Matrix matrix, CUDAstring X, CUDAstring Y,
		int MATCH, int MISMATCH, int GAP) {
	int i = blockIdx.y * blockDim.y + threadIdx.y;
	int j = blockIdx.x * blockDim.x + threadIdx.x;
	int xs = X.length + 1;
	int ys = Y.length + 1;

	if (i == 0 | j == 0 | i >= ys | j >= xs)
		return;

	int matchVal, yGapVal, xGapVal;
	// Match/mismatch
	if (Y.content[i - 1] == X.content[j - 1]) {
		matchVal = matrix.elements[(i - 1) * xs + j - 1] + MATCH;
	} else {
		matchVal = matrix.elements[(i - 1) * xs + j - 1] + MISMATCH;
	}
	// X Gap
	xGapVal = matrix.elements[(i - 1) * xs + j] + GAP;
	// Y Gap
	yGapVal = matrix.elements[(i) * xs + j - 1] + GAP;

	matrix.elements[i * xs + j] =
			(matchVal > xGapVal ? matchVal : xGapVal) > yGapVal ?
					(matchVal > xGapVal ? matchVal : xGapVal) : yGapVal;
}

void executeGPU(string X, string Y) {
	int xs = X.size() + 1;
	int ys = Y.size() + 1;
	size_t size = xs * ys * sizeof(int);

	Matrix matrix = mallocMatrix(xs, ys);
	Matrix d_matrix;
	d_matrix.width = matrix.width;
	d_matrix.height = matrix.height;

	CUDAstring d_X;
	d_X.length = X.size();
	char x_char[X.size()];
	strcpy(x_char, X.c_str());

	CUDAstring d_Y;
	d_Y.length = Y.size();
	char y_char[Y.size()];
	strcpy(y_char, Y.c_str());

	for (int i = 0; i < xs; i++) {
		matrix.elements[i] = GAP * i;
	}

	for (int j = 0; j < ys; j++) {
		matrix.elements[j * xs] = GAP * j;
	}

	cudaMalloc(&d_matrix.elements, size);
	cudaMalloc(&d_X.content, X.size() * sizeof(char));
	cudaMalloc(&d_Y.content, Y.size() * sizeof(char));

	cudaMemcpy(d_matrix.elements, matrix.elements, size,
			cudaMemcpyHostToDevice);
	cudaMemcpy(d_X.content, x_char, X.size() * sizeof(char),
			cudaMemcpyHostToDevice);
	cudaMemcpy(d_Y.content, y_char, Y.size() * sizeof(char),
			cudaMemcpyHostToDevice);

	// Number of threads in each thread block
	dim3 threadsPerBlock(32, 32);

	// Number of thread blocks in grid
//	dim3 numBlocks((xs * ys + 32) / threadsPerBlock.x,
//			(xs * ys + 32) / threadsPerBlock.y);
	dim3 numBlocks(1, 1);

	for (int k = 0; k < 100; ++k) {
		needlemanKernel<<<numBlocks, threadsPerBlock>>>(d_matrix, d_X, d_Y,
				MATCH, MISMATCH, GAP);
	}

	cudaDeviceSynchronize();
	cudaMemcpy(matrix.elements, d_matrix.elements, size,
			cudaMemcpyDeviceToHost);

	cudaFree(d_matrix.elements);
	cudaFree(d_X.content);
	cudaFree(d_Y.content);

	for (int i = 0; i < ys; i++) {
		for (int j = 0; j < xs; j++) {
			cout << matrix.elements[i * xs + j] << "\t";
		}
		cout << endl;
	}

	getAlignments(X, Y, matrix);
}

void getAlignments(string X, string Y, Matrix matrix) {
	int xs = X.size() + 1;
	int ys = Y.size() + 1;

	int i = ys - 1;
	int j = xs - 1;
	bool hadPath = true;

	vector < stack<int*> > stList = vector<stack<int*> >();
	stack<int*> st = stack<int*>();

	int *arr = (int*) malloc(2 * sizeof(int));
	arr[0] = i;
	arr[1] = j;
	st.push(arr);
	stList.push_back(st);

	while (hadPath) {
		hadPath = false;
		vector < stack<int*> > temp = stList;
		stList = vector<stack<int*> >();

		for (int ii = 0; ii < temp.size(); ++ii) {
			int* pointer = temp[ii].top();
			i = pointer[0];
			j = pointer[1];

			if (i - 1 >= 0
					&& readMatrix(matrix, i - 1, j) + GAP
							== readMatrix(matrix, i, j)) {
				stack<int*> newSt = stack<int*>(temp[ii]);
				arr = (int*) malloc(2 * sizeof(int));
				arr[0] = i - 1;
				arr[1] = j;
				newSt.push(arr);
				stList.push_back(newSt);

				hadPath = true;
			}

			if (j - 1 >= 0
					&& readMatrix(matrix, i, j - 1) + GAP
							== readMatrix(matrix, i, j)) {
				stack<int*> newSt = stack<int*>(temp[ii]);
				arr = (int*) malloc(2 * sizeof(int));
				arr[0] = i;
				arr[1] = j - 1;
				newSt.push(arr);
				stList.push_back(newSt);

				while (newSt.size() > 0) {
					newSt.pop();
				}

				hadPath = true;
			}

			if (i - 1 >= 0 && j - 1 >= 0 && Y[i - 1] == X[j - 1]) {
				if (readMatrix(matrix, i - 1, j - 1) + MATCH
						== readMatrix(matrix, i, j)) {
					stack<int*> newSt = stack<int*>(temp[ii]);
					arr = (int*) malloc(2 * sizeof(int));
					arr[0] = i - 1;
					arr[1] = j - 1;
					newSt.push(arr);
					stList.push_back(newSt);
					hadPath = true;
				}

			} else if (i - 1 >= 0 && j - 1 >= 0 && Y[i - 1] != X[j - 1]) {
				if (readMatrix(matrix, i - 1, j - 1) + MISMATCH
						== readMatrix(matrix, i, j)) {
					stack<int*> newSt = stack<int*>(temp[ii]);
					arr = (int*) malloc(2 * sizeof(int));
					arr[0] = i - 1;
					arr[1] = j - 1;
					newSt.push(arr);
					stList.push_back(newSt);
					hadPath = true;
				}
			}
		}
		if (stList.size() == 0) {
			stList = temp;
		}
	}

	for (int ii = 0; ii < stList.size(); ++ii) {
		stack<int*> stack = stList[ii];
		vector<int*> path = vector<int*>();

		while (!stack.empty()) {
			int* arr = stack.top();
			path.push_back(arr);
			stack.pop();
		}

		string xSeq = "";
		string matchSeq = "";
		string ySeq = "";

		for (int k = 1; k < path.size(); k++) {
			int i0 = path[k - 1][0];
			int j0 = path[k - 1][1];
			int i1 = path[k][0];
			int j1 = path[k][1];

			// if a match move
			if (i1 == i0 + 1 && j1 == j0 + 1) {
				xSeq += X[j0];
				ySeq += Y[i0];

				matchSeq += X[j0] == Y[i0] ? "|" : " ";
			}
			// if X gap
			else if (i1 == i0 + 1 && j1 == j0) {
				xSeq += "-";
				ySeq += Y[i1];
				matchSeq += " ";
			}
			// if Y gap
			else if (i1 == i0 && j1 == j0 + 1) {
				xSeq += X[i1];
				ySeq += "-";
				matchSeq += " ";
			}

		}
		cout << "\nAlignment" << endl;
		cout << "\t" + xSeq << endl;
		cout << "\t" + matchSeq << endl;
		cout << "\t" + ySeq + "\n" << endl;
	}

}
