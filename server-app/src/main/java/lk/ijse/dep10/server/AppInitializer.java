package lk.ijse.dep10.server;

import lk.ijse.dep10.server.db.DBConnection;
import lk.ijse.dep10.server.util.ActiveUser;
import lk.ijse.dep10.shared.Header;
import lk.ijse.dep10.shared.Message;
import lk.ijse.dep10.shared.login.LoginUser;
import lk.ijse.dep10.shared.psw.PasswordEncoder;
import lk.ijse.dep10.shared.singup.Gender;
import lk.ijse.dep10.shared.singup.SignUpUser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.util.ArrayList;


public class AppInitializer {


    private static String chatHistory = "";
    private static ArrayList<ActiveUser> activeUserList = new ArrayList<>();


    public static void main(String[] args) throws IOException {


        generateTables();       //generate database tables if not exits
        ServerSocket serverSocket = new ServerSocket(5050);
        System.out.println("Sever socket opened and waiting for a connection");


        while (true) {

            Socket localSocket = serverSocket.accept();     //accept new connection


            new Thread(() -> {
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(localSocket.getOutputStream());     //streams relative to new local socket
                    oos.flush();
                    ObjectInputStream ois = new ObjectInputStream(localSocket.getInputStream());


                    ActiveUser activeUser = null;
                    while (true) {
                        Message newMsg = (Message) ois.readObject();        //messages

                        if (newMsg.getHeader() == Header.NEW_MESSENGER) {
                            LoginUser loginUser = (LoginUser) newMsg.getBody();
                            String userName = loginUser.getUserName();

                            activeUser = new ActiveUser(userName, localSocket, oos, ois);

                            activeUserList.add(activeUser);
                            broadCastLoggedUsers();
                            sendChatHistoryToNewUser(oos);


                        } else if (newMsg.getHeader() == Header.EXIT) {

                            removeUser(activeUser, localSocket);


                        } else if (newMsg.getHeader() == Header.NEW_USER) {

                            if (isUserNameExists(newMsg.getBody())) {
                                Message message = new Message(Header.USER_NAME_EXISTS, null);
                                notifyUser(oos, message);

                            } else {
                                SignUpUser newUser = (SignUpUser) newMsg.getBody();
                                Connection connection = DBConnection.getInstance().getConnection();
                                PreparedStatement stm = connection.prepareStatement("INSERT INTO SignUpUsers (username, name, gender, country, birthday, password) VALUES (?,?,?,?,?,?)");
                                stm.setString(1, newUser.getUserName());
                                stm.setString(2, newUser.getName());
                                stm.setString(3, String.valueOf(newUser.getGender()));
                                stm.setString(4, newUser.getCountry());
                                stm.setString(5, newUser.getBirthDay());
                                stm.setString(6, PasswordEncoder.encode(newUser.getPassword()));
                                stm.executeUpdate();

                                Message message = new Message(Header.SIGN_UP_SUCCESS, null);
                                notifyUser(oos, message);


                            }


                        } else if (newMsg.getHeader() == Header.NEW_LOGIN) {
                            if (isValidLogin(newMsg.getBody())) {

                                Message message = new Message(Header.VALID_LOGIN, null);
                                notifyUser(oos, message);


                            } else {
                                Message message = new Message(Header.INVALID_LOGIN, null);
                                notifyUser(oos, message);

                            }
                        } else if (newMsg.getHeader() == Header.MESSAGE) {

                            String msg = (String) newMsg.getBody();
                            chatHistory += msg + "\n";
                            broadCastChatHistory();

                        } else if (newMsg.getHeader() == Header.USER_DETAILS) {
                            LoginUser loginUser = (LoginUser) newMsg.getBody();
                            String userName = loginUser.getUserName();

                            Connection connection = DBConnection.getInstance().getConnection();
                            PreparedStatement stm = connection.prepareStatement("SELECT *FROM SignUpUsers WHERE username=?");

                            stm.setString(1, userName);
                            ResultSet resultSet = stm.executeQuery();
                            resultSet.next();
                            String name = resultSet.getString(2);
                            String currentUserName = resultSet.getString(1);
                            String gender = (resultSet.getString(3));
                            String country = resultSet.getString(4);
                            String bd = String.valueOf(resultSet.getDate(5));
                            String password = resultSet.getString(6);


                            SignUpUser signUpUser = new SignUpUser(name, currentUserName, Gender.valueOf(gender), country, bd, password);

                            Message message = new Message(Header.USER_DETAILS, signUpUser);
                            oos.writeObject(message);
                            oos.flush();

                        } else if (newMsg.getHeader() == Header.USER_DETAILS_UPDATE) {
                            SignUpUser signUpUser = (SignUpUser) newMsg.getBody();
                            Connection connection = DBConnection.getInstance().getConnection();
                            PreparedStatement stm = connection.prepareStatement("UPDATE SignUpUsers SET name=?,country=?,password=? WHERE username=?");

                            stm.setString(1, signUpUser.getName());
                            stm.setString(2, signUpUser.getCountry());
                            stm.setString(3, signUpUser.getPassword());
                            stm.setString(4, signUpUser.getUserName());

                            stm.executeUpdate();

                        }


                    }
                } catch (Exception e) {
                    if (e instanceof SocketException) return;
                    if (e instanceof EOFException) return;
                    e.printStackTrace();

                }

            }).start();
        }
    }


    private static void broadCastChatHistory() {

        for (ActiveUser activeUser : activeUserList) {

            new Thread(() -> {
                ObjectOutputStream oos = activeUser.getOos();
                try {
                    oos.writeObject(new Message(Header.MESSAGE, chatHistory));
                    oos.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }).start();

        }

    }


    private static void broadCastLoggedUsers() {

        ArrayList<String> activeUsers = new ArrayList<>();

        for (ActiveUser users : activeUserList) {
            activeUsers.add(users.getUserName());
        }


        for (ActiveUser users : activeUserList) {

            ObjectOutputStream oos = users.getOos();

            Message message = new Message(Header.USERS, activeUsers);
            try {
                oos.writeObject(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


        }


    }

    private static void removeUser(ActiveUser activeUser, Socket localSocket) {
        if (activeUser != null) {
            if (activeUserList.contains(activeUser)) {

                activeUserList.remove(activeUser);
                broadCastLoggedUsers();
            }
        }
        if (!localSocket.isClosed()) {
            try {
                localSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }


    private static boolean isValidLogin(Object body) {
        boolean isValidLogin = false;
        LoginUser loginUser = (LoginUser) body;
        String userName = loginUser.getUserName();
        String enteredPassword = loginUser.getPassword();

        try {
            Connection connection = DBConnection.getInstance().getConnection();
            PreparedStatement stm = connection.prepareStatement("SELECT *FROM SignUpUsers WHERE username=?");
            stm.setString(1, userName);
            ResultSet resultSet = stm.executeQuery();
            if (resultSet.next()) {
                String dbPassword = resultSet.getString(6);
                if (PasswordEncoder.matches(enteredPassword, dbPassword)) {
                    isValidLogin = true;
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isValidLogin;


    }


    private static boolean isUserNameExists(Object user) {
        boolean isUserNameExist = false;

        SignUpUser signUpUser = (SignUpUser) user;
        String userName = signUpUser.getUserName();

        Connection connection = DBConnection.getInstance().getConnection();

        try {
            PreparedStatement stm = connection.prepareStatement("SELECT *FROM SignUpUsers WHERE username=?");
            stm.setString(1, userName);
            ResultSet resultSet = stm.executeQuery();
            if (resultSet.next()) isUserNameExist = true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return isUserNameExist;

    }

    private static void notifyUser(ObjectOutputStream oos, Message message) {

        try {
            oos.writeObject(message);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private static void sendChatHistoryToNewUser(ObjectOutputStream oos) {
        Message message = new Message(Header.MESSAGE, chatHistory);
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void generateTables() {
        try {
            Connection connection = DBConnection.getInstance().getConnection();
            Statement stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SHOW TABLES");
            if (!rst.next()) {
                InputStream is = AppInitializer.class.getResourceAsStream("/schema.sql");
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuilder dbScript = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    dbScript.append(line).append("\n");
                }
                br.close();
                stm.execute(dbScript.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
