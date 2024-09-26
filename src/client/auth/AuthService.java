package client.auth;

public interface AuthService {

    void close() throws Exception;

    boolean authUser(String username, String password) throws ClassNotFoundException;
}
