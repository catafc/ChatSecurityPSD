import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ClientSender {
	private static Map<String, List> availableClients;
	public static void main(String[] args) throws Exception{
    	//args
		int userId = Integer.parseInt(args[0]);
		String password = args[1];
		System.out.println(args[2]);
		String[] clientAddress = args[2].split(":");
		String address = clientAddress[0];
		int port = Integer.parseInt(clientAddress[1]);
		
		//SERVER CONECTION
	    InetAddress server_host = InetAddress.getLocalHost();
	    Socket server_socket = null;
	    //connection to server
	    server_socket = new Socket(server_host.getHostName(), 9876);

	    ObjectOutputStream outStream = new ObjectOutputStream(server_socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(server_socket.getInputStream());
	    add(userId, password, inStream, outStream, port);
	    outStream.close();
	    inStream.close();
		
	    //RECEIVER CLIENT
	    String nameReceiver;
	    Scanner myObj = new Scanner(System.in);
		System.out.println("<receiverName: ");
		String message = myObj.nextLine();
		if (message != null) {
			nameReceiver = message;
			System.out.println("nameReceiver: " + nameReceiver);

			sendFileMessage(userId, password, nameReceiver);
		}
	    
	    
	}
	//create client keystore, send client info to server and get available clients
	private static void add(int userId, String password, ObjectInputStream inStream, ObjectOutputStream outStream, int port) throws Exception {
		File kfile = new File("keystore." + userId);  //keystore
		if(!kfile.isFile()) { 
			System.out.println("creating keystore");
			Cifra.main(String.valueOf(userId), password);					
		} 
		System.out.println("writing");
		outStream.writeObject(String.valueOf(userId)); //name
		outStream.writeObject(port);
		availableClients = (HashMap<String, List>) inStream.readObject();
		System.out.println("Available Clients: " + availableClients);
	}
	
	 // SEND FILES
	private static void sendFileMessage(int userId, String password, String nameReceiver) throws Exception {
		// connection between client and server
		System.out.println("Send File");
		File kfile = new File("keystore." + userId); // keystore
		if (!kfile.isFile()) {
			System.out.println("Creating keystore");
			Cifra.main(String.valueOf(userId), password);
		}
		FileInputStream kfilein = new FileInputStream("keystore." + userId); // keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfilein, password.toCharArray());
		
		//get receiver from Server connections
		String clientAddress = null;
		String clientPort = null;
		// choose a client to chat
		if (availableClients.get(nameReceiver) != null) {
			clientAddress = (String) availableClients.get(nameReceiver).get(0);
			clientPort = (String) availableClients.get(nameReceiver).get(1);
		} else {
			System.out.println("This user doesn't exist.");
		}

		System.out.println("chat to client: " + nameReceiver);
		System.out.println("availableClients: " + availableClients);
		System.out.println("clientAddress: " + clientAddress);
		System.out.println("clientPort: " + clientPort);
		Socket client_socket = client_connection(clientAddress, Integer.valueOf(clientPort));
	    ObjectOutputStream client_outStream = new ObjectOutputStream(client_socket.getOutputStream());
		ObjectInputStream client_inStream = new ObjectInputStream(client_socket.getInputStream());
		client_outStream.writeObject("ALAN");
		
		//closes
		client_outStream.close();
		client_inStream.close();
	}
	private static Socket client_connection(String clientAddress, int connect_port) throws Exception {
	    Socket client_socket = null;
	    //connection to server client
	    try {
	    client_socket = new Socket(clientAddress, connect_port);
	    }catch(Exception e) {
	    	System.out.println(e);
	    }
	    return client_socket;
	}
}
