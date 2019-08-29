package ir.maxivityteam.touchpainting;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;
import ir.maxivityteam.touchpainting.portitem.SerialListFactory;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;

public class MainController implements Initializable {

    @FXML
    private ComboBox<SerialPort> list;
    @FXML
    private Button connect;

    @FXML
    private TextArea area;

    @FXML
    private Button switchmodeButton;

    @FXML
    private ComboBox switchModeComboBox;

    @FXML
    private Button disconnect;

    private int mode = 1;

    private SerialPort port;

    public void setup() {
        SerialListFactory factory = new SerialListFactory();
        SerialPort[] ports = SerialPort.getCommPorts();
        switchModeComboBox.setItems(FXCollections.observableArrayList(new String[]{"TouchPad mode", "Touch Screen Mode"}));
        list.setCellFactory(factory);
        list.setButtonCell(factory.call(null));
        list.setItems(FXCollections.observableArrayList(ports));

        connect.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent arg0) {
                connect();
                switchModeComboBox.setDisable(false);
                switchmodeButton.setDisable(false);
            }
        });

        switchmodeButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent arg0) {
                try {
                    switchMode();
                } catch (IOException ex) {
                    Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        disconnect.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent arg0) {
                MainController.this.port.closePort();
                Platform.runLater(() -> {
                    switchModeComboBox.setDisable(true);
                    switchmodeButton.setDisable(true);
                    area.setText("");
                });
            }
        });
    }

    private void connect() {
        System.err.println("oo");
        this.port = list.getSelectionModel().getSelectedItem();
        this.port.openPort();
        Thread t = new Thread(() -> {
            try {
                port.getOutputStream().write("\n".getBytes());
                Thread.sleep(1000);
                port.getOutputStream().write("getMode\n".getBytes());
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        t.start();
        StringBuilder b = new StringBuilder();
        port.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] newData = event.getReceivedData();
                for (int i = 0; i < newData.length; ++i) {
                    char data = (char) newData[i];
                    b.append(data);
                    if (data == '\n') {
                        String inputdata = b.toString();
                        b.setLength(0);

                        Pattern p = Pattern.compile("(\\d{1,3},\\d{1,3})");
                        Matcher matcher = p.matcher(inputdata);
                        if (matcher.find()) {
                            try {
                                boolean click = false;
                                if (inputdata.startsWith("c")) {
                                    click = true;
                                }
                                moveMouse(matcher.group(), click);
                            } catch (AWTException ex) {
                                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else if (inputdata.contains("mode")) {
                            Platform.runLater(() -> {
                                switchModeComboBox.setDisable(false);
                                switchmodeButton.setDisable(false);
                            });
                            p = Pattern.compile("\\d");
                            matcher = p.matcher(inputdata);
                            if (matcher.find()) {
                                setMode(Integer.parseInt(matcher.group()));
                            }
                        }

                    }
                }
            }
        });
    }

    private void setMode(int mode) {
        Platform.runLater(() -> {
            switchModeComboBox.getSelectionModel().select(mode - 1);
        });
        this.mode = mode;
    }

    private void switchMode() throws IOException {
        int mode = switchModeComboBox.getSelectionModel().getSelectedIndex();
        if (mode == 0) {
            changeMode(1);
        } else if (mode == 1) {
            changeMode(2);
        }
    }

    private void changeMode(int mode) {
        Thread t = new Thread(() -> {
            try {
                this.mode = mode;
                this.port.getOutputStream().write(("mode" + mode + "\n").getBytes());
                Thread.sleep(1000);
            } catch (IOException ex) {
                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        t.start();
    }

    boolean isMousePressed;

    private void moveMouse(String moveData, boolean click) throws AWTException {

        final int y = Integer.parseInt(moveData.split(",")[0]);
        final int x = Integer.parseInt(moveData.split(",")[1]);

        Point location = MouseInfo.getPointerInfo().getLocation();

        int prevx = location.x;
        int prevy = location.y;
//
//        Platform.runLater(() -> {
//            area.appendText("X = " + (x) + " Y = " + (y) + "\n");
//        });

        Robot robot = new Robot();
        if (this.mode == 1) {
            robot.mouseMove(prevx - (x * 2), prevy - (y * 2));
        } else if (this.mode == 2) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double screenHeight = (int) screenSize.getHeight();
            double screenWidth = (int) screenSize.getWidth();

            double touchH = 766;
            double touchW = 931;
            double rh = (screenHeight / touchH);
            double rw = (screenWidth / touchW);

            double sx = rw * x;
            double sy = rh * y;

            Platform.runLater(() -> {
                //      area.appendText(sx + ","  + sy + "\n");
            });

            if (click) {
                if (!isMousePressed) {
                    robot.mousePress(InputEvent.BUTTON1_MASK);
                    isMousePressed = true;
                }
            } else {
                isMousePressed = false;
                robot.mouseRelease(InputEvent.BUTTON1_MASK);

            }

            Thread tt = new Thread(() -> {

                robot.mouseMove((int) sx, (int) sy);
            });

            tt.start();

        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

}
