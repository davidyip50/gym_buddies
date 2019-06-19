package service.idm.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.idm.IDMService;
import service.idm.exceptions.ModelValidationException;
import service.idm.logger.ServiceLogger;
import service.idm.models.VerifyPrivilegeRequestModel;
import service.idm.models.VerifyPrivilegeResponseModel;
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

@Path("privilege")
public class VerifyPrivilege {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyPrivilege(String json, @Context HttpHeaders headers)
    {
        try
        {
            ServiceLogger.LOGGER.info("Verifying priv....");
            VerifyPrivilegeRequestModel requestModel;
            String ipAdd = headers.getHeaderString("ipAddress");

            requestModel = (VerifyPrivilegeRequestModel) ModelValidator.verifyModel(json,VerifyPrivilegeRequestModel.class);
            ModelValidator.verifyInfo(requestModel.getEmail());
            ModelValidator.verifyPrivilegeInput(requestModel.getPlevel());
            return checkPrivilege(requestModel.getEmail(),requestModel.getPlevel(),ipAdd);
        }
        catch (ModelValidationException e) {
            return ModelValidator.returnInvalidRequest(e,VerifyPrivilegeResponseModel.class);
        }
    }

    private Response checkPrivilege(String email, int plevel,String ipAddr)
    {
        try
        {
            ServiceLogger.LOGGER.info("Checking priv....");
            VerifyPrivilegeResponseModel responseModel;
            //*IDMService.insertTransaction(ses.getSessionID().toString(),ipAddr,new Timestamp(System.currentTimeMillis()));

            //Start Query
            String query = "SELECT plevel FROM users WHERE email = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1,email);

            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                int checkPLevel = rs.getInt("plevel");
                ServiceLogger.LOGGER.info("user lvl: " + checkPLevel + " request lvl: " + plevel);
                if(checkPLevel <= plevel)
                {
                    responseModel = new VerifyPrivilegeResponseModel(140, "User has sufficient privilege level.");
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
                else
                {
                    responseModel = new VerifyPrivilegeResponseModel(141, "User has insufficient privilege level.");
                    Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

                    return Response.status(Response.Status.OK).entity(responseModel).build();
                }
            }
            else
            {
                responseModel = new VerifyPrivilegeResponseModel(14, "User not found.");
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
    }
}
