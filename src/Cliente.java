import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Cliente {

    public static final String ENDCONNECTION = "Servidor.fim\n";
    public static final String BLOCKED = "Servidor.ilegal\n";
    public static final String UDPSTART = "Servidor.udp\n";

    public static class TCPConnection implements Runnable{

        String hostname;
        int portNumber;
        Socket socket;
        String textToSend = "Connected!";
        BufferedReader br;
        String recieved = null;

        TCPConnection(String host, int port) {
            this.hostname = host;
            this.portNumber = port;
        }

        public void open() throws IOException {
            socket = new Socket(hostname, portNumber);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void send(String s) throws IOException {
            PrintStream ps = new PrintStream(socket.getOutputStream());
            ps.println(s);
        }

        public String recieve() throws IOException, InterruptedException {
            String line;
            String text = "";
            while (!br.ready()) {
            }
            while (br.ready()) {
                line = br.readLine();
                TimeUnit.MILLISECONDS.sleep(1);
                text += line + "\n";
            }
            return text;
        }

        public void close() throws IOException {
            if (socket != null) {
                socket.close();
            }
        }

        public void runRec() throws IOException, InterruptedException {
            recieved = this.recieve();
        }

        public void run() {
            try {
                this.send(textToSend);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public static class UDPConnection implements Runnable {
        private String hostname;
        private DatagramSocket socket;
        private InetAddress address;
        private int port;

        public UDPConnection(String address, int port) throws SocketException, UnknownHostException {
            socket = new DatagramSocket();
            this.hostname = address;
            this.address = InetAddress.getByName(address);
            this.port = port;
            socket = new DatagramSocket(null);
            InetSocketAddress sockAd = new InetSocketAddress(hostname, port);
            socket.bind(sockAd);
        }

        public String recieveEcho() throws IOException {
            byte[] recBuf = new byte[256];
            Arrays.fill(recBuf, (byte) 0);
            DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength());
        }

        public void close() {
            socket.close();
        }

        public void run() {
            try {
                while(true) {
                    String messageRec = this.recieveEcho();
                    System.out.println();
                    System.out.println(messageRec);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void menu() {
        System.out.println("MENU CLIENTE\n");
        System.out.println("0 - Menu Inicial");
        System.out.println("1 - Listar utilizadores online");
        System.out.println("2 - Enviar mensagem a um utilizador");
        System.out.println("3 - Enviar mensagem a todos os utilizadores");
        System.out.println("4 - Lista branca de utilizadores");
        System.out.println("5 - Lista negra de utilizadores");
        System.out.println("99 - Sair");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        boolean valid;
        boolean exit = false;
        TCPConnection ligTCP = null;
        String opt;
        do {
            System.out.print("Introduza o endereço do servidor> ");
            opt = bufferRead.readLine();
            try {
                ligTCP = new TCPConnection(opt, 7142);
                ligTCP.open();
                valid = true;
            }
            catch (UnknownHostException e) {
                valid = false;
                System.out.println("Este host é desconhecido!");
            }
            catch (ConnectException e) {
                valid = false;
                System.out.println("Não foi possivel estabelecer ligação ao servidor.");
            }
        } while (!valid);
        String porta = ligTCP.recieve();
        if (porta.equals(BLOCKED)) {
            System.out.println("Este endereço IP está bloqueado no servidor!");
            System.exit(0);
        }
        int portUDP = Integer.parseInt(porta.replace("\n", ""));
        UDPConnection ligUDP = new UDPConnection(opt, portUDP);
        Thread udpThread = new Thread(ligUDP);
        menu();
        udpThread.start();
        while(!exit){
            System.out.println();
            System.out.print("Opção? ");
            bufferRead = new BufferedReader(new InputStreamReader(System.in));
            String s = bufferRead.readLine();
            if ("0".equals(s)) { //mostrar menu, continuar
                menu();
                continue;
            }
            else if ("99".equals(s)) {
            }
            else {
                ligTCP.textToSend = s;
                ligTCP.run();
                ligTCP.runRec();
                if ("2".equals(s) && ligTCP.recieved.equals(UDPSTART)) {
                    System.out.println();
                    System.out.print("Utilizador? ");
                    String destinatario = bufferRead.readLine();
                    System.out.println();
                    System.out.print("Mensagem? ");
                    String mensagem = bufferRead.readLine();
                    ligTCP.textToSend = mensagem + "|" + destinatario;
                    ligTCP.run();
                }
                else if("3".equals(s)&& ligTCP.recieved.equals(UDPSTART)){
                    System.out.println();
                    System.out.print("Mensagem? ");
                    String mensagem = bufferRead.readLine();
                    ligTCP.textToSend = mensagem + "|" + "all";
                    ligTCP.run();
                }
                else if (!ligTCP.recieved.equals(UDPSTART) && (("2".equals(s) || "3".equals(s)))) {
                    System.out.println("Existe uma failrule na tentativa de iniciar o cliente UDP!");
                }
            }
            if (ENDCONNECTION.equals(ligTCP.recieved) || "99".equals(s)) { //server-side end connection
                ligTCP.close();
                udpThread.stop(); //necessário, senao rebenta
                ligUDP.close();
                System.out.println("A sair");
                System.out.println("Cliente desconectado...");
                exit = true;
            }
            else if (UDPSTART.equals(ligTCP.recieved)) {
                continue;
            }
            else {
                if (ligTCP.recieved != null) {
                    System.out.print(ligTCP.recieved);
                    ligTCP.recieved = null;
                }
            }
        }
    }
}



