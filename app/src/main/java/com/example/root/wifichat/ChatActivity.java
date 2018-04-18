package com.example.root.wifichat;

import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = ChatActivity.class.getSimpleName();

    public static final int SERVERPORT = 3000;

    public Socket tempSocket;

    public static  String SERVER_IP = "192.168.43.61";
    ClientThread clientThread;
    Thread thread;
    TextView messageTv;
    EditText sendEdt,ServerIpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        messageTv = (TextView) findViewById(R.id.messageTv);
        sendEdt = (EditText)findViewById(R.id.send_text);
        ServerIpText = (EditText)findViewById(R.id.serverIp);


    }

    public void updateMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageTv.append(message + "\n");
            }
        });
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {
            try
            {
                SERVER_IP = ServerIpText.getText().toString();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            messageTv.setText("");
            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();
            return;
        }

        if (view.getId() == R.id.send_data) {
            //       clientThread.sendMessage("Hello from Client");
//            sendMessage("Hello from clent");
            String text = sendEdt.getText().toString();
            sendEdt.setText("");
            new  send().execute(text);
        }
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);

                BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    new send().execute(String.valueOf(batLevel));
                }

                tempSocket = socket;

                while (!Thread.currentThread().isInterrupted()) {

                    Log.i(TAG, "Waiting for message from server...");





                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    Log.i(TAG, "Message received from the server : " + message);

                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        updateMessage(getTime() + " | Server : " + message);
                        break;
                    }

                    updateMessage(getTime() + " | Server : " + message);

                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        void sendMessage(String message) {
            try {
                if (null != socket) {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: disconnected");
        if (null != clientThread) {
            new send().execute("disconnect");
            clientThread = null;
        }
        if(null!=tempSocket)
        {
            try {
                tempSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendMessage(String message){
        try {
            if (null != tempSocket) {
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(tempSocket.getOutputStream())),
                        true);
                out.println(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class send extends AsyncTask<String,Void,Void>
    {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                if (null != tempSocket) {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(tempSocket.getOutputStream())),
                            true);
                    out.println(strings[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
