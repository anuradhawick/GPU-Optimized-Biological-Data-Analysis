package SmithWaterman;

import NeedlemanWunsch.NeedlemanWunsch;
import com.aparapi.Kernel;
import com.aparapi.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by anuradhawick on 12/31/17.
 *
 * Gap extensions are not considered
 */
@SuppressWarnings("Duplicates")
public class SmithWaterman {
    public static void main(String[] args) {
        CPUSmithWaterman cpuSmithWaterman = new CPUSmithWaterman("GGTTGACTA", "TGTTACGG");
        cpuSmithWaterman.execute();

        GPUSmithWaterman gpuSmithWaterman = new GPUSmithWaterman("GGTTGACTA", "TGTTACGG");
        gpuSmithWaterman.execute();
    }

    static List<int[]> getMaxCell(int xs, int ys, int[] matrix) {
        int maxVal = 0;
        List<int[]> maxCells = new ArrayList<>();

        for (int i = 0; i < ys; i++) {
            for (int j = 0; j < xs; j++) {
                maxVal = maxVal > matrix[i * xs + j] ? maxVal : matrix[i * xs + j];
            }
        }

        for (int i = 0; i < ys; i++) {
            for (int j = 0; j < xs; j++) {
                if (matrix[i * xs + j] == maxVal) {
                    maxCells.add(new int[]{i, j});
                }
            }
        }

        return maxCells;
    }

    private static int readMatrix(int i, int j, int[] matrix, int xs) {
        return matrix[i * xs + j];
    }

    private static Stack<int[]> getClone(Stack<int[]> s, int x, int y) {
        Stack<int[]> newStack = (Stack<int[]>) s.clone();
        newStack.push(new int[]{x, y});
        return newStack;
    }

    static void getAlignments(int maxI, int maxJ, int xs, int ys, int GAP, int MATCH, int MISMATCH, char[] X, char[] Y, int[] matrix) {
        int i = maxI;
        int j = maxJ;

        List<Stack<int[]>> stackList = new ArrayList<Stack<int[]>>();
        Stack<int[]> stack = new Stack<int[]>();

        stack.push(new int[]{i, j});
        stackList.add(stack);

        while (readMatrix(i, j, matrix, xs) != 0) {
            List<Stack<int[]>> temp = stackList;
            stackList = new ArrayList<Stack<int[]>>();

            for (Stack<int[]> s : temp) {
                int[] pointer = s.peek();
                i = pointer[0];
                j = pointer[1];

                if (i - 1 >= 0 && readMatrix(i - 1, j, matrix, xs) + GAP == readMatrix(i, j, matrix, xs)) {
                    stackList.add(getClone(s, i - 1, j));
                }

                if (j - 1 >= 0 && readMatrix(i, j - 1, matrix, xs) + GAP == readMatrix(i, j, matrix, xs)) {
                    stackList.add(getClone(s, i, j - 1));
                }

                if (i - 1 >= 0 && j - 1 >= 0 && Y[i - 1] == X[j - 1]) {
                    if (readMatrix(i - 1, j - 1, matrix, xs) + MATCH == readMatrix(i, j, matrix, xs)) {
                        stackList.add(getClone(s, i - 1, j - 1));
                    }

                } else if (i - 1 >= 0 && j - 1 >= 0 && Y[i - 1] != X[j - 1]) {
                    if (readMatrix(i - 1, j - 1, matrix, xs) + MISMATCH == readMatrix(i, j, matrix, xs)) {
                        stackList.add(getClone(s, i - 1, j - 1));
                    }
                }
            }

            if (stackList.size() == 0) {
                stackList = temp;
            }
        }

        for (Stack<int[]> s : stackList) {
            List<int[]> path = new ArrayList<int[]>();
            while (s.size() > 0) {
                path.add(s.pop());
            }

            String xSeq = "";
            String matchSeq = "";
            String ySeq = "";

            for (int k = 1; k < path.size(); k++) {
                int i0 = path.get(k - 1)[0];
                int j0 = path.get(k - 1)[1];
                int i1 = path.get(k)[0];
                int j1 = path.get(k)[1];

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
            System.out.println("\nAlignment");
            System.out.println("\t" + xSeq);
            System.out.println("\t" + matchSeq);
            System.out.println("\t" + ySeq + "\n");
        }
    }
}


@SuppressWarnings("ALL")
class CPUSmithWaterman {
    private int GAP = -3, MISMATCH = -3, MATCH = 3, xs, ys, matrix[];
    char[] X;
    char[] Y;

    CPUSmithWaterman(String X, String Y) {
        this.X = X.toCharArray();
        this.Y = Y.toCharArray();
        xs = this.X.length + 1;
        ys = this.Y.length + 1;
        matrix = new int[xs * ys];
    }


    void execute() {

        for (int i = 1; i < ys; i++) {
            for (int j = 1; j < xs; j++) {
                int matchVal, yGapVal, xGapVal;
                // Match/mismatch
                if (Y[i - 1] == X[j - 1]) {
                    matchVal = matrix[(i - 1) * xs + j - 1] + MATCH;
                } else {
                    matchVal = matrix[(i - 1) * xs + j - 1] + MISMATCH;
                }
                // X Gap
                xGapVal = matrix[(i - 1) * xs + j] + GAP;
                // Y Gap
                yGapVal = matrix[i * xs + j - 1] + GAP;

                if (xGapVal > yGapVal) {
                    matrix[i * xs + j] = xGapVal;
                } else {
                    matrix[i * xs + j] = yGapVal;
                }
                if (matrix[i * xs + j] < matchVal) {
                    matrix[i * xs + j] = matchVal;
                }
                if (matrix[i * xs + j] < 0) {
                    matrix[i * xs + j] = 0;
                }
            }
        }

        for (int i = 0; i < ys; i++) {
            for (int j = 0; j < xs; j++) {
                System.out.print(matrix[i * xs + j] + "\t");
            }
            System.out.println("");
        }

        System.out.println("");

        List<int[]> maxCells = SmithWaterman.getMaxCell(xs, ys, matrix);

        for (int[] cell : maxCells) {
            SmithWaterman.getAlignments(cell[0], cell[1], xs, ys, GAP, MATCH, MISMATCH, X, Y, matrix);
        }

    }

}

class GPUSmithWaterman {
    private int GAP = -3;
    private int MISMATCH = -3;
    private int MATCH = 3;
    private int xs;
    private int ys;
    private int matrix[];
    char[] X;
    char[] Y;

    GPUSmithWaterman(String X, String Y) {
        this.X = X.toCharArray();
        this.Y = Y.toCharArray();
        xs = this.X.length + 1;
        ys = this.Y.length + 1;
        matrix = new int[xs * ys];
    }

    void execute() {
        final int gxs = xs, gys = ys, gMATCH = MATCH, gMISMATCH = MISMATCH, gGAP = GAP;
        final int[] gmatrix = matrix;
        final char[] gX = X;
        final char[] gY = Y;

        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int i = getGlobalId() / gxs;
                int j = getGlobalId() % gxs;

                if (i >= gys | j >= gxs) return;
                if (i == 0 | j == 0) return;

                int matchVal = 0, yGapVal, xGapVal;
                // Match/mismatch
                if (gY[i - 1] == gX[j - 1]) {
                    matchVal = gmatrix[(i - 1) * gxs + j - 1] + gMATCH;
                } else {
                    matchVal = gmatrix[(i - 1) * gxs + j - 1] + gMISMATCH;
                }
                // X Gap
                xGapVal = gmatrix[(i - 1) * gxs + j] + gGAP;
                // Y Gap
                yGapVal = gmatrix[i * gxs + j - 1] + gGAP;

                if (xGapVal > yGapVal) {
                    gmatrix[i * gxs + j] = xGapVal;
                } else {
                    gmatrix[i * gxs + j] = yGapVal;
                }
                if (gmatrix[i * gxs + j] < matchVal) {
                    gmatrix[i * gxs + j] = matchVal;
                }
                if (gmatrix[i * gxs + j] < 0) {
                    gmatrix[i * gxs + j] = 0;
                }
            }
        };

        Range range = Range.create(gxs * gys);
        kernel.execute(range, (gxs + gys));

        matrix = gmatrix;

        for (int i = 0; i < gys; i++) {
            for (int j = 0; j < gxs; j++) {
                System.out.print(gmatrix[i * gxs + j] + "\t");
            }
            System.out.println("");
        }

        List<int[]> maxCells = SmithWaterman.getMaxCell(xs, ys, matrix);

        for (int[] cell : maxCells) {
            SmithWaterman.getAlignments(cell[0], cell[1], xs, ys, GAP, MATCH, MISMATCH, X, Y, matrix);
        }
    }
}