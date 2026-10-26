package tw.familyquest.tv;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline question bank. All content and display labels live in JSON. */
public final class WordBank {
    public static final int MAX_ANSWER_LENGTH = 24;
    public static final String[] LEVEL_IDS = {"beginner", "intermediate", "advanced"};
    public static final class Word {
        public final String id, english, chinese;
        private final String customHint;
        private final List<String> accepted = new ArrayList<>();
        Word(JSONObject json) throws JSONException {
            id = required(json, "id", 80); english = answer(required(json, "english", MAX_ANSWER_LENGTH));
            chinese = required(json, "zh", 64);
            customHint = json.has("hint") ? json.getString("hint") : null;
            if (customHint != null && customHint.length() > 100) throw new JSONException("Hint too long: " + id);
            accepted.add(english);
            JSONArray aliases = json.optJSONArray("acceptedAnswers");
            if (aliases != null) for (int i=0;i<aliases.length();i++) accepted.add(answer(aliases.getString(i)));
        }
        private static String answer(String value) throws JSONException {
            String normalized = Rules.normalize(value);
            if (normalized.isEmpty() || normalized.length()>MAX_ANSWER_LENGTH || !normalized.matches("[a-z .'-]+"))
                throw new JSONException("Answers must be 1–24 English characters, spaces, apostrophes, hyphens or periods");
            return value;
        }
        public boolean matches(String value) {
            for (String candidate : accepted) if (Rules.correct(candidate, value)) return true;
            return false;
        }
        public String hint() {
            if (customHint != null) return customHint;
            String s = Rules.normalize(english);
            return s.charAt(0) + s.substring(1).replaceAll("[a-z]", " _");
        }
    }
    public final String title;
    private final Map<String,List<Word>> levels = new LinkedHashMap<>();
    private final Map<String,String> names = new LinkedHashMap<>();
    private final Random random = new java.security.SecureRandom();
    private final ArrayDeque<Word> deck = new ArrayDeque<>();
    private String currentLevel = "", previousId = "";
    public WordBank(Context context) throws IOException, JSONException { this(read(context)); }
    WordBank(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.getInt("schemaVersion") != 1) throw new JSONException("Unsupported wordlist schemaVersion");
        title = required(root, "title", 40);
        JSONArray source = root.getJSONArray("levels");
        Set<String> ids = new HashSet<>();
        for (int i=0;i<source.length();i++) {
            JSONObject level = source.getJSONObject(i); String levelId = level.getString("id");
            if (!Arrays.asList(LEVEL_IDS).contains(levelId) || levels.containsKey(levelId)) throw new JSONException("Invalid/duplicate level: " + levelId);
            names.put(levelId, required(level,"name",20));
            List<Word> words = new ArrayList<>(); Set<String> spellings = new HashSet<>();
            JSONArray entries = level.getJSONArray("words");
            for (int j=0;j<entries.length();j++) {
                JSONObject entry = entries.getJSONObject(j); Word word = new Word(entry);
                if (!ids.add(word.id)) throw new JSONException("Duplicate word id: " + word.id);
                if (entry.optBoolean("enabled",true) && spellings.add(Rules.normalize(word.english))) words.add(word);
            }
            if (words.isEmpty()) throw new JSONException("Each level needs an enabled question: " + levelId);
            levels.put(levelId, words);
        }
        if (levels.size()!=3) throw new JSONException("Include beginner, intermediate and advanced levels");
    }
    private static String required(JSONObject json, String key, int max) throws JSONException {
        String s=json.getString(key).trim();
        if(s.isEmpty() || s.length()>max) throw new JSONException("Invalid " + key);
        return s;
    }
    private static String read(Context context) throws IOException {
        try (InputStream in = context.getAssets().open("wordlists.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] chunk = new byte[4096]; int n;
            while ((n=in.read(chunk))!=-1) { out.write(chunk,0,n); if(out.size()>4_000_000)throw new IOException("Wordlist exceeds 4 MB"); }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    public String label(String level) { return names.get(level); }
    public int count(String level) { return levels.get(level).size(); }
    public Word next(String level) {
        if (!level.equals(currentLevel)) { deck.clear(); currentLevel=level; previousId=""; }
        if (deck.isEmpty()) {
            List<Word> copy = new ArrayList<>(levels.get(level)); Collections.shuffle(copy,random);
            if (copy.size()>1 && copy.get(0).id.equals(previousId)) Collections.swap(copy,0,1);
            deck.addAll(copy);
        }
        Word word=deck.removeFirst(); previousId=word.id; return word;
    }
}
