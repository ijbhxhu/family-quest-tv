package tw.familyquest.tv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;

/** Small, offline game surfaces. Artwork is static; only focused controls and feedback animate. */
final class QuestArt {
    static final int INK = 0xff101e32, WHITE = 0xfff6f8ff, MUTED = 0xffa6b9d1;
    static final int LIME = 0xffc6f36a, MINT = 0xff81e9db, BLUE = 0xff263d5f, PEACH = 0xffffbd96;
    private static Typeface regular, heavy;
    static Typeface font(Context c, boolean bold) {
        if(regular==null){regular=c.getResources().getFont(R.font.quest_font);heavy=Typeface.create(regular,Typeface.BOLD);}
        return bold?heavy:regular;
    }
    static float d(Context c, float n) { return n * c.getResources().getDisplayMetrics().density; }

    static final class World extends View {
        private final Bitmap image;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private boolean quiet;
        World(Context c) {
            super(c); Bitmap custom=null;
            try(java.io.InputStream in=c.getAssets().open("wallpaper.jpg")){custom=BitmapFactory.decodeStream(in);}catch(java.io.IOException ignored){}
            image=custom; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        void quiet(boolean value) { quiet = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            canvas.drawColor(INK);
            p.setShader(null); p.setAlpha(255);
            if(image!=null){
                float scale=Math.max((float)w/image.getWidth(),(float)h/image.getHeight());
                float bw=image.getWidth()*scale,bh=image.getHeight()*scale;
                canvas.drawBitmap(image,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),p);
            } else {
                p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff13263f,0xff214d60,0xff173846},null,Shader.TileMode.CLAMP));
                canvas.drawRect(0,0,w,h,p);p.setShader(null);
                canvas.save();canvas.scale(w/960f,h/540f);
                p.setColor(0x335fe3b6);canvas.drawCircle(745,200,142,p);
                p.setColor(0x223888b7);canvas.drawCircle(745,200,185,p);
                cube(canvas,725,295,118,78,0xff8cbf7b,0xff365e63,0xff254458);
                cube(canvas,638,255,53,38,0xffa8d997,0xff547f6a,0xff39606a);
                cube(canvas,815,235,62,50,0xff80b8a5,0xff3b7c80,0xff2e596a);
                cube(canvas,735,170,41,58,LIME,0xff81b35a,0xff4b845c);
                cube(canvas,872,334,40,30,0xff93c9cb,0xff397287,0xff2b5269);
                cube(canvas,602,370,29,24,0xffaac49a,0xff4f7870,0xff35596a);
                cube(canvas,848,132,13,16,MINT,0xff44b3aa,0xff358785);
                cube(canvas,631,143,10,12,LIME,0xff83b769,0xff508566);
                p.setColor(0xffa3d1cc);
                for(int i=0;i<17;i++)canvas.drawCircle(555+(i*79)%350,74+(i*47)%300,i%3==0?2:1,p);
                canvas.restore();
            }
            p.setShader(new LinearGradient(0, 0, w, h*.25f,
                    quiet ? new int[]{0xf50a1628,0xf00c1930,0xe30c1930} : new int[]{0xff091729,0xef10203a,0x3513223a},
                    new float[]{0,.48f,1}, Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p);
            p.setShader(new LinearGradient(0,h*.55f,0,h, new int[]{0x00101c30,0xff101c30}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,p); p.setShader(null);
            if (quiet) {
                p.setColor(0x0b96c7e8); p.setStrokeWidth(d(getContext(),1));
                float gap = d(getContext(),36);
                for(float x=-h; x<w+h; x+=gap) canvas.drawLine(x,0,x+h,h,p);
            }
        }
        private void cube(Canvas c,float x,float y,float size,float height,int top,int left,int right){
            float half=size*.46f;
            polygon(c,top,x,y-half,x+size,y,x,y+half,x-size,y);
            polygon(c,left,x-size,y,x,y+half,x,y+half+height,x-size,y+height);
            polygon(c,right,x,y+half,x+size,y,x+size,y+height,x,y+half+height);
        }
        private void polygon(Canvas c,int color,float... points){
            Path path=new Path();path.moveTo(points[0],points[1]);
            for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();p.setColor(color);c.drawPath(path,p);
        }
    }

    static final class Tile extends Button {
        final int face;
        Tile(Context c, String label, boolean primary) {
            super(c); face = primary ? LIME : BLUE;
            setText(label); setAllCaps(false); setTextSize(17); setTypeface(font(c,false));
            setIncludeFontPadding(false); setGravity(android.view.Gravity.CENTER);
            setTextColor(primary ? INK : WHITE); setPadding((int)d(c,10),0,(int)d(c,10), (int)d(c,3));
            setMinWidth(0); setMinimumWidth(0); setMinHeight(0); setMinimumHeight(0);
            setStateListAnimator(null); setBackground(new Face(c, face));
            setFocusable(true); setFocusableInTouchMode(true);
            setOnTouchListener((v,event)->{ if(event.getAction()==MotionEvent.ACTION_DOWN)requestFocus(); return false; });
            setOnFocusChangeListener((v, focused) -> {
                setTextColor(focused ? INK : (face == LIME ? INK : WHITE));
                animate().scaleX(focused ? 1.035f : 1).scaleY(focused ? 1.035f : 1).setDuration(130).start();
            });
        }
    }

    static final class Face extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float radius, depth, border;
        private final int color;
        private boolean focus, pressed;
        Face(Context c, int color) { this.color=color; radius=d(c,9); depth=d(c,4); border=d(c,2); }
        @Override public boolean isStateful() { return true; }
        @Override protected boolean onStateChange(int[] states) {
            focus=false; pressed=false;
            for(int state:states) { if(state==android.R.attr.state_focused)focus=true; if(state==android.R.attr.state_pressed)pressed=true; }
            invalidateSelf(); return true;
        }
        @Override public void draw(Canvas c) {
            Rect b=getBounds(); RectF r=new RectF(b.left+border,b.top+border,b.right-border,b.bottom-border);
            p.setStyle(Paint.Style.FILL); p.setColor(0xff050d1b); c.drawRoundRect(r,radius,radius,p);
            r.bottom-=pressed ? 1 : depth; r.top+=pressed ? depth-1 : 0;
            p.setColor(focus ? MINT : color); c.drawRoundRect(r,radius,radius,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(focus ? border*1.5f : border*.5f);
            p.setColor(focus ? WHITE : 0x309bc8f2); c.drawRoundRect(r,radius,radius,p);
            p.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter filter) { p.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static final class Slots extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private String answer="", expected="";
        private int result;
        Slots(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
        void show(String answer, String expected, int result) {
            this.answer=answer; this.expected=expected; this.result=result;
            setContentDescription(answer.isEmpty() ? "輸入英文，共 " + expected.length() + " 個字元" : "已輸入 " + answer);
            invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            int count=Math.max(expected.length(),answer.length()); if(count==0)return;
            float gap=d(getContext(),5), box=Math.min(d(getContext(),38),(getWidth()-gap*(count-1))/count);
            float top=d(getContext(),3), bottom=Math.min(getHeight()-d(getContext(),3),top+d(getContext(),43));
            p.setTypeface(font(getContext(),false));
            p.setTextSize(Math.min(d(getContext(),24),box*.67f)); p.setTextAlign(Paint.Align.CENTER);
            for(int i=0;i<count;i++) {
                boolean space=i<expected.length() && expected.charAt(i)==' ';
                float left=i*(box+gap); RectF r=new RectF(left,top,left+box,bottom);
                p.setStyle(Paint.Style.FILL); p.setColor(space ? 0x0affffff : 0xff1c3350); c.drawRoundRect(r,d(getContext(),6),d(getContext(),6),p);
                boolean cursor=i==answer.length();
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(d(getContext(),cursor?2:1));
                p.setColor(result==1 ? MINT : result==2 ? PEACH : cursor ? LIME : 0xff425875);
                c.drawRoundRect(r,d(getContext(),6),d(getContext(),6),p); p.setStyle(Paint.Style.FILL);
                if(i<answer.length()) {
                    p.setColor(result==1?MINT:WHITE);
                    float y=(top+bottom-p.ascent()-p.descent())/2;
                    c.drawText(String.valueOf(answer.charAt(i)),left+box/2,y,p);
                } else if(cursor) {
                    p.setColor(LIME); c.drawRoundRect(left+box*.32f,bottom-d(getContext(),10),left+box*.68f,bottom-d(getContext(),8),1,1,p);
                }
            }
        }
    }

    static final class Gems extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private int progress, total=5;
        Gems(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        void progress(int value, int total) { this.progress=value; this.total=total; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            int count=Math.min(total,10);
            float unit=getWidth()/(float)count, size=Math.min(d(getContext(),14),unit*.35f);
            p.setColor(0xff34485f); p.setStrokeWidth(d(getContext(),2));
            c.drawLine(unit/2,getHeight()/2f,getWidth()-unit/2,getHeight()/2f,p);
            for(int i=0;i<count;i++) {
                boolean filled=total<=10?i<progress:i<Math.floor(progress*10f/total);
                cube(c,p,unit*(i+.5f),getHeight()/2f,size,filled?LIME:0xff36516f,filled?0xff72a931:0xff1b314b);
            }
        }
    }

    static void cube(Canvas c, Paint p, float x, float y, float r, int top, int side) {
        Path path=new Path(); path.moveTo(x,y-r); path.lineTo(x+r,y-r*.4f); path.lineTo(x,y+r*.2f); path.lineTo(x-r,y-r*.4f); path.close();
        p.setStyle(Paint.Style.FILL); p.setColor(top); c.drawPath(path,p);
        path.reset();path.moveTo(x-r,y-r*.4f);path.lineTo(x,y+r*.2f);path.lineTo(x,y+r);path.lineTo(x-r,y+r*.4f);path.close();
        p.setColor(side);c.drawPath(path,p);
        path.reset();path.moveTo(x,y+r*.2f);path.lineTo(x+r,y-r*.4f);path.lineTo(x+r,y+r*.4f);path.lineTo(x,y+r);path.close();
        p.setColor(top);c.drawPath(path,p);
        p.setColor(0x22ffffff);c.drawPath(path,p);
    }

    static final class Trophy extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Trophy(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas c) {
            float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.29f;
            p.setShader(new RadialGradient(x,y,r*1.8f,0x667eddd2,0x007eddd2,Shader.TileMode.CLAMP));
            c.drawCircle(x,y,r*1.8f,p);p.setShader(null);
            c.save();c.rotate(-12,x,y);cube(c,p,x,y,r,LIME,0xff7bae36);c.restore();
            for(int i=0;i<4;i++) {
                double a=i*Math.PI/2+.3;float sx=x+(float)Math.cos(a)*r*1.8f,sy=y+(float)Math.sin(a)*r*1.45f;
                p.setColor(i%2==0?MINT:WHITE);c.save();c.rotate(45,sx,sy);c.drawRect(sx-3,sy-3,sx+3,sy+3,p);c.restore();
            }
        }
    }
}
