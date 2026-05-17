package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;

public interface ICRUD<T> {
    void add(T var1) throws SQLException;

    void addMeth2(T var1) throws SQLException;

    void modifier(T var1) throws SQLException;

    void delete(T var1) throws SQLException;

    List<T> afficherList() throws SQLException;
}