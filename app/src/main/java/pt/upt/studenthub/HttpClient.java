package pt.upt.studenthub;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Cliente HTTP partilhado, com timeouts, cancelamento e libertação da ligação. */
public final class HttpClient {

    static final int TIMEOUT_MS = 10_000;
    private HttpClient() { }

    /** Erro HTTP que preserva o código devolvido pelo servidor. */
    public static final class HttpStatusException extends IOException {
        public final int code;
        /** Guarda o código HTTP para a Activity apresentar uma mensagem precisa. */
        public HttpStatusException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }
    /** Envia um GET */
    public static String get(String url) throws IOException {
        checkInterrupted();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET"); // HTTP method
            conn.setConnectTimeout(TIMEOUT_MS); // max time to open the connection
            conn.setReadTimeout(TIMEOUT_MS); // max time waiting for data
            conn.setRequestProperty("Accept", "application/json"); // request header
            checkInterrupted();
            int code = conn.getResponseCode(); // sends the request, waits for the status
            checkInterrupted();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new HttpStatusException(code);
            }
            // try-with-resources: the reader (and the stream) are always closed
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null) {
                    checkInterrupted();
                    body.append(line);
                }
            }
            checkInterrupted();
            return body.toString();
        } finally {
            conn.disconnect(); // always release the connection
        }
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Request cancelled");
        }
    }
}
