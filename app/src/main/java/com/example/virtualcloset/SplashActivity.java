package com.example.virtualcloset;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
//          startActivity(new Intent(this, MainActivity.class));
//        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(loginButtonClickListener);

        Button signUpButton = findViewById(R.id.signupButton);
        signUpButton.setOnClickListener(signUpButtonClickListener);
    }

    private View.OnClickListener loginButtonClickListener = new View.OnClickListener(){
        public void onClick(View v) {
            onLoginClicked(v);
        }
    };

    private View.OnClickListener signUpButtonClickListener = new View.OnClickListener(){
        public void onClick(View v){
            onSignupClicked(v);
        }
    };

    //when login button is clicked, goes to login activity
    public void onLoginClicked(View view){
        Intent intentLogin = new Intent(this, LoginActivity.class);
        startActivity(intentLogin);
    }

    //when signup button is clicked, goes to register activity
    public void onSignupClicked (View v){
        Intent intentSignup = new Intent(this, RegisterActivity.class);
        startActivity(intentSignup);
    }
}
