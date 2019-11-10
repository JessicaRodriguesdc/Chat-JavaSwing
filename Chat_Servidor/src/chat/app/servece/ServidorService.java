package chat.app.servece;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import chat.app.bean.ChatMenssage;
import chat.app.bean.ChatMenssage.Action;


/**
 * @author Jéssica Rodrigues
 */

public class ServidorService {

	private ServerSocket serverSocket;
	private Socket socket;
	private Map<String, ObjectOutputStream> mapOnlines = new HashMap<String , ObjectOutputStream>();

	public ServidorService() {
		try {
			serverSocket = new ServerSocket(4444);
			
			System.out.println("Servidor on!");
			
			while (true) {
                socket = serverSocket.accept();

                new Thread(new ListenerSocket(socket)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
	private class ListenerSocket implements Runnable {

        private ObjectOutputStream output;
        private ObjectInputStream input;

        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream (socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void run() {
            ChatMenssage message = null;
            try {
                while ((message = (ChatMenssage) input.readObject()) != null) {
                    Action action = message.getAction();

                    if (action.equals(Action.CONNECT)) {
                        boolean isConnect = connect(message, output);
                        if (isConnect) {
                            mapOnlines.put(message.getName(), output);
                            sendOnlines();
                        }
                    } else if (action.equals(Action.DESCONNECT)) {
                    	desconect(message, output);
                        sendOnlines();
                        return;
                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(message);
                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);
                    }
                }
            } catch (IOException ex) {
                ChatMenssage cm = new ChatMenssage();
                cm.setName(message.getName());
                desconect(cm, output);
                sendOnlines();
                System.out.println(message.getName() + " deixou o chat!");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean connect(ChatMenssage message, ObjectOutputStream output) {
        if (mapOnlines.size() == 0) {
            message.setText("YES");
            send(message, output);
            return true;
        }

        if (mapOnlines.containsKey(message.getName())) {
            message.setText("NO");
            send(message, output);
            return false;
        } else {
            message.setText("YES");
            send(message, output);
            return true;
        }
    }
	
	private void desconect (ChatMenssage message, ObjectOutputStream output) {
		 mapOnlines.remove(message.getName());
	     message.setText(" até logo!");
	     message.setAction(Action.SEND_ONE);
	     sendAll(message);
	     
         System.out.println("User " + message.getName() + " sai da sala");
	}
	
	   private void send(ChatMenssage message, ObjectOutputStream output) {
	        try {
	            output.writeObject(message);
	        } catch (IOException ex) {
	            Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
	        }
	    }
	
	private void sendAll(ChatMenssage message) {
		 for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
	            if (!kv.getKey().equals(message.getName())) {
	                message.setAction(Action.SEND_ONE);
	                try {
	                    kv.getValue().writeObject(message);
	                } catch (IOException ex) {
	                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
	                }
	            }
	        }
	}

	private void sendOne(ChatMenssage message) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (kv.getKey().equals(message.getNameReserved())) {
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
	
	private void sendOnlines() {
        Set<String> setNames = new HashSet<String>();
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            setNames.add(kv.getKey());
        }

        ChatMenssage message = new ChatMenssage();
        message.setAction(Action.USER_ONLINE);
        message.setSetOlines(setNames);
        
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            message.setName(kv.getKey());
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(ServidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
