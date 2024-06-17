
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TCPServer {
    private ServerSocket serverSocket;
    private boolean isRunning;

    public TCPServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
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

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
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
            System.out.println("Server durduruldu.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();
                DataOutputStream dataout = new DataOutputStream(clientSocket.getOutputStream());
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    String dataReceived = new String(buffer, 0, bytesRead);
                    System.out.println("Client'dan gelen veri: " + dataReceived);

                    // Veritabanına kaydet
                    saveToDatabase(dataReceived);

                    //dataout.writeUTF("300,500");
                    //dataout.close();
                }

                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       private static void saveToDatabase(String data) {
        String query = "INSERT INTO tablodeneme1 (veri) VALUES (?)";//ilkDeger  deneme
        try (Connection connection = DatabaseHelper.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            System.out.println("Database connection established");
            preparedStatement.setString(1, data);
            int rowsAffected = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    }

    public static void main(String[] args) {


     
        int port = 12345; // Sunucunun dinleyeceği port numarası
        TCPServer server = new TCPServer(port);
        server.start();

          

   
    }
}
