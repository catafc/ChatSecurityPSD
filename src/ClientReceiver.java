import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.operator.OperatorCreationException;

public class ClientReceiver {
    private static ServerSocket server;
    
    private static Map<String, List> availableClients;
	private static Map<String, String> msgs = new HashMap<String, String>();
    private static String password;
    private static int userId;
    
    public static void main(String[] args) throws Exception{
    	//args
    	userId = Integer.parseInt(args[0]);
		password = args[1];
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
			
    		Key priv_key = sendPubKey(clientserver_oos);
            receivesMsg(clientserver_oos, clientserver_ois, priv_key);
            
            //close
            clientserver_ois.close();
            clientserver_oos.close();
            //clientserver_socket.close();
        }
	    //outStream.close();
	    //inStream.close();
        
    }
    
    //create client keystore, send client info to server and get available clients
	private static void add(int userId, String password, ObjectInputStream inStream, ObjectOutputStream outStream, int port) throws Exception {
		File kfile = new File("keystore." + userId);  //keystore
		if(!kfile.isFile()) { 
			System.out.println("creating keystore");
			Cifra.main(String.valueOf(userId), password);					
		} 
		outStream.writeObject(port);
		outStream.writeObject(String.valueOf(userId)); //name
		availableClients = (HashMap<String, List>) inStream.readObject();
		System.out.println("Available Clients: " + availableClients);
		//availableClientesUpdate(inStream, outStream);
	}
	/*
	private static void availableClientesUpdate(ObjectInputStream inStream, ObjectOutputStream outStream) throws Exception, IOException {
		outStream.writeObject(String.valueOf(userId)); //name
		availableClients = (HashMap<String, List>) inStream.readObject();
		System.out.println("Available Clients: " + availableClients);
	}*/
	
	private static void receivesMsg(ObjectOutputStream outStream, ObjectInputStream inStream, Key priv_key) throws IOException, Exception {
		
		
		//read hash, msg and AES key
		byte[] hash_msg = (byte[]) inStream.readObject();
		byte[] enc_msg = (byte[]) inStream.readObject();
		byte[] enc_AESkey = (byte[]) inStream.readObject();
		byte[] enc_MACkey = (byte[]) inStream.readObject();
		byte[] enc_RSAid = (byte[]) inStream.readObject();
		
		//decrypt AES key with private (self) RSA
		Cipher cwRSA = Cipher.getInstance("RSA");
		cwRSA.init(Cipher.UNWRAP_MODE, priv_key); 
		Key keyAES = cwRSA.unwrap(enc_AESkey, "AES", Cipher.SECRET_KEY);
		Key keyMAC = cwRSA.unwrap(enc_MACkey, "AES", Cipher.SECRET_KEY);
		
		//decrypt sender id
		Cipher cRSA = Cipher.getInstance("RSA");
		cRSA.init(Cipher.DECRYPT_MODE, priv_key); 
		byte[] id = new byte[12];
		byte[] sender_id = cRSA.doFinal(enc_RSAid);
		
		//decrypt msg
		Cipher cAES = Cipher.getInstance("AES");
		cAES.init(Cipher.DECRYPT_MODE, keyAES);
		
		byte[] msg = new byte[1024];
		msg = cAES.doFinal(enc_msg);
		
		String msg_integrity = checkMsgIntegrity(msg, hash_msg, keyMAC);
		outStream.writeObject(msg_integrity);
		
		System.out.println("----\nreceived msg: " + new String(msg) + "\nfrom: " + new String(sender_id) + "----\n");
		System.out.println("^ " + msg_integrity);
		
		save_msg(new String(sender_id), new String(msg));
	}
	
	private static Key sendPubKey(ObjectOutputStream outStream) throws Exception, CertificateException, NoSuchProviderException, OperatorCreationException, KeyStoreException, IOException {
		File kfile = new File("keystore." + userId);  //keystore
		if(!kfile.isFile()) { 
			Cifra.main(String.valueOf(userId), password);					
		} 
		FileInputStream kfilein = new FileInputStream("keystore." + userId);  //keystore
		KeyStore kstore = KeyStore.getInstance("JKS");
		kstore.load(kfilein, password.toCharArray());

		Certificate c = (Certificate) kstore.getCertificate(String.valueOf(userId)); 
		PublicKey pubk = c.getPublicKey();
		
		//sends public key to sender
		outStream.writeObject(pubk);
		
		Key myPrivateKey = kstore.getKey(String.valueOf(userId), password.toCharArray()); 
		return myPrivateKey;
	}
	
	private static String checkMsgIntegrity(byte[] msg, byte[] hash_msg, Key keyMAC) throws Exception{
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(keyMAC);
		mac.update(msg);
		byte[] new_hash_msg = mac.doFinal();

		String new_hash_msg_string = new String(new_hash_msg);
		String old_hash_msg_string = new String(hash_msg);
		
		if(old_hash_msg_string.equals(new_hash_msg_string)) {
			return "message integrity confirmed";
		} else{
			return "message was corrupted";
		}
	}
	
	private static void save_msg(String sender, String msg) {
		msgs.put(sender, msg);
	}
}
