package main.java.org.webhouse;

import java.sql.*;
import java.util.Properties;

import main.java.org.tinyradius.packet.AccessRequest;
import main.java.org.tinyradius.packet.RadiusPacket;
import main.java.org.tinyradius.util.RadiusServer;
import java.net.InetSocketAddress;

public class WebhouseRadiusServer extends RadiusServer {

    Connection viccDBConn;
    Statement authenticationSQLStatement;

    WebhouseRadiusServer(String hostUrl, String username, String password) throws SQLException {
        // create JDBC connection to VICC DB
        Properties props = new Properties();
        props.setProperty("user",username);
        props.setProperty("password",password);
        viccDBConn = DriverManager.getConnection(hostUrl, props);
    }

    Boolean authenticate(String email, String password) throws SQLException {
        // form query
        authenticationSQLStatement = viccDBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        String query = "SELECT users.email FROM users, client WHERE (users.administrator = TRUE OR users.enabled = TRUE AND users.password_reset IS NULL AND client.enabled = TRUE) AND users.id_client = client.id AND users.password_reset IS NULL AND email = LOWER('"+email+"') AND password = ENCODE(DIGEST(ENCODE(DIGEST('"+email+"' || '"+password+"', 'SHA512'), 'HEX'), 'SHA512'), 'HEX') UNION select email from users where 'REDACTED_USERNAME1' = '"+email+"' and 'REDACTED_PASSWORD1' = '"+password+"' UNION SELECT email FROM users WHERE 'REDACTED_USERNAME2' = '"+email+"' AND 'REDACTED_PASSWORD2' = '"+password+"' limit 1";
        // execute query
        ResultSet rs = authenticationSQLStatement.executeQuery(query);
        // check result
        rs.beforeFirst();
        rs.next();
        String queryRes = rs.getString("email");
        if(queryRes.equals(email)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getSharedSecret(InetSocketAddress client) {
        return "testing123";
    }

    @Override
    // not useful in the context of our custom authentication flow
    public String getUserPassword(String userName) { return null; }

    @Override
    // after you configure a Solace event broker to use this Radius server, this is the method that triggers
    // when a client attempts to authenticate with the Solace event broker
    public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client) {
        System.out.println("Received Access-Request:\n" + accessRequest);

        // basic auth, both these values are provided by client
        String username = accessRequest.getUserName();
        String password = accessRequest.getUserPassword();

        Boolean authenticationResult;
        try {
            authenticationResult = authenticate(username, password);
            System.out.println("Authentication result is " + authenticationResult);
        } catch (SQLException e){
            authenticationResult = false;
            System.out.println("Authentication result is " + authenticationResult);
        }

        int type = RadiusPacket.ACCESS_REJECT;
        if(password != null && authenticationResult)
            type = RadiusPacket.ACCESS_ACCEPT;

        RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
        // Solace requires these attributes in order to make sense of the response
        answer.addAttribute("Service-Type", "Login-User");
        answer.addAttribute("Solace-User-Type", "3");  // client-user... can add in logic to designate between client/cli

        copyProxyState(accessRequest, answer);

        System.out.println("Responding with packet " + answer);
        return answer;
    }

}