package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

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
            throw new IllegalStateException("Ошибка загрузки Properties");
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
            throw new IllegalStateException("Ошибка сохранения Post");
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
                return posts;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка выгрузки Post");
        }
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
            throw new IllegalStateException("Ошибка выгрузки Post");
        }
        return rsl;
    }

    private Post doPost(ResultSet resultSet) {
        try {
            return new Post(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("link"),
                    resultSet.getString("text"),
                    resultSet.getTimestamp("created").toLocalDateTime());
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка создания Post");
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("grabber.properties")) {
            properties.load(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Ошибка загрузки Properties");
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
            throw new IllegalStateException("Ошибка при демонстрации методов");
        }
    }
}