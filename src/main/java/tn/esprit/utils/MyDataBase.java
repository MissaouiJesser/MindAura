package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    private final String URL ="jdbc:mysql://localhost:3306/mind";
    private final String USERNAME ="root";
    private final String PWD ="";

    private Connection conx;
    public static MyDataBase instance;

    private MyDataBase(){
        try {
            conx = DriverManager.getConnection(URL,USERNAME,PWD);
            System.out.println("Connexion établie!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public static MyDataBase getInstance(){
        if (instance == null){
            instance = new MyDataBase();
        }
        return instance;



    }
    public Connection getConx() {
        return conx;
    }



}
