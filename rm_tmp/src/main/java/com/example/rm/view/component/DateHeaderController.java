package com.example.rm.view.component;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DateHeaderController {

    @FXML private Label lblDate;

    public void setDateText(String text) {
        lblDate.setText(text);
    }
}
