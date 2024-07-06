package ServerSide;

import java.io.*;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.google.gson.Gson;

import DatabaseHelper.DatabaseHelper;
import Model.ResponseModel;

public class TCPServer {
    private ServerSocket serverSocket;
    private boolean isRunning;
    private Map<Socket, ClientHandler> clients;
    private Map<String, Set<ClientHandler>> gameRooms;
    public Thread clientThread;

    public TCPServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000000);
            clients = new ConcurrentHashMap<>();
            gameRooms = new ConcurrentHashMap<>();
            isRunning = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        isRunning = true;
        System.out.println("Server başlatıldı. Port: " + serverSocket.getLocalPort());

        while (isRunning) {
            System.out.println("Bağlantı bekleniyor...");
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Yeni bir istemci bağlandı: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.put(clientSocket, clientHandler);
                joinRoom("5", clientHandler);

                clientThread = new Thread(clientHandler);
                clientThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            serverSocket.close();
            for (ClientHandler client : clients.values()) {
                client.stop();
            }
            System.out.println("Server durduruldu.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void joinRoom(String roomName, ClientHandler client) {
        gameRooms.computeIfAbsent(roomName, k -> ConcurrentHashMap.newKeySet()).add(client);
    }

    public void broadcast(String message, String roomName, ClientHandler sender) {
        Set<ClientHandler> room = gameRooms.get(roomName);
        if (room != null) {
            for (ClientHandler client : room) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void leaveRoom(String roomName, ClientHandler client) {
        Set<ClientHandler> room = gameRooms.get(roomName);
        if (room != null) {
            room.remove(client);
            if (room.isEmpty()) {
                gameRooms.remove(roomName);
            }
        }
    }

    public static String generateUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    private static void saveToDatabase(String data) {
        String query = "INSERT INTO testTable (veri) VALUES (?)";// ilkDeger deneme  tablodeneme1
        try (Connection connection = DatabaseHelper.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            System.out.println("Database connection established");
            preparedStatement.setString(1, data);
            int rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public synchronized void removeFromRooms(ClientHandler client) {
        for (Set<ClientHandler> room : gameRooms.values()) {
            room.remove(client);
        }
        clientThread.interrupt();
    }
   

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client.clientSocket);
    }

    private  class ClientHandler implements Runnable {
        private Socket clientSocket;
        private boolean running;
        private TCPServer server;
        private DataOutputStream dataout;
        

        public ClientHandler(Socket socket, TCPServer server) {
            this.clientSocket = socket;
            this.server = server;
            this.running = true;
            try {
                this.dataout = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                InputStream input = clientSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String message;

                while (running && (message = reader.readLine()) != null) {
                    System.out.println("Client'dan gelen veri : " + clientSocket.getInetAddress().getHostAddress() + " "+ message);
                    ResponseModel responseModel ;
                    Gson gson = new Gson();
                    try {
                        responseModel = gson.fromJson(message, ResponseModel.class);
                        System.out.println("Converted ResponseModel: " + responseModel.getUserUuid() + ", " + responseModel.getContentType() + ", " + responseModel.getContent());


                        if(responseModel.getContentType().equals("joinRoom")){
                            server.joinRoom(responseModel.getRoomNumber(), this);
                        }else if (responseModel.getContentType().equals("leaveRoom")){
                            server.leaveRoom(responseModel.getRoomNumber(), this);
                        }else if(responseModel.getContentType().equals("chat")){

                        }else if(responseModel.getContentType().equals("draw")){

                        }else if(responseModel.getContentType().equals("firstConnect")){

                        }else if(responseModel.getContentType().equals("getAllRooms")){

                        }else if(responseModel.getContentType().equals("createRoom")){
                            
                        }
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                   



                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        String command = parts[0];
                        String content = parts[1];
                        if (command.startsWith("/join")) {
                            server.joinRoom(content, this);
                        }
                         else if (command.startsWith("/leave")) {
                            server.leaveRoom(content, this);
                        } else if (command.startsWith("/draw")) {
                            String[] drawParts = content.split(":", 2);
                            if (drawParts.length == 2) {
                                String roomName = drawParts[0];
                                String drawData = drawParts[1];
                                server.broadcast("draw:" + drawData, roomName, this);
                            }
                        } else if (command.startsWith("/guess")) {
                            String[] guessParts = content.split(":", 2);
                            if (guessParts.length == 2) {
                                String roomName = guessParts[0];
                                String guessData = guessParts[1];
                                server.broadcast("guess:" + guessData, roomName, this);
                            }
                        }

                    } else {
                        // Herhangi bir mesajı aynı odadaki diğer istemcilere ilet
                        server.broadcast(message, "5", this);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Client bağlantısı kesildi: " + clientSocket.getInetAddress().getHostAddress());

            } finally {
                server.removeFromRooms(this);
                server.removeClient(this);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void sendMessage(String message) {
            try {
                dataout.writeUTF(message + "\n");
                dataout.flush();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }

        public void stop() {

            running = false;
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        
    }
    

}
