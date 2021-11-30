public class Message {
    private final String nickname;
    private final String time;
    private final String text;
    private final byte[] file;
    private final byte error;
    public Message(String nickname , String time , String text , byte[] file , byte error){
        this.nickname = nickname;
        this.time = time;
        this.text = text;
        this.file = file;
        this.error = error;
    }

    public String getNickname() {
        return nickname;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public byte[] getFile() {
        return file;
    }

    public byte getError() {
        return error;
    }

    @Override
    public String toString() {
        return "Message{" +
                "time='" + time + '\'' +
                ", name='" + nickname + '\'' +
                ", text='" + text + '\'' +
                ", file='" + file + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}