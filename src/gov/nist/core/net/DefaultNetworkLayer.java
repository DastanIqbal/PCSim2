package gov.nist.core.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
//import javax.crypto.Cipher;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.KeyManagerFactory;

	//public static final Cipher TLS_NULL_WITH_NULL_NULL =  Cipher.getInstance("TLS_NULL_WITH_NULL_NULL");
		// new Cipher("null", "null", "null", "null", 0, 0x00, 0x00, "TLS_NULL_WITH_NULL_NULL", ProtocolVersion.TLS_1);
/**
 * default implementation which passes straight through to java platform
 *
 * @author m.andrews
 *
 */
public class DefaultNetworkLayer implements NetworkLayer {

    private SSLSocketFactory       sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    // PC 2.0 force the only protocol supported by the SSL Socket to be 
    // TLS v 1.0
    private String [] sslProtocols = { "TLSv1" };

    /**
     * single default network layer; for flexibility, it may be better not to make it a singleton,
     * but singleton seems to make sense currently.
     */
    public static final DefaultNetworkLayer SINGLETON = new DefaultNetworkLayer();

    private DefaultNetworkLayer() {
        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    }

    public ServerSocket createServerSocket(int port, int backlog, InetAddress bindAddress)
            throws IOException {
        return new ServerSocket(port, backlog, bindAddress);
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return new Socket(address, port);
    }

    public DatagramSocket createDatagramSocket() throws SocketException {
        return new DatagramSocket();
    }

    public DatagramSocket createDatagramSocket(int port, InetAddress laddr) throws SocketException {
        return new DatagramSocket(port, laddr);
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLServerSocket createSSLServerSocket(int port, int backlog, InetAddress bindAddress) throws IOException
    {
    	 // PC 2.0 force the only protocol supported by the SSL Socket to be 
        // TLS v 1.0
        SSLServerSocket sock = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, backlog, bindAddress);
        sock.setEnabledProtocols(sslProtocols);
        String [] cs = sock.getSupportedCipherSuites();
        sock.setEnabledCipherSuites(cs);
//        String [] cs = new String [2];
//        cs[0] = "TLS_RSA_WITH_AES_128_CBC_SHA";
//        cs[1] = "SSL_RSA_WITH_3DES_EDE_CBC_SHA";
//        sock.setEnabledCipherSuites(cs);
//        cs = sock.getEnabledCipherSuites();
//       	String [] scs = sock.getSupportedCipherSuites();
//        System.out.println("\nSCS - " + scs.length);
//    	for (int i = 0; i < scs.length; i++) {
//    		System.out.println("[" + i + "]" + scs[i]);
//    	}
//        cs = sock.getEnabledCipherSuites();
//        System.out.println("CS - " + cs.length);
//    	for (int i = 0; i < cs.length; i++) {
//    		System.out.println("[" + i + "]" + cs[i]);
//    	}
//   	 sock.setEnabledCipherSuites(scs);
       
        return sock;
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLSocket createSSLSocket(InetAddress address, int port) throws IOException
    {
         SSLSocket sock = (SSLSocket) sslSocketFactory.createSocket(address, port);
         sock.setEnabledProtocols(sslProtocols);
         return sock;
    }

    /* Added by Daniel J. Martinez Manzano <dani@dif.um.es> */
    public SSLSocket createSSLSocket(InetAddress address, int port, InetAddress myAddress) throws IOException
    {
        SSLSocket sock = (SSLSocket) sslSocketFactory.createSocket(address, port, myAddress,0);
        sock.setEnabledProtocols(sslProtocols);
		return sock;
    }

   public Socket createSocket(InetAddress address, int port, InetAddress myAddress )  throws IOException {
	return new Socket(address, port, myAddress, 0 );
   }

   
}
