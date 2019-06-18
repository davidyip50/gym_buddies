package service.idm.checks;

import service.idm.exceptions.*;
import service.idm.models.VerifySessionResponseModel;

import javax.ws.rs.core.Response;

public class Checker {
    public static void checkUserInfo(String email, char[] password) throws InvalidEmailInput, InvalidEmailLength, EmptyPassword, InvalidPasswordLength, InvalidPasswordRequirements {
        boolean isEmailValid = email.matches("^.+@.+\\..+");
        if(!isEmailValid)
        {
            throw new InvalidEmailInput();
        }
        else if( email.length() > 50 || email.length() == 0 )
        {
            throw new InvalidEmailLength();
        }
        else if(password == null || password.length == 0)
        {
            throw new EmptyPassword();
        }
        else if(password.length < 7 || password.length > 16)
        {
            throw new InvalidPasswordLength();
        }
        else if(checkPassword(password) == false)
        {
            throw new InvalidPasswordRequirements();
        }
    }

    public static void checkUserInfo(String email) throws InvalidEmailInput, InvalidEmailLength {
        boolean isEmailValid = email.matches("^.+@.+\\..+");
        if(!isEmailValid)
        {
            throw new InvalidEmailInput();
        }
        if( email.length() > 50 || email.length() == 0 )
        {
            throw new InvalidEmailLength();
        }
    }

    public static void checkSession(String sessionID) throws InvalidSessionLength {
        if(sessionID.length() == 0 || sessionID.length() > 128)
        {
            throw new InvalidSessionLength();
        }
    }

    private static boolean checkPassword(char[] password) {
        boolean checkSpecial = false;
        boolean checkAlpha = false;
        boolean checkNum = false;
        for(int i = 0; i < password.length; ++i)
        {
            if(Character.isDigit(password[i]))
            {
                checkNum = true;
            }
            if(Character.isAlphabetic(password[i]))
            {
                checkAlpha = true;
            }
            if((password[i] > 32 && password[i] < 48) || (password[i] > 57 && password[i] < 65)
                    || (password[i] > 90 && password[i] < 96)|| (password[i] > 122 && password[i] < 127))
            {
                checkSpecial = true;
            }
        }
        return checkSpecial && checkAlpha && checkNum;
    }
}
