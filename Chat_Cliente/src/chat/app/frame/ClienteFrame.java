package chat.app.frame;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.TextArea;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import chat.app.bean.ChatMenssage;
import chat.app.bean.ChatMenssage.Action;
import chat.app.servece.ClienteService;
import javax.swing.UIManager;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JList;
import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.JFormattedTextField;
import java.awt.List;
import java.awt.TextField;

public class ClienteFrame extends JFrame {

	private JButton btnConectar;
	private JButton btnExit;
	private JButton btnLimpar;
	private JButton btnEnviar;
	
	private JTextArea textAreaRecebe;
	
	private JPanel contentPane;
	private JPanel conectar;
	private JPanel onlines;
	private JPanel mensagens;
	
	private JTextField textName;
	private JTextArea textAreaEnvio;
	private JList listOlines;
	
	private Socket socket;
	private ChatMenssage message;
	private ClienteService service;
	
	
	public ClienteFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 499, 362);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		conectar = new JPanel();
		conectar.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Conectar", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		conectar.setBounds(6, 0, 341, 62);
		contentPane.add(conectar);
		conectar.setLayout(null);
		
		textName = new JTextField();
		textName.setBounds(10, 21, 122, 23);
		conectar.add(textName);
		textName.setColumns(10);
		
		btnConectar = new JButton("Conectar");
		btnConectar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = textName.getText();
				if(!name.isEmpty()) {
					message = new ChatMenssage();
					message.setAction(Action.CONNECT);
					message.setName(name);
					
					if(socket == null) {
						service = new ClienteService();
						socket = service.connect();
						
						new Thread(new ListenerSocket(socket)).start();
					}
					
					service.send(message);
				}
			}
		});
		btnConectar.setBounds(142, 21, 95, 23);
		conectar.add(btnConectar);
		
		btnExit = new JButton("Exit");
		btnExit.setEnabled(false);
		btnExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ChatMenssage message = new ChatMenssage();
		        message.setName(message.getName());
		        message.setAction(Action.DESCONNECT);
		        service.send(message);
		        desconect();
			}
		});
		btnExit.setBounds(245, 21, 58, 23);
		conectar.add(btnExit);
		
		onlines = new JPanel();
		onlines.setBorder(new TitledBorder(null, "Onlines", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		onlines.setBounds(357, 0, 116, 312);
		contentPane.add(onlines);
		onlines.setLayout(null);
		
		listOlines = new JList();
		listOlines.setBounds(10, 24, 96, 277);
		onlines.add(listOlines);
		
		mensagens = new JPanel();
		mensagens.setBorder(new EmptyBorder(0, 0, 0, 0));
		mensagens.setBounds(10, 73, 337, 239);
		contentPane.add(mensagens);
		mensagens.setLayout(null);
		
		btnLimpar = new JButton("Limpar");
		btnLimpar.setEnabled(false);
		btnLimpar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		       textAreaEnvio.setText("");
			}
		});
		btnLimpar.setBounds(156, 216, 102, 23);
		mensagens.add(btnLimpar);
		
		btnEnviar = new JButton("Enviar");
		btnEnviar.setEnabled(false);
		btnEnviar.setBounds(268, 216, 69, 23);
		mensagens.add(btnEnviar);
		
		textAreaRecebe = new JTextArea();
		textAreaRecebe.setEditable(false);
		textAreaRecebe.setEnabled(false);
		textAreaRecebe.setBounds(0, 0, 337, 142);
		mensagens.add(textAreaRecebe);
		
		textAreaEnvio = new JTextArea();
		textAreaEnvio.setBounds(0, 153, 337, 52);
		mensagens.add(textAreaEnvio);
		btnEnviar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String text = textAreaEnvio.getText();
		        String name = message.getName();
		        
		        message = new ChatMenssage();
		        
		        if (listOlines.getSelectedIndex() > -1) {
		            message.setNameReserved((String) listOlines.getSelectedValue());
		            message.setAction(Action.SEND_ONE);
		            listOlines.clearSelection();
		        } else {
		            message.setAction(Action.SEND_ALL);
		        }
		        
		        if (!text.isEmpty()) {
		            message.setName(name);
		            message.setText(text);

		            textAreaRecebe.append("Você disse: " + text + "\n");
		            
		            service.send(message);
		        }
		        
		        textAreaEnvio.setText("");
			}
		});
	}
	
	private class ListenerSocket implements Runnable{
		private ObjectInputStream input;
		
		public ListenerSocket(Socket socket) {
			try {
				this.input=new ObjectInputStream(socket.getInputStream());
			} catch (IOException ex) {
				Logger.getLogger(ClienteFrame.class.getName()).log(Level.SEVERE,null,ex);
			}
		}
		
		 @Override
	        public void run() {
	            ChatMenssage message = null;
	            try {
	                while ((message = (ChatMenssage) input.readObject()) != null) {
	                    Action action = message.getAction();

	                    if (action.equals(Action.CONNECT)) {
	                        connect(message);
	                    } else if (action.equals(Action.DESCONNECT)) {
	                    	desconect();
	                        socket.close();
	                    } else if (action.equals(Action.SEND_ONE)) {
	                        System.out.println("::: " + message.getText() + " :::");
	                        receive(message);
	                    } else if (action.equals(Action.USER_ONLINE)) {
	                    	atualizar(message);
	                    }
	                }
	            } catch (IOException ex) {
	                Logger.getLogger(ClienteFrame.class.getName()).log(Level.SEVERE, null, ex);
	            } catch (ClassNotFoundException ex) {
	                Logger.getLogger(ClienteFrame.class.getName()).log(Level.SEVERE, null, ex);
	            }
	        }
	    }
	private void connect(ChatMenssage message) {
		if (message.getText().equals("NO")) {
            this.textName.setText("");
            JOptionPane.showMessageDialog(this, "Conexão não realizada!\nTente novamente com um novo nome.");
            return;
        }

        this.message = message;
        this.btnConectar.setEnabled(false);
        this.textName.setEditable(false);

        this.btnExit.setEnabled(true);
        this.textAreaEnvio.setEnabled(true);
        this.textAreaRecebe.setEnabled(true);
        this.btnEnviar.setEnabled(true);
        this.btnLimpar.setEnabled(true);


        JOptionPane.showMessageDialog(this, "Você está conectado no chat!");
    }
	
	private void desconect() {
		 	
			this.btnConectar.setEnabled(true);
	        this.textName.setEditable(true);

	        this.btnExit.setEnabled(false);
	        this.textAreaEnvio.setEnabled(false);
	        this.textAreaRecebe.setEnabled(false);
	        this.btnEnviar.setEnabled(false);
	        this.btnLimpar.setEnabled(false);
	        
	        this.textAreaRecebe.setText("");
	        this.textAreaEnvio.setText("");

	        JOptionPane.showMessageDialog(this, "Você saiu do chat!");
	    }
	
	private void receive(ChatMenssage message) {
        this.textAreaRecebe.append(message.getName() + " diz: " + message.getText() + "\n");
	}
	
	private void atualizar(ChatMenssage message) {
		System.out.println(message.getSetOlines().toString());
        
        Set<String> names = message.getSetOlines();
        
        names.remove(message.getName());
        
        String[] array = (String[]) names.toArray(new String[names.size()]);
        
        this.listOlines.setListData(array);
        this.listOlines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.listOlines.setLayoutOrientation(JList.VERTICAL);
 
	}
}
