package client.auth;

import java.sql.*;

public class AuthServiceImpl implements AuthService {
    private boolean result;
    private Connection con;
    private PreparedStatement ps;
    private PreparedStatement checkNameExist;
    private PreparedStatement updateUserName;

    private final String URL ="jdbc:sqlite:authorizationUsers.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
            e.printStackTrace();
            }
    }
    public AuthServiceImpl()  {
        try {
            con = DriverManager.getConnection(URL);
            ps = con.prepareStatement("SELECT * FROM authorizationUsers WHERE Name = ? AND Password= ?");
            checkNameExist = con.prepareStatement("SELECT * FROM authorizationUsers WHERE Name = ? ");
            updateUserName = con.prepareStatement("UPDATE authorizationUsers SET Name = ? WHERE Name = ? AND Password= ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        con.close();
    }

    @Override
    public boolean authUser(String username, String password) throws ClassNotFoundException {
        try {
            System.out.println("подключилось!");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rst = ps.executeQuery();
            result=rst.next();
            System.out.println(result);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(result);
        return result;
    }
}
