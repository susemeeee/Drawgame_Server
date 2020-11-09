/*
 * ServerFrame.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */

import javax.swing.*;
import java.awt.*;

public class ServerFrame {
    private JFrame frame;
    private JTextArea logArea;
    private JButton startButton;

    public ServerFrame(){
        frame = new JFrame();
        initFrame();
    }

    private void initFrame(){
        frame.setSize(new Dimension(415, 530));
        frame.setResizable(false);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        setView();
    }

    private void setView(){
        logArea = new JTextArea();
        logArea.setBounds(25, 25, 350, 375);
        logArea.setEditable(false);
        logArea.setVisible(true);
        frame.add(logArea);

        startButton = new JButton("Start");
        startButton.setBounds(25, 425, 350, 50);
        startButton.setVisible(true);
        frame.add(startButton);

        frame.repaint();
    }
}
