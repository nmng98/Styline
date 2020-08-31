package com.example.virtualcloset;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import com.google.firebase.auth.UserProfileChangeRequest;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;



public class RegisterActivity extends AppCompatActivity {

    EditText userEmail, userName, userPassword, userConfirm;
    ImageView registerBtn;
    TextView alreadyAcct;
    private FirebaseAuth mAuth;

    public static String theirEmail, theirPassword, theirUserName, theirConfirm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        userEmail = this.findViewById(R.id.email);
        userName = this.findViewById(R.id.user);
        userPassword = this.findViewById(R.id.password);
        userConfirm = this.findViewById(R.id.confirmPassword);
        registerBtn = this.findViewById(R.id.rectangleRegister);
        alreadyAcct = this.findViewById(R.id.alreadyAccount);


        // if user clicks TextView link, transfer them to traditional log in page
        alreadyAcct.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent arg1) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // registering user
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((userPassword.getText().toString()).equals(userConfirm.getText().toString())) {
                    if (!userEmail.getText().toString().isEmpty()) {
                        sendToFireBase();

                    }
                }
            }
        });

    }
    // verifying that user information is acceptable
    private void sendToFireBase() {
        mAuth.createUserWithEmailAndPassword(userEmail.getText().toString(), userPassword.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName.getText().toString())
                                    .build();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                        else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });
        }
}
