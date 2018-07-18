import source.EventsSocket;
import source.SocketThread;

import java.net.Socket;

import static source.Const.ANSI_COLOR.ANSI_BLUE;
import static source.Const.ANSI_COLOR.ANSI_RESET;
import static source.Const.*;

public class Main_tests {

    public static void main(String[] args) {
//        String test = "Тестовый Аккаунт лоха";
//        System.out.println( Pattern.compile("^Тестовый").matcher(test).find() );

//        try {
//            SocketThread st = new SocketThread("127.0.0.1", 41500, new Ev() );
//            st.start();
//            st.join(1000);
//            st.Disconnect();
//        } catch (Exception e){
//            System.out.println("Error Main_tests: " + e);
//        }
    }
}

class Ev implements EventsSocket{
    String uuid;
    @Override
    public void EventSocket_Connected(Socket socket, SocketThread st) {
        System.out.println("[EventSocket_Connected]");
        //st.sendComm(BIND,"+79252009715","My Ebanyi Device 1");
        //st.sendComm(SIGN_IN,"+79252009715","2ce92afe-8088-4834-8326-63c5efff3aec","43170");

    }

    @Override
    public void EventSocket_Closed(Socket socket, SocketThread st) {
        System.out.println("[EventSocket_Closed]");
    }

    @Override
    public void EventSocket_ReadData(String data, SocketThread st) {
        PrintColor( ANSI_BLUE, st.getSocket().getInetAddress().toString() + ": "
                + data.replace(END,"<END>").replace(NU,"<NU>")
                + ANSI_RESET
        );

    }
}