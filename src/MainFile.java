import ServerSide.TCPServer;

public class MainFile {
   public static void main(String[] args) {

        int port = 12345; // Sunucunun dinleyeceği port numarası
        TCPServer server = new TCPServer(port);
        server.start();

   
    }
}
