import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionService {
    private String address;
    private int port;
    private String nick;
    private String login;
    private String password;
    private DataInputStream input;
    private DataOutputStream output;
    private String inMSG;
    private Socket socket;
    private DefaultListModel<String> usersOnline = new DefaultListModel<>();


    public ConnectionService(String address, int port, String login, String password) {
        this.address = address;
        this.port = port;
        this.login = login;
        this.password = password;
    }

    //Подключение потока передачи данных

    public void connect () throws IOException {
            socket = new Socket(address, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
    }


    //Метода приема информации с сервера

    public synchronized String getInMSG () {
        try {
            MessageDTO dto = MessageDTO.convertFromJson(input.readUTF());
            switch (dto.getMessageType()) {
                case PUBLIC_MESSAGE, PRIVATE_MESSAGE -> inMSG = showMessage(dto);
                case CLIENTS_LIST_MESSAGE -> {
                    List<String> inputUserList = new ArrayList<>(dto.getUsersOnline());
                    usersOnline.removeAllElements();
                    inputUserList.add(0, "Send all users");
                    for (String u : inputUserList){
                        usersOnline.addElement(u);
                    }
                    inMSG = "*" + dto.getMessageType();
                }
                case AUTH_CONFIRM -> {
                    inMSG = "New user added\n";
                    nick = dto.getFrom();
                    //Main.getWindow().insertMSG();
                    Main.getWindow().setTitle(nick);
                    Main.getWindow().getMenuChangeNick().setEnabled(true);
                    Main.getWindow().fillHistoryText();
                    Main.getWindow().getMenuRegistration().setEnabled(false);
                }
                case CHANGE_NICK -> {
                    Main.getWindow().setTitle(dto.getFrom());
                    nick = dto.getFrom();
                }
                case ERROR_MESSAGE -> inMSG = errMessage(dto);
            }
            return inMSG;

        }catch (IOException e){
            inMSG = "Error. Connection is failed.";
        }
        return null;
    }
    private String showMessage(MessageDTO dto) {
        String msg = String.format("[%s] [%s] -> %s\n", dto.getMessageType(), dto.getFrom(), dto.getBody());
        Main.getWindow().saveHistory(msg);
        return msg;
    }

    private String errMessage(MessageDTO dto) {
        String msg = String.format("[%s] -> %s\n", dto.getMessageType(), dto.getBody());
        return msg;
    }

    //Передача текста на сервер

    public void setOutMSG(String msg) throws IOException {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.PUBLIC_MESSAGE);
        dto.setBody(msg);
        output.writeUTF(dto.convertToJson());
    }
    public void disconnection() throws IOException {
        socket.close();
    }

    public DefaultListModel<String> getUsersOnline() {
        return usersOnline;
    }

    public void authentication() throws IOException {
        MessageDTO dto = new MessageDTO();
        dto.setLogin(login);
        dto.setPassword(password);
        dto.setBody(nick);
        dto.setMessageType(MessageType.SEND_AUTH_MESSAGE);
        output.writeUTF(dto.convertToJson());
    }

    public void setOutPrivetMSG(String nick, String msg) throws IOException {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.PRIVATE_MESSAGE);
        dto.setTo(nick);
        dto.setBody(msg);
        output.writeUTF(dto.convertToJson());
    }
    public void setChangeNick(String oldNick, String newNick) throws IOException {
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.CHANGE_NICK);
        dto.setTo(oldNick);
        dto.setBody(newNick);
        output.writeUTF(dto.convertToJson());
    }

    public void sendRegistration(String login, String password, String nick) throws IOException {
        //Main.getWindow().connection();
        MessageDTO dto = new MessageDTO();
        dto.setMessageType(MessageType.REGISTRATION);
        dto.setLogin(login);
        dto.setPassword(password);
        dto.setBody(nick);
        output.writeUTF(dto.convertToJson());
        Main.getWindow().insertMSG();
    }


    public String getNick() {
        return nick;
    }
}
