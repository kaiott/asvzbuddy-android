package li.kaiott.asvzbuddy;

import android.content.Context;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public class ServerApi {
    static final int GET_STATUS = 0, GET_LESSONS = 1, GET_LESSON=2, POST_LESSON = 3, DELETE_LESSON = 4;
    static final int CHECK_IF_CREATED = 0, CHECK_IF_DUPLICATE = 1;
    static ArrayList<Lesson> lessons;
    static Boolean serverUp, tokenUpToDate;
    static final String APP_URL = "https://www.kaiott.li/asvzbuddy/";

    Context context;
    public ServerApi(Context context) {
        this.context = context;
    }

    static void getStatus(Context context) {
        String apiUrl = APP_URL + "status";
        RequestQueue requestQueue = Volley.newRequestQueue(context);
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
                        ((MainActivity) context).updateUI();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("getStatus:", error.toString());
                        serverUp = false;
                        tokenUpToDate = false;
                        ((MainActivity) context).updateUI();
                        //Handle error, not found, already exists, server down
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    static void getLesson(String id, int purpose, Context context) {
        String apiUrl = APP_URL + "lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("getLesson:", response.toString());
                        if (purpose == CHECK_IF_CREATED) {
                            Toast.makeText(context, "Lesson " + id + " added successfully", Toast.LENGTH_SHORT).show();
                        }
                        else if (purpose == CHECK_IF_DUPLICATE) {
                            Toast.makeText(context, "Lesson " + id + " already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof ClientError) {
                            Toast.makeText(context, "Lesson " + id + " could not be added: Try again", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Log.e("getLesson:", error.toString());
                        }
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    static void getLessons(Context context) {
        getStatus(context);
        String apiUrl = APP_URL + "lessons";
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonArrayRequest objectRequest = new JsonArrayRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i("getLessons:", response.toString());
                        lessons = parseLessons(response);
                        ((MainActivity) context).updateUI();
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

    static ArrayList<Lesson> parseLessons(JSONArray jsonArray) {
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

    static void postLesson(String id, Context context) {
        getStatus(context);
        //final String id = postLessonId.getText().toString();
        /*try {
            id = Integer.parseInt(postLessonId.getText().toString());
        } catch (NumberFormatException e) {
            Log.e("Main", "postLesson: ", e);
            Toast.makeText(this, "Provide valid id", Toast.LENGTH_SHORT).show();
            return;
        }*/
        String apiUrl = APP_URL + "lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest objectRequest = new JsonObjectRequest(
                Request.Method.PUT,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("postLesson:", response.toString());
                        getLessons(context);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error instanceof TimeoutError) {
                            Log.i("postLesson:", "onErrorResponse: probably posted just fine");
                            getLesson(id, CHECK_IF_CREATED, context);
                        }
                        else if (error instanceof ClientError) {
                            Log.i("postLesson:", "onErrorResponse: probably already posted");
                            getLesson(id, CHECK_IF_DUPLICATE, context);
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

    static void deleteLesson(String id, Context context) {
        getStatus(context);
        String apiUrl = APP_URL + "lessons/" + id;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
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
                        getLessons(context);
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



    static void makeRequest(int type, int id, Context context) {
    String apiUrl = APP_URL;
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
        RequestQueue requestQueue = Volley.newRequestQueue(context);
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
