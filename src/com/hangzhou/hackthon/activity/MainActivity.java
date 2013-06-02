package com.hangzhou.hackthon.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import com.hangzhou.hackthon.R;
import com.hangzhou.hackthon.net.HttpManager;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private int[] mTvIds = new int[] {R.id.tv0, R.id.tv1, R.id.tv2, R.id.tv3, R.id.tv4,
            R.id.tv5, R.id.tv6, R.id.tv7, R.id.tv8 };
    private TextView[] mTvs = new TextView[9];

    private ServerSocket mSocketServer;
    private Socket mSocketClient;

    private String mHost;
    private final int PORT = 4545;
    private final String MSG_RECEIVED = "socket://receive";
    private final String MSG_WIN = "socket://win";
    private final String MSG_LOSE = "socket://lose";
    private final String MSG_CONNECTED = "socket://hello";

    private final int DISCONNECT = -1;
    private final int WIN = 0;
    private final int LOSE = 1;
    private String[] mDatas;

    private boolean mIsServer;
    private ArrayList<Integer> mLastData = new ArrayList<Integer>();
    private int inputTimes = 0;
    private int originCount = 0;
    private ConnectToClientTask mConnectToClientTask;
    private ConnectToServerTask mConnectToServerTask;
    StringBuilder sb;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            mLastData.clear();
            enableTv();
            initState();
            String data = msg.getData().getString("data");
            Log.e("haozi", "data : " + data);
            switch (what) {
                case 0:
                    Toast.makeText(MainActivity.this, "没有玩家", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(MainActivity.this, "你赢了", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(MainActivity.this, "你输了", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(MainActivity.this, "有玩家连上了，可以开始了", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    mDatas = data.split(",");
                    for (String posString : mDatas) {
                        mLastData.add(Integer.parseInt(posString));
                    }
                    originCount = mLastData.size();
                    for (int i = 0; i < mDatas.length; i ++) {
                        try {
                            final int pos = Integer.parseInt(mDatas[i]);
                             handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mTvs[pos].setBackgroundColor(Color.parseColor("#000000"));
                                }
                            }, (i + 1)*500);

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mTvs[pos].setBackgroundResource(R.drawable.selector);
                                }
                            }, (i + 1)*1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    for (TextView tv : mTvs) {
                        tv.setBackgroundResource(R.drawable.selector);
                    }
                    Toast.makeText(MainActivity.this, "快点按照提示按啊", 1).show();
                    break;
            }
        }
    };

    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_activity);
        mRootView = findViewById(R.id.ll);
        for (int i = 0; i < 9; i ++) {
            mTvs[i] = (TextView) findViewById(mTvIds[i]);
        }
        mIsServer = getIntent().getBooleanExtra("server", false);
        mHost = getIntent().getStringExtra("host");
        for (int i = 0; i < mTvs.length; i ++) {
            final int pos = i;
            mTvs[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!view.isSelected()) {
                        handler.removeCallbacks(timerRunnable);
                        Log.e("haozi", "data size :" + mLastData.size());
                        if (originCount == 0 || inputTimes == originCount) {
                            mLastData.add(pos);
                            sb =  new StringBuilder();
                            for (int i = 0; i < mLastData.size(); i++) {
                                sb.append(mLastData.get(i));
                                if (i != mLastData.size() - 1) {
                                    sb.append(",");
                                }
                            }
                            send(sb.toString());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    HttpGet get = new HttpGet(String.format("http://192.168.0.105:8000/go?step=%s", sb.toString()));
                                    try {
                                        HttpManager.execute(get);
                                    }catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        try {
                                            get.abort();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }).start();
                            inputTimes += 1;
                            disableTv();
                        } else if (pos == mLastData.get(inputTimes)) {
                           inputTimes += 1;
                           handler.postDelayed(timerRunnable, 2000);
                        } else {
                            mLastData.add(pos);
                            sb =  new StringBuilder();
                            for (int i = 0; i < mLastData.size(); i++) {
                                sb.append(mLastData.get(i));
                                if (i != mLastData.size() - 1) {
                                    sb.append(",");
                                }
                            }
                            sendLostMsg(sb.toString());
                            initState();
                            Toast.makeText(MainActivity.this, "你输了", Toast.LENGTH_SHORT).show();
                            send(MSG_WIN);
                        }
                        view.setSelected(!view.isSelected());
                    }

                }
            });
        }
        if (mIsServer) {
            enableTv();
            initServerSocket();
        } else {
            Toast.makeText(this, "等待主机玩家输入", Toast.LENGTH_SHORT).show();
            disableTv();
            initClientSocket();
        }
    }

    private void initServerSocket() {
        if (mConnectToServerTask != null && mConnectToServerTask.getStatus() != AsyncTask.Status.FINISHED) {
            mConnectToServerTask.cancel(true);
        }
        mConnectToServerTask = new ConnectToServerTask();
        mConnectToServerTask.execute();
    }

    private void initClientSocket() {
        if (mConnectToClientTask != null && mConnectToClientTask.getStatus() != AsyncTask.Status.FINISHED) {
            mConnectToClientTask.cancel(true);
        }
        mConnectToClientTask = new ConnectToClientTask();
        mConnectToClientTask.execute();
    }

    private class ConnectToServerTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Socket socket = null;
            try {
                try {
                    mSocketServer = new ServerSocket(PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                while (true) {
                    mSocketClient = mSocketServer.accept();
                    while (true) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            return null;
                        }
                        String result = getReceiveMsg(mSocketClient);
                        if (result == null) {
                            handler.sendMessage(handler.obtainMessage(0));
                        } else if (result.equals(MSG_WIN)) {
                            handler.sendMessage(handler.obtainMessage(1));
                        } else if (result.equals(MSG_LOSE)) {
                            handler.sendMessage(handler.obtainMessage(2));
                        } else if (result.equals(MSG_CONNECTED)) {
                            handler.sendMessage(handler.obtainMessage(3));
                        } else{
                            Message message = handler.obtainMessage(4);
                            Bundle bundle = new Bundle();
                            Log.e("haozi", "resl :" + result);
                            bundle.putString("data", result);
                            message.setData(bundle);
                            handler.sendMessage(message);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer s) {
            super.onPostExecute(s);
        }
    }

    private class ConnectToClientTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                try {
                    mSocketClient = requestSocket(mHost, PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("haozi",  "ex" + Log.getStackTraceString(e));
                }
                sendMsg(mSocketClient, MSG_CONNECTED);
                while(true){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return null;
                    }
                    String result = getReceiveMsg(mSocketClient);
                    if (result == null) {
                        handler.sendMessage(handler.obtainMessage(0));
                    } else if (result.equals(MSG_WIN)) {
                        handler.sendMessage(handler.obtainMessage(1));
                    } else if (result.equals(MSG_LOSE)) {
                        handler.sendMessage(handler.obtainMessage(2));
                    } else if (result.equals(MSG_CONNECTED)) {
                        handler.sendMessage(handler.obtainMessage(3));
                    } else {
                        Message message = handler.obtainMessage(4);
                        Bundle bundle = new Bundle();
                        bundle.putString("data", result);
                        message.setData(bundle);
                        handler.sendMessage(message);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("haozi",  "ex2" + Log.getStackTraceString(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }

    private Socket requestSocket(String host, int port)
            throws UnknownHostException, IOException {
        Socket conSocket = new Socket();
        //创建套接字地址，其中 IP 地址为通配符地址，端口号为指定值。
        //有效端口值介于 0 和 65535 之间。端口号 zero 允许系统在 bind 操作中挑选暂时的端口。
        InetSocketAddress isa = new InetSocketAddress(host, port);
        //建立一个远程链接
        conSocket.connect(isa);
        return conSocket;
    }

    private String getReceiveMsg(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        System.out.println("server get input from client socket..");
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            return line;
        }
        return line;
    }

    private void sendMsg(Socket socket, String msg) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream()));
        writer.write(msg + "\n");
        writer.flush();
    }

    private void send(final String msg) {
        disableTv();
        inputTimes = 0;
        originCount = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mIsServer) {
                    try {
                        sendMsg(mSocketClient, msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        sendMsg(mSocketClient, msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    private void initState() {
        inputTimes = 0;
        mLastData.clear();
        initTvState();
    }

    private void initTvState() {
        for (TextView tv : mTvs) {
            tv.setSelected(false);
            tv.setBackgroundResource(R.drawable.selector);
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLastData.size() <= originCount) {
                sb =  new StringBuilder();
                for (int i = 0; i < mLastData.size(); i++) {
                    sb.append(mLastData.get(i));
                    if (i != mLastData.size() - 1) {
                        sb.append(",");
                    }
                }
                sendLostMsg(sb.toString());
                initState();
                Toast.makeText(MainActivity.this, "你输了", Toast.LENGTH_SHORT).show();
                send(MSG_WIN);
            } else {
                sb =  new StringBuilder();
                for (int i = 0; i < mLastData.size(); i++) {
                    sb.append(mLastData.get(i));
                    if (i != mLastData.size() - 1) {
                        sb.append(",");
                    }
                }
                send(sb.toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        HttpGet get = new HttpGet(String.format("http://192.168.0.105:8000/go?step=%s", sb.toString()));
                        try {
                            HttpManager.execute(get);
                        }catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                get.abort();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        }
    };

    private void sendLostMsg(final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpGet get = new HttpGet(String.format("http://192.168.0.105:8000/go?step=%s&loser=true", msg));
                try {
                    HttpManager.execute(get);
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        get.abort();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpGet get = new HttpGet(String.format("http://192.168.0.105:8000/out"));
                try {
                    HttpManager.execute(get);
                }catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        get.abort();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        if (mConnectToServerTask != null && mConnectToServerTask.getStatus() != AsyncTask.Status.FINISHED) {
            mConnectToServerTask.cancel(true);
        }

        if (mConnectToClientTask != null && mConnectToClientTask.getStatus() != AsyncTask.Status.FINISHED) {
            mConnectToClientTask.cancel(true);
        }

        if (mSocketClient != null) {
            try{
                mSocketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mSocketServer != null) {
            try{
                mSocketServer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private void disableTv() {
        for (TextView tv: mTvs) {
            tv.setEnabled(false);
        }
    }

    private void enableTv() {
        for (TextView tv: mTvs) {
            tv.setEnabled(true);
        }
    }
}
