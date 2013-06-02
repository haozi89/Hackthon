package com.hangzhou.hackthon.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.hangzhou.hackthon.R;
import com.hangzhou.hackthon.net.HttpManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: haozi
 * Date: 13-6-1
 * Time: 下午4:16
 * To change this template use File | Settings | File Templates.
 */
public class HomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiting_activity);
        Button btn = (Button) findViewById(R.id.server);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               new ConnectToServerTask().execute();
            }
        });

        Button btnClient = (Button) findViewById(R.id.client);
        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                       try {
                           HttpManager.execute(new HttpGet("http://192.168.0.105:8000/clear"));
                       }catch (Exception e) {
                           e.printStackTrace();
                       }
                    }
                }).start();
            }
        });
    }

    private class ConnectToServerTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                final HttpGet get = new HttpGet("http://192.168.0.105:8000/in");
                String res =  parseResponse(HttpManager.execute(get));
                Log.e("haozi", res);
                return res;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s!= null) {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                if (s.equalsIgnoreCase("host")) {
                    intent.putExtra("server", true);
                    startActivity(intent);
                } else {
                    intent.putExtra("host", s);
                    startActivity(intent);
                }
            }
        }
    }

    public String parseResponse(HttpResponse response) throws IOException {
        String body = null;
        StatusLine status = response.getStatusLine();
        int code = status.getStatusCode();

        HttpEntity entity = null;
        HttpEntity temp = response.getEntity();
        if (temp != null) {
            entity = new BufferedHttpEntity(temp);
        }
        body = EntityUtils.toString(entity);
        if (status.getStatusCode() >= 300) {
        }
        return body;
    }
}
