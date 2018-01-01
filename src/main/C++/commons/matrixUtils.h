#include <cstdlib>

using namespace std;

typedef struct {
	int width;
	int height;
	int* elements;
} Matrix;

__device__ int readGMat(Matrix mat, int row, int col) {
	return mat.elements[row * mat.width + col];
}

__device__ void writeGMat(Matrix mat, int row, int col, int val) {
	mat.elements[row * mat.width + col] = val;
}

int readMatrix(Matrix mat, int row, int col) {
	return mat.elements[row * mat.width + col];
}

void writeMatrix(Matrix mat, int row, int col, int val) {
	mat.elements[row * mat.width + col] = val;
}

Matrix mallocMatrix(int width, int height) {
	Matrix mat = Matrix();
	mat.width = width;
	mat.height = height;
	mat.elements = (int*) malloc(width * height * sizeof(int));
	return mat;
}

Matrix getRandomMatrix(int width, int height) {
	Matrix mat = mallocMatrix(width, height);
	for (int i = 0; i < height; ++i) {
		for (int j = 0; j < width; ++j) {
			writeMatrix(mat, i, j, rand() % 10);
		}
	}

	return mat;
}
