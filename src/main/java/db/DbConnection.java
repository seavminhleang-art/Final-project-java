package db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DbConnection {
    private static DbConnection instance;

    private final BlockingQueue<Connection> pool;
    private final String url;
    private final String username;
    private final String password;

    private DbConnection() {
        Properties props = loadProperties();
        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        int size = Integer.parseInt(props.getProperty("db.pool.size", "10"));

        this.pool = new ArrayBlockingQueue<>(size);
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PostgreSQL driver not found on classpath", e);
        }
        for (int i = 0; i < size; i++) {
            pool.add(createConnection());
        }
    }

    public static synchronized DbConnection getInstance() {
        if (instance == null) {
            instance = new DbConnection();
        }
        return instance;
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DbConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath");
            }
            props.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }
        return props;
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create DB connection", e);
        }
    }

    public Connection borrow() {
        try {
            Connection c = pool.take();
            if (c.isClosed()) {
                c = createConnection();
            }
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while borrowing connection", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check connection state", e);
        }
    }

    public void release(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }

    public void shutdown() {
        pool.forEach(c -> {
            try {
                c.close();
            } catch (SQLException ignored) {
            }
        });
    }
}
