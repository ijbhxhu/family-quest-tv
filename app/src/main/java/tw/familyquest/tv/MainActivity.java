package tw.familyquest.tv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private FamilyState state;
    private FrameLayout stage;
    private LinearLayout root, keyboard, questionCard;
    private TextView status, feedback, prompt, hint, questionNumber, energyTitle, energyCaption, bankBadge;
    private QuestArt.Slots answerSlots;
    private QuestArt.Gems gems;
    private WordBank.Word word;
    private final StringBuilder answer = new StringBuilder();
    private final Map<String,Button> letterKeys = new HashMap<>();
    private String lastLetter = "q";
    private boolean answered, parentPage, resumed, quizPaused, homePage;
    private int turnNumber;
    private long transitionDue, transitionRemaining;
    private View rewardToast;
    private final List<Dialog> dialogs = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable parentTimeout = () -> { if (parentPage) showHome(); };
    private final Runnable advance = () -> { if (resumed && !quizPaused && word != null && answered) loadQuestion(); };
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (word != null && !state.isOpen()) showHome();
            if (status != null) status.setText(summary());
            updateEnergy();
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); state = FamilyState.get(this);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (state.hasPin() && state.serverEnabled()) startForegroundService(new Intent(this, ParentServerService.class));
        showHome();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); showHome(); }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        if (parentPage) showHome();
        handler.removeCallbacks(ticker); handler.post(ticker);
        resumeAdvance();
    }
    @Override protected void onPause() {
        if(parentPage) closeDialogs();
        resumed=false; pauseAdvance(); handler.removeCallbacks(ticker); super.onPause();
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String value, int size, int color) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("kern"); v.setTypeface(QuestArt.font(this,false)); v.setIncludeFontPadding(false); return v;
    }
    private TextView bold(String value, int size, int color) {
        TextView v=text(value,size,color); v.setTypeface(QuestArt.font(this,true)); return v;
    }
    private LinearLayout vertical() { LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private void gap(LinearLayout target,int height) { target.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height))); }
    private GradientDrawable panel(int color) {
        GradientDrawable bg=new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(15)); bg.setStroke(dp(1),0x244f7a9c); return bg;
    }
    private void reset(boolean quiet) {
        closeDialogs();
        handler.removeCallbacks(advance); handler.removeCallbacks(parentTimeout);
        transitionDue=transitionRemaining=0; quizPaused=false; word=null; answered=false; parentPage=false; homePage=false;
        status=bankBadge=energyTitle=energyCaption=null; gems=null; rewardToast=null;
        stage=new FrameLayout(this); QuestArt.World world=new QuestArt.World(this); world.quiet(quiet);
        stage.addView(world,new FrameLayout.LayoutParams(-1,-1)); setContentView(stage);
    }
    private LinearLayout base(boolean quiet) {
        reset(quiet); root=vertical(); root.setPadding(dp(32),dp(22),dp(32),dp(20));
        stage.addView(root,new FrameLayout.LayoutParams(-1,-1)); return root;
    }
    private void page(String title) {
        reset(true); ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root=vertical(); root.setPadding(dp(36),dp(26),dp(36),dp(26)); scroll.addView(root);
        stage.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        root.addView(bold(title,28,QuestArt.WHITE)); gap(root,16);
    }
    private Button tile(String label, boolean primary, Runnable action) {
        QuestArt.Tile b=new QuestArt.Tile(this,label,primary); b.setId(View.generateViewId());
        b.setOnClickListener(v -> { try { action.run(); } catch(Exception e) { message(e.getMessage()); } }); return b;
    }
    private Button button(String label,LinearLayout parent,Runnable action) {
        Button b=tile(label,false,action);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(parent.getOrientation()==LinearLayout.HORIZONTAL?0:-1,dp(50),parent.getOrientation()==LinearLayout.HORIZONTAL?1:0);
        lp.setMargins(dp(3),dp(4),dp(3),dp(4)); parent.addView(b,lp); return b;
    }
    private LinearLayout row() { LinearLayout r=horizontal(); root.addView(r); return r; }
    private void showDialog(Dialog dialog) { dialogs.add(dialog); dialog.show(); }
    private void closeDialogs() { for(Dialog dialog:new ArrayList<>(dialogs))if(dialog.isShowing())dialog.dismiss(); dialogs.clear(); }
    private void message(String value) { showDialog(new AlertDialog.Builder(this).setMessage(value).setPositiveButton("知道了",null).create()); }
    private String levelLabel() { return state.words.label(state.level()); }
    private String clock() { long s=(state.remaining()+999)/1000; return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60); }
    private String summary() { return "答對 "+state.progress()+" / "+state.questions()+" 題   ·   已存 "+state.bank()/60000+" 分鐘   ·   倒數 "+clock(); }
    private TextView chip(String label) {
        TextView v=bold(label,12,QuestArt.MINT); v.setGravity(Gravity.CENTER); v.setPadding(dp(13),dp(6),dp(13),dp(6));
        v.setBackground(panel(0xcc1b344b)); return v;
    }
    private void showHome() {
        base(false); homePage=true;
        LinearLayout top=horizontal();
        TextView logo=bold("◈  FAMILY QUEST",20,QuestArt.WHITE); logo.setLetterSpacing(.08f);
        top.addView(logo,new LinearLayout.LayoutParams(0,dp(40),1)); logo.setGravity(Gravity.CENTER_VERTICAL);
        Button parent=tile("家長管理",false,this::parentLogin); parent.setTextSize(14);
        top.addView(parent,new LinearLayout.LayoutParams(dp(122),dp(40))); root.addView(top);
        LinearLayout hero=vertical(); hero.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tags=horizontal(); tags.addView(chip(levelLabel()+"冒險"));
        TextView tag=text("  "+state.words.title,13,QuestArt.MUTED); tags.addView(tag); hero.addView(tags); gap(hero,14);
        TextView title=bold(state.isOpen()?"把單字，\n變成冒險時間。":"冒險充電中。\n"+state.startTime()+" 準時出發",39,QuestArt.WHITE);
        title.setLineSpacing(dp(2),1); hero.addView(title); gap(hero,15);
        TextView intro=text("每答對 "+state.questions()+" 題，解鎖 "+state.minutes()+" 分鐘。\n一題接一題，收集你的單字能量。",17,QuestArt.MUTED);
        intro.setLineSpacing(dp(3),1); hero.addView(intro); gap(hero,24);
        LinearLayout actions=horizontal();
        Button start=tile(state.isOpen()?"開始闖關  →":"查看開放時間",true,this::startQuiz); start.setTextSize(20);
        actions.addView(start,new LinearLayout.LayoutParams(dp(235),dp(61)));
        View spacer=new View(this); actions.addView(spacer,new LinearLayout.LayoutParams(dp(12),1));
        Button apps=tile(state.remaining()>0?"繼續使用 App":state.bank()>0?"使用已存時間":"如何取得時間？",false,()->{if(state.bank()>0 || state.remaining()>0)useTime();else message("答對 "+state.questions()+" 題，就能存入 "+state.minutes()+" 分鐘。按「開始闖關」出發吧！");});
        actions.addView(apps,new LinearLayout.LayoutParams(dp(185),dp(61))); hero.addView(actions); start.requestFocus();
        LinearLayout bottom=horizontal(); bottom.setPadding(dp(18),dp(12),dp(18),dp(12)); bottom.setBackground(panel(0xe9152942));
        LinearLayout balance=vertical(); balance.addView(text("我的時間背包",12,QuestArt.MUTED));
        bankBadge=bold("",23,QuestArt.LIME); balance.addView(bankBadge);
        bottom.addView(balance,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout energy=vertical(); energyTitle=bold("",15,QuestArt.WHITE); energy.addView(energyTitle);
        gems=new QuestArt.Gems(this); energy.addView(gems,new LinearLayout.LayoutParams(dp(220),dp(26))); bottom.addView(energy);
        TextView schedule=text("每天 "+state.startTime()+" 後開放\n按開始計時才會扣除時間",12,QuestArt.MUTED); schedule.setGravity(Gravity.END); schedule.setLineSpacing(dp(5),1);
        bottom.addView(schedule,new LinearLayout.LayoutParams(0,-2,1)); root.addView(bottom); updateEnergy();
    }
    private void useTime() {
        if(state.remaining()<=0) state.startSession(); showApps();
    }
    private void startQuiz() {
        if(!state.isOpen()) { message("每天 "+state.startTime()+" 後開放挑戰。休息一下，等冒險時間開始再回來吧！");return; }
        base(true); turnNumber=0; letterKeys.clear();
        LinearLayout header=horizontal();
        Button pause=tile("Ⅱ  暫停",false,this::pauseQuiz); pause.setTextSize(14);
        header.addView(pause,new LinearLayout.LayoutParams(dp(100),dp(37)));
        TextView wordQuest=bold("  WORD QUEST",19,QuestArt.WHITE); wordQuest.setLetterSpacing(.08f);
        header.addView(wordQuest,new LinearLayout.LayoutParams(0,-2,1));
        questionNumber=text("",13,QuestArt.MUTED); header.addView(questionNumber);
        TextView grade=chip(levelLabel()); LinearLayout.LayoutParams gradeLp=new LinearLayout.LayoutParams(-2,-2); gradeLp.setMargins(dp(16),0,0,0); header.addView(grade,gradeLp);
        root.addView(header); gap(root,12);
        LinearLayout content=horizontal(); content.setGravity(Gravity.TOP);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        questionCard=vertical(); questionCard.setPadding(dp(22),dp(17),dp(22),dp(14)); questionCard.setBackground(panel(0xe8142942));
        LinearLayout.LayoutParams mainLp=new LinearLayout.LayoutParams(0,-1,.68f);mainLp.setMargins(0,0,dp(12),0);content.addView(questionCard,mainLp);
        questionCard.addView(text("這個英文怎麼拼？",13,QuestArt.MUTED));
        prompt=bold("",38,QuestArt.WHITE);prompt.setMaxLines(2);prompt.setGravity(Gravity.CENTER_VERTICAL);
        prompt.setAutoSizeTextTypeUniformWithConfiguration(22,38,2,TypedValue.COMPLEX_UNIT_SP);
        questionCard.addView(prompt,new LinearLayout.LayoutParams(-1,0,1));
        answerSlots=new QuestArt.Slots(this);questionCard.addView(answerSlots,new LinearLayout.LayoutParams(-1,dp(52)));
        hint=text("",14,QuestArt.MUTED);questionCard.addView(hint,new LinearLayout.LayoutParams(-1,dp(25)));
        feedback=bold("",15,QuestArt.MINT);feedback.setSingleLine(false);feedback.setMaxLines(2);
        questionCard.addView(feedback,new LinearLayout.LayoutParams(-1,dp(35)));
        LinearLayout reward=vertical();reward.setPadding(dp(18),dp(16),dp(18),dp(13));reward.setBackground(panel(0xe51c3046));
        content.addView(reward,new LinearLayout.LayoutParams(0,-1,.32f));
        reward.addView(text("本輪能量",12,QuestArt.MUTED));gap(reward,7);
        energyTitle=bold("",28,QuestArt.WHITE);reward.addView(energyTitle);
        gems=new QuestArt.Gems(this);reward.addView(gems,new LinearLayout.LayoutParams(-1,dp(39)));
        energyCaption=text("",14,QuestArt.MUTED);energyCaption.setLineSpacing(dp(3),1);reward.addView(energyCaption);
        reward.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));
        bankBadge=bold("",19,QuestArt.LIME);reward.addView(bankBadge);gap(reward,6);
        TextView help=text("按返回可暫停、使用已存時間",11,QuestArt.MUTED);reward.addView(help);
        gap(root,12);
        keyboard=vertical();root.addView(keyboard);
        ArrayList<LinearLayout> keyRows=new ArrayList<>();
        String[] layout={"QWERTYUIOP","ASDFGHJKL","ZXCVBNM"};
        for(int index=0;index<layout.length;index++) {
            LinearLayout keys=horizontal();keys.setPadding(dp(index==1?25:index==2?72:0),0,dp(index==1?25:index==2?72:0),0);
            keyboard.addView(keys,new LinearLayout.LayoutParams(-1,dp(42)));keyRows.add(keys);
            for(char c:layout[index].toCharArray()) {
                String key=String.valueOf(c).toLowerCase(Locale.ROOT);Button b=keyButton(String.valueOf(c),keys,1,false,()->type(key));
                letterKeys.put(key,b);b.setOnFocusChangeListener((v,focused)->{
                    b.setTextColor(focused?QuestArt.INK:QuestArt.WHITE);b.animate().scaleX(focused?1.035f:1).scaleY(focused?1.035f:1).setDuration(130).start();
                    if(focused)lastLetter=key;
                });
                if(index==0)b.setNextFocusUpId(pause.getId());
            }
            edgeLock(keys);
        }
        LinearLayout tools=horizontal();keyboard.addView(tools,new LinearLayout.LayoutParams(-1,dp(42)));keyRows.add(tools);
        keyButton("空格",tools,2,false,()->type(" "));keyButton("'",tools,1,false,()->type("'"));keyButton("-",tools,1,false,()->type("-"));
        keyButton(".",tools,1,false,()->type("."));
        keyButton("⌫  刪除",tools,2,false,this::erase);keyButton("送出  ↵",tools,3,true,this::submit);edgeLock(tools);
        for(int i=0;i<tools.getChildCount();i++)tools.getChildAt(i).setNextFocusDownId(tools.getChildAt(i).getId());
        pause.setNextFocusLeftId(pause.getId());pause.setNextFocusUpId(pause.getId());pause.setNextFocusRightId(pause.getId());pause.setNextFocusDownId(letterKeys.get("q").getId());
        TextView footer=text("方向鍵選字母   ·   OK 輸入   ·   送出後自動接下一題",13,QuestArt.MUTED);footer.setGravity(Gravity.CENTER);
        root.addView(footer,new LinearLayout.LayoutParams(-1,dp(22)));
        loadQuestion();letterKeys.get("q").requestFocus();
    }
    private Button keyButton(String label,LinearLayout row,float weight,boolean primary,Runnable action) {
        Button b=tile(label,primary,action);b.setTextSize(17);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,weight);lp.setMargins(dp(3),dp(2),dp(3),dp(2));row.addView(b,lp);return b;
    }
    private void edgeLock(LinearLayout row) {
        if(row.getChildCount()==0)return;View first=row.getChildAt(0),last=row.getChildAt(row.getChildCount()-1);
        first.setNextFocusLeftId(first.getId());last.setNextFocusRightId(last.getId());
    }
    private void loadQuestion() {
        if(!state.isOpen()){showHome();return;}
        handler.removeCallbacks(advance);transitionDue=transitionRemaining=0;
        if(rewardToast!=null){stage.removeView(rewardToast);rewardToast=null;}
        word=state.words.next(state.level());answered=false;answer.setLength(0);turnNumber++;
        prompt.setText(word.chinese);prompt.setContentDescription("word:"+word.id+" "+word.chinese);
        hint.setText("提示  "+word.hint());questionNumber.setText("第 "+String.format(Locale.ROOT,"%02d",turnNumber)+" 題");
        feedback.setText("慢慢想，你可以的。");feedback.setTextColor(QuestArt.MUTED);
        answerSlots.show("",word.english,0);keyboard.setAlpha(1);updateEnergy();
        questionCard.setAlpha(.35f);questionCard.setTranslationY(dp(5));
        questionCard.animate().alpha(1).translationY(0).setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
        Button focus=letterKeys.get(lastLetter);if(focus!=null)focus.requestFocus();
    }
    private void updateEnergy() {
        if(bankBadge!=null)bankBadge.setText(state.remaining()>0 ? "使用中  "+clock() : "◈  "+state.bank()/60000+" 分鐘已存入");
        if(energyTitle!=null)energyTitle.setText(homePage ? "單字能量  "+state.progress()+" / "+state.questions() : state.progress()+" / "+state.questions());
        if(gems!=null)gems.progress(state.progress(),state.questions());
        if(energyCaption!=null)energyCaption.setText("再答對 "+(state.questions()-state.progress())+" 題\n獲得 "+state.minutes()+" 分鐘遊玩時間");
    }
    private void type(String value) {
        if(word==null || answered || quizPaused)return;
        if(answer.length()<WordBank.MAX_ANSWER_LENGTH){answer.append(value);answerSlots.show(answer.toString(),word.english,0);}
    }
    private void erase() {
        if(word!=null && !answered && !quizPaused && answer.length()>0){answer.deleteCharAt(answer.length()-1);answerSlots.show(answer.toString(),word.english,0);}
    }
    private void submit() {
        if(answered || word==null || quizPaused)return;
        if(answer.toString().trim().isEmpty()){feedback.setText("先輸入英文，再按送出。");feedback.setTextColor(QuestArt.PEACH);return;}
        if(!state.isOpen()){showHome();return;}
        answered=true;boolean correct=word.matches(answer.toString());boolean earned=false;long before=state.bank();
        if(correct){earned=state.earnCorrect();feedback.setText(earned?(state.bank()>before?"能量集滿！時間獎勵已放進背包。":"本輪完成！背包已滿，先使用時間吧！"):"答對了！+1 單字能量");feedback.setTextColor(QuestArt.MINT);}
        else{feedback.setText("正確拼法："+word.english+"  ·  記住它，下題再挑戰！");feedback.setTextColor(QuestArt.PEACH);}
        answerSlots.show(answer.toString(),word.english,correct?1:2);keyboard.setAlpha(.65f);updateEnergy();
        if(earned)celebrate((int)((state.bank()-before)/60000));
        transitionRemaining=earned?1900:correct?1000:3000;resumeAdvance();
    }
    private void celebrate(int awarded) {
        LinearLayout toast=horizontal();toast.setPadding(dp(18),dp(10),dp(24),dp(10));toast.setBackground(panel(0xff253b45));
        toast.setElevation(dp(18));toast.addView(new QuestArt.Trophy(this),new LinearLayout.LayoutParams(dp(90),dp(90)));
        LinearLayout words=vertical();words.addView(text("任務達成！",14,QuestArt.MINT));gap(words,5);
        words.addView(bold(awarded>0?"+"+awarded+" 分鐘":"背包已滿",34,QuestArt.LIME));gap(words,4);words.addView(text(awarded>0?"已存入背包，繼續下一題":"最多 120 分鐘，先使用時間吧",12,QuestArt.WHITE));toast.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(345),dp(120),Gravity.TOP|Gravity.END);lp.setMargins(0,dp(83),dp(44),0);
        stage.addView(toast,lp);rewardToast=toast;toast.setAlpha(0);toast.setScaleX(.85f);toast.setScaleY(.85f);
        toast.animate().alpha(1).scaleX(1).scaleY(1).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
        toast.announceForAccessibility(awarded>0?"任務達成，已存入 "+awarded+" 分鐘":"任務達成，時間背包已滿");
    }
    private void pauseAdvance() {
        handler.removeCallbacks(advance);
        if(transitionDue>0)transitionRemaining=Math.max(200,transitionDue-SystemClock.elapsedRealtime());transitionDue=0;
    }
    private void resumeAdvance() {
        if(!resumed || quizPaused || word==null || !answered || transitionRemaining<=0)return;
        handler.removeCallbacks(advance);transitionDue=SystemClock.elapsedRealtime()+transitionRemaining;handler.postDelayed(advance,transitionRemaining);
    }
    private void pauseQuiz() {
        if(word==null || quizPaused)return;quizPaused=true;pauseAdvance();
        AlertDialog d=new AlertDialog.Builder(this).setTitle("冒險暫停").setMessage("已答對的能量會保留。時間背包："+state.bank()/60000+" 分鐘。")
            .setPositiveButton("繼續闖關",(dialog,which)->{})
            .setNeutralButton(state.remaining()>0?"繼續使用 App":"使用已存時間",(dialog,which)->{try{useTime();}catch(Exception e){message(e.getMessage());}})
            .setNegativeButton("回大廳",(dialog,which)->showHome()).create();
        d.setOnDismissListener(dialog->{quizPaused=false;resumeAdvance();});showDialog(d);
        d.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(state.bank()>0 || state.remaining()>0);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code=event.getKeyCode();boolean down=event.getAction()==KeyEvent.ACTION_DOWN;
        if(word!=null && !quizPaused){
            if(code==KeyEvent.KEYCODE_BACK || code==KeyEvent.KEYCODE_HOME || code==KeyEvent.KEYCODE_VOLUME_UP || code==KeyEvent.KEYCODE_VOLUME_DOWN || code==KeyEvent.KEYCODE_VOLUME_MUTE)return super.dispatchKeyEvent(event);
            if(answered)return true;
            if((code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_NUMPAD_ENTER || code==KeyEvent.KEYCODE_DPAD_CENTER) && event.getRepeatCount()>0)return true;
            if(code==KeyEvent.KEYCODE_DEL){if(down)erase();return true;}
            if(code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_NUMPAD_ENTER){if(down)submit();return true;}
            int unicode=event.getUnicodeChar();
            if((unicode>='a'&&unicode<='z') || (unicode>='A'&&unicode<='Z') || unicode==' ' || unicode=='\'' || unicode=='-' || unicode=='.'){
                if(down)type(String.valueOf((char)unicode).toLowerCase(Locale.ROOT));return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() { if(word!=null)pauseQuiz();else if(!homePage)showHome();else super.onBackPressed(); }
    private void showApps() {
        page("冒險時間，開始！");status=text(summary(),16,QuestArt.MUTED);root.addView(status);gap(root,16);
        ArrayList<AppCatalog.App> apps=new ArrayList<>(AppCatalog.list(this));
        apps.sort(Comparator.comparingInt(a->state.restricted().contains(a.pkg)?0:1));
        LinearLayout current=null;
        for(int i=0;i<apps.size();i++){
            if(i%2==0){current=row();}
            AppCatalog.App app=apps.get(i);button(app.label+(state.restricted().contains(app.pkg)?"  ·  使用已解鎖時間":""),current,()->AppCatalog.launch(this,app.pkg));
        }
        gap(root,16);button("回大廳",root,this::showHome);
    }
    private EditText input(String hint, String value, boolean pin) {
        EditText v = new EditText(this); v.setHint(hint); v.setText(value); v.setTextSize(19); v.setSingleLine(true);
        v.setInputType(InputType.TYPE_CLASS_NUMBER | (pin ? InputType.TYPE_NUMBER_VARIATION_PASSWORD : 0)); return v;
    }
    private void parentLogin() {
        if (!state.hasPin()) { setupPin(); return; }
        EditText pin = input("家長 PIN", "", true);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("家長管理").setView(pin).setNegativeButton("取消", null).setPositiveButton("登入", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (state.verifyPin(pin.getText().toString())) { d.dismiss(); showParent(); }
            else pin.setError("PIN 錯誤或嘗試過多，請稍後再試。");
        })); showDialog(d);
    }
    private void setupPin() {
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL); fields.setPadding(dp(20), 0, dp(20), 0);
        EditText pin = input("設定 4–12 位家長 PIN", "", true), again = input("再輸入一次", "", true); fields.addView(pin); fields.addView(again);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("設定家長 PIN").setView(fields).setNegativeButton("取消", null).setPositiveButton("儲存", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                if (!pin.getText().toString().equals(again.getText().toString())) throw new IllegalArgumentException("兩次 PIN 不一致。");
                state.setPin(pin.getText().toString()); d.dismiss(); showParent();
            } catch (IllegalArgumentException e) { pin.setError(e.getMessage()); }
        })); showDialog(d);
    }
    private void showParent() {
        page("家長管理"); parentPage = true;
        handler.removeCallbacks(parentTimeout); handler.postDelayed(parentTimeout, 5 * 60_000L);
        root.addView(text("手機與電視需連相同區網。管理頁採 HTTPS，本機憑證會讓瀏覽器顯示憑證提示。\n無障礙服務只用來判斷目前 App；不讀取畫面文字或按鍵。", 16, 0xffc4d2e8));
        button("設定題數、分鐘與受限 App", root, this::settingsDialog);
        button("開啟系統無障礙設定", root, () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button("啟動手機管理頁", root, () -> {
            state.serverEnabled(true);
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            startForegroundService(new Intent(this, ParentServerService.class));
            handler.postDelayed(() -> message(ParentServerService.addresses(this) + "\n以家長 PIN 登入。\n" + ParentServerService.statusText), 1200);
        });
        button("查看手機管理網址", root, () -> message(ParentServerService.addresses(this) + "\n" + ParentServerService.statusText));
        button("停止手機管理並登出所有手機", root, () -> { state.serverEnabled(false); stopService(new Intent(this, ParentServerService.class)); state.revokeTokens(); message("手機管理已停止。"); });
        LinearLayout actions = row(); button("獎勵 10 分鐘", actions, () -> { state.bonus(10); message("已存入 10 分鐘。"); });
        button("立即結束倒數", actions, () -> { state.stopSession(); if (GateService.instance != null) GateService.instance.recheck(); message("已結束本次倒數。"); });
        button("更換家長 PIN", root, this::setupPin); button("登出家長管理", root, this::showHome);
    }
    private void settingsDialog() {
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.VERTICAL); fields.setPadding(dp(20), dp(8), dp(20), dp(8));
        EditText questions = input("答對幾題", String.valueOf(state.questions()), false), minutes = input("換幾分鐘", String.valueOf(state.minutes()), false);
        fields.addView(text("答對題數 / 獎勵分鐘", 17, Color.WHITE)); fields.addView(questions); fields.addView(minutes);
        fields.addView(text("每日開放時間（24 小時制，小時 / 分鐘）", 17, Color.WHITE));
        EditText openHour = input("小時", state.startTime().substring(0,2), false), openMinute = input("分鐘", state.startTime().substring(3), false);
        fields.addView(openHour); fields.addView(openMinute);
        Spinner levels = new Spinner(this); levels.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{state.words.label("beginner"), state.words.label("intermediate"), state.words.label("advanced")}));
        String[] ids = {"beginner", "intermediate", "advanced"}; levels.setSelection(Arrays.asList(ids).indexOf(state.level())); fields.addView(levels);
        Switch enabled = new Switch(this); enabled.setText("啟用 App 使用限制"); enabled.setChecked(state.enabled()); fields.addView(enabled);
        Map<String,CheckBox> apps = new LinkedHashMap<>(); Set<String> selected = state.restricted();
        for (AppCatalog.App app : AppCatalog.list(this)) { CheckBox b = new CheckBox(this); b.setText(app.label); b.setChecked(selected.contains(app.pkg)); fields.addView(b); apps.put(app.pkg, b); }
        ScrollView scroll = new ScrollView(this); scroll.addView(fields);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("使用規則").setView(scroll).setNegativeButton("取消", null).setPositiveButton("儲存", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                Set<String> chosen = new HashSet<>(); for (Map.Entry<String,CheckBox> e : apps.entrySet()) if (e.getValue().isChecked()) chosen.add(e.getKey());
                String startTime = String.format(Locale.ROOT, "%02d:%02d", Integer.parseInt(openHour.getText().toString()), Integer.parseInt(openMinute.getText().toString()));
                state.configure(Integer.parseInt(questions.getText().toString()), Integer.parseInt(minutes.getText().toString()), ids[levels.getSelectedItemPosition()], startTime, chosen, enabled.isChecked());
                d.dismiss(); showParent();
            } catch (Exception e) { message(e.getMessage()); }
        })); showDialog(d);
    }
}
