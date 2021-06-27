package li.kaiott.asvzbuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "MainActivity";
    Button postLessonButton;
    ImageView backendStatusImage;
    RecyclerView lessonRecycler;
    SwipeRefreshLayout mRefreshLayout;
    MenuItem trashItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = "fcm_default_channel";
            String channelName = "Enrollment";
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (action != null && action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.equals("text/plain")) {
                handleSentText(intent);
            }
        }
        postLessonButton = findViewById(R.id.button_post_lesson);
        backendStatusImage = findViewById(R.id.image_backend_status);
        lessonRecycler = findViewById(R.id.recycler_lessons);
        mRefreshLayout = findViewById(R.id.swiperefresh);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                @Override
                                                public void onRefresh() {
                                                    Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
                                                    mRefreshLayout.setRefreshing(false);
                                                    getLessons();
                                                }
                                            }

        );

        ServerApi.lessons = new ArrayList<>();
        ServerApi.serverUp = false;
        ServerApi.tokenUpToDate = false;
        getLessons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Toast.makeText(MainActivity.this, "no token yet", Toast.LENGTH_SHORT).show();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = "FCM registration Token: "+ token;
                        Log.d(TAG, msg);
                        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void handleSentText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Pattern pattern = Pattern.compile(".*https[^0-9]+[0-9]+");
            Matcher matcher = pattern.matcher(sharedText);
            boolean found = false;
            String url = "";
            while (matcher.find()) {
                Log.i(TAG, "handleSentText: found " + matcher.group());
                url = matcher.group();
                found = true;
            }
            if (!found) {
                Toast.makeText(this, "Please share a valid url", Toast.LENGTH_SHORT).show();
            }
            else {
                pattern = Pattern.compile("[0-9]+");
                matcher = pattern.matcher(url);
                while (matcher.find()) {
                    Log.i(TAG, "handleSentText: found lesson: " + matcher.group());
                    postLesson(matcher.group());
                }
            }
            Log.i(TAG, "handleSentText: " + sharedText);
        }
    }

    protected void updateUI() {
        if (!ServerApi.serverUp) {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_error_24);
        } else if (ServerApi.tokenUpToDate) {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_check_circle_24);
        } else {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_warning_24);
        }

        ArrayList<Lesson> lessonsToDisplay = Lesson.filterInterval(ServerApi.lessons, OffsetDateTime.now(), null);
        LessonTileAdapter mAdapter = new LessonTileAdapter(this, lessonsToDisplay);
        lessonRecycler.setAdapter(mAdapter);
        lessonRecycler.setLayoutManager(new LinearLayoutManager(this));

        updateMenu();
    }
    
    public void updateMenu() {
        Log.i(TAG, "updateMenu: lessons has entries: " + ServerApi.lessons.size());
        if (trashItem != null) trashItem.setVisible(Lesson.selectedCount > 0);
    }

    protected void getStatus() {
        ServerApi.getStatus(this);
    }

    protected void getLesson(String id, int purpose) {
        ServerApi.getLesson(id, purpose, this);
    }

    protected void getLessons() {
        ServerApi.getLessons(this);
    }

    public void postLesson(View view) {
        AlertDialog dialog = makeAddLessonDialog();
        dialog.show();
    }
    protected void postLesson(String id) {
        ServerApi.postLesson(id, this);
    }

    protected void deleteLesson(String id) {
        ServerApi.deleteLesson(id, this);
    }

    protected void deleteSelected() {
        for (Lesson lesson : ServerApi.lessons) {
            if (lesson.selected) {
                deleteLesson(String.valueOf(lesson.getId()));
            }
        }
    }

    protected AlertDialog makeAddLessonDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setHint("000000");
        mBuilder.setTitle("Enter lesson id")
                .setView(input)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String id = input.getText().toString();
                        postLesson(id);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return mBuilder.create();
    }

    protected AlertDialog makeDeleteLessonsDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        final TextView input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        mBuilder.setTitle("Delete?")
                .setMessage("Are you sure that you want to delete the " + Lesson.selectedCount + " selected lessons from the server?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteSelected();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return mBuilder.create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        trashItem = menu.findItem(R.id.image_trash);
        if (trashItem !=null) trashItem.setVisible(Lesson.selectedCount > 0);
        if (trashItem == null) {
            Log.i(TAG, "onCreateOptionsMenu: I dont understand this shiiiit");
        }
        else {
            Log.i(TAG, "onCreateOptionsMenu: all good all good");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == trashItem) {
            Log.i(TAG, "onOptionsItemSelected: we selected the trash, let's delete stuff");
            AlertDialog dialog = makeDeleteLessonsDialog();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}