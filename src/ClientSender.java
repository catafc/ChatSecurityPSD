import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.operator.OperatorCreationException;

public class ClientSender {
	private static String password;
	private static int userId;
	private static int port;
	private static Map<String, List> availableClients;
	private static Map<String, List<String>> msgs = new HashMap<String, List<String>>();
	
	public static void main(String[] args) throws Exception{
    	//args
		userId = Integer.parseInt(args[0]);
		password = args[1];
		String[] clientAddress = args[2].split(":");
		String address = clientAddress[0];
		port = Integer.parseInt(clientAddress[1]);
		
		//SERVER CONECTION
	    InetAddress server_host = InetAddress.getLocalHost();
	    Socket server_socket = null;
	    //connection to server
	    server_socket = new Socket(server_host.getHostName(), 9876);

	    ObjectOutputStream outStream = new ObjectOutputStream(server_socket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(server_socket.getInputStream());
	    //add(userId, password, inStream, outStream, port);

		
	    //RECEIVER CLIENT
	    String nameReceiver;
	    Scanner myObj = new Scanner(System.in);
	    while(true) {
	    	System.out.println("choose: \n1. send msg to user: <send> \n2. search for keyword in SENT messages: <search>");
	    	String option = myObj.nextLine();
	    	if(option.equals("send")) {
	    		System.out.println("1. type receivers' name: ");
				String message = myObj.nextLine();
				if (message != null) {
					nameReceiver = message;
					add(userId, password, inStream, outStream);
					String res = sendMessage(userId, password, nameReceiver);
					if(res != "") {
						System.out.println(res);
					}
				}
	    	}else if(option.equals("search")) {
	    		System.out.println("2. type user and keywords to search: <user> <keyword> <keyword>"); //2 alo ola
	    		String line = myObj.nextLine();
	    		nameReceiver = line.substring(0, 1);
	    		String keywords = line.substring(2, line.length());
	    		List<String> found = searchKeyword(nameReceiver, keywords);
	    		if(found.isEmpty()) {
	    			System.out.println("No messages found with keyword(s) '" + keywords + "' for receiver " + nameReceiver);
	    		} else {
	    			System.out.println("Messages with keyword(s) '" + keywords + "' for receiver " + nameReceiver + ":");
	    			for(int i = 0; i < found.size(); i++) {
	    				System.out.println("> " + found.get(i));
	    			}
	    		}
	    	}else {
	    		System.out.println("invalid option, try again");
	    	}
			
	    }
	    //outStream.close();
	    //inStream.close();
	    
	}
	private static List<String> searchKeyword(String receiver, String keyword) {
		
		List<String> messages = msgs.get(receiver);
		List<String> msgsFound = new ArrayList<String>();
		
		for(int i = 0; i < messages.size(); i++) {
			if(messages.get(i).contains(keyword)) {
				msgsFound.add(messages.get(i));
			}
		}
		return msgsFound;
		
	}
	
	//create client keystore, send client info to server and get available clients
	private static void add(int userId, String password, ObjectInputStream inStream, ObjectOutputStream outStream) throws Exception {
		File kfile = new File("keystore." + userId);  //keystore
		if(!kfile.isFile()) { 
			System.out.println("creating keystore");
			Cifra.main(String.valueOf(userId), password);					
		} 
		outStream.writeObject(port);
		outStream.writeObject(String.valueOf(userId)); //name
		//if(availableClients != null) availableClients.clear();
		
		availableClients = (HashMap<String, List>) inStream.readObject();
		System.out.println("Available Clients: " + availableClients);
	}
	/*
	private static void availableClientesUpdate(ObjectInputStream inStream, ObjectOutputStream outStream) throws Exception, IOException {
		outStream.writeObject(String.valueOf(userId)); //name
		availableClients = (HashMap<String, List>) inStream.readObject();
		System.out.println("Available Clients: " + availableClients);
	}*/
	
	private static String sendMessage(int userId, String password, String nameReceiver) throws Exception {
		//get receiver from Server connections
		String clientAddress = null;
		String clientPort = null;
		//choose a client to chat
		if (availableClients.get(nameReceiver) != null) {
			clientAddress = (String) availableClients.get(nameReceiver).get(0);
			clientPort = (String) availableClients.get(nameReceiver).get(1);
		} else {
			return "This user doesn't exist.";
		}
		Socket client_socket = client_connection(clientAddress, Integer.valueOf(clientPort));
	    ObjectOutputStream client_outStream = new ObjectOutputStream(client_socket.getOutputStream());
		ObjectInputStream client_inStream = new ObjectInputStream(client_socket.getInputStream());
		
		//the clients receive their public keys
		PublicKey pubk_receiver = (PublicKey) client_inStream.readObject();		
		
		//insert message
		Scanner myObj1 = new Scanner(System.in);
		System.out.println("Enter message: ");
		String message = myObj1.nextLine();
		if(message != null) secure_msg(client_outStream, message, pubk_receiver);
		String msg_integrity = (String) client_inStream.readObject();	
		if(msg_integrity.equals("message integrity confirmed")){
			save_msg(nameReceiver, message);
			System.out.println("message sent to: " + nameReceiver);
		}else {
			System.out.println(nameReceiver + " received corrupted message, send it again");
		}
		
		//closes
		client_outStream.close();
		client_inStream.close();
		
		return "";
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
	
	private static void secure_msg(ObjectOutputStream outStream, String msg, PublicKey pubk_receiver) throws Exception {
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		SecretKey key_Mac = kf.generateSecret(keySpec);

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(key_Mac);
		byte buf[] = msg.getBytes();
		mac.update(buf);
		
		outStream.writeObject(mac.doFinal());
		
		// get random AES key
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128);
		SecretKey key = kg.generateKey();
		
		// cipher message with AES key
		Cipher cAES = Cipher.getInstance("AES");
		cAES.init(Cipher.ENCRYPT_MODE, key);
		byte buf2[] = msg.getBytes();
		byte[] encryp_message = cAES.doFinal(buf2);

		//send AES encrypted msg
		outStream.writeObject(encryp_message);
		
		// Encrypt the AES key with the public key of the receiver
		Cipher cwRSA = Cipher.getInstance("RSA");
		cwRSA.init(Cipher.WRAP_MODE, pubk_receiver);
		byte[] wrappedKeyAES = cwRSA.wrap(key);
		byte[] wrappedKeyMAC = cwRSA.wrap(key_Mac);
		
		//send AES encrypted with receiver pub RSA key
		outStream.writeObject(wrappedKeyAES);
		outStream.writeObject(wrappedKeyMAC);
		
		Cipher cRSA = Cipher.getInstance("RSA");
		cRSA.init(Cipher.ENCRYPT_MODE, pubk_receiver);
		byte[] encId = cRSA.doFinal(String.valueOf(userId).getBytes());
		
		outStream.writeObject(encId);
	}
	
	private static void save_msg(String idReceiver, String msg) {
		List<String> conv = msgs.get(idReceiver);
		if(conv == null) {
			conv = new ArrayList<String>();
			conv.add(msg);
			msgs.put(idReceiver, conv);
		} else {
			conv.add(msg);
			msgs.put(idReceiver, conv);
		}
	}
}
