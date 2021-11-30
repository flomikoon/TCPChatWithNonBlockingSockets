import java.io.*;
import java.net.Socket;

public class ClientCoppy {

    private static Socket clientSocket; //сокет для общения
    private static BufferedReader reader; // нам нужен ридер читающий с консоли, иначе как
    // мы узнаем что хочет сказать клиент?
    private static BufferedInputStream in; // поток чтения из сокета
    private static BufferedWriter out; // поток записи в сокет

    public static void main(String[] args) {
        try {
            clientSocket = new Socket("localhost", 8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new ClientThread(clientSocket);
    }
}