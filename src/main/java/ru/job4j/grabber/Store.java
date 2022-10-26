package ru.job4j.grabber;

import java.sql.SQLException;
import java.util.List;

public interface Store {
    void save(Post post);

    List<Post> getAll() throws SQLException;

    Post findById(int id);
}