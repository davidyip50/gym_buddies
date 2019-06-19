package service.idm.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.idm.IDMService;
import service.idm.exceptions.ModelValidationException;
import service.idm.logger.ServiceLogger;
import service.idm.models.VerifySessionRequestModel;
import service.idm.models.VerifySessionResponseModel;
import service.idm.security.Session;
import service.idm.security.Token;
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

@Path("session")
public class VerifySession {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifySession(String json, @Context HttpHeaders headers)
    {
        try
        {
            VerifySessionRequestModel requestModel;
            String ipAdd = headers.getHeaderString("ipAddress");

            requestModel = (VerifySessionRequestModel) ModelValidator.verifyModel(json, VerifySessionRequestModel.class);
            ModelValidator.verifyInfo(requestModel.getEmail());
            ModelValidator.verifySessionID(requestModel.getSessionID());

            return checkSession(requestModel.getEmail(),requestModel.getSessionID(),ipAdd);
        }
        catch (ModelValidationException e) {
            return ModelValidator.returnInvalidRequest(e,VerifySessionResponseModel.class);
        }

    }

    private Response checkSession(String email, String sessionID,String ipAddr)
    {
        try {
            VerifySessionResponseModel responseModel;
            ServiceLogger.LOGGER.info("sessionID: " + sessionID);

            String query = "SELECT count(*) FROM sessions WHERE email = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if(rs.next())
            {
                int totalUsers = rs.getInt("count(*)");
                if(totalUsers < 1)
                {
                    ServiceLogger.LOGGER.info("email " +  email + "users: " + totalUsers);
                    responseModel = new VerifySessionResponseModel( 14,
                            "User not found");
                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
            }

            String query2 = "SELECT status, timeCreated, lastUsed, exprTime FROM sessions WHERE sessionID = ? AND email = ?";

            PreparedStatement ps2 = IDMService.getCon().prepareStatement(query2);
            ps2.setString(1, sessionID);
            ps2.setString(2, email);
            ServiceLogger.LOGGER.info("query: " + ps2.toString());

            rs = ps2.executeQuery();

            if(rs.next())
            {
                //used for testing
                //testVerify(sessionID);
                Timestamp timeCreated = rs.getTimestamp("timeCreated");
                Timestamp lastUsed = rs.getTimestamp("lastUsed");
                Timestamp exprTime = rs.getTimestamp("exprTime");
                int status = rs.getInt("status");

                Token tok = Token.rebuildToken(sessionID);
                Session currentSession = Session.rebuildSession(email, tok, timeCreated, lastUsed, exprTime);
                Session newSession = Session.createSession(email);


                if( status == 1 )
                {
                    Timestamp ts = new Timestamp(System.currentTimeMillis());
                    if(ts.after(exprTime))
                    {
                        responseModel = new VerifySessionResponseModel( 131,
                                "Session is expired.");
                        Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));
                        return Response.status(Response.Status.OK).entity(responseModel).build();
                    }
                    if(currentSession.getExprTime().getTime() - System.currentTimeMillis() < Session.SESSION_TIMEOUT)
                    {
                        updateSession(sessionID, 4, lastUsed);
                        //insert a new session
                        insertNewSession(newSession);
                        responseModel = new VerifySessionResponseModel( 133,
                                "Session is revoked", newSession.getSessionID().toString());
                        Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                        return Response.status(Response.Status.OK).entity(responseModel).build();
                    }
                    if(System.currentTimeMillis() - currentSession.getLastUsed().getTime() > Session.SESSION_TIMEOUT)
                    {
                        updateSession(sessionID,5, lastUsed);
                        responseModel = new VerifySessionResponseModel( 134,
                                "Session has timed out.", sessionID);
                        Transactions.insertTransaction(sessionID,ipAddr,new Timestamp(System.currentTimeMillis()));

                        return Response.status(Response.Status.OK).entity(responseModel).build();
                    }
                    //if(System.currentTimeMillis() - currentSession.getLastUsed().getTime() < Session.SESSION_TIMEOUT)
                    else
                    {
                        currentSession.update();
                        updateSession(sessionID,1, currentSession.getLastUsed());
                        responseModel = new VerifySessionResponseModel( 130,
                                "Session is active.", sessionID);
                        Transactions.insertTransaction(sessionID,ipAddr,new Timestamp(System.currentTimeMillis()));
                        return Response.status(Response.Status.OK).entity(responseModel).build();
                    }

                }
                if( status == 2 )
                {
                    responseModel = new VerifySessionResponseModel( 132,
                            "Session is closed");
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
                if( status == 3 )
                {
                    responseModel = new VerifySessionResponseModel( 131,
                            "Session is expired");
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
                if( status == 4)
                {
                    responseModel = new VerifySessionResponseModel( 133,
                            "Session is revoked");
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
            }
            else
            {
                responseModel = new VerifySessionResponseModel( 134,
                        "Session not found");
                Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                return Response.status(Response.Status.OK).entity(responseModel).build();
            }
        }
        catch (SQLException e)
        {
            ServiceLogger.LOGGER.warning("Failed to insert into database");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    private void insertNewSession(Session newSession)
    {
        try{
            String query = "INSERT sessions(email, sessionID, status, timeCreated, lastUsed, exprTime)" +
                    "VAlUES(?,?,?,?,?,?)";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            System.out.println(newSession.getEmail());
            ps.setString(1, newSession.getEmail());
            ps.setString(2, newSession.getSessionID().toString());
            ps.setInt(3, 1);
            ps.setTimestamp(4, newSession.getTimeCreated());
            ps.setTimestamp(5, newSession.getLastUsed());
            ps.setTimestamp(6, newSession.getExprTime());

            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            ServiceLogger.LOGGER.warning("Failed to insert into database");
            e.printStackTrace();
        }
    }


    private void updateSession(String session, int status, Timestamp lastUsed)
    {
        try{
            String query = "UPDATE sessions SET status = ?, lastUsed = ? WHERE sessionID = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setInt(1,status);
            ps.setTimestamp(2, lastUsed);
            ps.setString(3, session);

            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            ServiceLogger.LOGGER.warning("Failed to insert into database");
            e.printStackTrace();
        }
    }

    private void testVerify(String session)
    {
        try{
            String query = "UPDATE sessions SET lastUsed = ? WHERE sessionID = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, session);

            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            ServiceLogger.LOGGER.warning("Failed to insert into database");
            e.printStackTrace();
        }
    }
}
