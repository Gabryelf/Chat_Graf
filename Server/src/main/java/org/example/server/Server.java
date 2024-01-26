package org.example.server;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    private static final String CHAT_HISTORY_FILE = "chat_history.txt";
    private static final String CLIENT_INFO_FILE = "client_info.txt";

    private JTextArea chatArea;
    private Map<String, PrintWriter> clients = new HashMap<>();
    private Map<String, UserInfo> userInfoMap = new HashMap<>();
    private ExecutorService clientThreadPool = Executors.newCachedThreadPool();

    public Server() {
        JFrame frame = new JFrame("Chat Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JButton disconnectButton = new JButton("Отключить пользователя");
        disconnectButton.addActionListener(e -> disconnectUser());
        frame.add(disconnectButton, BorderLayout.SOUTH);

        JButton saveHistoryButton = new JButton("Сохранить историю");
        saveHistoryButton.addActionListener(e -> saveChatHistory());
        frame.add(saveHistoryButton, BorderLayout.NORTH);

        frame.setVisible(true);

        loadChatHistory();
        loadClientInfo();
        startServer();
    }

    private void loadChatHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(CHAT_HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatArea.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveChatHistory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CHAT_HISTORY_FILE, true))) {
            String chatText = chatArea.getText();
            writer.write(chatText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadClientInfo() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(CLIENT_INFO_FILE))) {
            userInfoMap = (Map<String, UserInfo>) objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("Client info file not found. Creating a new file.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveClientInfo() {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(CLIENT_INFO_FILE))) {
            objectOutputStream.writeObject(userInfoMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            appendMessage("Сервер запущен на порту " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientThreadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String username = generateUniqueUsername(clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
            String host = clientSocket.getInetAddress().getHostAddress();
            int port = clientSocket.getPort();
            UserInfo userInfo = new UserInfo(username, host, port);

            clients.put(username, out);
            userInfoMap.put(username, userInfo);
            saveClientInfo();

            broadcastMessage("Пользователь " + username + " присоединился к чату");

            String message;
            while ((message = in.readLine()) != null) {
                if ("/exit".equals(message)) {
                    break;
                } else if (message.startsWith("/private")) {
                    sendPrivateMessage(username, message);
                } else if (message.startsWith("/group")) {
                    sendGroupMessage(username, message);
                } else {
                    broadcastMessage(username + ": " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            removeClient(clientSocket);
        }
    }

    private String generateUniqueUsername(String host, int port) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return "User_" + host.replace(".", "_") + "_" + port + "_" + dateFormat.format(new Date());
    }

    private void sendPrivateMessage(String sender, String message) {
        String[] parts = message.split(" ", 3);
        String recipient = parts[1];
        String privateMessage = sender + " (private): " + parts[2];

        PrintWriter recipientOut = clients.get(recipient);
        if (recipientOut != null) {
            recipientOut.println(privateMessage);
        }
    }

    private void sendGroupMessage(String sender, String message) {
        String groupMessage = sender + " (group): " + message.substring(7);
        broadcastMessage(groupMessage);
    }

    private void broadcastMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });

        for (PrintWriter clientOut : clients.values()) {
            clientOut.println(message);
        }
    }

    private void removeClient(Socket clientSocket) {
        for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
            if (entry.getValue().checkError() || entry.getValue().equals(clientSocket)) {
                clients.remove(entry.getKey());
                userInfoMap.remove(entry.getKey());
                broadcastMessage("Пользователь " + entry.getKey() + " покинул чат");
                saveClientInfo();
                break;
            }
        }
    }

    private void disconnectUser() {
        String username = JOptionPane.showInputDialog("Отключить пользователя:");
        if (clients.containsKey(username)) {
            PrintWriter clientOut = clients.get(username);
            clientOut.println("/exit");
        } else {
            JOptionPane.showMessageDialog(null, "Пользователь не найден");
        }
    }

    public static void main(String[] args) {
        new Server();
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private static class UserInfo implements Serializable {
        private String username;
        private String host;
        private int port;

        public UserInfo(String username, String host, int port) {
            this.username = username;
            this.host = host;
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }
    }
}





