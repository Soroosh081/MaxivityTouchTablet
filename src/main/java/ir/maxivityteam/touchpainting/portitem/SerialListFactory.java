/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir.maxivityteam.touchpainting.portitem;

import com.fazecast.jSerialComm.SerialPort;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

/**
 *
 * @author Soroosh
 */
public class SerialListFactory implements Callback<ListView<SerialPort>, ListCell<SerialPort>> {

    @Override
    public ListCell<SerialPort> call(ListView<SerialPort> arg0) {
        return new ListCell<SerialPort>() {
            @Override
            protected void updateItem(SerialPort item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(item.getDescriptivePortName());
                }
            }
        };
    }

}
