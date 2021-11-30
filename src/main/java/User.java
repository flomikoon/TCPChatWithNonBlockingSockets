import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class User {

    private String nickname;
    private final SocketChannel socketChannel;
    private String timezone;
    public User(SocketChannel socketChannel, String nickname , String timezone){
        this.socketChannel = socketChannel;
        this.nickname = nickname;
        this.timezone = timezone;
    }

    public String getNickname() {
        return nickname;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public void setNickname(String nickname){this.nickname = nickname;}

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    @Override
    public String toString() {
        return nickname + " , " + socketChannel + " , " + timezone;
    }
}
