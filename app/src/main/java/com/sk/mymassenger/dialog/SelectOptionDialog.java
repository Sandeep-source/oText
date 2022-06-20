package com.sk.mymassenger.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.sk.mymassenger.db.Database;
import com.sk.mymassenger.DeleteMessage;
import com.sk.mymassenger.R;
import com.sk.mymassenger.db.message.Message;

import java.util.ArrayList;
import java.util.Arrays;

public class SelectOptionDialog extends Dialog {
    private Context context;
    private View.OnClickListener reactListener;

    public SelectOptionDialog(Context context) {
        super(context);
        this.context=context;
    }

    public void setContext(Context context) {
        this.context = context;


    }

    public void setReactListener(View.OnClickListener reactListener) {
        this.reactListener = reactListener;
    }

    public void setReplyListener(View.OnClickListener replyListener) {
        this.replyListener = replyListener;
    }

    public void setForwardListener(View.OnClickListener forwardListener) {
        this.forwardListener = forwardListener;
    }

    public void setMsgData(Message msgData) {
        this.msgData = msgData;
    }

    private View.OnClickListener replyListener;
    private View.OnClickListener forwardListener;

    public Message getMsgData() {
        return msgData;
    }

    private Message msgData;
    public SelectOptionDialog(@NonNull Context context,int x, int y) {
        super(context);
        this.context=context;
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x=x;
        params.y=y-60;
        params.gravity= Gravity.START|Gravity.TOP;
    }
    public  void setAlign(int x,int y){
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x=x;
        params.y=y-60;
        params.gravity= Gravity.START|Gravity.TOP;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_option);
         findViewById(R.id.opt_delete).setOnClickListener(view -> {
             Intent intent=new Intent(context, DeleteMessage.class);
             intent.putExtra("mode", Database.Msg.TBL_MSG);
             intent.putExtra("umode",msgData.getMsgMode());
             intent.putExtra(Database.Msg.OUSER_ID,msgData.getOUserId());
             intent.putExtra(Database.Msg.MUSER_ID,msgData.getMUserId());
             intent.putExtra("ids", new ArrayList<>(Arrays.asList(msgData.getMsgId())));
             context.startService(intent);
             cancel();
         });
         findViewById(R.id.opt_react).setOnClickListener(view -> {
             ReactDialog reactDialog=new ReactDialog(context,reactListener);
             reactDialog.show();
             cancel();
         });
         findViewById(R.id.opt_reply).setOnClickListener(replyListener);
         findViewById(R.id.opt_forward).setOnClickListener(forwardListener);
         setCancelable(true);
    }
}
