import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class ServerThread extends Thread{
    private Map<SocketChannel, String> dataTracking = new HashMap<>();
    private Selector selector;
    public static LinkedList<User> clientList = new LinkedList<>();
    private boolean flag = false;
    public ServerThread() throws IOException {
        start();
    }

    @Override
    public void run() {
        try {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();

            serverChannel.configureBlocking(false);

            serverChannel.socket().bind(new InetSocketAddress(8080));
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (selector.select() > -1) {

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(SelectionKey key) throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept(); // Приняли
        socketChannel.configureBlocking(false); // Неблокирующий
        socketChannel.register(key.selector() , SelectionKey.OP_READ); // Регистрируем в селекторе
        clientList.add(new User(socketChannel , null , null));
    }

    private void read(SelectionKey key) throws IOException
    {
        SocketChannel channel = (SocketChannel) key.channel();

        Message message = new Protocol(channel).convertPacketToMsgServer();

        String clientName = null;

        for (User vr : clientList) {
            SocketChannel v = vr.getSocketChannel();
            if (v == channel) {
                clientName = vr.getNickname();
            }
        }

        if(clientName == null){
            register(message , channel);
        } else {
            String aa = clientName;
            if (message.getError() == 3) { //если пользователь хочет отключится
                for (User vr : clientList) {
                    SocketChannel v = vr.getSocketChannel();
                    if (v != channel) {
                        v.write(ByteBuffer.wrap(new Protocol(new Message(aa, null, null, null , (byte) 3)).getPacket()));
                    }
                }
                aa = "Пользователь " + aa + " отключился от чата";
                serverMessage(null , aa);
                for (User vr : clientList) {
                    SocketChannel v = vr.getSocketChannel();
                    if (v == channel) {
                        clientList.remove(vr);
                    }
                }
                channel.close();
            } else {
                // пишем всем
                for (User vr : clientList) {
                    if (vr.getSocketChannel() != channel) {
                    String time_zone = vr.getTimezone();
                    if (time_zone != null) {
                        Date date = new Date();
                        DateFormat df = new SimpleDateFormat("HH:mm");
                        df.setTimeZone(TimeZone.getTimeZone(time_zone));
                        time_zone = df.format(date);
                    }
                    Message msg = new Message(aa, time_zone, message.getText(), message.getFile() , (byte) 0);
                    byte[] prot = new Protocol(msg).getPacket();
                    ByteBuffer by = ByteBuffer.allocate(16777216);
                    by.put(prot);
                    by.flip();

                    int sum = 0;
                    while (sum < prot.length) {
                        sum = sum + vr.getSocketChannel().write(by);
                    }
                    }
                }
                serverMessage(aa , message.getText());
            }
        }
    }

    private void serverMessage(String aa , String text){
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("HH:mm");
        df.setTimeZone(TimeZone.getDefault());

        String mes = "";
        if (!Objects.equals(df.format(date), "")) {
            mes = mes + "<" + df.format(date) + ">";
        }
        if (!Objects.equals(aa , "") && !Objects.equals(aa ,null)) {
            mes = mes + " [" + aa + "]";
        }
        System.out.println((mes + " " + text).trim());
    }

    private void register(Message message , SocketChannel socketChannel) {
        try {
            String nickname = message.getNickname();
            String timezone = message.getTime();

            for (User user : clientList) {
                if (Objects.equals(user.getNickname(), nickname)) {
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                for (User user : clientList) {
                    if (user.getSocketChannel() == socketChannel) {
                        user.setNickname(nickname);
                        user.setTimezone(timezone);
                    }
                }

                for (User vr : clientList) {
                    SocketChannel v = vr.getSocketChannel();
                    v.write(ByteBuffer.wrap(new Protocol(new Message(nickname, null, null, null , (byte) 1)).getPacket()));
                }
                serverMessage(null , "Пользователь " + nickname + " вошел в чат");
            } else {
                socketChannel.write(ByteBuffer.wrap(new Protocol(new Message(null, null,
                        null, null , (byte) 2)).getPacket()));
                flag = false;
                //Отключаем клиента
                for (User vr : clientList) {
                    SocketChannel v = vr.getSocketChannel();
                    if (v == socketChannel) {
                        clientList.remove(vr);
                    }
                }
                socketChannel.close();
            }
        } catch (IOException ignored) {
        }
    }
}
