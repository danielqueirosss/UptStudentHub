package pt.upt.studenthub;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
/**
 * Minimal HTTP client based on HttpURLConnection (built into Android, no extra library).
 * BLOCKING code: it must only be called from a background thread.
 */
public final class HttpClient {
    /** Maximum time to connect and to wait for data (milliseconds). */
    static final int TIMEOUT_MS = 10_000;
    private HttpClient() { }
    /** Thrown when the server answers, but with a status code other than 200 OK. */
    public static final class HttpStatusException extends IOException {
        public final int code;
        public HttpStatusException(int code) {
            super("HTTP " + code);
            this.code = code;
        }
    }
    /** Sends a GET request and returns the response body as text. */
    public static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET"); // HTTP method
            conn.setConnectTimeout(TIMEOUT_MS); // max time to open the connection
            conn.setReadTimeout(TIMEOUT_MS); // max time waiting for data
            conn.setRequestProperty("Accept", "application/json"); // request header
            int code = conn.getResponseCode(); // sends the request, waits for the status
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
                    if (Thread.currentThread().isInterrupted()) { // cooperative cancellation (Week 6)
                        throw new InterruptedIOException("Request cancelled");
                    }
                    body.append(line);
                }
            }
            return body.toString();
        } finally {
            conn.disconnect(); // always release the connection
        }
    }
}
