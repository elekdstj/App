/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientChat;

import com.sun.corba.se.spi.activation.RepositoryPackage.ServerDef;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author marc
 */
public class myFrame extends JFrame {

    private JTextArea ChatBox = new JTextArea(10, 26);
    private JScrollPane myChatHistory = new JScrollPane(ChatBox, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JTextArea UserBox = new JTextArea(10, 10);
    private JScrollPane myUserPane = new JScrollPane(UserBox, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JTextArea UserText = new JTextArea(5, 40);
    private JScrollPane myUserHistory = new JScrollPane(UserText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private JButton Send = new JButton("Send");
    private JButton Start = new JButton("Connect");
    private Client ChatClient;
    private ReadThread myRead = new ReadThread();
    private JTextField Server = new JTextField(20);
    private JLabel myLabel = new JLabel("Server Name :");
    private JTextField User = new JTextField(20);
    private String ServerName;
    private String UserName;

    public myFrame() {
        setResizable(false);
        setTitle("Client");
        setSize(600, 500);
        Container cp = getContentPane();
        cp.setLayout(new FlowLayout());
        cp.add(new JLabel("Chat History"));
        cp.add(myChatHistory);
        cp.add(myUserPane);
        cp.add(new JLabel("Chat Box : "));
        cp.add(myUserHistory);
        cp.add(Send);
        cp.add(Start);
        cp.add(myLabel);
        cp.add(Server);
        cp.add(User);
        Send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ChatClient != null) {
                    System.out.println(UserText.getText());
                    ChatClient.SendMassage(UserText.getText());
                }
            }
        });
        Start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ChatClient = new Client();
                ChatClient.start();

            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        new myFrame();
    }

    public class Client extends Thread {

        private static final int PORT = 9999;
        private LinkedList Clients;
        private ByteBuffer ReadBuffer;
        private ByteBuffer writeBuffer;
        private SocketChannel SChan;
        private Selector ReadSelector;
        private CharsetDecoder asciiDecoder;

        public Client() {
            Clients = new LinkedList();
            ReadBuffer = ByteBuffer.allocateDirect(300);
            writeBuffer = ByteBuffer.allocateDirect(300);
            asciiDecoder = Charset.forName("US-ASCII").newDecoder();
            setVisible(true);
        }

        public void run() {

            ServerName = Server.getText();
            System.out.println(ServerName);
            UserName = User.getText();

            UserBox.append(UserName + "\n");

            Connect(ServerName);
            myRead.start();
            while (true) {

                ReadMassage();

                try {
                    Thread.sleep(30);
                } catch (InterruptedException ie) {
                }
            }

        }

        public void Connect(String hostname) {
            try {
                ReadSelector = Selector.open();
                InetAddress addr = InetAddress.getByName(hostname);
                SChan = SocketChannel.open(new InetSocketAddress(addr, PORT));
                SChan.configureBlocking(false);

                SChan.register(ReadSelector, SelectionKey.OP_READ, new StringBuffer());
            } catch (Exception e) {
            }
        }

        public void SendMassage(String messg) {
            prepareBuffer(UserName + " says: " + messg);
            channelWrite(SChan);
        }

        public void prepareBuffer(String massg) {
            writeBuffer.clear();
            writeBuffer.put(massg.getBytes());
            writeBuffer.putChar('\n');
            writeBuffer.flip();
        }

        public void channelWrite(SocketChannel client) {
            long num = 0;
            long len = writeBuffer.remaining();
            while (num != len) {
                try {
                    num += SChan.write(writeBuffer);

                    Thread.sleep(5);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException ex) {

                }

            }
            writeBuffer.rewind();
        }

        public void ReadMassage() {

            try {

                ReadSelector.selectNow();

                Set readyKeys = ReadSelector.selectedKeys();

                Iterator i = readyKeys.iterator();

                while (i.hasNext()) {

                    SelectionKey key = (SelectionKey) i.next();
                    i.remove();
                    SocketChannel channel = (SocketChannel) key.channel();
                    ReadBuffer.clear();

                    long nbytes = channel.read(ReadBuffer);

                    if (nbytes == -1) {
                        ChatBox.append("You logged out !\n");
                        channel.close();
                    } else {

                        StringBuffer sb = (StringBuffer) key.attachment();

                        ReadBuffer.flip();
                        String str = asciiDecoder.decode(ReadBuffer).toString();
                        sb.append(str);
                        ReadBuffer.clear();

                        String line = sb.toString();
                        if ((line.indexOf("\n") != -1) || (line.indexOf("\r") != -1)) {
                            line = line.trim();

                            ChatBox.append("> " + line);
                            ChatBox.append("" + '\n');
                            sb.delete(0, sb.length());
                        }
                    }
                }
            } catch (IOException ioe) {
            } catch (Exception e) {
            }
        }
    }

    class ReadThread extends Thread {

        public void run() {
            ChatClient.ReadMassage();

        }
    }

}
