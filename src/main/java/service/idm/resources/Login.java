package service.idm.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.idm.IDMService;
import service.idm.exceptions.ModelValidationException;
import service.idm.logger.ServiceLogger;
import service.idm.models.LoginRequestModel;
import service.idm.models.LoginResponseModel;
import service.idm.security.Crypto;
import service.idm.security.Session;
import service.idm.security.Token;
import service.idm.utilities.ByteToString;
import service.idm.utilities.ModelValidator;
import service.idm.utilities.Transactions;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;

@Path("login")
public class Login {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(String json, @Context HttpHeaders headers)
    {
        try
        {

            String ipAdd = headers.getHeaderString("ipAddress");
            ServiceLogger.LOGGER.info("ip add: " + ipAdd);

            LoginRequestModel requestModel = (LoginRequestModel) ModelValidator.verifyModel(json,LoginRequestModel.class);
            ModelValidator.verifyInfo(requestModel.getEmail(),requestModel.getPassword());

            Response response = authenticateUser(requestModel.getEmail(),requestModel.getPassword(),ipAdd);
            //clear out password
            ByteToString.clearPass(requestModel.getPassword());
            return response;
        }
        catch (ModelValidationException e) {
            return ModelValidator.returnInvalidRequest(e,LoginResponseModel.class);
        }
    }

    private Response authenticateUser(String email, char[] password,String ipAddr)
    {
        try {
            LoginResponseModel responseModel;

            insertCheckUser(email);

            if(checkLocked(email))
            {
                insertCheckUser(email);
                responseModel = new LoginResponseModel(12,
                        "Account has been locked");
                Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));
                return Response.status(Response.Status.OK).entity(responseModel).build();

            }
            String query = "SELECT salt, pword FROM users WHERE email = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if(rs.next()) {
                ServiceLogger.LOGGER.info("pass: " + password.toString());

                byte[] dataPass = ByteToString.convert(rs.getString("pword"));
                byte[] salt = ByteToString.convert(rs.getString("salt"));
                byte[] userPass = Crypto.hashPassword( password, salt,
                        10000, 512 );
                if(Arrays.equals(userPass, dataPass))
                {
                    //create Session
                    Session ses = Session.createSession(email);
                    Token tok = ses.getSessionID();
                    String stringTok = tok.toString();
                    responseModel = new LoginResponseModel(120,
                            "User logged in successfully.", stringTok);
                    //check if session already exist
                    checkForSession(email);
                    //insert into Session database
                    insertSession(ses);
                    //return OK to client
                    resetUserCount(email);
                    Transactions.insertTransaction(ses.getSessionID().toString(),ipAddr,new Timestamp(System.currentTimeMillis()));
                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
                else
                {
                    responseModel = new LoginResponseModel(11,
                            "Passwords do not match.");
                    //insertCheckUser(email);
                    incrementCount(email);
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));
                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
            }
            else
            {
                responseModel = new LoginResponseModel(14,
                        "User not found.");
                return Response.status(Response.Status.OK).entity(responseModel).build();
            }
        } catch (SQLException e) {
            ServiceLogger.LOGGER.warning("Query failed");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }



    private void insertSession(Session session) throws SQLException {
            String query = "INSERT sessions(email, sessionID, status, timeCreated, lastUsed, exprTime)" +
                    "VAlUES(?,?,?,?,?,?)";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, session.getEmail());
            ps.setString(2, session.getSessionID().toString());
            ps.setInt(3, 1);
            ps.setTimestamp(4, session.getTimeCreated());
            ps.setTimestamp(5, session.getLastUsed());
            ps.setTimestamp(6, session.getExprTime());

            boolean rs = ps.execute();
    }

    private void checkForSession(String email) throws SQLException {
            String query = "SELECT COUNT(*) FROM sessions WHERE email = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                if(rs.getInt("COUNT(*)") > 0)
                {
                    query = "UPDATE sessions SET status = 4 WHERE email = ? AND status = 1";
                    ps = IDMService.getCon().prepareStatement(query);

                    ps.setString(1, email);
                    ps.executeUpdate();
                }
            }
        }

    private boolean checkLocked(String email) throws SQLException {
        String query = "SELECT times\n" +
                "from createUser\n" +
                "WHERE email = ?";

        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();
        if(rs.next())
        {
            int counter = rs.getInt("times");
            ServiceLogger.LOGGER.info("Counter: " + counter);
            if(counter >= 3)
            {
                return true;
            }
            else
            {
                return false;
            }

        }
        else
        {
            return false;
        }
    }

    private void insertCheckUser(String email) throws SQLException {
        String query = "SELECT count(*)\n" +
                "From createUser\n" +
                "WHERE email = ?";
        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();
        if(rs.next())
        {
            if(rs.getInt("count(*)") > 0)
            {
                return;
            }
            else
            {
                query = "INSERT INTO createUser(email)\n" +
                        "VALUES (?)";

                ps = IDMService.getCon().prepareStatement(query);
                ps.setString(1, email);

                ps.execute();
            }
        }

    }

    private boolean incrementCount(String email) throws SQLException {
        String query = "SELECT times\n" +
                "from createUser\n" +
                "WHERE email = ?";

        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if(rs.next())
        {
            int counter = rs.getInt("times");
            ServiceLogger.LOGGER.info("Counter: " + counter);
            if(counter >= 3)
            {
                return true;
            }
            else
            {
                query = "UPDATE createUser\n" +
                        "SET times = ?\n" +
                        "WHERE email = ?";

                ps = IDMService.getCon().prepareStatement(query);
                ps.setInt(1, counter + 1);
                ps.setString(2, email);
                ps.executeUpdate();
                return false;
            }

        }
        else
        {
            return false;
        }
    }

    private void resetUserCount(String email) throws SQLException {
        String query = "UPDATE createUser\n" +
                "SET times = 0\n" +
                "WHERE email = ?";

        PreparedStatement ps = IDMService.getCon().prepareStatement(query);
        ps.setString(1, email);
        ps.executeUpdate();
    }
}
