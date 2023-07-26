import util.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        Connection connection = ConnectionManager.get();
        ConnectionManager.closePool();

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM aircraft");

        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            System.out.println(resultSet.getObject(1));
        }
    }
}