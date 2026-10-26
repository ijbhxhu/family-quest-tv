package tw.familyquest.tv;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.os.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import org.json.*;

public final class ParentServerService extends Service {
    // 8443 is already owned by the TV's casting service on many Google TV sets.
    public static final int PORT = 18443;
    public static volatile String statusText = "手機管理尚未啟動";
    private volatile SSLServerSocket server;
    private volatile boolean running;
    private Thread listener;
    private final ThreadPoolExecutor clients = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(4));
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1);
    private final Set<Socket> connections = ConcurrentHashMap.newKeySet();
    private final Handler main = new Handler(Looper.getMainLooper());
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        deadlines.setRemoveOnCancelPolicy(true);
        if (!FamilyState.get(this).hasPin()) { stopSelf(); return START_NOT_STICKY; }
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("parent", "手機家長管理", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, "parent").setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Family Quest 家長管理已啟動").setContentText("同區網 HTTPS 管理；可在家長頁面停止")
                .setContentIntent(open).setOngoing(true).build();
        startForeground(1, notification);
        if (!running) { running = true; listener = new Thread(this::listen, "FamilyQuest-HTTPS"); listener.start(); }
        return START_NOT_STICKY;
    }
    private void listen() {
        try {
            SSLContext tls = LocalTls.create();
            server = (SSLServerSocket) tls.getServerSocketFactory().createServerSocket(PORT, 8, InetAddress.getByName("0.0.0.0"));
            if (!running) { server.close(); return; }
            server.setEnabledProtocols(new String[]{"TLSv1.2"});
            statusText = "手機管理已啟動。憑證 SHA-256：\n" + LocalTls.fingerprint();
            while (running) {
                Socket socket = server.accept();
                if (!socket.getInetAddress().isSiteLocalAddress() && !socket.getInetAddress().isLoopbackAddress()) { socket.close(); continue; }
                connections.add(socket);
                if(!running){closeConnection(socket);break;}
                ScheduledFuture<?> deadline=null;
                try {
                    // Includes queue wait, TLS handshake and the entire HTTP request.
                    deadline=deadlines.schedule(()->closeConnection(socket),8,TimeUnit.SECONDS);
                    ScheduledFuture<?> pending=deadline;
                    clients.execute(()->{try{handle(socket);}finally{pending.cancel(false);closeConnection(socket);}});
                } catch (RejectedExecutionException e) {
                    if(deadline!=null)deadline.cancel(false);closeConnection(socket);
                }
            }
        } catch (Exception e) {
            if (running) { statusText = "手機管理啟動失敗：" + e.getClass().getSimpleName(); stopSelf(); }
        }
    }
    private void closeConnection(Socket socket) {
        connections.remove(socket);
        try{socket.close();}catch(IOException ignored){}
    }
    private void handle(Socket socket) {
        try (Socket close = socket) {
            socket.setSoTimeout(4000);
            HttpRequest req = HttpRequest.read(socket.getInputStream());
            // Public static assets can be opened from a message/link. State and actions stay same-origin.
            if (!(socket.getLocalAddress().getHostAddress() + ":" + PORT).equals(req.headers.get("host"))) { reply(socket,403,"application/json","{\"error\":\"來源不符\"}"); return; }
            if (req.method.equals("GET") && (req.path.equals("/") || req.path.equals("/app.js") || req.path.equals("/style.css"))) {
                String asset = req.path.equals("/") ? "parent.html" : req.path.substring(1);
                String type = req.path.endsWith(".js") ? "text/javascript" : req.path.endsWith(".css") ? "text/css" : "text/html";
                reply(socket, 200, type, asset(asset)); return;
            }
            if (!req.validOrigin(socket.getLocalAddress().getHostAddress(), PORT)) { reply(socket, 403, "application/json", "{\"error\":\"來源不符\"}"); return; }
            FamilyState state = FamilyState.get(this);
            if (req.method.equals("POST") && req.path.equals("/api/login")) {
                try { String token = state.login(new JSONObject(req.body).getString("pin")); reply(socket, 200, "application/json", new JSONObject().put("token", token).toString()); }
                catch (Exception e) { reply(socket, 401, "application/json", new JSONObject().put("error", "PIN 錯誤或嘗試過多；請稍後再試。").toString()); }
                return;
            }
            String bearer = req.headers.getOrDefault("authorization", "");
            if (!bearer.startsWith("Bearer ") || !state.authenticated(bearer.substring(7))) {
                reply(socket, 401, "application/json", "{\"error\":\"請重新登入\"}"); return;
            }
            FutureTask<String> task = new FutureTask<>(() -> {
                if (!running || !state.authenticated(bearer.substring(7))) throw new IllegalArgumentException("請重新登入");
                return api(req);
            }); main.post(task);
            try { reply(socket, 200, "application/json", task.get(4, TimeUnit.SECONDS)); }
            catch (ExecutionException e) { reply(socket, 400, "application/json", new JSONObject().put("error", e.getCause().getMessage()).toString()); }
            catch (TimeoutException e) { task.cancel(false); reply(socket, 503, "application/json", "{\"error\":\"電視忙碌，請稍後再試\"}"); }
        } catch (Exception ignored) { /* Malformed/slow requests are closed, without logging credentials. */ }
    }
    private String api(HttpRequest req) throws Exception {
        FamilyState s = FamilyState.get(this);
        if (req.path.equals("/api/state") && req.method.equals("GET")) return snapshot().toString();
        if (!req.method.equals("POST")) throw new IllegalArgumentException("不支援的操作");
        JSONObject data = new JSONObject(req.body);
        switch (req.path) {
            case "/api/config": {
                Set<String> selected = new HashSet<>(); JSONArray apps = data.getJSONArray("apps");
                for (int i=0; i<apps.length(); i++) selected.add(apps.getString(i));
                s.configure(data.getInt("questions"), data.getInt("minutes"), data.getString("level"), data.optString("startTime", s.startTime()), selected, data.getBoolean("enabled")); break;
            }
            case "/api/bonus": s.bonus(data.getInt("minutes")); break;
            case "/api/start": s.startSession(); break;
            case "/api/stop": s.stopSession(); if (GateService.instance != null) GateService.instance.recheck(); break;
            case "/api/launch":
                if (GateService.instance == null) throw new IllegalArgumentException("請先在電視開啟 Family Quest 無障礙服務，才能遙控開啟 App。");
                AppCatalog.launch(GateService.instance, data.getString("package")); break;
            case "/api/home":
                if (GateService.instance == null) throw new IllegalArgumentException("電視尚未開啟 Family Quest 無障礙服務。");
                GateService.instance.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME); break;
            case "/api/volume": {
                int direction = data.getInt("direction"); if (direction != 1 && direction != -1) throw new IllegalArgumentException("音量方向無效");
                ((AudioManager)getSystemService(AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI); break;
            }
            case "/api/logout": s.revokeTokens(); break;
            default: throw new IllegalArgumentException("不支援的操作");
        }
        return snapshot().toString();
    }
    private JSONObject snapshot() throws JSONException {
        FamilyState s = FamilyState.get(this); JSONArray apps = new JSONArray(), levels = new JSONArray();
        for(String id:WordBank.LEVEL_IDS) levels.put(new JSONObject().put("id",id).put("name",s.words.label(id)).put("count",s.words.count(id)));
        for (AppCatalog.App app : AppCatalog.list(this)) apps.put(new JSONObject().put("package", app.pkg).put("label", app.label).put("restricted", s.restricted().contains(app.pkg)));
        return new JSONObject().put("questions", s.questions()).put("minutes", s.minutes()).put("level", s.level()).put("startTime", s.startTime()).put("openNow", s.isOpen())
                .put("progress", s.progress()).put("bankMinutes", s.bank()/60_000).put("remainingSeconds", (s.remaining()+999)/1000)
                .put("enabled", s.enabled()).put("serviceConnected", GateService.instance != null).put("apps", apps).put("levels",levels).put("wordlistTitle",s.words.title);
    }
    private String asset(String name) throws IOException {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] bytes = new byte[4096]; int n;
            while ((n = in.read(bytes)) != -1) out.write(bytes,0,n); return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    private void reply(Socket socket, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + (status == 200 ? "OK" : "Error") + "\r\nContent-Type: " + type + "; charset=utf-8\r\nContent-Length: " + bytes.length
                + "\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY"
                + "\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'\r\n\r\n";
        OutputStream out = socket.getOutputStream(); out.write(headers.getBytes(StandardCharsets.US_ASCII)); out.write(bytes); out.flush();
    }
    public static String addresses(Context context) {
        StringBuilder out = new StringBuilder();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) continue;
                for (InetAddress a : Collections.list(nic.getInetAddresses())) if (a instanceof Inet4Address && a.isSiteLocalAddress()) out.append("https://").append(a.getHostAddress()).append(':').append(PORT).append('\n');
            }
        } catch (SocketException ignored) { }
        return out.length() > 0 ? out.toString() : "目前沒有可用的區網位址。";
    }
    @Override public void onDestroy() {
        running = false; FamilyState.get(this).revokeTokens(); clients.shutdownNow();
        try { if (server != null) server.close(); } catch (IOException ignored) { }
        for(Socket socket:connections)closeConnection(socket);
        deadlines.shutdownNow();
        if (!statusText.startsWith("手機管理啟動失敗")) statusText = "手機管理已停止"; super.onDestroy();
    }
}
