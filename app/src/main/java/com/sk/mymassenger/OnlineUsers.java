package com.sk.mymassenger;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OnlineUsers extends Thread{

    private final String userid;
    private final ImageView icon;
    private Handler handler;
    private TextView view;
    private DataSnapshot dataSnapshot;
    private Context context;

    public OnlineUsers(Handler handler, TextView view, ImageView icon, Context context, DataSnapshot dataSnapshot, String userid) {
         this.userid=userid;
        this.handler = handler;
        this.view = view;
        this.icon=icon;
        this.context=context;
        this.dataSnapshot = dataSnapshot;
    }



    @Override
    public void run() {

            getVal( dataSnapshot );

    }
    private void   getVal(DataSnapshot snapshot){

        String status= (String) snapshot.child( "status" ).getValue();

        if(status.equals( "typing..." )){
           String For= (String) snapshot.child( "for" ).getValue();
           if(For!=null&&For.equals(userid)){
               handler.post( ()->{
                   if(!view.getText().toString().equals("typing...")) {
                       icon.setImageDrawable(context.getDrawable(R.drawable.typing_dots_animated));
                       AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) icon.getDrawable();
                       view.setText(status);
                       drawable.start();
                   }

               } );
           }else{
               handler.post( ()->{
                   view.setText("online");
                   icon.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_signal_wifi_4_bar_24));
               } );
           }
        }
        else if(status.equals("online")){
            handler.post( ()->{
                view.setText(status);

                icon.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_signal_wifi_4_bar_24));
            } );

        }else {
            Date date=new Date( (Long) snapshot.child( "last seen").getValue() );
            new ResolveDate().resolve( date ,"last seen", view,handler);

            handler.post( ()->{

                icon.setImageDrawable(context.getDrawable(R.drawable.ic_ofline));
            } );
        }
    }
    public static class ResolveDate extends Thread{
        String TAG="ResolveDate : ";
        private String[] day={"","yesterday"};
        private Date date;
        private String type;
        private TextView textView;
        private Handler handler;

        public void resolve(Date date, String type, TextView holderDate,Handler handler){
          this.date=date;
          this.type=type;
          this.textView=holderDate;
          this.handler=handler;
          start();
        }

        @Override
        public void run() {
            long current=new Date().getTime();
            long time=date.getTime();
            int d= (int) ((current-time)/84600000);
            String val;
            Log.d( TAG, "resolve: "+d +" def :"+(current-time));
            if(d>=0&&d<2){
                SimpleDateFormat format=new SimpleDateFormat( "hh:mm a" );
                if(type.equals( "last seen" ))
                   val= "last seen "+day[d]+" at "+format.format( date );
                else
                    val= day[d]+" "+format.format( date );
            }else if(d<7) {
                SimpleDateFormat format = new SimpleDateFormat( "EEE" );
                SimpleDateFormat format2 = new SimpleDateFormat( "hh:mm a" );
                if(type.equals( "last seen" ))
                   val="last seen " + format.format( date )+" at "+format2.format( date );
                else
                    val=format.format(date)+" "+format2.format( date );
            }else if(d<356){
                SimpleDateFormat format = new SimpleDateFormat( "dd MMM" );
                SimpleDateFormat format2 = new SimpleDateFormat( "hh:mm a " );
                if(type.equals( "last seen" ))
                    val="last seen " + format.format( date )+" at "+format2.format( date );
                else
                    val=format.format(date)+" "+format2.format( date );
            }
            SimpleDateFormat format = new SimpleDateFormat( "dd MMM yy" );
            SimpleDateFormat format2 = new SimpleDateFormat( "hh:mm a " );
            if(type.equals( "last seen" ))
                val="last seen " + format.format( date )+" at "+format2.format( date );
            else
                val=format.format(date)+" "+format2.format( date );

            String finalVal = val;
            handler.post(()->{textView.setText(finalVal);});
        }
    }
}
