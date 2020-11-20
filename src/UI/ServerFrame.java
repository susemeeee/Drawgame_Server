/*
 * UI.ServerFrame.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */
package UI;

import net.Connection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerFrame {
    private Connection connection;
    private boolean isStarted;

    private JFrame frame;
    private JTextArea logArea;
    private JPanel logPanel;
    private JButton startButton;
    private JScrollPane scrollPane;

    private ServerFrame(){
        frame = new JFrame();
        isStarted = false;
        connection = new Connection();
        initFrame();
        setView();
    }

    private void initFrame(){
        frame.setSize(new Dimension(415, 530));
        frame.setResizable(false);
        frame.setLayout(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                connection.stopServer();
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }

    private void setView(){
        logPanel = new JPanel();
        logPanel.setLocation(new Point(25, 25));
        logPanel.setSize(new Dimension(350, 375));
        logPanel.setLayout(new GridLayout());
        logPanel.setVisible(true);

        logArea = new JTextArea();
        logArea.setEditable(false);
        scrollPane = new JScrollPane(logArea);
        scrollPane.setVisible(true);
        logArea.setVisible(true);
        logPanel.add(scrollPane);
        frame.add(logPanel);

        frame.repaint();
        frame.revalidate();

        startButton = new JButton("START");
        startButton.setBounds(25, 425, 350, 50);
        startButton.setBackground(Color.GREEN);
        startButton.addActionListener(e -> {
            if(!isStarted){
                connection.startServer("localhost", 9002);
                startButton.setText("STOP");
                startButton.setBackground(Color.RED);
                frame.repaint();

                isStarted = true;
            }
            else{
                connection.stopServer();
            }
        });
        startButton.setVisible(true);
        frame.add(startButton);

        frame.repaint();
    }

    public void appendLogLine(String msg){
        logArea.append(msg + "\n");
        scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        frame.repaint();
    }

    public static ServerFrame getInstance(){
        return LazyHolder.INSTANCE;
    }

    private static class LazyHolder{
        private static final ServerFrame INSTANCE = new ServerFrame();
    }
}
