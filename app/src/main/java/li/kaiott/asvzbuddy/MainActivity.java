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

import com.android.volley.ClientError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    static final int GET_STATUS = 0, GET_LESSONS = 1, GET_LESSON=2, POST_LESSON = 3, DELETE_LESSON = 4;
    static final int CHECK_IF_CREATED = 0, CHECK_IF_DUPLICATE = 1;
    static final String TAG = "MainActivity";
    ArrayList<Lesson> lessons;
    Boolean serverUp, tokenUpToDate;
    Button postLessonButton;
    ImageView backendStatusImage;
    RecyclerView lessonRecycler;
    SwipeRefreshLayout mRefreshLayout;
    MenuItem trash;

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
        if (action.equals(Intent.ACTION_SEND) && type != null) {
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

        lessons = new ArrayList<>();
        serverUp = false;
        tokenUpToDate = false;
        getLessons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(MainActivity.this, "no token yet", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
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
        if (!serverUp) {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_error_24);
        } else if (tokenUpToDate) {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_check_circle_24);
        } else {
            backendStatusImage.setImageResource(R.drawable.ic_baseline_warning_24);
        }

        LessonTileAdapter mAdapter = new LessonTileAdapter(this, lessons);
        lessonRecycler.setAdapter(mAdapter);
        lessonRecycler.setLayoutManager(new LinearLayoutManager(this));

        updateMenu();
    }
    
    public void updateMenu() {
        Log.i(TAG, "updateMenu: can this be called from our class");
        Log.i(TAG, "updateMenu: lessons has entries: " + lessons.size());
        if (trash != null) trash.setVisible(Lesson.selectedCount > 0);
    }
    
    public void getStatus(View view) {
        getStatus();
    }
    protected void getStatus() {
        String apiUrl = "https://www.kaiott.li/asvzbuddy/status";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("getStatus:", response.toString());
                        try {
                            String serverStatus = response.getString("server_status");
                            String tokenStatus = response.getString("token_status");
                            serverUp = serverStatus.equals("running");
                            tokenUpToDate = tokenStatus.equals("up_to_date");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            serverUp = false;
                            tokenUpToDate = false;
                        }
                        updateUI();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("getStatus:", error.toString());
                        serverUp = false;
                        tokenUpToDate = false;
                        updateUI();
                        //Handle error, not found, already exists, server down
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    protected void getLesson(String id, int purpose) {
        String apiUrl = "https://www.kaiott.li/asvzbuddy/lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("getLesson:", response.toString());
                        if (purpose == CHECK_IF_CREATED) {
                            Toast.makeText(MainActivity.this, "Lesson " + id + " added successfully", Toast.LENGTH_SHORT).show();
                        }
                        else if (purpose == CHECK_IF_DUPLICATE) {
                            Toast.makeText(MainActivity.this, "Lesson " + id + " already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof ClientError) {
                            Toast.makeText(MainActivity.this, "Lesson " + id + " could not be added: Try again", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Log.e("getLesson:", error.toString());
                        }
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    public void getLessons(View view) {
        getLessons();
    }
    protected void getLessons() {
        getStatus();
        String apiUrl = "https://www.kaiott.li/asvzbuddy/lessons";
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest objectRequest = new JsonArrayRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("getLessons:", response.toString());
                        lessons = parseLessons(response);
                        updateUI();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("REST Response", error.toString());
                        //Handle error, not found, already exists, server down
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    protected ArrayList<Lesson> parseLessons(JSONArray jsonArray) {
        ArrayList<Lesson> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("lesson_id");
                int status = jsonObject.getInt("status");
                String title = jsonObject.getString("title");
                OffsetDateTime starts = OffsetDateTime.parse(jsonObject.getString("starts"));
                OffsetDateTime ends = OffsetDateTime.parse(jsonObject.getString("ends"));
                result.add(new Lesson(id, status, title, starts, ends));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.i("Main", "parseLessons: parsed " + result.size() + " lessons");
        return result;
    }

    public void postLesson(View view) {
        AlertDialog dialog = makeAddLessonDialog();
        dialog.show();
    }
    protected void postLesson(String id) {
        getStatus();
        //final String id = postLessonId.getText().toString();
        /*try {
            id = Integer.parseInt(postLessonId.getText().toString());
        } catch (NumberFormatException e) {
            Log.e("Main", "postLesson: ", e);
            Toast.makeText(this, "Provide valid id", Toast.LENGTH_SHORT).show();
            return;
        }*/
        String apiUrl = "https://www.kaiott.li/asvzbuddy/lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.PUT,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("postLesson:", response.toString());
                        getLessons();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof TimeoutError) {
                            Log.i("postLesson:", "onErrorResponse: probably posted just fine");
                            getLesson(id, CHECK_IF_CREATED);
                        }
                        else if (error instanceof ClientError) {
                            Log.i("postLesson:", "onErrorResponse: probably already posted");
                            getLesson(id, CHECK_IF_DUPLICATE);
                        }
                        else {
                            Log.e("postLesson:", "unidentified error", error);
                        }
                    }
                }
        );
        objectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(objectRequest);
    }

    protected void deleteLesson(String id) {
        getStatus();
        String apiUrl = "https://www.kaiott.li/asvzbuddy/lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, apiUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("")) {
                            Log.i("REST Response", "successfully deleted");
                        }
                        else {
                            Log.i("REST Response", response);
                        }
                        getLessons();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("REST Response", error.toString());
                //Handle error, not found, already exists, server down
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                Log.i("deleteLesson:", "parseNetworkResponse: " + response.statusCode);
                if (response.statusCode == 204) {
                    Log.i("Main", "parseNetworkResponse: Lesson " + id  + " successfully deleted");
                }
                else if (response.statusCode == 404) {
                    Log.i("Main", "parseNetworkResponse: Lesson " + id  + " not found");
                }
                return super.parseNetworkResponse(response);
            }
        };
        requestQueue.add(stringRequest);
    }
    protected void deleteSelected() {
        for (Lesson lesson : lessons) {
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
                .setMessage("Are you sure that you want to delete all " + Lesson.selectedCount + " selected lessons from the server?")
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
        trash = menu.findItem(R.id.image_trash);
        if (trash !=null) trash.setVisible(Lesson.selectedCount > 0);
        if (trash == null) {
            Log.i(TAG, "onCreateOptionsMenu: I dont understand this shiiiit");
        }
        else {
            Log.i(TAG, "onCreateOptionsMenu: all good all good");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == trash) {
            Log.i(TAG, "onOptionsItemSelected: we selected the trash, let's delete stuff");
            AlertDialog dialog = makeDeleteLessonsDialog();
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void makeRequest(int type) {
        makeRequest(type, 0);
    }
    protected void makeRequest(int type, int id) {
        String apiUrl = "https://www.kaiott.li/asvzbuddy/";
        int method = Request.Method.GET;
        switch (type) {
            case GET_STATUS:
                apiUrl += "status";
                method = Request.Method.GET;
                break;
            case GET_LESSONS:
                apiUrl += "lessons";
                method = Request.Method.GET;
                break;
            case POST_LESSON:
                apiUrl += "lessons/" + id;
                method = Request.Method.POST;
                break;
            case DELETE_LESSON:
                apiUrl += "lessons/" + id;
                method = Request.Method.DELETE;
                break;
            default: throw new IllegalArgumentException("Request type not allowed (" + type + ")");
        }
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonArrayRequest objectRequest = new JsonArrayRequest(
                method,
                apiUrl,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("REST Response", response.toString());
                        //Parse response(type, id);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("REST Response", error.toString());
                        //Handle error, not found, already exists, server down
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

}