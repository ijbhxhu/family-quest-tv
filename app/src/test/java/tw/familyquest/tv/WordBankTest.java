package tw.familyquest.tv;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class WordBankTest {
    private JSONObject bank() throws Exception {
        JSONObject root=new JSONObject().put("schemaVersion",1).put("title","Test bank");JSONArray levels=new JSONArray();
        for(String id:WordBank.LEVEL_IDS){
            JSONArray words=new JSONArray();
            words.put(new JSONObject().put("id",id+"-1").put("english","color").put("zh","顏色").put("acceptedAnswers",new JSONArray().put("colour")));
            words.put(new JSONObject().put("id",id+"-2").put("english","book").put("zh","書本").put("hint","讀物"));
            words.put(new JSONObject().put("id",id+"-3").put("english","skip").put("zh","略過").put("enabled",false));
            levels.put(new JSONObject().put("id",id).put("name",id).put("words",words));
        }
        return root.put("levels",levels);
    }
    @Test public void smallBanksAliasesHintsAndDisabledWordsWork() throws Exception {
        WordBank b=new WordBank(bank().toString());assertEquals(2,b.count("beginner"));
        Map<String,WordBank.Word> round=new HashMap<>();for(int i=0;i<2;i++){WordBank.Word w=b.next("beginner");round.put(w.english,w);}
        assertTrue(round.get("color").matches(" COLOUR "));assertFalse(round.get("color").matches("book"));
        assertEquals("讀物",round.get("book").hint());assertEquals("Test bank",b.title);
    }
    @Test public void shuffleDoesNotRepeatAtRoundBoundary() throws Exception {
        WordBank b=new WordBank(bank().toString());String last="";
        for(int i=0;i<40;i++){String now=b.next("beginner").id;assertNotEquals(last,now);last=now;}
    }
    @Test public void longAlternativeFitsSupportedInputLength() throws Exception {
        JSONObject json=new JSONObject().put("id","long-alias").put("english","bike").put("zh","自行車")
                .put("acceptedAnswers",new JSONArray().put("two-wheeled road vehicle"));
        WordBank.Word word=new WordBank.Word(json);
        assertTrue(word.matches("two-wheeled road vehicle"));
        assertEquals(WordBank.MAX_ANSWER_LENGTH,"two-wheeled road vehicle".length());
    }
    @Test public void duplicateSpellingsAppearOncePerRound() throws Exception {
        JSONObject j=bank();JSONArray words=j.getJSONArray("levels").getJSONObject(0).getJSONArray("words");
        words.put(new JSONObject().put("id","alias-row").put("english","COLOR").put("zh","顏色"));
        assertEquals(2,new WordBank(j.toString()).count("beginner"));
    }
    @Test(expected=JSONException.class) public void missingLevelRejected() throws Exception { JSONObject j=bank();j.getJSONArray("levels").remove(0);new WordBank(j.toString()); }
    @Test(expected=JSONException.class) public void duplicateIdRejected() throws Exception { JSONObject j=bank();JSONArray words=j.getJSONArray("levels").getJSONObject(0).getJSONArray("words");words.getJSONObject(1).put("id","beginner-1");new WordBank(j.toString()); }
    @Test(expected=JSONException.class) public void emptyEnabledLevelRejected() throws Exception { JSONObject j=bank();JSONArray words=j.getJSONArray("levels").getJSONObject(0).getJSONArray("words");for(int i=0;i<words.length();i++)words.getJSONObject(i).put("enabled",false);new WordBank(j.toString()); }
    @Test(expected=JSONException.class) public void untypeableAnswerRejected() throws Exception { JSONObject j=bank();j.getJSONArray("levels").getJSONObject(0).getJSONArray("words").getJSONObject(0).put("english","two/three");new WordBank(j.toString()); }
}
