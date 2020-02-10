package chat.win;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChatServerThread extends Thread {

	private String nickname;
	private Socket socket;
	private List<Writer> listWriters;
	private Map<String, Object> map;
	private PrintWriter printWriter;

	public ChatServerThread(Socket socket, List<Writer> listWriters, Map<String, Object> map) {
		this.socket = socket;
		this.listWriters = listWriters;
		this.map = map;
	}

	@Override
	public void run() {
		// 1. Remote Host Information
		InetSocketAddress remoteInetSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		int remotePort = remoteInetSocketAddress.getPort();
		String remoteInetAddress = remoteInetSocketAddress.getAddress().getHostAddress();
		ChatServer.log("connected by client[" + remoteInetAddress + " : " + remotePort + "]");

		// 2. 스트림 얻기
		try {
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
					true);
			// 3. 요청 처리
			while (true) {
				String request = bufferedReader.readLine();
				if (request == null) {
					doQuit(printWriter);
					ChatServer.log("#" + Thread.currentThread().getId() + "클라이언트로 부터 연결 끊김");
					break;
				}
				// 4. 프로토콜 분석
				String[] tokens = request.split(":");

				if ("join".equals(tokens[0])) {
					doJoin(tokens[1], printWriter);
				} else if ("message".equals(tokens[0])) {
					try {
						doMessage(tokens[1]);
					} catch (Exception e) {
						doMessage(" ");
					}
				} else if ("quit".equals(tokens[0])) {
					doQuit(printWriter);
					break;
				} else if ("to".equals(tokens[0]) && tokens.length > 2) {
					tomsg(tokens[1], tokens[2]);
				} else if ("ban".equals(tokens[0]) && tokens.length == 2) {
					ban(tokens[1]);
				} else {
					// ChatServer.log("에러:알수 없는 요청(" + tokens[0] + ")");
					if ("to".equals(tokens[0])) {
						printWriter.println("<<to 잘못된 명령어 입니다.>> ex)/to : 사용자 이름 : message/");
						printWriter.flush();
					} else if ("ban".equals(tokens[0])) {
						printWriter.println("<<ban 잘못된 명령어 입니다.>> ex)/dan : 사용자 이름/");
						printWriter.flush();
					}

				}
			}
		} catch (IOException e) {
			ChatServer.log(nickname + "님이 퇴장 하셨습니다.");
			doQuit(printWriter);
		} finally {
			try {
				if (socket != null && !socket.isClosed())
					socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void ban(String name) {
		boolean find = false;
		if (listWriters.get(0) == this.printWriter) {
			for (String key : map.keySet()) {
				if (key.equals(name))
					find = true;
			}
			if (!find) {
				PrintWriter printWriter = (PrintWriter) map.get(this.nickname);
				printWriter.println("<<" + name + "은(는) 존재하지 않는 사용자 입니다.>>");
				printWriter.flush();
				return;
			}

			for (String key : map.keySet()) {
				if (!key.equals(name)) {
					PrintWriter printWriter = (PrintWriter) map.get(key);
					printWriter.println("<<" + name + "님을 추방했습니다.>>");
				} else {
					PrintWriter printWriter = (PrintWriter) map.get(key);
					printWriter.println("ban");
					printWriter.flush();
				}
			}
		} else {
			PrintWriter printWriter = (PrintWriter) map.get(this.nickname);
			printWriter.println("알림! 당신은 방장이 아닙니다.");
			printWriter.flush();
		}
	}

	private void tomsg(String name, String message) {
		boolean find = false;
		PrintWriter printWriter = null;
		for (String key : map.keySet()) {
			if (key.equals(name)) {
				find = true;
				printWriter = (PrintWriter) map.get(key);
				printWriter.println(this.nickname + "님의 귓속말 : " + message);
				printWriter.flush();
				break;
			}
		}
		if (find) {
			printWriter = (PrintWriter) map.get(this.nickname);
			printWriter.println(name + "님에게 귓속말 : " + message);
			printWriter.flush();
		} else {
			printWriter = (PrintWriter) map.get(this.nickname);
			printWriter.println("<<" + name + "님이 없습니다.>>");
			printWriter.flush();
		}
	}

	private void doJoin(String nickName, Writer writer) {
		this.nickname = nickName;

		Date now = new Date();
		SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd-hh시 mm분 ss초");
		String day = sdfd.format(now);

		/* writer pool에 저장 */
		addWriter(writer, nickName);
		String data = nickName + "님이 참여하였습니다. ( " + day + " )";
		broadcast(data);
		if (listWriters.size() == 1) {
			String msg = "--채팅방에 아무도 없습니다.--";
			String boss = "**방장이 되었습니다.**";
			broadcast(msg);
			broadcast(boss);
		}

		// ack
//		printWriter.println("join:ok");
//		printWriter.flush();

	}

	private void addWriter(Writer writer, String name) {
		synchronized (listWriters) {
			listWriters.add(writer);
		}
		synchronized (map) {
			map.put(name, writer);
		}
	}

	private void broadcast(String data) {
		synchronized (listWriters) {
			for (Writer writer : listWriters) {
				PrintWriter printWriter = (PrintWriter) writer;
				printWriter.println(data);
			}
		}
	}

	private void doMessage(String message) {
		broadcast(nickname + " : " + message);
	}

	private void doQuit(Writer writer) {
		Date now = new Date();
		SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd-hh시 mm분 ss초");
		String day = sdfd.format(now);

		if (listWriters.get(0) == this.printWriter && listWriters.size() > 1) {
			PrintWriter printWriter = (PrintWriter) listWriters.get(1);
			printWriter.println("**방장을 위임받았습니다.**");
		}
		removeWriter(writer);
		String data = nickname + "님이 퇴장 하였습니다. ( " + day + " )";
		broadcast(data);

		if (listWriters.size() == 1) {
			String msg = "--채팅방에 아무도 없습니다.--";
			broadcast(msg);
		}

	}

	private void removeWriter(Writer writer) {
		synchronized (listWriters) {
			listWriters.remove(writer);
		}
	}

}