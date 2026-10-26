package tw.familyquest.tv;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class HttpRequestTest {
    private HttpRequest parse(String s) throws IOException { return HttpRequest.read(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))); }
    @Test public void sameOriginAllowedButRebindingAndCsrfDenied() throws Exception {
        HttpRequest r=parse("GET /api/state HTTP/1.1\r\nHost: 192.168.1.50:8443\r\nOrigin: https://192.168.1.50:8443\r\n\r\n");
        assertTrue(r.validOrigin("192.168.1.50",8443)); assertFalse(r.validOrigin("192.168.1.51",8443));
        r.headers.put("origin","https://attacker.example"); assertFalse(r.validOrigin("192.168.1.50",8443));
        r.headers.remove("origin"); r.headers.put("sec-fetch-site","cross-site"); assertFalse(r.validOrigin("192.168.1.50",8443));
    }
    @Test public void bodyIsReadToByteLength() throws Exception {
        HttpRequest r=parse("POST /api/login HTTP/1.1\r\nHost: 192.168.1.50:8443\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}"); assertEquals("{}",r.body);
    }
    @Test(expected=IOException.class) public void duplicateLengthRejected() throws Exception { parse("POST /api/login HTTP/1.1\r\nHost: x\r\nContent-Length: 2\r\nContent-Length: 4\r\n\r\n{}"); }
    @Test(expected=IOException.class) public void chunkedRejected() throws Exception { parse("POST /api/login HTTP/1.1\r\nHost: x\r\nTransfer-Encoding: chunked\r\n\r\n"); }
    @Test(expected=IOException.class) public void oversizedBodyRejected() throws Exception { parse("POST /api/login HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\nContent-Length: 9999999\r\n\r\n"); }
    @Test(expected=IOException.class) public void formCsrfRejected() throws Exception { parse("POST /api/stop HTTP/1.1\r\nHost: x\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 2\r\n\r\na="); }
    @Test(expected=IOException.class) public void incompleteBodyRejected() throws Exception { parse("POST /api/login HTTP/1.1\r\nHost: x\r\nContent-Type: application/json\r\nContent-Length: 10\r\n\r\n{}"); }
}
