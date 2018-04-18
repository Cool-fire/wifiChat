package com.example.root.wifichat;

import android.annotation.SuppressLint;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = ServerActivity.class.getSimpleName();

    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 3000;
    TextView messageTv;
    TextView ipAd;
    private EditText inputTexttoServer,batteryEdt;
    private Integer clientBattery = 25;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        ipAd = (TextView)findViewById(R.id.ipAd);
        messageTv = (TextView) findViewById(R.id.messageTv);
        inputTexttoServer = (EditText)findViewById(R.id.send_text);
        batteryEdt = (EditText)findViewById(R.id.batteryEdt);

        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            Log.d(TAG, String.valueOf(batLevel));
        }


        @SuppressLint("WifiManagerLeak") WifiManager wf = (WifiManager)getSystemService(WIFI_SERVICE);
        try {
            String ip = Formatter.formatIpAddress(wf.getConnectionInfo().getIpAddress());
            Log.d(TAG, ip);
            ipAd.setText(ip);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

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
        if (view.getId() == R.id.start_server) {
            Log.d(TAG, "Starting Server...");
            messageTv.setText("");
            updateMessage("Starting Server...");
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
        if (view.getId() == R.id.send_data) {

            sendMessage(inputTexttoServer.getText().toString());

            inputTexttoServer.setText("");
        }
    }

    private void sendMessage(String s) {
        new send().execute(s);
    }

    private class send extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                if (null != tempClientSocket) {
                    if(!tempClientSocket.isClosed())
                    {
                        Log.d(TAG, "send from server to client"+tempClientSocket.getRemoteSocketAddress().toString());

                        PrintWriter out = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                true);

                        out.println(strings[0]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        Log.d(TAG, socket.getRemoteSocketAddress().toString());
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            updateMessage("Server Started...");
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    String read = input.readLine();
                    Log.i(TAG, "Message Received from Client : " + read);

                    try{
                        if(batteryEdt.getText().toString().matches(""))
                        {
                            clientBattery = 25;
                            Log.d(TAG, "battery: "+clientBattery);
                        }
                        else
                        {
                            clientBattery = Integer.valueOf(batteryEdt.getText().toString());
                            Log.d(TAG, "battery: "+clientBattery);
                        }

                        if(Integer.valueOf(read) <= clientBattery)
                        {
                            sendMessage("Disconnecting due to less battery");
                            read = "Client Disconnected due to shortage of battery";
                            updateMessage(getTime() + " | Client : " + read);
                            tempClientSocket.close();
                            Thread.interrupted();
                            break;
                        }
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }


                     if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Client Disconnected";
                        updateMessage(getTime() + " | Client : " + read);
                        break;
                    }

                    updateMessage(getTime() + " | Client : " + read);

                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
            if(tempClientSocket!=null)
            {
                try {
                    Log.d(TAG, "onDestroy: ");
                    tempClientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
            if(tempClientSocket!=null)
            {
                Log.d(TAG, "onStop: ");
                try {
                    tempClientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}