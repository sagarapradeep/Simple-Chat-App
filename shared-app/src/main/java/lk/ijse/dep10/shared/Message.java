package lk.ijse.dep10.shared;

import java.io.Serializable;

public class Message implements Serializable {
    private Header header;
    private Object body;

    public Message() {
    }

    public Message(Header header, Object body) {
        this.header = header;
        this.body = body;
    }

    /*getter*/
    public Header getHeader() {
        return header;
    }

    public Object getBody() {
        return body;
    }


    /*setter*/
    public void setHeader(Header header) {
        this.header = header;
    }

    public void setBody(Object body) {
        this.body = body;
    }
}
