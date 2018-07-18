package server.tcp;

import source.Const;
import source.EventsSocket;
import source.SocketThread;

import java.net.Socket;

class ServerSocketThread extends SocketThread {  // Расширяет функциональность базового класса
    ServerSocketThread(Socket socket,EventsSocket action) {
        super(socket,action);
    }

    private int attemptError=2;
    private int attemptSign=10;      //Кол-во попыток на вход/регистрацию

    Integer userid =-1;
    String uuid="";
    String phone="";

    boolean baned=false;
    boolean logged =false;

    void reset(){
        int userid =-1;
        uuid="";
        phone="";
    }
    void setAttemptError() {
        --this.attemptError;
        ban_check();
    }
    void setAttemptLogin() {
        --this.attemptSign;
        ban_check();
    }
    void ban_check(){
        if( attemptSign<1 ||  attemptError <1 ) {
            /*
            baned = true;
            try {
                TimeUnit.MILLISECONDS.sleep(10000);
                sendComm(QUIT);
                Disconnect();
            } catch (Exception ignored) {
            }*/
        }
    }
    @Override
    public final void sendException(Const.EnumComm comm, Const.EnumExceptions exc) {
        switch (exc) {
            case ER_UNKNOWN:             break;
            case ER_WRONG_PHONE:                break;
            case ER_SIGNIN_WRONG_SMSCODE:       break;
            case ER_SIGNIN_WRONG_PASS:  setAttemptError();        break;
            case ER_SIGNIN_NOT_ACTIVATED:       break;
            case ER_BIND_TIMEOUT:                break;
            case ER_BIND_UNKNOWN:                break;
            case ER_ARGS_COUNT:  setAttemptError();            break;
        }
        this.sendComm(Const.EnumComm.ERROR, comm.toString(), exc.toString());
    }
    public final void sendException(Const.EnumComm comm, Const.EnumExceptions exc, String description) {
        switch (exc) {
            case ER_UNKNOWN:             break;
            case ER_WRONG_PHONE:                break;
            case ER_SIGNIN_WRONG_SMSCODE:       break;
            case ER_SIGNIN_WRONG_PASS:  setAttemptError();        break;
            case ER_SIGNIN_NOT_ACTIVATED:       break;
            case ER_BIND_TIMEOUT:                break;
            case ER_BIND_UNKNOWN:                break;
            case ER_ARGS_COUNT:  setAttemptError();            break;
        }
        this.sendComm(Const.EnumComm.ERROR, comm.toString(), exc.toString(), description);
    }
}
