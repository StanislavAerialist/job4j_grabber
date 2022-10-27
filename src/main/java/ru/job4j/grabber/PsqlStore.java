package ru.job4j.grabber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PsqlStore.class.getName());

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(
                    cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password")
            );
        } catch (Exception e) {
            LOG.error("Ошибка подключения");
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement = cnn.prepareStatement(
                "insert into post(name, link, text, created) "
                  + "values (?, ?, ?, ?) on conflict (link) do nothing",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getLink());
            statement.setString(3, post.getDescription());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOG.error("Ошибка сохранения Post");
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = cnn.prepareStatement("select * from post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(doPost(resultSet));
                }
            }
        } catch (SQLException e) {
            LOG.error("Ошибка выгрузки Post");
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post rsl = null;
        try (PreparedStatement statement = cnn.prepareStatement("select * from post where id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    rsl = doPost(resultSet);
                }
            }
        } catch (Exception e) {
            LOG.error("Ошибка выгрузки Post");
        }
        return rsl;
    }

    private Post doPost(ResultSet resultSet) {
        Post rsl = null;
        try {
             rsl = new Post(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("link"),
                    resultSet.getString("text"),
                    resultSet.getTimestamp("created").toLocalDateTime());
        } catch (SQLException e) {
            LOG.error("Ошибка создания Post");
        }
        return rsl;
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("grabber.properties")) {
            properties.load(in);
        } catch (IOException ex) {
            LOG.error("Ошибка загрузки Properties");
        }
        return properties;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        try (PsqlStore psqlStore = new PsqlStore(load())) {
            Post post = new Post("Самый главный", "Какая-то ссылка", "Здесь должно быть описание",
                    LocalDateTime.now());
            psqlStore.save(post);
            System.out.println(psqlStore.findById(post.getId()));
        } catch (Exception e) {
            LOG.error("Ошибка при демонстрации методов");
        }
    }
}