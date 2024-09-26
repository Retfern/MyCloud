package common.message;

public class NewChanelForSendFileMessage extends AbstractMessage {
    private String userName;

    public NewChanelForSendFileMessage (String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

}
