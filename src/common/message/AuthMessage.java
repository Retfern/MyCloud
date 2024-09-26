package common.message;

public class AuthMessage extends AbstractMessage {
private String userName;
private String password;
private String command;
private boolean authSuccessfull;

public AuthMessage (String userName, String password) {
    this.command = "/auth";
    this.userName = userName;
    this.password = password;
}

public AuthMessage (boolean result, String userName) {
    this.command = "/auth";
    this.authSuccessfull = result;
    this.userName = userName;
}

    public String getPassword() {
        return password;
    }

    public String getUserName() {
        return userName;
    }

    public String getCommand (){
    return command;
    }

    public boolean isAuthSuccessfull() {
        return authSuccessfull;
    }
}
