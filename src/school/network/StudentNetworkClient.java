package school.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/** TCP client used by students to communicate with SchoolServer. */
public final class StudentNetworkClient {

    private StudentNetworkClient() {
    }

    public static String sendRequest(String host, int port, String request) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                writer.println(request);

                StringBuilder response = new StringBuilder();
                String line;
                boolean first = true;

                while ((line = reader.readLine()) != null) {
                    if ("END".equals(line)) {
                        break;
                    }
                    if (first && ("OK".equals(line) || "ERROR".equals(line))) {
                        first = false;
                        continue;
                    }
                    first = false;
                    if (!response.isEmpty()) {
                        response.append(System.lineSeparator());
                    }
                    response.append(line);
                }

                return response.toString();
            }
        }
    }
}
