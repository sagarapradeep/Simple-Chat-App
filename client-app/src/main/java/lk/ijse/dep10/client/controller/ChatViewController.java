package lk.ijse.dep10.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lk.ijse.dep10.shared.Header;
import lk.ijse.dep10.shared.Message;
import lk.ijse.dep10.shared.login.LoginUser;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import java.util.ArrayList;


public class ChatViewController {
    public TextField txtMsg;
    public TextArea txtMessageArea;

    public Circle crlProfilePic;
    public Button btnViewProfile;
    public AnchorPane root;
    public Label lblName;
    LoginUser currentUser;

    /*User defined*/
    ObjectInputStream ois;
    ObjectOutputStream oos;
    private DialogPane dialogPane;
    @FXML
    private Button btnSend;
    private Socket remoteSocket;


    @FXML
    private ListView<String> lstUsers;

    public void initialize() {
        connect();
        readServerResponse();


        Platform.runLater(()->{
            initialMessage();
            closeSocketOnStageCloseRequest();
            Image image = new Image("/img/user.png");
            crlProfilePic.setFill(new ImagePattern(image));
            lblName.setText(currentUser.getUserName());





        });




    }





    private void initialMessage() {


        Message message = new Message(Header.NEW_MESSENGER, currentUser);
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void readServerResponse() {
        new Thread(() -> {

            while (true) {
                try {
                    Message newMsg = (Message) ois.readObject();

                    if (newMsg.getHeader() == Header.USERS) {

                        ArrayList<String> activeUsersList = (ArrayList<String>) newMsg.getBody();
                        Platform.runLater(() -> {
                            lstUsers.getItems().clear();
                            lstUsers.getItems().addAll(activeUsersList);

                        });

                    } else if (newMsg.getHeader() == Header.MESSAGE) {
                        txtMessageArea.setText((String) newMsg.getBody());

                    }

                } catch (IOException | ClassNotFoundException e) {
                    if (!remoteSocket.isClosed()) {
                        try {
                            remoteSocket.close();
                            e.printStackTrace();
                            System.exit(0);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                }
            }


        }).start();

    }

    public void initData(LoginUser currentUser) {

        this.currentUser = currentUser;

    }

    private void connect() {
        try {
            remoteSocket = new Socket("192.168.8.101", 5050);

            oos = new ObjectOutputStream(remoteSocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(remoteSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to connect the server Please try again!").showAndWait();
            System.exit(2);

        }
    }


    @FXML
    void btnSendOnAction(ActionEvent event) {

        String msg = currentUser.getUserName() + ": " + txtMsg.getText() + "\n";

        Message newMsg = new Message(Header.MESSAGE, msg);
        try {
            oos.writeObject(newMsg);
            oos.flush();
            txtMsg.clear();

        } catch (IOException e) {
            e.printStackTrace();

        }


    }

    public void txtMsgOnAction(ActionEvent actionEvent) {


    }



    public void btnViewProfileOnAction(ActionEvent actionEvent) throws IOException {
        Stage stage = new Stage();

        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("/view/UserProfileView.fxml"));
        AnchorPane root = fxmlLoader.load();

        UserProfileViewController ctrl = fxmlLoader.getController();
        ctrl.initDataUserProfile(currentUser);
        stage.setScene(new Scene(root));


        stage.sizeToScene();

        stage.show();


    }
    private void closeSocketOnStageCloseRequest() {

        txtMsg.getScene().getWindow().setOnCloseRequest(event -> {
            try {
                oos.writeObject(new Message(Header.EXIT, null));
                oos.flush();
                if (!remoteSocket.isClosed()){
                    remoteSocket.close();

                    System.exit(0);

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
