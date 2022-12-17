import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;

public class Cifra {
	public static void main(String id, String password)
      throws NoSuchAlgorithmException, CertificateException, NoSuchProviderException, OperatorCreationException, IOException, KeyStoreException {

	// gera chaves assimetricas RSA  
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    
    // define informacao para o certificado
    X500Name dnName = new X500Name("CN=" + id);
    BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
    Instant startDate = Instant.now();
    Instant endDate = startDate.plus(2 * 365, ChronoUnit.DAYS);
    
    // classe que assina o certificado - certifcado auto assinado
    String signatureAlgorithm = "SHA256WithRSA";
    ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
    
    // cria o certificado
    X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
        dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
        keyPair.getPublic());
    Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build((ContentSigner)contentSigner));
    
    // guarda chave privada + certificado na keystore
    
    KeyStore kstore = KeyStore.getInstance("JKS");//PKCS12
    if ((new File("keystore." + id)).exists()){  // **** file da keystore
		FileInputStream kfile1 = new FileInputStream("keystore." + id); 
		kstore.load(kfile1, password.toCharArray()); // **** password da keystore 
		kfile1.close();
    } else {
		kstore.load(null, null); // **** caso em que o file da keystore ainda nao existe
    }
    		
	Certificate chain [] = {certificate, certificate};
	
	
	// **** atencao ao alias do user e 'a password da chave privada //password.toCharArray()
	kstore.setKeyEntry(id, (Key)keyPair.getPrivate(), password.toCharArray(), chain);
	FileOutputStream kfile = new FileOutputStream("keystore." + id); // keystore
	kstore.store(kfile, password.toCharArray());
			
  }
}
