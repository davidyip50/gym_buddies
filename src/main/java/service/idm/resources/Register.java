package service.idm.resources;

import service.idm.IDMService;
import service.idm.exceptions.ModelValidationException;
import service.idm.logger.ServiceLogger;
import service.idm.models.Model;
import service.idm.models.RegisterRequestModel;
import service.idm.models.RegisterResponseModel;
import service.idm.security.Crypto;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Path("register")
public class Register {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    /*** Registers a user on success. If failed ModelValidationException is thrown***/
    public Response registerUser(String json, @Context HttpHeaders headers)
    {
        try {
            //Grab ip address of user
            String ipAdd = headers.getHeaderString("ipAddress");
            //Check if JSON Model is correct
            RegisterRequestModel requestModel = (RegisterRequestModel) ModelValidator.verifyModel(json,RegisterRequestModel.class);
            //Checks if User input is correct
            ModelValidator.verifyInfo(requestModel.getEmail(),requestModel.getPassword());

            Response response = returnResponse(requestModel.getEmail(),requestModel.getPassword(),ipAdd);
            return response;
        } catch (ModelValidationException e) {
            e.printStackTrace();
            return ModelValidator.returnInvalidRequest(e, RegisterResponseModel.class);
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    private Response returnResponse(String email, char[] password, String ipAddr) throws SQLException {
        RegisterResponseModel responseModel;

        if(checkDupUser(email) == true)
        {
            responseModel = new RegisterResponseModel(16,
                    "Email already in use");
            Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

            return Response.status(Response.Status.OK).entity(responseModel).build();
        }
        else
        {
            insertUser(email, password);
            responseModel = new RegisterResponseModel(110,
                    "User registered successfully");
            Transactions.insertTransaction(ipAddr,new Timestamp(System.currentTimeMillis()));

            return Response.status(Response.Status.OK).entity(responseModel).build();
        }
    }

    private boolean checkDupUser(String email)
    {
        try {
            String query = "SELECT u.email FROM users AS u WHERE u.email = ?";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);
            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                return true;
            }
            return false;
        }
        catch (SQLException e)
        {
            ServiceLogger.LOGGER.warning("Query failed");
            e.printStackTrace();
            return false;
        }
    }

    private void insertUser(String email, char[] password) throws SQLException {
            byte[] salt = Crypto.genSalt();
            byte[] hashPass = Crypto.hashPassword(password,salt,10000, 512);

            String query = "INSERT users(email,status,plevel,salt,pword)" + "VALUES(?,?,?,?,?)";

            PreparedStatement ps = IDMService.getCon().prepareStatement(query);

            ps.setString(1, email);
            ps.setInt(2,1);
            ps.setInt(3, 5);
            ps.setString(4, ByteToString.convertBytes(salt));
            ps.setString(5, ByteToString.convertBytes(hashPass));
            ps.executeUpdate();
    }
}
