package org.example.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private static final int PORT = 12345;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private PrintWriter out;

    public Client() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());
        frame.add(inputField, BorderLayout.SOUTH);

        JButton sendButton = new JButton("Отправить");
        sendButton.addActionListener(e -> sendMessage());
        frame.add(sendButton, BorderLayout.EAST);

        JButton privateButton = new JButton("Личное сообщение");
        privateButton.addActionListener(e -> sendPrivateMessage());
        frame.add(privateButton, BorderLayout.WEST);

        frame.setVisible(true);

        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket("localhost", PORT);
            out = new PrintWriter(socket.getOutputStream(), true);

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.execute(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        appendMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText();
        out.println(message);
        inputField.setText("");  // Clear the input field after sending the message
    }

    private void sendPrivateMessage() {
        String recipient = JOptionPane.showInputDialog("Введите определитель пользователя:");
        String message = JOptionPane.showInputDialog("Введите сообщение:");
        out.println("/private " + recipient + " " + message);
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        new Client();
    }
}


