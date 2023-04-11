package lk.ijse.dep10.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lk.ijse.dep10.shared.Header;
import lk.ijse.dep10.shared.Message;
import lk.ijse.dep10.shared.login.LoginUser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LoginViewController {

    public AnchorPane root;
    Socket remoteSocket;
    ObjectInputStream ois;
    ObjectOutputStream oos;



    private boolean isValidUser;
    private LoginUser loginUser;


    @FXML
    private Button btnLogin;
    @FXML
    private Button btnSignUp;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private TextField txtUserName;

    public void initialize() {

        Platform.runLater(() -> {

            closeSocketOnStageCloseRequest();

        });


        isValidUser = false;
        try {
            remoteSocket = new Socket("192.168.8.101", 5050);
            oos = new ObjectOutputStream(remoteSocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(remoteSocket.getInputStream());

            readServerResponse();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open the client side socket!").showAndWait();
            System.exit(2);
        }
    }


    private void readServerResponse() {

        new Thread(() -> {

            while (true) {
                try {
                    Message newMsg = (Message) ois.readObject();

                    if (newMsg.getHeader() == Header.VALID_LOGIN) {


                        Platform.runLater(() -> {
                            Stage stage = (Stage) btnLogin.getScene().getWindow();
                            try {

                                FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/view/chatView.fxml"));
                                AnchorPane root = fxmlLoader.load();

                                ChatViewController ctrl = fxmlLoader.getController();
                                ctrl.initData(loginUser);


                                Scene scene = new Scene(root);
                                stage.setScene(scene);

                                oos.writeObject(new Message(Header.EXIT, null));
                                oos.flush();
                                if (!remoteSocket.isClosed()) {
                                    remoteSocket.close();

                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            stage.show();
                            stage.centerOnScreen();

                        });
                    } else {
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.WARNING, "Invalid Login try again!").showAndWait();
                            txtPassword.selectAll();
                            txtUserName.selectAll();
                            txtUserName.requestFocus();

                        });
                    }


                } catch (IOException e) {
                    return;

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Failed to close network sockets!").showAndWait();
                    System.exit(2);
                }
            }

        }).start();


    }


    @FXML
    void btnLoginOnAction(ActionEvent event) throws IOException {
        if (txtPassword.getText().isEmpty() || txtPassword.getText().isBlank()) {
            txtPassword.requestFocus();
            return;

        }

        if (txtUserName.getText().isEmpty() || txtUserName.getText().isBlank()) {
            txtUserName.requestFocus();
            return;

        }

        String userName = txtUserName.getText();
        String password = txtPassword.getText();
        loginUser = new LoginUser(userName, password);

        Message loginMessage = new Message(Header.NEW_LOGIN, loginUser);

        oos.writeObject(loginMessage);
        oos.flush();


    }


    @FXML
    void btnSignUpOnAction(ActionEvent event) throws IOException {

        Stage stage = (Stage) btnSignUp.getScene().getWindow();

        stage.setScene(new Scene(new FXMLLoader(this.getClass().getResource("/view/SignUpView.fxml")).load()));
        stage.sizeToScene();
        stage.show();
        stage.centerOnScreen();

        Message message = new Message(Header.EXIT, null);
        try {
            oos.writeObject(message);
            oos.flush();
            if (!remoteSocket.isClosed()) {
                remoteSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void closeSocketOnStageCloseRequest() {
        txtPassword.getScene().getWindow().setOnCloseRequest(event -> {
            try {
                oos.writeObject(new Message(Header.EXIT, null));
                oos.flush();
                if (!remoteSocket.isClosed()) {
                    remoteSocket.close();

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
