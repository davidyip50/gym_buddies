package service.idm.utilities;

import org.glassfish.jersey.jackson.JacksonFeature;
import service.idm.IDMService;
import service.idm.logger.ServiceLogger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;

public class Transactions {
    private static final String API_KEY = "6a7a54f42d7f5d8eb7056ed2de6b14c1";
    private static final String GEO_URL = "http://api.ipstack.com/";

    public static void insertTransaction(String sessionID, String ip, Timestamp time) throws SQLException {
        checkLocation(ip);
        String query = "INSERT INTO transactions2(sessionid, source_ip, request_time)\n" +
                "VALUES (?,?,?)";

        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, sessionID);
        ps.setString(2, ip);
        ps.setTimestamp(3, time);

        ps.execute();
    }

    public static void insertTransaction(String ip, Timestamp time) throws SQLException {
        checkLocation(ip);
        String query = "INSERT INTO transactions2(source_ip, request_time)\n" +
                "VALUES (?,?)";

        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, ip);
        ps.setTimestamp(2, time);

        ps.execute();
    }

    public static void insertGeoLocation(String ip) throws SQLException {
        try {
            String requestURL = GEO_URL + ip + "?access_key=" + API_KEY;
            ServiceLogger.LOGGER.info("Building client...");
            Client client = ClientBuilder.newClient();
            client.register(JacksonFeature.class);

            ServiceLogger.LOGGER.info("Building WebTarget...");
            WebTarget webTarget = client.target(requestURL);
            ServiceLogger.LOGGER.info("Starting invocation builder...");
            Invocation.Builder invocation = webTarget.request(MediaType.APPLICATION_JSON);
            ServiceLogger.LOGGER.info("Setting payload of the request");
            Response response = invocation.get();
            HashMap<String, String> jsonText = response.readEntity(HashMap.class);
            ServiceLogger.LOGGER.info(jsonText.get("ip"));

            if(jsonText.get("latitude") != null && jsonText.get("longitude") != null)
            {
                insertIntoLocation(jsonText.get("ip"),jsonText.get("hostname"),jsonText.get("type"),
                        jsonText.get("continent_code"), jsonText.get("continent_name"),jsonText.get("country_code")
                        ,jsonText.get("country_name"), jsonText.get("region_code"),jsonText.get("region_name")
                        ,jsonText.get("city"),jsonText.get("zip"));
            }
            else
            {
                insertIntoLocation(jsonText.get("ip"),jsonText.get("hostname"),jsonText.get("type"),
                        jsonText.get("continent_code"), jsonText.get("continent_name"),jsonText.get("country_code")
                        ,jsonText.get("country_name"), jsonText.get("region_code"),jsonText.get("region_name")
                        ,jsonText.get("city"),jsonText.get("zip"),jsonText.get("latitude"),jsonText.get("longitude"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void checkLocation(String ip) throws SQLException {
        String query = "SELECT count(*)\n" +
                "FROM locations\n" +
                "where ip = ?";
        PreparedStatement ps = IDMService.getCon().prepareStatement(query);

        ps.setString(1, ip);
        ResultSet rs = ps.executeQuery();

        if(rs.next())
        {
            if(rs.getInt("count(*)") >= 0)
            {
                return;
            }
            else
            {
                insertGeoLocation(ip);
            }
        }
    }
    private static void insertIntoLocation(String ip,String hostname,String type,String continent_code,String continent_name,
                                           String country_code,String country_name,String region_code,
                                           String region_name,String city,String zip) throws SQLException {
        String query = "INSERT INTO locations(ip, hostname, type, continent_code, continent_name, country_code, country_name, region_code, region_name, city, zip)\n" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, ip);
        ps.setString(2, hostname);
        ps.setString(3, type);
        ps.setString(4, continent_code);
        ps.setString(5, continent_name);
        ps.setString(6, country_code);
        ps.setString(7, country_name);
        ps.setString(8, region_code);
        ps.setString(9, region_name);
        ps.setString(10, city);
        ps.setString(11, zip);
        ps.execute();

    }

    private static void insertIntoLocation(String ip,String hostname,String type,String continent_code,String continent_name,
                                           String country_code,String country_name,String region_code,
                                           String region_name,String city,String zip,String lat, String lon) throws SQLException {
        String query = "INSERT INTO locations(ip, hostname, type, continent_code, continent_name, country_code, country_name, region_code, region_name, city, zip, latitude, longitude)\n" +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, ip);
        ps.setString(2, hostname);
        ps.setString(3, type);
        ps.setString(4, continent_code);
        ps.setString(5, continent_name);
        ps.setString(6, country_code);
        ps.setString(7, country_name);
        ps.setString(8, region_code);
        ps.setString(9, region_name);
        ps.setString(10, city);
        ps.setString(11, zip);
        ps.setString(12, lat);
        ps.setString(13, lon);
        ps.execute();

    }
}
