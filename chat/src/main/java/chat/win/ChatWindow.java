package chat.win;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatWindow {

	private Frame frame;
	private Panel pannel;
	private Button buttonSend;
	private TextField textField;
	private TextArea textArea;
	private Socket socket;

	public ChatWindow(String name, Socket socket) {
		this.socket = socket;
		frame = new Frame("" + name);
		pannel = new Panel();
		buttonSend = new Button("Send");
		textField = new TextField();
		textArea = new TextArea(30, 80);

		new ChatClientThread(socket).start();
	}

	public void show() {
		// Button
		/**
		 * UI초기화 작업
		 */
		buttonSend.setBackground(Color.GRAY);
		buttonSend.setForeground(Color.WHITE);
		buttonSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				sendMessage();
			}
		});

		// Textfield
		textField.setColumns(80);
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				char keyCode = e.getKeyChar();
				if (keyCode == KeyEvent.VK_ENTER) {
					sendMessage();
				}
			}
		});

		// Pannel
		pannel.setBackground(Color.LIGHT_GRAY);
		pannel.add(textField);
		pannel.add(buttonSend);
		frame.add(BorderLayout.SOUTH, pannel);

		// TextArea
		textArea.setEditable(false);
		frame.add(BorderLayout.CENTER, textArea);

		// Frame
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				PrintWriter printwriter;
				try {
					printwriter = new PrintWriter(
							new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
					printwriter.println("quit");
					System.exit(0);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}
		});
		frame.setVisible(true);
		frame.pack();
		/**
		 * IOStream 초기화 작업
		 */
//		bufferedreader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
//		printwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
//		/**
//		 * 쓰레드 생성 작업
//		 */
//		new ChatClientThread(socket).start();
	}

	private void sendMessage() {
		try {
			PrintWriter printwriter = new PrintWriter(
					new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
			String message = textField.getText();
			if ("quit".equals(message) == true) {
				printwriter.println("quit");
				System.exit(0);
			} else if (message.startsWith("to:") || message.startsWith("ban:")) {
				printwriter.println(message);
			} else {
				printwriter.println("message:" + message);
			}

			textField.setText("");
			textField.requestFocus();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class ChatClientThread extends Thread {

		Socket socket = null;

		ChatClientThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				BufferedReader bufferedreader = new BufferedReader(
						new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
				while (true) {

					String message = bufferedreader.readLine();
					if (message == null) {
						textArea.append(" ");
					}
					if ("ban".equals(message)) {
						System.exit(0);
					}
					textArea.append(message);
					textArea.append("\n");

				}
			} catch (IOException e) {
				textArea.append("서버 연결 끊김\n");
				System.out.println("서버 연결 끊김\n" + e);
				e.printStackTrace();
			}
		}
	}
}
