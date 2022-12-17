import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientReceiver {
    private static ServerSocket server;
    
    private static Map<String, List> availableClients;
    
    public static void main(String[] args) throws Exception{
    	//args
    	int userId = Integer.parseInt(args[0]);
		String password = args[1];
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
	    
        //CLIENT SERVER
    	//create socket server 
        server = new ServerSocket(port);
        while(true){
            System.out.println("Waiting for the client request");
            //creating socket and waiting for client connection 
            Socket clientserver_socket = server.accept();
            //read from socket
            ObjectInputStream clientserver_ois = new ObjectInputStream(clientserver_socket.getInputStream());
            ObjectOutputStream clientserver_oos = new ObjectOutputStream(clientserver_socket.getOutputStream());

            receivesFile(userId, password, clientserver_ois, clientserver_oos);
                     
            //close
            clientserver_ois.close();
            clientserver_oos.close();
            clientserver_socket.close();
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
	private static void receivesFile(int userId, String password, ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException, Exception {
	String alan = (String) inStream.readObject();
	System.out.println(alan);
	}
}
