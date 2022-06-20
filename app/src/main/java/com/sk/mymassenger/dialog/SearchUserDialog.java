package com.sk.mymassenger.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;

import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.SpinnerBinding;

public class SearchUserDialog extends Dialog {
    private  Handler handler;
    private SpinnerBinding binding;
    public SearchUserDialog(@NonNull Context context) {
        super(context);
    }

    public SearchUserDialog(Context context, Handler handler) {
        super(context);
        this.handler=handler;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding=SpinnerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);



    }
    public void setSuccess(){



    }
    public void setFailed(View.OnClickListener listener){
        handler.post(()->{
            binding.searchProgress.setVisibility(View.GONE);
            //binding.searchProgress.setIndeterminate();
            AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) binding.searchDrawable.getDrawable();
            if(drawable!=null){
                drawable.start();
            }

            binding.progressMessage.setText("Your friend is not on our platform");
            binding.searchProgress.setBackground(getContext().getDrawable(R.drawable.ic_baseline_close_24));
            binding.bottom.setVisibility(View.VISIBLE);
            binding.searchDrawable.setImageResource(R.drawable.animated_user);

            binding.cancel.setOnClickListener((v)->cancel());
            binding.invite.setOnClickListener(listener);
        });


    }
    public void start(){
        AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) binding.searchDrawable.getDrawable();
        if(drawable!=null){
            drawable.start();
        }
    }


    public void setFetching() {
        handler.post(()->{
            binding.progressMessage.setText("Getting ready to start...");
            //binding.done.setVisibility(View.VISIBLE);
            binding.searchProgress.setBackgroundResource(R.drawable.fui_ic_check_circle_black_128dp);
            binding.searchDrawable.setImageResource(R.drawable.animated_user);
            AnimatedVectorDrawable drawable= (AnimatedVectorDrawable) binding.searchDrawable.getDrawable();
            if(drawable!=null){
                drawable.start();
            }
        });

    }
}
