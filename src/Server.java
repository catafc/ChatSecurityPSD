import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Server {
    private static ServerSocket server;
    private static int port = 9876;
    
    private static Map<String, List> availableClients = new HashMap<String, List>();
    
    public static void main(String args[]) throws Exception{
    	Server server = new Server();
		server.startServer();
    }
    
    public void startServer() throws Exception{
    	try {
    		server = new ServerSocket(port);
    	}
    	catch(IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
    	}
        while(true){
        	try {
	            System.out.println("Waiting for the client request");
	            //creating socket and waiting for client connection
	            Socket socket = server.accept();
				ServerThread newServerThread = new ServerThread(socket);
				newServerThread.start();
	            
        	} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
    
    
  //Threads used for comunication with clients
  	class ServerThread extends Thread {
  		private Socket socket = null;
  		private String clientName;
  		private int port;
  		
  		ServerThread(Socket inSoc) {
  			socket = inSoc;
  			System.out.println("server thread for each client");
  		}
  		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				boolean cycle = true;
				while(cycle) {
					try {
						
						port = (int) inStream.readObject();
						clientName = (String) inStream.readObject();

						System.out.println("clientName " + clientName);
						if(!availableClients.containsKey(clientName)) {
							List<String> socketL = new ArrayList<String>();
							socketL.add(String.valueOf(socket.getInetAddress()).substring(1));
							socketL.add(String.valueOf(port));
							availableClients.put(clientName, socketL);
						}
						System.out.println("available try: " + availableClients);
						outStream.writeObject(availableClients);
						System.out.println("available try AFTER: " + availableClients);

						outStream.flush();
					}catch(SocketException e) {
						System.out.println("user " + clientName + " disconnected");
						System.out.println("Exception: " + e);
						if(availableClients.get(clientName) != null) {
							System.out.println("available: " + availableClients);
							availableClients.remove(clientName);
							System.out.println("available AFETER: " + availableClients);

							cycle = false;
						}
					}
					
				}
				
				outStream.close();
				inStream.close();
				socket.close();
				
				/*while(true) {
					String clientId = (String) inStream.readObject();
					if(clientId != null) {
						System.out.println("updating available clients to client: " + clientName);
						outStream.writeObject(availableClients);
					}
				}*/
			}catch (Exception e) {
				e.printStackTrace();
			}
			
		
  		}
		
  	}
  	
}
