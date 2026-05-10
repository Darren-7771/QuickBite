module com.example.projectrplbo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens com.example.projectrplbo to javafx.fxml;
    opens com.example.projectrplbo.controller to javafx.fxml;
    opens com.example.projectrplbo.model to javafx.base;

    exports com.example.projectrplbo;
    exports com.example.projectrplbo.model;
    exports com.example.projectrplbo.chatbot;
    exports com.example.projectrplbo.db;
    exports com.example.projectrplbo.controller;
}
