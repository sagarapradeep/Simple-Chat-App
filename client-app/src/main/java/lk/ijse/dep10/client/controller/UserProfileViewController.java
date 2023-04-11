package lk.ijse.dep10.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import lk.ijse.dep10.shared.Header;
import lk.ijse.dep10.shared.Message;
import lk.ijse.dep10.shared.login.LoginUser;
import lk.ijse.dep10.shared.psw.PasswordEncoder;
import lk.ijse.dep10.shared.singup.Gender;
import lk.ijse.dep10.shared.singup.SignUpUser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;

public class UserProfileViewController {

    public Button btnEditProfile;
    public Label lblOldPassword;
    public PasswordField txtNewPsw;
    public PasswordField txtNewPswRe;
    public HBox hPsw1;
    public HBox hPsw2;
    public TextField txtPsw;
    public Button btnEditPsw;
    public HBox hOldPassword;
    public Button btnCansel;
    public Button btnSave;


    @FXML
    private ComboBox cmbCountryList;

    @FXML
    private DatePicker dtBirthDay;

    @FXML
    private RadioButton rdoFemale;

    @FXML
    private RadioButton rdoMale;

    @FXML
    private AnchorPane root;

    @FXML
    private TextField txtName;

    private SignUpUser currentSignUpUser;
    private SignUpUser newSignUpUser;


    @FXML
    private TextField txtUserName;
    private LoginUser currentUser;
    private Socket remoteSocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;


    public void initialize() {
        connect();
        readServerResponse();

        Platform.runLater(() -> {
            loadAllData();
            disableProperties();
            closeSocketOnStageCloseRequest();


        });


    }

    private void disableProperties() {
        txtName.setDisable(true);
        txtUserName.setDisable(true);
        rdoMale.setDisable(true);
        rdoFemale.setDisable(true);
        hOldPassword.visibleProperty().setValue(false);
        dtBirthDay.setDisable(true);
        cmbCountryList.setDisable(true);
        txtPsw.setDisable(true);


        hPsw1.visibleProperty().setValue(false);
        hPsw2.visibleProperty().setValue(false);
        btnEditPsw.visibleProperty().setValue(false);
        btnCansel.visibleProperty().setValue(false);
        btnSave.visibleProperty().setValue(false);

    }

    private void connect() {
        try {
            remoteSocket = new Socket("192.168.8.101", 5050);
            oos = new ObjectOutputStream(remoteSocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(remoteSocket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();

            System.exit(2);

        }
    }

    private void loadAllData() {
        Message message = new Message(Header.USER_DETAILS, currentUser);
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

                    if (newMsg.getHeader() == Header.USER_DETAILS) {

                        currentSignUpUser = (SignUpUser) newMsg.getBody();

                        Platform.runLater(() -> {
                            txtName.setText(currentSignUpUser.getName());
                            txtUserName.setText(currentSignUpUser.getUserName());
                            if ((currentSignUpUser.getGender() == Gender.Male)) {
                                rdoMale.setSelected(true);
                            } else {
                                rdoFemale.setSelected(true);
                            }
                            dtBirthDay.setValue(LocalDate.parse(currentSignUpUser.getBirthDay()));
                            cmbCountryList.setValue(currentSignUpUser.getCountry());
                            txtPsw.setText(currentSignUpUser.getPassword());


                        });

                    }


                } catch (IOException | ClassNotFoundException e) {
                    if (!remoteSocket.isClosed()) {
                        try {
                            remoteSocket.close();
                            System.exit(0);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                }
            }


        }).start();

    }


    public void initDataUserProfile(LoginUser currentUser) {

        this.currentUser = currentUser;

    }

    public void btnEditProfileOnAction(ActionEvent actionEvent) {
        txtName.setDisable(false);
        cmbCountryList.setDisable(false);
        txtPsw.setDisable(false);


        btnEditPsw.visibleProperty().setValue(true);

        btnSave.visibleProperty().setValue(true);


    }

    public void txtPswOnAction(ActionEvent actionEvent) {
    }

    public void btnEditPswOnAction(ActionEvent actionEvent) {
        hPsw1.visibleProperty().setValue(true);
        hPsw2.visibleProperty().setValue(true);
        hOldPassword.visibleProperty().setValue(true);
        btnCansel.visibleProperty().setValue(true);

        txtPsw.clear();
        txtNewPsw.setDisable(false);
        txtNewPswRe.setDisable(false);
        btnSave.setVisible(true);
    }

    public void btnCanselOnAction(ActionEvent actionEvent) {
        hPsw1.visibleProperty().setValue(false);
        hPsw2.visibleProperty().setValue(false);
        hOldPassword.visibleProperty().setValue(false);
        btnCansel.visibleProperty().setValue(false);


    }

    public void btnSaveOnAction(ActionEvent actionEvent) {
        if (!dataValidate()) return;


        if (!btnCansel.isVisible()) {
            newSignUpUser = new SignUpUser(txtName.getText(), currentSignUpUser.getUserName(),
                    currentSignUpUser.getGender(), (String) cmbCountryList.getValue(), currentSignUpUser.getBirthDay(), currentSignUpUser.getPassword());

        } else {
            newSignUpUser = new SignUpUser(txtName.getText(), currentSignUpUser.getUserName(),
                    currentSignUpUser.getGender(), (String) cmbCountryList.getValue(), currentSignUpUser.getBirthDay(), PasswordEncoder.encode(txtNewPsw.getText()));

        }
        Message message = new Message(Header.USER_DETAILS_UPDATE, newSignUpUser);
        try {
            oos.writeObject(message);
            oos.flush();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        disableProperties();

    }

    private boolean dataValidate() {
        boolean isDataValid = true;
        if (btnCansel.isVisible()) {
            if (!PasswordEncoder.matches(txtPsw.getText(), currentSignUpUser.getPassword())) {
                txtNewPsw.clear();
                txtNewPswRe.clear();
                txtPsw.requestFocus();
                isDataValid = false;
            }
            if (!txtNewPsw.getText().equals(txtNewPswRe.getText())) {
                txtNewPswRe.clear();
                txtNewPsw.requestFocus();
                txtNewPsw.selectAll();
                isDataValid = false;
            }


        }
        if (!txtName.getText().matches("[A-Za-z]{5,}( )*")) {
            txtName.requestFocus();
            txtName.selectAll();
            isDataValid = false;

        }
        if (cmbCountryList.getSelectionModel().getSelectedItem() == null) {
            isDataValid = false;
            cmbCountryList.requestFocus();

        }


        return isDataValid;
    }


    private void closeSocketOnStageCloseRequest() {
        txtName.getScene().getWindow().setOnCloseRequest(event -> {
            try {
                oos.writeObject(new Message(Header.EXIT, null));
                oos.flush();
                if (!remoteSocket.isClosed()) remoteSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
