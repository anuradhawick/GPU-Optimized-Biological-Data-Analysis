package reference;

import com.aparapi.Kernel;
import com.aparapi.Range;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by anuradhawick on 12/30/17.
 */
public class DynamicProgramming {
    public static void main(String[] args) {
        ExecuteKernel kernel = new ExecuteKernel();
        kernel.loadData("/Users/anuradhawick/Documents/cuda/src/main/java/input.txt");
        kernel.runCPU();
        kernel.runGPU();

    }

}


class ExecuteKernel {
    final int[] matrix = new int[80 * 80];

    void loadData(String fileName) {
        String line;
        int row = 0;
        try {
            FileReader fileReader = new FileReader(fileName);

            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                String[] temp = line.split(",");
                for (int i = 0; i < 80; i++) {
                    matrix[row * 80 + i] = Integer.parseInt(temp[i]);
                }
                row++;
            }
            bufferedReader.close();
        } catch (Exception ex) {
            System.out.println("Unable to open file '" + fileName + "'");
        }
    }

    void runCPU() {
        int top, left;
        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 80; j++) {
                if (i != 0 && j != 0) {
                    top = matrix[(i - 1) * 80 + j];
                    left = matrix[i * 80 + j - 1];

                    if (top + matrix[i * 80 + j] > left + matrix[i * 80 + j]) {
                        matrix[i * 80 + j] += left;
                    } else {
                        matrix[i * 80 + j] += top;
                    }
                } else if (j != 0) {
                    matrix[i * 80 + j] += matrix[i * 80 + j - 1];
                } else if (i != 0) {
                    matrix[i * 80 + j] += matrix[(i - 1) * 80 + j];
                }
            }
        }
        System.out.println("Answer from CPU dynamic programming" + matrix[80 * 80 - 1]);
    }

    void runGPU() {
        final int[] matOriginal = new int[80*80];
        final int[] mat = matrix;

        System.arraycopy(matrix, 0, matOriginal, 0, matrix.length);
        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int top, left;
                int i = getGlobalId() / 80;
                int j = getGlobalId() % 80;

                if (i != 0 && j != 0) {
                    top = mat[(i - 1) * 80 + j];
                    left = mat[i * 80 + j - 1];

                    if (top + matOriginal[i * 80 + j] > left + matOriginal[i * 80 + j]) {
                        mat[i * 80 + j] = matOriginal[i * 80 + j] + left;
                    } else {
                        mat[i * 80 + j] = matOriginal[i * 80 + j] + top;
                    }
                } else if (j != 0) {
                    mat[i * 80 + j] = matOriginal[i * 80 + j] + mat[i * 80 + j - 1];
                } else if (i != 0) {
                    mat[i * 80 + j] = matOriginal[i * 80 + j] + mat[(i - 1) * 80 + j];
                }
            }
        };
        kernel.setExplicit(true);
        kernel.put(mat).put(matOriginal);
        Range range = Range.create(80 * 80);
        kernel.execute(range, 160).get(mat);
        System.out.println("Answer from GPU dynamic programming" + mat[80 * 80 - 1]);
    }


}