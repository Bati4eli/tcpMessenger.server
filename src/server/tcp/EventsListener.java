package server.tcp;

import java.net.ServerSocket;
import java.net.Socket;

public interface EventsListener {
    void EventListen_Start(ServerSocket serverSocket);
    void EventListen_Accept(Socket socket, ServerSocket serverSocket);
    void EventListen_Stop(ServerSocket serverSocket);
}
