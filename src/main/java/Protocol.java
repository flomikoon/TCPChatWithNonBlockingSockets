import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Protocol {
    private Message msg;
    private BufferedInputStream in;
    private SocketChannel channel;

    public Protocol(Message msg) {
        this.msg = msg;
    }

    public Protocol(BufferedInputStream in){
        this.in = in;
    }

    public Protocol(SocketChannel channel){this.channel = channel;}

    public byte[] getPacket(){
        byte[] nick = new byte[0];

        if(msg.getNickname() != null){
            nick = msg.getNickname().getBytes(StandardCharsets.UTF_8);
        }

        byte[] tex = new byte[0];
        if(msg.getText() != null) {
            tex = msg.getText() .getBytes(StandardCharsets.UTF_8);
        }

        byte[] tim = new byte[0];
        if(msg.getTime() != null) {
            tim = msg.getTime().getBytes(StandardCharsets.UTF_8);
        }

        byte[] file = new byte[0];
        if(msg.getFile() != null) {
            file = msg.getFile();
        }

        int nick_size = nick.length;
        int tex_size = tex.length;
        int tim_size = tim.length;
        int file_size = file.length;

        byte[] msg_size = new byte[4];

        if(msg.getError() == 0 ) {
            msg_size[0] = 0;
            msg_size[1] = 0;
        } else {
            msg_size[0] = 1;
            msg_size[1] = msg.getError();
        }

        msg_size[2] = (byte) nick_size;
        msg_size[3] = (byte) tim_size;

        byte[] byText = new byte[]{
                (byte) (tex_size >> 16),
                (byte) (tex_size  >> 8),
                (byte) tex_size
        };

        byte[] byFile = new byte[]{
                (byte) (file_size >> 16),
                (byte) (file_size  >> 8),
                (byte) file_size
        };

        byte[] result = Arrays.copyOf(msg_size , msg_size.length + byText.length + byText.length + nick_size + tex_size + tim_size + file_size);
        System.arraycopy(byText, 0, result, msg_size.length, byText.length);
        System.arraycopy(byFile, 0, result, msg_size.length + byText.length, byFile.length);
        System.arraycopy(nick, 0, result, msg_size.length + byText.length + byFile.length, nick.length);
        System.arraycopy(tim, 0, result, msg_size.length + byText.length + nick.length + byFile.length, tim.length);
        System.arraycopy(tex, 0, result, msg_size.length + byText.length + nick.length + tim.length + byFile.length, tex.length);
        System.arraycopy(file, 0, result, msg_size.length + byText.length + nick.length + tim.length + byFile.length + tex.length, file_size);
       // System.out.println(Arrays.toString(result));
        return  result;
    }

    public Message convertPacketToMsg() throws IOException {
        byte[] type = new byte[2];
        in.read(type);

        Message returnMessage = null;
         if (type[0] == 1) {
            byte[] bytes_file = new byte[0];
            if(type[1] == 1) {
                byte[] bytes = new byte[8];
                in.read(bytes);
                int nick_name_size = bytes[0] & 0xff;
                byte[] bytes_nick = new byte[nick_name_size];
                in.read(bytes_nick, 0 , nick_name_size);
                returnMessage =   new Message(new String(bytes_nick, 0, nick_name_size, StandardCharsets.UTF_8),
                        "", "", bytes_file, type[1]);
            }
            if(type[1] == 2){
                returnMessage =   new Message("", "", "", bytes_file, type[1]);
            }
            if(type[1] == 3){
                byte[] bytes = new byte[8];
                in.read(bytes);
                int nick_name_size = bytes[0] & 0xff;
                byte[] bytes_nick = new byte[nick_name_size];
                in.read(bytes_nick, 0 , nick_name_size);
                returnMessage =  new Message(new String(bytes_nick, 0, nick_name_size, StandardCharsets.UTF_8)
                        , "", "", bytes_file, type[1]);
            }
        } else  {
            byte[] bytes = new byte[8];
            in.read(bytes); //читаем длину сообщения

            int nick_name_size = bytes[0] & 0xff;
            int time_size = bytes[1] & 0xff;

            int tex_size = 0;

            for (int i = 2; i < 5; i++) { // b => 2
                tex_size = (tex_size << 8) + (bytes[i] & 0xff);
            }

            int file_size = 0;

            for (int i = 5; i < 8; i++) { // b => 5
                file_size = (file_size << 8) + (bytes[i] & 0xff);
            }

            byte[] bytes_nick = new byte[nick_name_size];
            in.read(bytes_nick, 0, nick_name_size);


            byte[] bytes_time = new byte[time_size];
            in.read(bytes_time, 0, time_size);

            byte[] bytes_tex = new byte[tex_size];
            in.read(bytes_tex, 0, tex_size);


            byte[] bytes_file = new byte[file_size];

            if (file_size != 0) {
                for (int i = 0; i < file_size; i++) {
                    bytes_file[i] = (byte) in.read();
                }
            }

            String time_string = new String(bytes_time, 0, time_size, StandardCharsets.UTF_8);

            returnMessage =  new Message(new String(bytes_nick, 0, nick_name_size, StandardCharsets.UTF_8),
                    time_string, new String(bytes_tex, 0, tex_size, StandardCharsets.UTF_8), bytes_file , type[1]);
        }
         return returnMessage;
    }

    public Message convertPacketToMsgServer() throws IOException {
        byte[] type = new byte[2];

        ByteBuffer readBuffer = ByteBuffer.allocate(2);
        readBuffer.clear();
        channel.read(readBuffer);
        readBuffer.flip();

        readBuffer.get(type, 0, 2);

        if (type[0] == 0) {
            readBuffer = ByteBuffer.allocate(8);
            readBuffer.clear();
            channel.read(readBuffer);
            readBuffer.flip();

            byte[] bytes = new byte[8];
            readBuffer.get(bytes, 0, bytes.length);

            int nick_name_size = bytes[0] & 0xff;
            int time_size = bytes[1] & 0xff;

            int tex_size = 0;

            for (int i = 2; i < 5; i++) { // b => 2
                tex_size = (tex_size << 8) + (bytes[i] & 0xff);
            }

            int file_size = 0;

            for (int i = 5; i < 8; i++) { // b => 5
                file_size = (file_size << 8) + (bytes[i] & 0xff);
            }

            byte[] bytes_nick = new byte[nick_name_size];
            readBuffer = ByteBuffer.allocate(nick_name_size);
            readBuffer.clear();
            channel.read(readBuffer);
            readBuffer.flip();
            readBuffer.get(bytes_nick, 0, nick_name_size);


            byte[] bytes_time = new byte[time_size];
            readBuffer = ByteBuffer.allocate(time_size);
            readBuffer.clear();
            channel.read(readBuffer);
            readBuffer.flip();
            readBuffer.get(bytes_time, 0, time_size);

            byte[] bytes_tex = new byte[tex_size];
            readBuffer = ByteBuffer.allocate(tex_size);
            readBuffer.clear();
            channel.read(readBuffer);
            readBuffer.flip();
            readBuffer.get(bytes_tex, 0, tex_size);

            byte[] bytes_file = new byte[file_size];

            int count = 0;

            if (file_size != 0) {
                for (int i = 0; i < file_size; i = i + 100) {
                    int rsize = 0;
                    if (count + 100 < file_size) {
                        rsize = 100;
                        count = count + 100;
                    } else {
                        rsize = file_size - count;
                        count = count + rsize;
                    }
                    //System.out.println("i= " + i + "count= " + count + "rsize= " + rsize + "filesize= " + file_size);
                    readBuffer = ByteBuffer.allocate(rsize);
                    readBuffer.clear();
                    channel.read(readBuffer);
                    readBuffer.flip();
                    byte[] read = new byte[rsize];
                    readBuffer.get(read, 0, rsize);
                    bytes_file[i] = read[0];
                    System.arraycopy(read, 0, bytes_file, count - rsize, rsize);
                }
            }

            //send tesi.jpg --file
            //send test --file

            String time_string = new String(bytes_time, 0, time_size, StandardCharsets.UTF_8);

            return new Message(new String(bytes_nick, 0, nick_name_size, StandardCharsets.UTF_8),
                    time_string, new String(bytes_tex, 0, tex_size, StandardCharsets.UTF_8), bytes_file , type[1] );
        }else{
            return new Message( null , null , null ,null , type[1]);
        }
    }
}
