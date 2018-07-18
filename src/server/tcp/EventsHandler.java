package server.tcp;

import source.EventsSocket;
import source.SocketThread;

import java.net.ServerSocket;
import java.net.Socket;

public interface EventsHandler extends EventsSocket,EventsListener {
    void EventHandler_Exceptions(Exception e);
    @Override
    void EventListen_Start(ServerSocket serverSocket);
    @Override
    void EventListen_Accept(Socket socket, ServerSocket serverSocket);
    @Override
    void EventListen_Stop(ServerSocket serverSocket);

    @Override
    void EventSocket_Connected(Socket sl, SocketThread st);
    @Override
    void EventSocket_Closed(Socket sl, SocketThread st);
    @Override
    void EventSocket_ReadData(String data, SocketThread st);
}
