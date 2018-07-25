package server.tcp;

import source.Const;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import static source.Const.PrintColor;

public class ServerListener  extends Thread {
    private volatile boolean state=false;
    private ServerSocket serverSocket;
    private EventsListener action;          // интерфейс эвентов данного класса
    private int port=0;
    public ServerListener(int port,EventsListener action) {
        this.port = port;
        this.action = action;
    }
    public void run() {
        try{
            serverSocket = new ServerSocket(port); // Start listen port
            action.EventListen_Start(serverSocket);        // отправляем эвент о старте
            state=true;
            PrintColor(Const.ANSI_COLOR.ANSI_GREEN, "[Listener start]");
        } catch (Exception e) {
            System.out.println("Error Listener:" + e );

        }
        while(!serverSocket.isClosed()){
            try {
                Socket socket = serverSocket.accept();
                action.EventListen_Accept(socket,serverSocket);
            } catch (SocketException e){
                //При закрытии из вне, будет выбрасывать следующий эксепшен:
                //java.net.SocketException: socket closed
            } catch (Exception e) {
                System.out.println("Error Listener:" + e );
            }
        }
        stopServerListener();             // циклично закрываем все соединения
        action.EventListen_Stop(serverSocket);     // отправляем эвент об остановке
        System.out.println("[Listener stop]");
    }//run
    public boolean isRun(){return state;}
    public void stopServerListener(){
        state=false;
        try {
            serverSocket.close();
        } catch (IOException e){}
    }
}//ServerListener
