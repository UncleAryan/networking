import java.net.*;
import java.io.*;

public class Client {
	// Reads keys one at a time and prints * for each, since Java can't do this on a Windows console by itself
	private static final String MASK_SCRIPT = "try{$p='';while(($k=[Console]::ReadKey($true)).Key -ne 'Enter'){"
			+ "if($k.Key -eq 'Backspace'){if($p){$p=$p.Remove($p.Length-1);[Console]::Error.Write(('{0} {0}' -f [char]8))}}"
			+ "else{$p+=$k.KeyChar;[Console]::Error.Write('*')}};[Console]::Error.WriteLine();[Console]::Out.Write($p)}catch{exit 1}";

	private Socket socket = null;
	private DataOutputStream out = null;
	private DataInputStream in = null;
	private BufferedReader input = null;

	public Client(String address, int port) {
		try {
			socket = new Socket(address, port);
			input = new BufferedReader(new InputStreamReader(System.in));
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

			System.out.println(in.readUTF());
			boolean loggedIn = false;
			for (int tries = 1; tries <= 3 && !loggedIn; tries++) {
				out.writeUTF(readLogin());
				System.out.print("Passwd: ");
				out.writeUTF(readPassword());
				loggedIn = in.readBoolean();
				if (!loggedIn) System.out.println("Login incorrect (" + tries + " of 3)\n");
			}

			if (loggedIn) {
				System.out.println(in.readUTF());
				String line = "";
				while (!line.equals("Over")) {
					System.out.print("Enter a message (\"Over\" to quit): ");
					line = input.readLine();
					out.writeUTF(line);
				}
			} else {
				System.out.println("Too many failed attempts");
			}
			System.out.println("Closing connection");

			socket.close();
			out.close();
			input.close();
		} catch (IOException | InterruptedException i) {
			System.out.println(i);
		}
	}

	private String readLogin() throws IOException {
		while (true) {
			System.out.print("Login: ");
			String login = input.readLine();
			if (login.length() <= 8) return login;
			System.out.println("Login can be up to 8 characters");
		}
	}

	private String readPassword() throws IOException, InterruptedException {
		System.out.flush();
		if (System.getProperty("os.name").startsWith("Windows")) {
			Process ps = new ProcessBuilder("powershell", "-NoProfile", "-Command", MASK_SCRIPT)
					.redirectInput(ProcessBuilder.Redirect.INHERIT)
					.redirectError(ProcessBuilder.Redirect.INHERIT)
					.start();
			String pass = new String(ps.getInputStream().readAllBytes());
			if (ps.waitFor() == 0) return pass;
		} else if (stty("-echo -icanon min 1") == 0) {
			// ponytail: Ctrl+C here leaves the terminal without echo, run "reset" to fix it
			try {
				StringBuilder pass = new StringBuilder();
				for (int c = input.read(); c != '\n' && c != '\r' && c != -1; c = input.read()) {
					if (c == 127 || c == '\b') {
						if (pass.length() > 0) {
							pass.setLength(pass.length() - 1);
							System.out.print("\b \b");
						}
					} else {
						pass.append((char) c);
						System.out.print('*');
					}
					System.out.flush();
				}
				return pass.toString();
			} finally {
				stty("echo icanon");
				System.out.println();
			}
		}
		// No real terminal (like the IntelliJ run window), so the password can't be masked
		return input.readLine();
	}

	private static int stty(String args) throws IOException, InterruptedException {
		return new ProcessBuilder("sh", "-c", "stty " + args + " < /dev/tty").start().waitFor();
	}

	public static void main(String[] args) {
		Client client = new Client(args.length > 0 ? args[0] : "localhost", 5000);
	}
}
