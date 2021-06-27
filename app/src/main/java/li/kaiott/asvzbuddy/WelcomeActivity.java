package li.kaiott.asvzbuddy;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WelcomeActivity extends AppCompatActivity {
    Button signinButton, skipButton, signinInfoButton, skipInfoButton;
    EditText nethzUserText, nethzPasswordText;
    public final String preferencesName = "li.kaiott.asvzbuddy.SharedPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        nethzUserText = findViewById(R.id.edit_username);
        nethzPasswordText = findViewById(R.id.edit_password);

        boolean firstStart = getSharedPreferences(preferencesName, MODE_PRIVATE).getBoolean("firstStartup", true);
        firstStart = true;
        if (!firstStart) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        //Toast.makeText(this, "firstStart = " + firstStart, Toast.LENGTH_SHORT).show();
        getSharedPreferences("li.kaiott.asvzbuddy.SharedPreferences", MODE_PRIVATE).edit().putBoolean("firstStartup", false).apply();
        //Toast.makeText(this, "firstStart = " + firstStart, Toast.LENGTH_SHORT).show();
    }

    public void signIn(View view) {
        String nethzUserName = nethzUserText.getText().toString();
        String nethzPassword = nethzPasswordText.getText().toString();
        updateUser(nethzUserName, nethzPassword);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void skipSignIn(View view) {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void showInfo(View view) {
        String infoText = "";
        if (view.getId() == R.id.btn_signin_info) {
            infoText = "Your nethz credentials are required to automatically enroll for ASVZ classes";
        }
        else if (view.getId() == R.id.btn_skip_info) {
            infoText = "if you don't sign in, you won't be able to automatically enroll for ASVZ classes. You will still be able to set up watchers.";
        }
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        final TextView input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        mBuilder.setTitle("Info")
                .setMessage(infoText)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        mBuilder.create().show();
    }

    public void updateUser(String username, String password) {
        getSharedPreferences(preferencesName, MODE_PRIVATE).edit()
                .putString("nethz_user", username)
                .putString("nethz_password", password)
                .apply();
    }


}