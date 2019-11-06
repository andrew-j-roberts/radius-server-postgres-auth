/**
 * $Id: TestServer.java,v 1.6 2006/02/17 18:14:54 wuttke Exp $
 * Created on 08.04.2005
 * @author Matthias Wuttke
 * @version $Revision: 1.6 $
 */
package main.java.org.webhouse;

public class Main {

	public static void main(String[] args)
	throws Exception {

		// VICC DB Connection Details
		String hostUrl = "jdbc:postgresql://localhost:5432/postgres";
		String username = "postgres";
		String password = "admin";

		WebhouseRadiusServer server = new WebhouseRadiusServer(hostUrl, username, password);

		server.start(true, true);
		System.out.println("Server started on port " + server.getAuthPort());

		Thread.sleep(1000*60*30);
		System.out.println("Stop server");
		server.stop();
	}
}
