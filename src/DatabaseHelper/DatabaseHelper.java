package DatabaseHelper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/tablodeneme";//test
    private static final String USER = "root";
    private static final String PASSWORD = "dogufb84";

    static {
        try {
            // MySQL JDBC sürücüsünü yükleyin
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("JDBC Driver loaded");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        System.out.println("Attempting to connect to the database...");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
