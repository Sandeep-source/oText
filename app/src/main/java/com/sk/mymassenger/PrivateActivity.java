package com.sk.mymassenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.sk.mymassenger.databinding.ActivityPrivateBinding;
import com.sk.mymassenger.databinding.PasswordLayoutBinding;
import com.sk.mymassenger.db.Database;

public class PrivateActivity extends AppCompatActivity {
    private String password;
    private String userid;
    private String extraMode;
    private String open;
    private ActivityPrivateBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        binding=ActivityPrivateBinding.inflate(getLayoutInflater());
        userid= FirebaseAuth.getInstance().getCurrentUser().getUid();
        extraMode=getIntent().getStringExtra( "Work" );
        extraMode=extraMode==null?"":extraMode;
         open = getIntent().getStringExtra( "start" );
        open=open==null?"":open;
        setContentView(binding.getRoot());

    }

    @Override
    protected void onStart() {
        super.onStart();
        PasswordFragment fragment=new PasswordFragment();
        Bundle bundle=new Bundle(  );
        bundle.putString( "extramode",extraMode );
        bundle.putString( "start",open );
        bundle.putString( "User",getIntent().getStringExtra( "User" ) );
        fragment.setArguments( bundle );
        getSupportFragmentManager().beginTransaction().replace( binding.fragmentContainer.getId(),fragment).commit();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    public static class PasswordFragment extends Fragment {
        private String password="";
        private String enteredPassword="";
        private String extraMode;
        private String open;
        private String confirmPassword="";
        private PasswordLayoutBinding binding;
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreateView( inflater, container, savedInstanceState );
            binding=PasswordLayoutBinding.inflate(inflater);
            View view=binding.getRoot();
            SharedPreferences preferences=getContext().getSharedPreferences( "Password",MODE_PRIVATE );
            if(preferences!=null){
                password=preferences.getString( "private",null );
            }
            open=getArguments().getString( "start" ,"");
            extraMode=getArguments().getString( "extramode" );
            ViewGroup group=((ViewGroup)view.findViewById( R.id.buttons ));
            for (int i = 0; i < group.getChildCount(); i++) {
                Button button=(Button) group.getChildAt( i );
                if(button.getText().equals( "C" )){
                    button.setOnClickListener( (v)->clearOne( v ) );
                }else{
                    button.setOnClickListener( (v)->addPassChar( v ) );
                }
            }
            return view;
         }
        public void addPassChar(View view){
            if(extraMode.equals( "SetPass" )){
                if(enteredPassword.length()<4){
                    binding.enterPassword.setText( binding.enterPassword.getText()+((Button) view).getText().toString());
                    enteredPassword+=((Button) view).getText().toString();
                    if(enteredPassword.length()==4){
                        binding.infoText.setText( "Confirm Password" );
                        binding.enterPassword.setText("");
                    }
                }else {
                    binding.enterPassword.setText(binding.enterPassword.getText()+((Button) view).getText().toString());
                    confirmPassword+=((Button) view).getText().toString();
                    if(confirmPassword.length()==4){
                        if(confirmPassword.equals( enteredPassword )){
                            SharedPreferences preferences=getContext().getSharedPreferences( "Password",MODE_PRIVATE );
                            preferences.edit().putString( "private",confirmPassword ).apply();
                            binding.infoText.setText( "Done" );
                            getActivity().setResult( RESULT_OK );
                            getActivity().finish();
                        }else{
                            binding.infoText.setText( "Password and Confirm Password are not matched" );
                            binding.infoText.setTextColor(Color.parseColor( "#ff4242" ));
                            confirmPassword="";
                            enteredPassword="";
                        }
                    }
                }

            }else {
                enteredPassword += ((Button) view).getText();
                binding.enterPassword.setText(binding.enterPassword.getText()+((Button) view).getText().toString());
                if (enteredPassword.length() == 4) {
                    if (password.equals( enteredPassword )) {
                        if(open.equals( "start" )){
                            Intent intent=new Intent( getContext(),HomeActivity.class );
                            intent.putExtra( "User",getArguments().getString( "User" ) );
                            intent.putExtra("start",true);
                            startActivity(intent);
                            getActivity().finish();
                        }else {
                            Intent intent=new Intent(getContext(),HomeActivity.class);

                            intent.putExtra("userMode", Database.Recent.MODE_PRIVATE);
                            getActivity().startActivity(intent);
                        }

                    } else {
                        binding.enterPassword.setText( "" );
                        binding.infoText.setText( "Wrong Password" );
                        binding.infoText.setTextColor(Color.parseColor( "#ff4242" ));

                        enteredPassword="";
                    }
                }
            }
        }

        public void clearOne(View view) {
            String temp=binding.enterPassword.getText().toString();
            if(temp.length()>0)
           binding.enterPassword.setText( temp.substring( 0,temp.length()-1 ) );
        }


    }
}