import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.TimeZone;

public class ClientThread {
    private final Socket clientSocket;
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // нам нужен ридер читающий с консоли, иначе как
    private static BufferedInputStream in;
    private static BufferedOutputStream out;

    public ClientThread(Socket socket) {
        this.clientSocket = socket;
        new ReadMsg().start(); // нить читающая сообщения из сокета в бесконечном цикле
        new WriteMsg().start(); // поток пишуший в сокет
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    in = new BufferedInputStream(clientSocket.getInputStream()); // читаем из сокета
                    Message message = new Protocol(in).convertPacketToMsg();
                    if(!Objects.equals(message.getNickname(), "") || !Objects.equals(message.getText(), "") ||
                            !Objects.equals(message.getTime(), "") || message.getError() == 2 ){
                        if(message.getError() == 1){
                            System.out.println("Пользователь " + message.getNickname() +  " вошел в чат");
                        } else if(message.getError() == 3){
                            System.out.println("Пользователь " + message.getNickname() +  " покинул чат");
                        } else if(message.getError() == 2){
                            System.out.println("такой пользователь уже существует");
                            downService();
                            break;
                        } else {
                            String msg = "";
                            if (!Objects.equals(message.getTime(), "")) {
                                msg = msg + "<" + message.getTime() + ">";
                            }
                            if (!Objects.equals(message.getNickname(), "")) {
                                msg = msg + " [" + message.getNickname() + "]";
                            }
                            System.out.println((msg + " " + message.getText()).trim());

                            if (message.getFile().length != 0) {
                                FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + "\\getfiles\\" + "i" + message.getText().substring(5));
                                fos.write(message.getFile(), 0, message.getFile().length);
                                fos.close();
                            }
                        }
                    } else {
                        downService();
                        break;
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    public class WriteMsg extends Thread {
        @Override
        public void run() {
            System.out.println("Введите свой никнейм:");
                try {
                    out = new BufferedOutputStream(clientSocket.getOutputStream());

                    Message m = new Message(reader.readLine(), TimeZone.getDefault().getID() , null , null , (byte) 0);
                    out.write(new Protocol(m).getPacket());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                while (true){
                    try {
                        String s = reader.readLine();

                        if(s.startsWith("send ") && s.endsWith(" --file")){

                            String path = System.getProperty("user.dir") + '\\' + "files\\" + s.substring(5 , s.length() - 7);

                            File file = new File(path);
                            byte[] fileInArray = new byte[(int)file.length()];
                            FileInputStream fileStream = new FileInputStream(file);
                            fileStream.read(fileInArray);

                            Message mmm = new Message(null , null,  s.substring(0 , s.length() - 7) , fileInArray , (byte) 0);

                            out.write(new Protocol(mmm).getPacket());
                            out.flush();
                            fileStream.close();
                        } else {
                            if (!s.equals("stop")) {
                                out.write(new Protocol(new Message(null, null, s, null ,  (byte) 0)).getPacket());
                                out.flush();
                                //Message -> bytes; Получаем наш пакет: первые 3 байта: длина ника , длина времени , длина текста , дальше наше сообщение
                                // Отправляем пакет
                            } else {
                                out.write(new Protocol(new Message(null, null, "stop", null , (byte) 3)).getPacket());
                                out.flush();
                                downService();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

        }
    }

    private void downService() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
                in.close();
                out.close();
                System.out.println("Клиент закрыт");
                System.exit(1);
            }
        } catch (IOException ignored) {
        }
    }
}

