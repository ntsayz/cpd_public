import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;
 
/**
 * This program demonstrates a simple TCP/IP socket server.
 *
 * @author www.codejava.net
 */
public class SocketServer {
    private static int global_sum = 0;
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws IOException{
        if (args.length < 1) return;
 
        int port = Integer.parseInt(args[0]);
 
        ServerSocket serverSocket = new ServerSocket(port);
 
        System.out.println("Server is listening on port " + port);

        while (true) {
            Socket socket = serverSocket.accept();

            new Thread(new ClientHandling(socket)).start();
 
        }
 
    }

    private static class ClientHandling implements Runnable {
        private Socket clientSocket;

        public ClientHandling(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try{ 
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                int clientSum = 0;
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    try{
                        int number_to_add = Integer.parseInt(inputLine);
                        clientSum += number_to_add;
                        out.println("Current sum is: " + clientSum);
                
                    } catch (NumberFormatException e) {
                        out.println("ERROR! Please enter a valid number!");
                    }
                }
                lock.lock();
                try{
                    global_sum += clientSum;
                } finally {
                    lock.unlock();
                }

                out.println("Final Global Sum is " + global_sum);
                clientSocket.close();

            } catch (IOException ex) {
                System.out.println("Server exception: " + ex.getMessage());
                ex.printStackTrace();
            }  

        }
    }

}