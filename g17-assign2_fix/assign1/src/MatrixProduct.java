public class MatrixProduct {
    
    public static void multiplyMatrices(int size) {
        double[][] matrixA = new double[size][size];
        double[][] matrixB = new double[size][size];
        double[][] matrixC = new double[size][size];

        // Initialize matrices
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrixA[i][j] = 1.0;
                matrixB[i][j] = i + 1;
            }
        }

        // Matrix multiplication
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double sum = 0.0;
                for (int k = 0; k < size; k++) {
                    sum += matrixA[i][k] * matrixB[k][j];
                }
                matrixC[i][j] = sum;
            }
        }

        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        System.out.println("Time: " + duration + " seconds");

        // Display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int j = 0; j < Math.min(10, size); j++) {
            System.out.print(matrixC[0][j] + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        for (int size = 600; size <= 3000; size += 400) {
            System.out.println("Running matrix multiplication for size: " + size);
            multiplyMatrices(size);
        }
    }
}