package chat.win;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClientApp {

	private static final int SERVER_PORT = 46;

	public static void main(String[] args) {
		String name = null;
		Scanner scanner = new Scanner(System.in);

		while (true) {

			System.out.println("이름을 입력하세요.");
			System.out.print(">>> ");
			name = scanner.nextLine();

			if (name.isEmpty() == false) {
				break;
			}
			System.out.println("이름은 한글자 이상 입력해야 합니다.\n");
//			usercount++;
//			names[usercount] = name;

		}

		scanner.close();

		// 1. socket생성
		// 2. eonnect to server
		// 3. iosream 생성
		// 4. join 프로토콜이 성공 응답을 받으면 그때 보여줌
		// new ChatWindow(name, socket).show();
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress("0.0.0.0", SERVER_PORT));
			log("채팅방에 입장하였습니다.");
			PrintWriter printwriter = new PrintWriter(
					new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

			String request = "join:" + name;
			printwriter.println(request);

			new ChatWindow(name, socket).show();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void log(String log) {
		System.out.println(log);
	}
}
