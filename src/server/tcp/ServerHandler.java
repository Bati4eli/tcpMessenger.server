package server.tcp;

import org.json.JSONObject;
import server.db.DeviceInfo;
import server.db.SqlServer;
import server.db.UserInfo;
import source.CommHandler;
import source.EventsSocket;
import source.SocketThread;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static server.db.SqlServer.dBook.IS_ONLINE;
import static source.Const.ANSI_COLOR.*;
import static source.Const.*;
import static source.Const.EnumComm.*;
import static source.Const.EnumExceptions.*;

/**
 * Класс агрегирующий в себе все подклассы и их события
 */
public class ServerHandler extends Thread implements CommHandler {

    private int counterActive =0, counterClosed =0,counterLoggeds=0;
    private long counterRegistered=0;

    private ServerListener serverListener;
    private EventsHandler action;

    public final int getCounterActive() {
        return counterActive;
    }
    public final int getCounterClosed() {
        return counterClosed;
    }
    public final long getCounterLoggeds() {
        return counterLoggeds;
    }
    public final long getCounterRegistered() {
        return counterRegistered;
    }
    public final int bindFrequencyMinutes =10;

    private ArrayList<ServerSocketThread> arrayThreads = new ArrayList<>();
    public ServerHandler(int port,EventsHandler action) {
        serverListener= new ServerListener(port,new EventsListenerForHandler());
        this.action =action;
    }

    @Override
    public final void run(){
        try {
            SqlServer.get().connectDB(this);
            counterRegistered=SqlServer.get().countRegistered();
            serverListener.start();
            serverListener.join();
        } catch (Exception e) {
            System.out.println("Error Handler: " + e);
            action.EventHandler_Exceptions(e); // передаем выше исключение
        } finally {
            SqlServer.get().closeDB();
        }
    }
    public void stopServer(){
        serverListener.stopServerListener();
    }
    @Override
    public void Handler(SocketThread st, String data) {
        ServerSocketThread sst=(ServerSocketThread)st;
        String[] args= data.split(NU,-1);
        EnumComm enumComm= getEnumComm(args[0]);

        System.out.println("\tОбрабатываем["+enumComm+"]"+data.replace(NU,"<NU>"));
        try {
            if(!sst.logged && !sst.baned) { // Если клиент не залогинился, то ему доступны только следующие команды
                switch (enumComm) {
                    case SIGN_IN:   sign_in(sst,args);      break;      // Запрос на вход       (userid,device_id, {sms-code} )  -> (true/false)
                    case BIND:      bind(sst,args);         break;      // Запрос на привязку   (userid,device_name)             -> (device_id)
                    case QUIT:      sst.Disconnect();       break;
                    case UNKNOWN:   unknown(sst,args[0]);   break;
                    default: sst.setAttemptError();
                }
            } else if(sst.logged){ // Если клиент залогинился
                switch (enumComm) {
                    case SYNC_BOOK: sync_book(sst,args);break;
                    case SEND_MSG:  send_msg(sst,args); break;
                    case SIGN_OUT:  sign_out(sst);      break;//Обрабатываем выход
                    case QUIT:      sst.Disconnect();   break;
                    case TEST:                          break;//нет
                    case ERROR:                         break;//нет
                    case UNKNOWN:   unknown(sst,args[0]);   break;
                    default: sst.setAttemptError();
                }
            } 
        }catch (Exception e){
            sst.sendException(enumComm,ER_UNKNOWN);
            System.out.println("Error Handler: " + e);
            action.EventHandler_Exceptions(e); // передаем выше исключение
        }

    }
    @Override
    public void sign_in(SocketThread st, String[] args) {
        ServerSocketThread sst=(ServerSocketThread)st;
        sst.setAttemptLogin();
        EnumComm comm= getEnumComm(args[0]);
        if (args.length == 3 ){         //--------- Авторизация
            authorization(sst, args);
        }else if(args.length == 4) {    //--------- Активация
            activation(sst, args);
        } else {
            sst.sendException(comm,ER_ARGS_COUNT);
        }
    }
    public void notifyAboutNewRegistered(Integer userid,ArrayList<Integer> friendsList){

    }
    private void authorization(ServerSocketThread sst, String[] args) {
        EnumComm comm = SIGN_IN;
        DeviceInfo deviceInfo =SqlServer.get().getDeviceInfo(args[1],args[2]);
        // TODO: 18.05.2018 добавить автобан клиентов , которые логинятся с одним UUID
        switch (deviceInfo.deviceState) {
            case DS_ACTIVATED:
                sendSignInOk(sst, args);
                break;
            case DS_NOT_ACTIVATED:
                sst.sendException(comm,ER_SIGNIN_NOT_ACTIVATED);
                break;
            case DS_NON : case DS_DELETED:
                //sst.sendException(comm,ER_SIGNIN_WRONG_PASS);
                sst.sendComm(SIGN_OUT); //посылаем клиенту, что данную связку он уже может смело удалить, ибо она заблокированна
                break;
        }
    }
    private void activation(ServerSocketThread sst, String[] args) {
        String phone = args[1];
        String device = args[2];
        String sms=args[3];
        ///Boolean ans1 =true ;//Pattern.compile("^[0-9]+$").matcher(sms).find() ;
        DeviceInfo deviceInfo =SqlServer.get().activate(phone,device,sms);
        switch (deviceInfo.deviceState){
            case DS_ACTIVATED:
                sendSignInOk(sst, args);
                break;
            case DS_NOT_ACTIVATED:
                if(deviceInfo.attempts>0) { // кол-во оставшихся попыток
                    sst.sendException(SIGN_IN,ER_SIGNIN_WRONG_SMSCODE, deviceInfo.attempts.toString());  //отправляем кол-во оставшихся попыток
                    break;
                } //иначе уведомляем что этот девайс заблокированн
            case DS_NON: case DS_DELETED:
                sst.sendComm(SIGN_OUT); //посылаем клиенту, что данную связку он уже может смело удалить, ибо она заблокированна
                break;
        }
    }
    private void sendSignInOk( ServerSocketThread sst, String[] args) {
        UserInfo ui = SqlServer.get().getUserInfo(args[1]);

        sst.logged=true;
        sst.phone   = args[1];
        sst.userid  = ui.userid ;
        sst.uuid    = args[2];
        sst.sendComm(SIGN_IN , "true",sst.phone, ui.nick, ui.fullname );
        sendCommOnline(sst.userid);                    //уведомляем его список контактов, что в онлайне
        ++counterLoggeds;
    }
    private String getSmsCode(){
        StringBuilder sb =new StringBuilder();
        int size=5;
        sb.append(  Integer.toString( (int)( Math.random()*( Math.pow(10,size) ) ) )    );
        int lng=sb.length();
        for(int i=1;i<=size-lng;i++){
            sb.append("0");
        }
        return sb.toString() ;
    }
    private int amountOnlineDevices(int userid){
        int i=0;
        for (ServerSocketThread el : arrayThreads) {
            if (el.logged && el.userid==userid ) {
                ++i;
            }
        }
        return i;
    }

    /**
     * Команда должна вызываться исключительно после того как ServerSocketThread был помечен онлайн или офлайн.
     * Т.е. на этом этапе везде где стоит userid != null , этот девайс онлайн.
     * @param userid
     */
    private void sendCommOnline(Integer userid ){
        int onlineDevices= amountOnlineDevices(userid);
        boolean state = onlineDevices>0;
        boolean isLeft = onlineDevices==0;
        boolean isEntered = onlineDevices==1;
        if(isLeft||isEntered) {
            ArrayList<Integer> friendList = SqlServer.get().getFriendList(userid);
            for (ServerSocketThread sst : arrayThreads) {
                for (Integer el : friendList) {
                    if (el.equals(sst.userid)) {
                        sst.sendComm(ONLINE, userid.toString(), state ? "true" : "false");
                        break;
                    }
                }
            }
        }
    }
    @Override
    public void bind(SocketThread st, String[] args) {
        ServerSocketThread sst=(ServerSocketThread)st;
        sst.setAttemptError();
        final int cnt=3;
        EnumComm comm= getEnumComm(args[0]);
        if (args.length == cnt) {   // принимаем от клиента его телефон и имя устройства, отправляем ему UUID
            boolean ans1 = Pattern.compile("^\\+7\\d{10}$").matcher(args[1]).find() ;
            UserInfo ui = SqlServer.get().getUserInfo(args[1]);
            boolean ans2 = ui.lastBind >=bindFrequencyMinutes;
            if(ans1 && ans2 ) {
                String uuid = UUID.randomUUID().toString();
                String smsCode = getSmsCode();
                String phone = args[1];
                String device_name = args[2];
                System.out.println("SMS_CODE FOR " + phone + " : " + smsCode);
                if ( SqlServer.get().bind(phone, uuid, device_name, smsCode) ) {
                    sst.sendComm(comm, uuid);
                } else {
                    sst.sendException(comm, ER_BIND_UNKNOWN);
                }
            } else if(!ans2) {
                sst.sendException(comm,ER_BIND_TIMEOUT);
            } else {
                sst.sendException(comm,ER_WRONG_PHONE);
            }
        }else {
            sst.sendException(comm,ER_ARGS_COUNT);
        }
    }
    @Override
    public void sign_out(SocketThread st) {
        ServerSocketThread sst = (ServerSocketThread) st;
        if (sst.logged) {
            --counterLoggeds;
            int userid = sst.userid;
            SqlServer.get().sign_out(sst.userid,sst.uuid);  // помечаем в БД как деактивированную эту связку
            sst.reset();
            sendCommOnline(userid);           //уведомляем его список контактов, что в офлайне
        }
    }
    @Override
    public void send_msg(SocketThread st, String[] args) {
        ServerSocketThread sst=(ServerSocketThread)st;
        final int cnt=3;
        EnumComm comm= getEnumComm(args[0]);
        if (args.length == cnt) {
            int toid= Integer.parseInt(args[1]);
            for (ServerSocketThread el : arrayThreads) {
                if (!el.getSocket().isClosed() && el.logged && toid == el.userid) {   // Только залогинившимся
                    el.sendComm(SEND_MSG, sst.phone , args[2]);    // COMMAND (от кого) MSG
                }
            }
        }else {
            sst.sendException(comm,ER_ARGS_COUNT);
        }
    }
    @Override
    public void sync_book(SocketThread st, String[] args) {
        ServerSocketThread sst=(ServerSocketThread)st; 
        final int cnt=2;
        EnumComm comm= getEnumComm(args[0]);
        if (args.length == cnt) {
            try {
                JSONObject book = new JSONObject(args[1]);
                book = SqlServer.get().sync_book(sst.userid, book); // { "+79999999": {"FULLNAME":"name"} }
                // TODO: 08.06.2018 добавить сюда подгрузку контактов из переписки
                book =addStateOnline(book);
                sst.sendComm(comm, book.toString());
            } catch (Exception e) {
                sst.sendException(SYNC_BOOK, ER_BOOK_SYNC);
            }
        } else {
            sst.sendException(comm,ER_ARGS_COUNT);
        }
    }

    private JSONObject addStateOnline(JSONObject book) {
        Iterator keys = book.keys();
        while (keys.hasNext()){
            Integer userid=Integer.parseInt( (String)keys.next() );
            JSONObject item = book.getJSONObject(userid.toString());
            item.put(IS_ONLINE.name(),amountOnlineDevices(userid)>0);
        }
        return book;
    }

    @Override
    public void find_user(SocketThread socketThread, String[] strings) {}
    @Override
    public void online(SocketThread socketThread, String[] strings) {}
    @Override
    public void unknown(SocketThread st, String args) {
        ServerSocketThread sst=(ServerSocketThread)st;
        sst.sendComm(UNKNOWN,args);         //Отвечаем что не знаем такую команду
        sst.setAttemptError();
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException ignored) {}
    }
    @Override
    public void error(SocketThread st, String[] args) {
        ServerSocketThread sst=(ServerSocketThread)st;
    }
    public class EventsSocketsForHandler implements EventsSocket{
        @Override
        public void EventSocket_Connected(Socket sl, SocketThread st) {
            ++counterActive;
            action.EventSocket_Connected(sl,st);
            PrintColor(ANSI_GREEN,"[Accept client IP: " + sl.getInetAddress() + " ]");
        }
        @Override
        public synchronized void EventSocket_Closed(Socket sl, SocketThread st) {
            ServerSocketThread sst=(ServerSocketThread)st;
            --counterActive; ++counterClosed;
            if(sst.logged) {
                --counterLoggeds;
                int userid = sst.userid;
                sst.reset();
                sendCommOnline(userid);           //уведомляем его список контактов, что в офлайне
            }
            action.EventSocket_Closed(sl,st);   //передаем эвент выше
            arrayThreads.remove(st);            //высвобождаем из листа
            PrintColor(ANSI_RED, "[Disconnect client IP: " + sl.getInetAddress() + " ]");
        }
        @Override
        public void EventSocket_ReadData(String data, SocketThread st) {
            PrintColor( ANSI_BLUE, st.getSocket().getInetAddress().toString() + ": "
                + data.replace(END,"<END>").replace(NU,"<NU>")
                + ANSI_RESET
            );
            action.EventSocket_ReadData(data,st);
            for (String pack:data.split(END,0)) {
                Handler(st, pack);
            }
        }
    }
    public class EventsListenerForHandler implements EventsListener{
        @Override
        public void EventListen_Start(ServerSocket serverSocket) {
            action.EventListen_Start(serverSocket);
        }
        @Override
        public void EventListen_Accept(Socket socket, ServerSocket serverSocket) {
            /// --- action.EventListen_Accept(serverSocket); // в gui нет смысла передавать этот эвент он есть уже там как EventSocket_Connected
            ServerSocketThread st=new ServerSocketThread(socket,new EventsSocketsForHandler());
            arrayThreads.add(st);
            st.start();
        }
        @Override
        public void EventListen_Stop(ServerSocket serverSocket) {

            for(ServerSocketThread sst: arrayThreads){
                sst.Disconnect();
            }
            arrayThreads.clear();

            action.EventListen_Stop(serverSocket);
        }
    }
}
