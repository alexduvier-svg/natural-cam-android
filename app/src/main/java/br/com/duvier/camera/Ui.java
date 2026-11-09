package br.com.duvier.camera;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

final class Ui {
    static final int BG=0xff08090b,SURFACE=0xff191b1e,TEXT=0xfff5f5f5,MUTED=0xffc1c3c6,ACCENT=0xffff982b;
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static TextView text(Context c,String name,float size,int color){TextView t=new TextView(c);t.setText(name);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    static TextView action(Context c,String name){TextView t=text(c,name,14,TEXT);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,48));t.setMinWidth(dp(c,48));t.setPadding(dp(c,12),0,dp(c,12),0);t.setClickable(true);t.setFocusable(true);return t;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static GradientDrawable rounded(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static HorizontalScrollView horizontal(Context c,LinearLayout row){HorizontalScrollView h=new HorizontalScrollView(c);h.setHorizontalScrollBarEnabled(false);h.setOverScrollMode(View.OVER_SCROLL_NEVER);h.addView(row,new android.view.ViewGroup.LayoutParams(-2,-1));return h;}
    static Dialog sheet(Activity activity,String title,View content){
        Dialog d=new Dialog(activity);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(activity,16),dp(activity,12),dp(activity,16),dp(activity,18));root.setBackground(rounded(activity,SURFACE,20));
        LinearLayout bar=row(activity);TextView name=text(activity,title,18,TEXT);name.setTypeface(null,Typeface.BOLD);bar.addView(name,new LinearLayout.LayoutParams(0,dp(activity,48),1));TextView done=action(activity,"Concluir");done.setTextColor(ACCENT);done.setOnClickListener(v->d.dismiss());bar.addView(done);root.addView(bar);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));d.setContentView(root);Window w=d.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);return d;
    }
    static final class Icon extends View {
        final String type;final Paint p=new Paint(3);boolean selected=false;String label;
        Icon(Context c,String type,String description){super(c);this.type=type;setContentDescription(description);setClickable(true);setFocusable(true);setMinimumWidth(dp(c,48));setMinimumHeight(dp(c,48));}
        void selected(boolean value){selected=value;invalidate();}
        protected void onDraw(Canvas canvas){super.onDraw(canvas);float size=dp(getContext(),22),cx=getWidth()/2f,cy=getHeight()/2f-(label==null?0:dp(getContext(),8));canvas.save();canvas.translate(cx-size/2,cy-size/2);canvas.scale(size/24,size/24);p.setColor(selected?ACCENT:TEXT);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path path=new Path();
            switch(type){
                case "info":p.setStyle(Paint.Style.FILL);for(int y=0;y<3;y++)for(int x=0;x<3;x++)canvas.drawCircle(6+x*6,6+y*6,1.55f,p);break;
                case "lock":canvas.drawRoundRect(5,10,19,21,2,2,p);canvas.drawArc(8,2,16,14,180,selected?180:140,false,p);canvas.drawCircle(12,15,1,p);break;
                case "quality":canvas.drawRect(3,3,9,9,p);canvas.drawRect(15,3,21,9,p);canvas.drawRect(3,15,9,21,p);canvas.drawRect(15,15,21,21,p);break;
                case "edit":path.moveTo(4,17);path.lineTo(15.5f,5.5f);path.lineTo(19,9);path.lineTo(7.5f,20.5f);path.lineTo(3.5f,21);path.close();canvas.drawPath(path,p);canvas.drawLine(14,7,17.5f,10.5f,p);break;
                case "back":canvas.drawLine(20,12,4,12,p);canvas.drawLine(4,12,11,5,p);canvas.drawLine(4,12,11,19,p);break;
                case "save":canvas.drawLine(12,3,12,16,p);canvas.drawLine(7,11,12,16,p);canvas.drawLine(17,11,12,16,p);path.moveTo(4,16);path.lineTo(4,21);path.lineTo(20,21);path.lineTo(20,16);canvas.drawPath(path,p);break;
                case "undo":canvas.drawArc(5,5,21,21,205,260,false,p);canvas.drawLine(5,12,5,5,p);canvas.drawLine(5,5,12,5,p);break;
                case "before":canvas.drawRect(3,4,21,20,p);canvas.drawLine(12,2,12,22,p);p.setStyle(Paint.Style.FILL);canvas.drawRect(5,6,10,18,p);break;
                case "focus":canvas.drawLine(3,9,3,3,p);canvas.drawLine(3,3,9,3,p);canvas.drawLine(15,3,21,3,p);canvas.drawLine(21,3,21,9,p);canvas.drawLine(3,15,3,21,p);canvas.drawLine(3,21,9,21,p);canvas.drawLine(15,21,21,21,p);canvas.drawLine(21,21,21,15,p);canvas.drawCircle(12,12,3,p);break;
            }
            canvas.restore();if(label!=null){p.setStyle(Paint.Style.FILL);p.setTextSize(dp(getContext(),11));p.setTypeface(Typeface.DEFAULT);p.setTextAlign(Paint.Align.CENTER);canvas.drawText(label,cx,getHeight()-dp(getContext(),8),p);}
        }
    }
}
