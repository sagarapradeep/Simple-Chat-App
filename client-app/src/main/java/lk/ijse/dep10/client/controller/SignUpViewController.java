package lk.ijse.dep10.client.controller;


import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import lk.ijse.dep10.shared.Header;
import lk.ijse.dep10.shared.Message;
import lk.ijse.dep10.shared.singup.Gender;
import lk.ijse.dep10.shared.singup.SignUpUser;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Locale;
import java.util.Objects;

public class SignUpViewController {

    public AnchorPane root;
    public PasswordField txtRePws;
    public PasswordField txtPws;
    public ComboBox cmbCountryList;


    Socket remoteSocket;


    /*output streams & input streams*/
    ObjectOutputStream oos;
    ObjectInputStream ois;
    OutputStream os;
    InputStream is;


    String enteredPsw;

    boolean isNameValid = false;
    boolean isUserNameValid = false;
    boolean isEnteredPswValid = false;
    boolean isReEnteredPwsValid = false;
    Image profilePic;
    @FXML
    private Button btnSignUp;
    @FXML
    private DatePicker dtBirthDay;
    @FXML
    private RadioButton rdoFemale;
    @FXML
    private RadioButton rdoMale;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtUserName;

    public void initialize() throws IOException {
        remoteSocket = new Socket("192.168.8.101", 5050);

        oos = new ObjectOutputStream(remoteSocket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(remoteSocket.getInputStream());

        Platform.runLater(() -> {
            closeSocketOnStageCloseRequest();


        });
        settingComboBox();          //setting combo box with countries
        readServerResponse();


        txtName.textProperty().addListener((value, previous, current) -> {          //name validation
            txtName.getStyleClass().remove("invalid");
            String name = current.strip();

            if (!name.matches("^[A-Za-z]{2,}( ([A-Za-z]{2,}))?$")) {
                txtName.getStyleClass().add("invalid");
                isNameValid = false;
                return;
            }

            isNameValid = true;


        });     //name validation

        txtUserName.textProperty().addListener((value, previous, current) -> {      //user name validation
            txtUserName.getStyleClass().remove("invalid");
            if (current.isBlank() || current.length() < 5) {
                txtUserName.getStyleClass().add("invalid");
                isUserNameValid = false;
                return;
            }
            isUserNameValid = true;

        });     //user name validation


        txtPws.textProperty().addListener((value, previous, current) -> {
            txtPws.getStyleClass().remove("invalid");
            String password = current.strip();
            if (current.isBlank() || password.length() < 8) {
                txtPws.getStyleClass().add("invalid");
                isEnteredPswValid = false;
                return;
            }
            enteredPsw = current.strip();
            isEnteredPswValid = true;
        });

        txtRePws.textProperty().addListener((value, previous, current) -> {
            txtRePws.getStyleClass().remove("invalid");
            if (!Objects.equals(enteredPsw, current)) {
                txtRePws.getStyleClass().add("invalid");
                txtRePws.requestFocus();
                isReEnteredPwsValid = false;
                return;
            }
            isReEnteredPwsValid = true;
        });


    }


    private void readServerResponse() {

        new Thread(() -> {


            while (true) {

                try {

                    Message newMessage = (Message) ois.readObject();

                    if (newMessage.getHeader() == Header.USER_NAME_EXISTS) {

                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.INFORMATION, "User name already exists!").showAndWait();
                            txtUserName.selectAll();
                            txtUserName.requestFocus();
                        });
                    } else if (newMessage.getHeader() == Header.SIGN_UP_SUCCESS) {

                        Stage stage = (Stage) btnSignUp.getScene().getWindow();
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.INFORMATION, "Signed up Successfully").showAndWait();

                            try {

                                stage.setScene(new Scene(new FXMLLoader(this.getClass().getResource("/view/LoginView.fxml")).load()));
                                oos.writeObject(new Message(Header.EXIT, null));
                                oos.flush();
                                if (!remoteSocket.isClosed()) {
                                    remoteSocket.close();

                                }


                            } catch (IOException e) {
                                e.printStackTrace();
                                Platform.runLater(()->{
                                    new Alert(Alert.AlertType.ERROR, "Failed to load login window").show();

                                });
                                System.exit(2);

                            }


                        });


                    }


                } catch (Exception e) {
                    if (e instanceof EOFException) return;
                    if (e instanceof SocketException) return;
                    e.printStackTrace();
                    System.out.println("failed to read server response in signup view controller");
                }
            }


        }).start();
    }

    @FXML
    void btnSignUpOnAction(ActionEvent event) {
        if (!finalValidationCheck()) return;


        SignUpUser newUSer = new SignUpUser(txtName.getText(),
                txtUserName.getText(), (rdoMale.isSelected() ? Gender.Male : Gender.Female),
                cmbCountryList.getValue().toString()
                , dtBirthDay.getValue().toString(), txtPws.getText());


        try {

            Message signUpMessage = new Message(Header.NEW_USER, newUSer);
            oos.writeObject(signUpMessage);
            oos.flush();



        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open a socket from signup window").showAndWait();
            Platform.exit();
        }


    }

    private boolean finalValidationCheck() {
        if (!isNameValid) {
            txtName.selectAll();
            txtName.requestFocus();
            return false;
        } else if (!isUserNameValid) {
            txtUserName.selectAll();
            txtUserName.requestFocus();
            return false;
        } else if ((rdoFemale.isSelected() && rdoMale.isSelected()) || (!(rdoFemale.isSelected() || rdoMale.isSelected()))) {
            rdoMale.requestFocus();

            return false;
        } else if (cmbCountryList.getValue() == null) {
            cmbCountryList.requestFocus();
            return false;
        } else if (dtBirthDay.getValue() == null) {
            dtBirthDay.requestFocus();
            return false;
        } else if (!isEnteredPswValid) {
            txtRePws.selectAll();
            txtRePws.clear();
            txtPws.selectAll();
            txtPws.requestFocus();
            return false;
        } else if (!isReEnteredPwsValid) {
            txtRePws.selectAll();
            txtRePws.requestFocus();
            return false;
        }
        return true;

    }


    public void settingComboBox() {
        String[] countryCodes = java.util.Locale.getISOCountries();
        ObservableList<String> countryList = cmbCountryList.getItems();

        for (String countryCode : countryCodes) {
            Locale locale = new Locale("", countryCode);
            String name = locale.getDisplayCountry();
            countryList.add(name);

        }

    }


    private void closeSocketOnStageCloseRequest() {
        txtName.getScene().getWindow().setOnCloseRequest(event -> {
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

