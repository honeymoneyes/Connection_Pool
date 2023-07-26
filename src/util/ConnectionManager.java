package util;

import javax.print.StreamPrintServiceFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class ConnectionManager {
    public static final String DB_URL = "db.url";
    public static final String DB_USERNAME = "db.username";
    public static final String DB_PASSWORD = "db.password";
    public static final String DB_POOL_SIZE = "db.pool.size";
    public static final Integer DEFAULT_POOL_SIZE = 10;
    public static BlockingQueue<Connection> pool;
    public static List<Connection> sourceConnections;

    private ConnectionManager() {
    }

    static {
        loadDriver();
        initConnectionPool();
    }

    private static void initConnectionPool() {
        String poolSize = PropertiesUtil.get(DB_POOL_SIZE); // получаем размер пула из properties
        var size = poolSize == null ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSize); // проверка на null
        pool = new ArrayBlockingQueue<>(size); // инициализация очереди
        sourceConnections = new ArrayList<>(size); // инициализая листа

        for (int i = 0; i < size; i++) {
            Connection connection = open(); // открытие соединения
            var proxyConnection = (Connection) Proxy.newProxyInstance(ConnectionManager.class.getClassLoader(), new Class[]{Connection.class},
                    (proxy, method, args) -> method.getName().equals("close") // если на proxy объекте вызывается close, то
                            ? pool.add((Connection) proxy) // объект кастится в Connection и помещается в pool
                            : method.invoke(connection, args)); // а если любой другой метод, то просто на изначальном

            pool.add(proxyConnection); // добавление proxy соединения в pool
            sourceConnections.add(connection); // добавление оригинальных соединений в list
        }
    }

    private static void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection get() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection open() {
        try {
            return DriverManager.getConnection(
                    PropertiesUtil.get(DB_URL),
                    PropertiesUtil.get(DB_USERNAME),
                    PropertiesUtil.get(DB_PASSWORD));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closePool() {
        for (Connection sourceConnection : sourceConnections) {
            try {
                sourceConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
