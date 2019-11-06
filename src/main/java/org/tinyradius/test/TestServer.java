/**
 * $Id: TestServer.java,v 1.6 2006/02/17 18:14:54 wuttke Exp $
 * Created on 08.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.6 $
 */
package main.java.org.tinyradius.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import main.java.org.tinyradius.packet.AccessRequest;
import main.java.org.tinyradius.packet.RadiusPacket;
import main.java.org.tinyradius.util.RadiusException;
import main.java.org.tinyradius.util.RadiusServer;

import java.sql.*;

/**
 * Test server which terminates after 30 s.
 * Knows only the client "localhost" with secret "testing123" and
 * the user "mw" with the password "test".
 */
public class TestServer {

	Connection con = null;
	PreparedStatement pstmt = null;

	public static void main(String[] args)
	throws IOException, Exception, SQLException {
		// first, establish connection to SQL db and form query
		// implement Radius server methods
		RadiusServer server = new RadiusServer() {
			// Authorize localhost/testing123
			public String getSharedSecret(InetSocketAddress client) {
				if (client.getAddress().getHostAddress().equals("127.0.0.1"))
					return "testing123";
				else
					return null;
			}
			
			// Authenticate mw
			public String getUserPassword(String userName) {
				if (userName.equals("mw"))
					return "test";
				else
					return null;
			}
			
			// Adds an attribute to the Access-Accept packet
			public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client) 
			throws RadiusException {
				System.out.println("Received Access-Request:\n" + accessRequest);
				RadiusPacket packet = super.accessRequestReceived(accessRequest, client);
				if (packet.getPacketType() == RadiusPacket.ACCESS_ACCEPT)
					packet.addAttribute("Reply-Message", "Welcome " + accessRequest.getUserName() + "!");
				if (packet == null)
					System.out.println("Ignore packet.");
				else
					System.out.println("Answer:\n" + packet);
				return packet;
			}
		};

		if (args.length >= 1)
			server.setAuthPort(Integer.parseInt(args[0]));
		if (args.length >= 2)
			server.setAcctPort(Integer.parseInt(args[1]));
		
		server.start(true, true);
		System.out.println(server.getAuthPort());
		System.out.println(server.getListenAddress());
		
		System.out.println("Server started.");
		
		Thread.sleep(1000*60*30);
		System.out.println("Stop server");
		server.stop();
	}
	
}
