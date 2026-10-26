package tw.familyquest.tv;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One bounded request per connection; no chunking or keep-alive. */
final class HttpRequest {
    String method, path, body;
    final Map<String,String> headers = new HashMap<>();
    static HttpRequest read(InputStream in) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream(); int tail = 0;
        while (true) {
            int b = in.read(); if (b < 0) throw new EOFException();
            header.write(b); tail = (tail << 8) | b;
            if (header.size() > 16384) throw new IOException("Headers too large");
            if (tail == 0x0d0a0d0a) break;
        }
        String[] lines = header.toString(StandardCharsets.US_ASCII.name()).split("\r\n");
        String[] first = lines[0].split(" ", -1);
        if (first.length != 3 || !first[2].equals("HTTP/1.1") || !(first[0].equals("GET") || first[0].equals("POST")))
            throw new IOException("Unsupported request");
        HttpRequest request = new HttpRequest(); request.method = first[0]; request.path = first[1];
        if (!request.path.matches("/[a-zA-Z0-9./_-]*")) throw new IOException("Invalid path");
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':'); if (colon < 1) throw new IOException("Invalid header");
            String key = lines[i].substring(0, colon).toLowerCase(Locale.ROOT);
            if (!key.matches("[a-z0-9-]+") || request.headers.put(key, lines[i].substring(colon+1).trim()) != null)
                throw new IOException("Duplicate or invalid header");
        }
        if (request.headers.containsKey("transfer-encoding") || !request.headers.containsKey("host")) throw new IOException("Invalid framing");
        int length;
        try { length = Integer.parseInt(request.headers.getOrDefault("content-length", "0")); }
        catch (NumberFormatException e) { throw new IOException("Invalid length"); }
        if (length < 0 || length > 8192 || (request.method.equals("GET") && length > 0)) throw new IOException("Invalid length");
        if (request.method.equals("POST") && !request.headers.getOrDefault("content-type", "").equals("application/json"))
            throw new IOException("JSON required");
        byte[] bytes = new byte[length]; int offset = 0;
        while (offset < length) { int n = in.read(bytes, offset, length-offset); if (n < 0) throw new EOFException(); offset += n; }
        request.body = new String(bytes, StandardCharsets.UTF_8); return request;
    }
    boolean validOrigin(String localAddress, int port) {
        String host = localAddress + ":" + port;
        return host.equals(headers.get("host")) && (!headers.containsKey("origin") || ("https://" + host).equals(headers.get("origin")))
                && !"cross-site".equals(headers.get("sec-fetch-site"));
    }
}
