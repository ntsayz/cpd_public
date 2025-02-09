import java.util.Scanner;

public class MatrixMultiplication {

     public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int op;
        int lin, col, blockSize;

        do {
            System.out.println("\n1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.println("3. Block Multiplication");
            System.out.print("Selection?: ");
            op = scanner.nextInt();

            if (op == 0) break;

            System.out.print("Dimensions: lins=cols ? ");
            lin = scanner.nextInt();
            col = lin;

            switch (op) {
                case 1:
                    OnMult(lin, col);
                    break;
                case 2:
                    OnMultLine(lin, col);
                    break;
                case 3:
                    System.out.print("Block Size? ");
                    blockSize = scanner.nextInt();
                    OnMultBlock(lin, col, blockSize);
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
                    break;
            }

        } while (op != 0);

        scanner.close();
        System.out.println("Program terminated.");
    }


    private static void OnMult(int m_ar, int m_br) {
        System.out.println("Multiplying using the OnMult method...");
    
        // Initialize matrices A, B, and C
        double[][] pha = new double[m_ar][m_ar];
        double[][] phb = new double[m_br][m_br];
        double[][] phc = new double[m_ar][m_br];
    
        // Initialize matrix A with 1.0
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_ar; j++) {
                pha[i][j] = 1.0;
            }
        }
    
        // Initialize matrix B with incremental values based on row
        for (int i = 0; i < m_br; i++) {
            for (int j = 0; j < m_br; j++) {
                phb[i][j] = i + 1;
            }
        }
    
        // Measure start time
        long startTime = System.nanoTime();
    
        // Perform matrix multiplication
        for (int i = 0; i < m_ar; i++) {
            for (int j = 0; j < m_br; j++) {
                double temp = 0.0;
                for (int k = 0; k < m_ar; k++) {
                    temp += pha[i][k] * phb[k][j];
                }
                phc[i][j] = temp;
            }
        }
    
        // Measure end time and calculate duration
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1e9; // Convert nanoseconds to seconds
    
        System.out.printf("Time: %3.3f seconds\n", duration);
    
        // Display 10 elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, m_br); j++) {
                System.out.print(phc[i][j] + " ");
            }
        }
        System.out.println();
    }

    private static void OnMultLine(int lin, int col) {
        double[] A = new double[lin * lin]; 
        double[] B = new double[col * col]; 
        double[] C = new double[lin * col]; 
    
        // Initialize matrices A and B in 1D format
        for (int i = 0; i < lin; i++) {
            for (int j = 0; j < lin; j++) {
                A[i*lin + j] = 1.0; 
            }
        }
    
        for (int i = 0; i < col; i++) {
            for (int j = 0; j < col; j++) {
                B[i*col + j] = i + 1; 
            }
        }
    
        // Measure start time
        long startTime = System.nanoTime();
    
      
        for (int i = 0; i < lin; i++) {
            for (int j = 0; j < col; j++) {
                double sum = 0;
                for (int k = 0; k < col; k++) { 
                    sum += A[i*lin + k] * B[k*col + j];
                }
                C[i*col + j] = sum; 
            }
        }
    
        // Measure end time
        long endTime = System.nanoTime();
        double duration = (endTime - startTime) / 1e9; // Convert to seconds
    
        System.out.println("Time: " + duration + " seconds");
    
        // Display elements of the result matrix to verify correctness
        System.out.println("Result matrix:");
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < Math.min(10, col); j++) {
                System.out.print(C[i*col + j] + " ");
            }
        }
        System.out.println();
    }
    

    

    private static void OnMultBlock(int lin, int col, int blockSize) {
        
        System.out.println("Multiplying using the OnMultBlock method with block size " + blockSize + "...");
    }

   
}

