import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by DJO on 15/10/20.
 */
public class Server {
    public static void main(String[] args) {
    if(args.length >0){
        if(args[0].equalsIgnoreCase("-h")){
            System.out.println("help message POP3 Server");
            System.out.println("Args : Link to baseDirectory");
            System.out.println("This server can handle Commandes : USER PASS LIST DELE RETR STAT");
            System.out.println("Any Password with command PASS will be accepted");
            System.out.println("Will be runing in PORT 110 (default port for POP3 Server )");

        }else {

            try {
                ServerSocket serverSocket=new ServerSocket(110);
                System.out.println("Server POP3 running in PORT 110");

                while (true){
                    Socket socket=serverSocket.accept();
                    popServer client=new popServer(socket,args[0]);
                    client.run();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }else{
        System.out.println("Run the Server with -h for more details and help message");
    }

    }
}
