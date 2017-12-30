import com.aparapi.Kernel;
import com.aparapi.Range;

import java.util.*;

/**
 * Created by anuradhawick on 12/30/17.
 */
public class NeedlemanWunsch {
    public static void main(String[] args) {
//        CPUNeedlemanWunsch cpuNeedlemanWunsch = new CPUNeedlemanWunsch("GCATGCU","GATTACA");
//        cpuNeedlemanWunsch.execute();

        GPUNeedlemanWunsch gpuNeedlemanWunsch = new GPUNeedlemanWunsch("GCATGCU","GATTACA");
        gpuNeedlemanWunsch.execute();
    }

    private static int readMatrix(int i, int j, int[] matrix, int xs) {
        return matrix[i * xs + j];
    }

    private static Stack<int[]> getClone(Stack<int[]> s, int x, int y) {
        Stack<int[]> newStack = (Stack<int[]>) s.clone();
        newStack.push(new int[]{x, y});
        return newStack;
    }

    public static void getAlignments(int xs, int ys, int GAP, int MATCH, int MISMATCH, char[] X, char[] Y, int[] matrix) {
        int i = xs - 1;
        int j = ys - 1;
        boolean hadPath = true;

        List<Stack<int[]>> stackList = new ArrayList<Stack<int[]>>();
        Stack<int[]> stack = new Stack<int[]>();

        stack.push(new int[]{i, j});
        stackList.add(stack);

        while (hadPath) {
            hadPath = false;
            List<Stack<int[]>> temp = stackList;
            stackList = new ArrayList<Stack<int[]>>();

            for (Stack<int[]> s : temp) {
                int[] pointer = s.peek();
                i = pointer[0];
                j = pointer[1];

                if (i - 1 >= 0 && readMatrix(i - 1, j, matrix, xs) + GAP == readMatrix(i, j, matrix, xs)) {
                    stackList.add(getClone(s, i - 1, j));
                    hadPath = true;
                }

                if (j - 1 >= 0 && readMatrix(i, j - 1, matrix, xs) + GAP == readMatrix(i, j, matrix, xs)) {
                    stackList.add(getClone(s, i, j - 1));
                    hadPath = true;
                }

                if (i - 1 >= 0 && j - 1 >= 0 && X[i - 1] == Y[j - 1]) {
                    if (readMatrix(i - 1, j - 1, matrix, xs) + MATCH == readMatrix(i, j, matrix, xs)) {
                        stackList.add(getClone(s, i - 1, j - 1));
                        hadPath = true;
                    }

                } else if (i - 1 >= 0 && j - 1 >= 0 && X[i - 1] != Y[j - 1]) {
                    if (readMatrix(i - 1, j - 1, matrix, xs) + MISMATCH == readMatrix(i, j, matrix, xs)) {
                        stackList.add(getClone(s, i - 1, j - 1));
                        hadPath = true;
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
                    xSeq += X[i0];
                    ySeq += Y[j0];

                    matchSeq += X[i0] == Y[j0] ? "|" : " ";
                }
                // if Y gap
                else if (i1 == i0 + 1 && j1 == j0) {
                    xSeq += X[i0];
                    ySeq += "-";
                    matchSeq += " ";
                }
                // if X gap
                else if (i1 == i0 && j1 == j0 + 1) {
                    xSeq += "-";
                    ySeq += Y[j0];
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


class CPUNeedlemanWunsch {
    private int GAP = -1, MISMATCH = -1, MATCH = 1, xs, ys, matrix[];
    char[] X;
    char[] Y;

    CPUNeedlemanWunsch(String X, String Y) {
        this.X = X.toCharArray();
        this.Y = Y.toCharArray();
        xs = this.X.length + 1;
        ys = this.Y.length + 1;
        matrix = new int[xs * ys];
    }


    void execute() {
        for (int i = 0; i < xs; i++) {
            matrix[i] = GAP * i;
        }

        for (int j = 0; j < ys; j++) {
            matrix[j * xs] = GAP * j;
        }

        for (int i = 1; i < xs; i++) {
            for (int j = 1; j < ys; j++) {
                int matchVal, yGapVal, xGapVal;
                // Match/mismatch
                if (X[i - 1] == Y[j - 1]) {
                    matchVal = matrix[(i - 1) * xs + j - 1] + MATCH;
                } else {
                    matchVal = matrix[(i - 1) * xs + j - 1] + MISMATCH;
                }
                // X Gap
                xGapVal = matrix[(i - 1) * xs + j] + GAP;
                // Y Gap
                yGapVal = matrix[(i) * xs + j - 1] + GAP;

                matrix[i * xs + j] = (matchVal > xGapVal ? matchVal : xGapVal) > yGapVal ? (matchVal > xGapVal ? matchVal : xGapVal) : yGapVal;
            }
        }

        for (int i = 0; i < xs; i++) {
            for (int j = 0; j < ys; j++) {
                System.out.print(matrix[i * xs + j] + "\t");
            }
            System.out.println("");
        }
    }
}


class GPUNeedlemanWunsch {
    private int GAP = -1;
    private int MISMATCH = -1;
    private int MATCH = 1;
    private int xs;
    private int ys;
    private int matrix[];
    char[] X;
    char[] Y;

    GPUNeedlemanWunsch(String X, String Y) {
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

        for (int i = 0; i < xs; i++) {
            gmatrix[i] = gGAP * i;
        }

        for (int j = 0; j < ys; j++) {
            gmatrix[j * xs] = gGAP * j;
        }

        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int i = getGlobalId() / gxs;
                int j = getGlobalId() % gxs;

                if (i >= gxs | j >= gys) return;
                if (i == 0 | j == 0) return;

                int matchVal = 0, yGapVal, xGapVal;

                // Match/mismatch
                if (gX[i - 1] == gY[j - 1]) {
                    matchVal = gmatrix[(i - 1) * gxs + j - 1] + gMATCH;
                } else {
                    matchVal = gmatrix[(i - 1) * gxs + j - 1] + gMISMATCH;
                }

                // X Gap
                xGapVal = gmatrix[(i - 1) * gxs + j] + gGAP;
                // Y Gap
                yGapVal = gmatrix[i * gxs + j - 1] + gGAP;

                // get max
                int maxx = 0;

                if (yGapVal > xGapVal) {
                    maxx = yGapVal;
                } else {
                    maxx = xGapVal;
                }

                if (matchVal > maxx) {
                    maxx = matchVal;
                }


                gmatrix[i * gxs + j] = maxx;
            }
        };

        Range range = Range.create(gxs * gys);
        kernel.execute(range, (gxs + gys));

        matrix = gmatrix;

        NeedlemanWunsch.getAlignments(xs, ys, GAP, MATCH, MISMATCH, X, Y, matrix);

        for (int i = 0; i < gxs; i++) {
            for (int j = 0; j < gys; j++) {
                System.out.print(gmatrix[i * gxs + j] + "\t");
            }
            System.out.println("");
        }
    }
}