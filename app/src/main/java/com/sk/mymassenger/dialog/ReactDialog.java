package com.sk.mymassenger.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sk.mymassenger.R;
import com.sk.mymassenger.databinding.RaectLayoutBinding;


public class ReactDialog extends Dialog {
    View.OnClickListener listener;
    public ReactDialog(Context context, View.OnClickListener listener) {
        super(context);
        this.listener=listener;
    }
   RaectLayoutBinding raectLayoutBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        raectLayoutBinding=RaectLayoutBinding.inflate(getLayoutInflater());
        setContentView(raectLayoutBinding.getRoot());
        ReactAdapter adapter=new ReactAdapter();
        getWindow().getAttributes().height=500;
        raectLayoutBinding.reactList.setAdapter(adapter);

    }

    private class ReactAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final String[] list;

        public ReactAdapter() {
            list=getContext().getResources().getStringArray(R.array.react_values);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.adapter_text_view,parent,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
             holder.getRoot().setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                     view.startAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.scale_anim));

                     listener.onClick(view);
                     cancel();
                 }
             });
             holder.getRoot().setText(list[position]);
        }

        @Override
        public int getItemCount() {
            return list.length;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public TextView getRoot() {
            return root;
        }

        private final TextView root;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.root=(TextView) itemView;
        }
    }
}
