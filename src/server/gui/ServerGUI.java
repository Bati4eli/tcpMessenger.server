package server.gui;

import server.tcp.EventsHandler;
import server.tcp.ServerHandler;
import source.SocketThread;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.net.InetAddress.getLocalHost;

public class ServerGUI {
    {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        }catch (Exception e){}
    }
    private Date startTime;

    private int port = 41500;
    private ServerHandler serverHandler;
    private boolean isRun = false;

    private JLabel jlbl;
    private JTextField tfPort;
    private JList listStatus;
    private DefaultListModel listModel;
    private JButton butEnter;
    private JFrame frame;
    private Timer  timer;
    private JPanel pan;

    public ServerGUI(boolean visible) {

        jlbl = new JLabel("Enter listen port:");
        tfPort = new JTextField(Integer.toString(port), 10);
        listModel = new DefaultListModel();
        listStatus = new JList(listModel);
        butEnter = new JButton("Start listen");
        frame = new JFrame("My server");
        pan = new JPanel();
        timer =new Timer(1000,new ActionListener(){
            // Добавляем элемент таймер для обновления статистики
            @Override
            public void actionPerformed(ActionEvent e) {
                updateState();
                //updateStateUpTime();
            }
        }); timer.start();
        stayElements(visible);
    }

    private void stayElements(boolean visible) {
        CreateEvents(); // переопределяем события
        //listStatus.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        frame.setLayout(null);
        pan.setLayout(null);
        pan.add(jlbl);
        pan.add(tfPort);
        pan.add(butEnter);
        frame.add(pan);
        frame.add(listStatus);
        //---- position:
        tfPort.setColumns(4);
        jlbl.setBounds(0, 0, 120, 25);
        tfPort.setBounds(0 + jlbl.getWidth() + jlbl.getX(), 0, 80, 25);
        butEnter.setBounds(10 + tfPort.getWidth() + tfPort.getX(), 0, 80, 25);
        pan.setBounds(5,5,300,30);

        listStatus.setBounds(5,50,800,300);
        listStatus.setFont(new Font("Courier New", Font.BOLD, 13));
        listStatus.setForeground(Color.DARK_GRAY);
        //--
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 420);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        Buton_Click();  // сразу стартуем сервер перед отрисовкой
        frame.setVisible(visible);

        //JOptionPane.showMessageDialog(frame, "This is a Message Box.");
    }
    public String dateF() {
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd MMMyyyy - HH:mm:ss zzz");
        return formatForDateNow.format(dateNow);
    }
    public void setVisible(boolean b) {
        frame.setVisible(b);
    }
    private void CreateEvents() {
        //---------------------
        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                int h = frame.getHeight();
                int w = frame.getWidth();
                listStatus.setBounds(5,40,w-30,h-100);
            }
            @Override
            public void componentMoved(ComponentEvent e) {            }
            @Override
            public void componentShown(ComponentEvent e) {            }
            @Override
            public void componentHidden(ComponentEvent e) {            }
        });
        butEnter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Buton_Click();
            } //actionPerformed
        });
        tfPort.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }//CreateEvents()
    public void Buton_Click(){ // Эвент на нажатие кнопки
        try {
            port = Integer.parseInt(tfPort.getText());
            if (!isRun) {
                serverHandler = new ServerHandler(port,new EventsHandlerForGUI());  // запускаем сервер и передаем обработку событий
                serverHandler.start();
            } else {
                serverHandler.stopServer();
            }
        } catch (Exception ex) {
            listModel.clear();
            listModel.addElement("Error: " + ex);
        }
    }
    public void updateState(){
        if(isRun) {
            long lca = serverHandler.getCounterActive();
            long lcc = serverHandler.getCounterClosed();
            long lcl = serverHandler.getCounterLoggeds();
            long lcr = serverHandler.getCounterRegistered();
            listModel.set(1, "Active/Disconnected: " + lca + "/" + lcc + " ");
            listModel.set(2, "Logged/Registered  : " + lcl + "/" + lcr + " ");
            updateStateUpTime();
        }
    }
    public void updateStateUpTime(){
        if(isRun){
            StringBuilder sb = new StringBuilder();
            Date now=new Date();
            long milliseconds = now.getTime() - startTime.getTime();
            int seconds = (int) (milliseconds / (1000))%60;
            int minutes = (int) (milliseconds / (60 * 1000))%60;
            int hours = (int) (milliseconds / (60 * 60 * 1000))%24;
            int days = (int) (milliseconds / (24 * 60 * 60 * 1000));
            String str = sb.append("Uptime:         ").append(days).append("d ").append(hours).append("h ").append(minutes).append("m ").append(seconds).append("s").toString();
            listModel.set(3,str);
            sb.setLength(0);
        }
    }


    class EventsHandlerForGUI implements EventsHandler {
        @Override
        public void EventHandler_Exceptions(Exception e) {
            listModel.clear();
            listModel.addElement(e.toString() );
        }

        @Override
        public void EventListen_Start(ServerSocket serverSocket) {
            try {
                butEnter.setText("STOP");
                isRun = true;
                tfPort.setEditable(!isRun);
                startTime = new Date();
                listModel.clear();
                listModel.addElement("[Start listen: " + getLocalHost() +": " + serverSocket.getLocalPort() +  " ] ");
                for(int i=0;i<3;i++) listModel.addElement(" ");
                updateState();
            } catch (Exception e) {
            }
        }
        @Override
        public void EventListen_Accept(Socket socket, ServerSocket serverSocket){/*null*/}
        @Override
        public void EventListen_Stop(ServerSocket serverSocket) {
            butEnter.setText("START");
            isRun = false;
            tfPort.setEditable(!isRun);
            listModel.addElement("[Listen stoped]");
        }
        @Override
        public void EventSocket_Connected(Socket sl, SocketThread st) {
            updateState();
        }
        @Override
        public void EventSocket_Closed(Socket sl, SocketThread st) {
            updateState();
        }
        @Override
        public void EventSocket_ReadData(String data, SocketThread st) {
            //super.EventSocket_ReadData(data, st);
            //listModel.addElement("EventSocket_ReadData");
        }
    }
}

