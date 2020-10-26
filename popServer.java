import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by e2004223 on 14/10/20.
 */
public class popServer implements Runnable{

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String link;
    private String user;
    private File[] listFile=new File[0]; //For reading Emails in Subdirectory
    private String confirm="+OK ";
    private String refuse="-ERR ";
    popServer(Socket socket_,String baseDirectory){
        this.socket=socket_;
        this.link=baseDirectory;
        try {
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out=new PrintWriter(socket_.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

            try{
                FileInputStream fis;
                BufferedInputStream bis;
                OutputStream os;
                System.out.println(socket);
                InputStream is = this.socket.getInputStream ();
                os = this.socket.getOutputStream ();

                BufferedReader sockin = new BufferedReader (new InputStreamReader (is));
                PrintWriter sockout = new PrintWriter (os, true);

                this.communicatePopThunderbird(sockin,sockout,confirm+"POP3 Server ready");

                System.out.println("The end of session");

            }catch (Exception e){
                 try{

                    this.out.close();
                    this.in.close();
                    this.socket.close();
                }catch (Exception ee){
                    ee.printStackTrace();
                }
                e.printStackTrace();

            }



    }

    public void communicatePopThunderbird(BufferedReader bf,PrintWriter pw,String Msg_)  {
        try {
            String  inSideMessage=Msg_;
            showMyMessageAndSendIt(inSideMessage,pw);
            String response = bf.readLine();
          while (!this.socket.isClosed()){
              if(response!=null){
                  System.out.println("Thunderbird : " + response);
                  String[] commandAndDetails=response.split(" ");
                  switch((commandAndDetails)[0]){
                      case "USER":
                              this.user=commandAndDetails[1];//the username
                              if(getInEmailsForUser(this.user)){
                                  inSideMessage=confirm;
                              }else{
                                  inSideMessage=refuse;
                              }
                              showMyMessageAndSendIt(inSideMessage,pw);
                              break;
                      case "PASS":
                              //accept any password
                              //no way to check
                              inSideMessage=confirm;
                              showMyMessageAndSendIt(inSideMessage,pw);
                              break;
                      case "STAT":
                            inSideMessage=confirm+(this.listFile==null?0:this.listFile.length)+" "+getSizeEmails()+" (octets)";
                            showMyMessageAndSendIt(inSideMessage,pw);
                            break;
                      case "LIST":
                            if(commandAndDetails.length>1){
                                if(Integer.valueOf(commandAndDetails[1])>(this.listFile==null?0:this.listFile.length)){
                                    inSideMessage=refuse+"No such messages Only "+(this.listFile==null?0:this.listFile.length)+" messages";
                                    showMyMessageAndSendIt(inSideMessage,pw);
                                }else{
                                    showMyMessageAndSendIt(refuse,pw);//List 2
                                }
                            }else{
                                inSideMessage=confirm+(this.listFile==null?0:this.listFile.length)+" messages ("+getSizeEmails()+" octets)";
                                showMyMessageAndSendIt(inSideMessage,pw);
                                ArrayList<String[]> listData=this.getNumberSizeEmails();
                                for(String[] inListData:listData){
                                    inSideMessage=inListData[0]+" "+inListData[1]; //1 380 octets
                                    showMyMessageAndSendIt(inSideMessage,pw);
                                }
                                inSideMessage=".";//finish Operation
                                showMyMessageAndSendIt(inSideMessage,pw);

                            }
                            break;
                      case "RETR":
                            inSideMessage=confirm+getSizeEmail(Integer.valueOf(commandAndDetails[1]));
                            showMyMessageAndSendIt(inSideMessage,pw);
                            for(String message:getMessageInEmail(Integer.valueOf(commandAndDetails[1]))){
                                inSideMessage=message;//Exemple = <the POP3 server sends message 1>
                                showMyMessageAndSendIt(inSideMessage,pw);
                            }
                            inSideMessage=".";//finish Operation
                            showMyMessageAndSendIt(inSideMessage,pw);
                            break;
                      case "DELE":
                              if(deleteEmailAfterSendIt(Integer.valueOf(commandAndDetails[1]))){
                                  inSideMessage=confirm;
                              }else{
                                  inSideMessage=refuse;
                              }
                            showMyMessageAndSendIt(inSideMessage,pw);
                            break;
                            /*   The optional operations do not have to be implemented, except for the USER and PASS operations.
                        case "UIDL":break;
                        case "APOP":break;
                        case "NOOP":break;
                        case "RSET":break;
                              */
                      case "QUIT":
                          inSideMessage=confirm;
                          showMyMessageAndSendIt(inSideMessage,pw);
                          this.socket.close();
                          break;
                      default :
                          inSideMessage=refuse+"Command not valid in this state";
                          showMyMessageAndSendIt(inSideMessage,pw);
                          break;
                  }
                  if(!this.socket.isClosed()){
                      response =bf.readLine();
                  }
              }
          }
         }catch (Exception e){
            e.printStackTrace();
         }
    }

    public Boolean getInEmailsForUser(String user_){
        try{
            File directoryPath = new File(this.link+"/"+user_);
            this.listFile = directoryPath.listFiles();

             return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public int getSizeEmails(){
       try {
           int size = 0;
           for (File file : this.listFile) {
               Path path = Paths.get(file.getAbsolutePath());
               size += Files.size(path);
           }
           return size;
       }catch (Exception e){
           return 0;
       }
    }

    public ArrayList<String[]> getNumberSizeEmails(){
        ArrayList<String[]> listData=new ArrayList();
             for(int i=0;i<(this.listFile==null?0:this.listFile.length);i++){
                 try {
                     Path path = Paths.get(this.listFile[i].getAbsolutePath());
                     String[] aide = new String[2];
                     aide[0]=String.valueOf(i);
                     aide[1]=String.valueOf(Files.size(path));
                     listData.add(aide);
                 }catch(Exception e){
                     e.printStackTrace();
                 }
             }
        return listData;
    }
    public int getSizeEmail(int indice){
        int size = 0;
        try {
            Path path = Paths.get(this.listFile[indice].getAbsolutePath());
            size=(int)Files.size(path);
            return size;
        }catch(Exception e){
            return size;
         }
    }

    public ArrayList<String> getMessageInEmail(int indice){
        ArrayList<String> aide=new ArrayList();
            try {
                Scanner scan = new Scanner(new File(this.listFile[indice].getAbsolutePath()));
                while(scan.hasNextLine()){
                    String line = scan.nextLine();
                    aide.add(line);
                }
                scan.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        return aide;
    }

    public void showMyMessageAndSendIt(String  msg_,PrintWriter pw_){
        System.out.println("S : "+msg_);//Show in console
        pw_.println(msg_);//send it to client
     }

     public Boolean deleteEmailAfterSendIt(int indice){
        try {
            Path path = Paths.get(this.listFile[indice].getAbsolutePath());
            Files.deleteIfExists(path);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
     }

}
