import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

public class Server {
	private static final Path PASSWD = Paths.get("passwd.txt");
	private ServerSocket server = null;

	public Server(int port) {
		try {
			createPasswdFile();
			server = new ServerSocket(port);
			System.out.println("Server started on port " + port);

			// Every client gets its own thread, so many can be connected at once
			while (true) {
				System.out.println("Waiting for a client...");
				Socket socket = server.accept();
				System.out.println("Client accepted: " + socket.getInetAddress());
				new Thread(() -> handleClient(socket)).start();
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// Format: username,password,major,advisor
	private void createPasswdFile() throws IOException {
		if (Files.exists(PASSWD)) return;
		Files.write(PASSWD, List.of(
				"aryan,cmsc445,Computer Science,Dr. Adams",
				"alice,alice123,Mathematics,Dr. Brown",
				"bob,bob123,Physics,Dr. Clark",
				"charlie,charl123,Biology,Dr. Davis",
				"diana,diana123,Chemistry,Dr. Evans"));
		System.out.println("Created " + PASSWD.toAbsolutePath());
	}

	private boolean isValid(String login, String pass) throws IOException {
		for (String line : Files.readAllLines(PASSWD)) {
			String[] fields = line.split(",");
			if (fields.length == 4 && fields[0].equals(login) && fields[1].equals(pass)) return true;
		}
		return false;
	}

	private void handleClient(Socket socket) {
		try (socket;
			 DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
			out.writeUTF("Welcome to Computer Science Department\n\n");

			String user = null;
			for (int tries = 0; tries < 3 && user == null; tries++) {
				String login = in.readUTF();
				String pass = in.readUTF();
				if (isValid(login, pass)) user = login;
				out.writeBoolean(user != null);
			}
			if (user == null) {
				System.out.println(socket.getInetAddress() + " failed to log in 3 times");
				return;
			}
			out.writeUTF("Welcome to CMSC445-Comp Networking class\nYou are invited to use Aryan's machine");
			System.out.println(user + " logged in from " + socket.getInetAddress());

			String line = "";
			while (!line.equals("Over")) {
				line = in.readUTF();
				System.out.println(user + ": " + line);
			}
			System.out.println("Closing connection for " + user);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public static void main(String[] args) {
		Server server = new Server(5000);
	}
}
