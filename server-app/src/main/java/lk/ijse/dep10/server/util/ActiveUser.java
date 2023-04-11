package lk.ijse.dep10.server.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ActiveUser {
    private String userName;
    private Socket localSocket;

    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public ActiveUser(String userName, Socket localSocket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.userName = userName;
        this.localSocket = localSocket;
        this.oos = oos;
        this.ois = ois;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Socket getLocalSocket() {
        return localSocket;
    }

    public void setLocalSocket(Socket localSocket) {
        this.localSocket = localSocket;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    public void setOos(ObjectOutputStream oos) {
        this.oos = oos;
    }

    public ObjectInputStream getOis() {
        return ois;
    }

    public void setOis(ObjectInputStream ois) {
        this.ois = ois;
    }
}
