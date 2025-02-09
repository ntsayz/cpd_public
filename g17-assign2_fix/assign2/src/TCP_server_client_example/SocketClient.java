import java.net.*;
import java.util.Scanner;
import java.io.*;
 
/**
 * This program demonstrates a simple TCP/IP socket client.
 *
 * @author www.codejava.net
 */
public class SocketClient {
 
    public static void main(String[] args) {
        if (args.length < 2) return;
 
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
 
        try (Socket socket = new Socket(hostname, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {
 
            System.out.println("Connected to the server!");

            String inputLine;
            while ((inputLine = scanner.nextLine()) != null) {
                writer.println(inputLine); // Send user input to server
                String response = reader.readLine(); // Read response from server
                if (response != null) {
                    System.out.println("SERVER: " +response);
                } else {
                    break; // Break if server closes connection
                }
            }
 
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}