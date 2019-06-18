package service.idm.utilities;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.util.ExceptionUtils;
import service.idm.checks.Checker;
import service.idm.exceptions.*;
import service.idm.logger.ServiceLogger;
import service.idm.models.Model;
import service.idm.models.RegisterRequestModel;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;

import static service.idm.utilities.ResultCodes.*;
import static service.idm.utilities.HTTPStatusCodes.*;



public class ModelValidator {
    public static void verifyInfo(String email, char[] password) throws ModelValidationException {
        String warning;

        try {
            Checker.checkUserInfo(email,password);
        } catch (InvalidEmailInput e) {
            warning = "Email address has invalid format.";
            throw new ModelValidationException(warning, e);
        } catch (InvalidEmailLength e) {
            warning = "Email address has invalid length";
            throw new ModelValidationException(warning, e);
        } catch (EmptyPassword e) {
            warning = "Password has invalid length (cannot be empty/null)";
            throw new ModelValidationException(warning, e);
        } catch (InvalidPasswordLength e) {
            warning = "Password does not meet length requirements";
            throw new ModelValidationException(warning, e);
        } catch (InvalidPasswordRequirements e) {
            warning = "Password does not meet character requirements";
            throw new ModelValidationException(warning, e);
        }
    }

    public static void verifyInfo(String email) throws ModelValidationException {
        String warning;

        try {
            Checker.checkUserInfo(email);
        } catch (InvalidEmailInput e) {
            warning = "Email address has invalid format.";
            throw new ModelValidationException(warning, e);
        } catch (InvalidEmailLength e) {
            warning = "Email address has invalid length";
            throw new ModelValidationException(warning, e);
        }
    }

    public static void verifySessionID(String sessionID) throws ModelValidationException {
        String warning;

        try {
            Checker.checkSession(sessionID);
        } catch (InvalidSessionLength e) {
            warning = "Token has invalid length";
            throw new ModelValidationException(warning, e);
        }
    }

    public static Model verifyModel(String jsonText, Class modelType) throws ModelValidationException {
        ServiceLogger.LOGGER.info("Verifying model format...");
        ObjectMapper mapper = new ObjectMapper();
        String warning = "";
        Model model;

        try {
            ServiceLogger.LOGGER.info("Attempting to deserialize JSON to POJO");
            model = (Model) mapper.readValue(jsonText, modelType);
            ServiceLogger.LOGGER.info("Successfully deserialized JSON to POJO.");
        } catch (JsonMappingException e) {
            warning = "Unable to map JSON to POJO--request has invalid format.";
            ServiceLogger.LOGGER.warning(warning + "\n" + ExceptionUtils.exceptionStackTraceAsString(e));
            throw new ModelValidationException(warning, e);
        } catch (JsonParseException e) {
            warning = "Unable to parse JSON--text is not in valid JSON format.";
            ServiceLogger.LOGGER.warning(warning + "\n" + ExceptionUtils.exceptionStackTraceAsString(e));
            throw new ModelValidationException(warning, e);
        } catch (IOException e) {
            warning = "IOException while mapping JSON to POJO.";
            ServiceLogger.LOGGER.warning(warning + "\n" + ExceptionUtils.exceptionStackTraceAsString(e));
            throw new ModelValidationException(warning, e);
        }
        ServiceLogger.LOGGER.info("Model has been validated.");
        return model;
    }

    public static Response returnInvalidRequest(ModelValidationException e, Class modelType) {
        try {
            Class<?> model = Class.forName(modelType.getName());
            Constructor<?> constructor;
            constructor = model.getConstructor(Integer.TYPE,String.class);
            Object object = null;
            int resultCode;

            if (e.getCause() instanceof JsonMappingException) {
                object = constructor.newInstance(JSON_MAPPING_EXCEPTION,ResultCodes.setMessage(JSON_MAPPING_EXCEPTION));
                resultCode = JSON_MAPPING_EXCEPTION;
            } else if (e.getCause() instanceof JsonParseException) {
                object = constructor.newInstance(JSON_PARSE_EXCEPTION,ResultCodes.setMessage(JSON_PARSE_EXCEPTION));
                resultCode = JSON_PARSE_EXCEPTION;

            } else if (e.getCause() instanceof InvalidEmailLength) {
                object = constructor.newInstance(EMAIL_INVALID_LENGTH,ResultCodes.setMessage(EMAIL_INVALID_LENGTH));
                resultCode = EMAIL_INVALID_LENGTH;
            }
            else if (e.getCause() instanceof InvalidEmailInput) {
                object = constructor.newInstance(EMAIL_INVALID_FORMAT,ResultCodes.setMessage(EMAIL_INVALID_FORMAT));
                resultCode = EMAIL_INVALID_FORMAT;
            }
            else if (e.getCause() instanceof EmptyPassword) {
                object = constructor.newInstance(PASSWORD_INVALID_LENGTH,ResultCodes.setMessage(PASSWORD_INVALID_LENGTH));
                resultCode = PASSWORD_INVALID_LENGTH;
            }
            else if (e.getCause() instanceof InvalidPasswordLength) {
                object = constructor.newInstance(PASSWORD_INSUFFICIENT_LENGTH,ResultCodes.setMessage(PASSWORD_INSUFFICIENT_LENGTH));
                resultCode = PASSWORD_INSUFFICIENT_LENGTH;
            }
            else if (e.getCause() instanceof InvalidPasswordRequirements) {
                object = constructor.newInstance(PASSWORD_INSUFFICIENT_CHARS,ResultCodes.setMessage(PASSWORD_INSUFFICIENT_CHARS));
                resultCode = PASSWORD_INSUFFICIENT_CHARS;
            }
            else if(e.getCause() instanceof InvalidSessionLength)
            {
                object = constructor.newInstance(TOKEN_INVALID_LENGTH,ResultCodes.setMessage(TOKEN_INVALID_LENGTH));
                resultCode = TOKEN_INVALID_LENGTH;
            }
            else {
                object = constructor.newInstance(INTERNAL_SERVER_ERROR,ResultCodes.setMessage(INTERNAL_SERVER_ERROR));
                resultCode = INTERNAL_SERVER_ERROR;
            }
            return Response.status(setHTTPStatus(resultCode)).entity(object).build();
        } catch (Exception ex) {
            ex.printStackTrace();
            ServiceLogger.LOGGER.warning("Unable to create ResponseModel " + modelType.getName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
