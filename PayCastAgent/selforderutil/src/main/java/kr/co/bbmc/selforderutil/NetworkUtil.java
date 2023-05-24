package kr.co.bbmc.selforderutil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Xml;


import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static android.content.Context.WIFI_SERVICE;

public class NetworkUtil {
    private static final String TAG = "NetworkUtil";
    private static boolean LOG = false;
    private static int MAX_CONNECTION_TIMEOUT = 10000;  //10초
    private static int MAX_SOCKET_TIMEOUT = 10000;  //10초

    public static String HttpResponseString(String url, String param, PropUtil prop) {

        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)

        if (Boolean.valueOf(prop.serverSSLEnabled)) {
            url = url.replace("http://", "https://");
        }
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("POST"); // post방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoOutput(true); //쓰기 모드
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        //add data
        if(param!= null) {
            try {
                wr.write(param);
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            try {
                wr.flush();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
        }
        try {
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }

    public static String HttpResponseString(String url, String param, Context c, boolean sslFlag) {
        if(sslFlag) {
            // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
            String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);
            if (Boolean.valueOf(ssl)) {
                url = url.replace("http://", "https://");
            }
        }

        URL Url = null; // URL화 한다.
        try {
            Url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("POST"); // post방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoOutput(true); //쓰기 모드
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            wr.write(param);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                wr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            conn.disconnect();
            return "";
        }
        try {
            wr.flush();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                wr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            conn.disconnect();
            return "";
        }
        try {
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                wr.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null))
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    try {
                        wr.close();
                    } catch (IOException wre) {
                        wre.printStackTrace();
                        //conn.disconnect();
                        return "";
                    }
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }

        return responseString;

    }
    /**
     * Ping a host and return an int value of 0 or 1 or 2 0=success, 1=fail, 2=error
     * <p>
     * Does not work in Android emulator and also delay by '1' second if host not pingable
     * In the Android emulator only ping to 127.0.0.1 works
     *
     * @param @String host in dotted IP address format
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int pingHost(String host) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec("ping -c 1 " + host);
        proc.waitFor();
        int exit = proc.exitValue();
        return exit;
    }
    public static class TcpClientRec {

        private static String server_ip;
        private static int server_port;
        // message to send to the server
        private String mServerMessage;
        // sends message received notifications
        private OnMessageReceived mMessageListener = null;
        // while this is true, the server will continue running
        public boolean mRun = false;
        // used to send messages
//        private PrintWriter mBufferOut;
        private OutputStream mBufferOut;
        // used to read messages from the server
        private BufferedReader mBufferIn;
        private ArrayList<Thread> mThreadList = new ArrayList<>();

        /**
         * Constructor of the class. OnMessagedReceived listens for the messages received from server
         */
        public TcpClientRec(OnMessageReceived listener, String ip, int port) {
            mMessageListener = listener;
            server_ip = ip;
            server_port = port;
            mThreadList = new ArrayList<>();
        }

        /**
         * Sends the message entered by client to the server
         *
         * @param message text entered by client
         */
        public void sendMessage(final String message) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mBufferOut != null) {
                        if(LOG)
                            Log.d(TAG, "Sending: " + message);
                        byte[] buf = new byte[1024]; //choose your buffer size if you need other than 1024
                        buf = message.getBytes();//.substring(0, message.length()).toCharArray();
                        try {
                            mBufferOut.write(buf);
                            mBufferOut.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.setName(message);
            thread.start();

            mThreadList.add(thread);
/*
            if (mBufferOut != null) {
                Log.d(TAG, "Sending: " + message);
//                        mBufferOut.println(message + "\r\n");
                byte[] buf = new byte[1024]; //choose your buffer size if you need other than 1024
                buf = message.getBytes();//.substring(0, message.length()).toCharArray();
                try {
                    mBufferOut.write(buf);
                    mBufferOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
*/
        }


        public void sendFile(final String fileName) {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mBufferOut != null) {
                        byte[] buffer = new byte[1024];
                        int readBytes = 0;

                        FileInputStream fis = null;
                        try {
                            fis = new FileInputStream(fileName);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return;
                        }
                        while(true)
                        {
                            try {
                                readBytes = fis.read(buffer);
                                if(readBytes <= 0)
                                    break;
                                else
                                {
                                    try {
                                        mBufferOut.write(buffer, 0, readBytes);
                                        mBufferOut.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
/*
                if (mBufferOut != null) {
                    byte[] buffer = new byte[1024];
                    int readBytes = 0;

                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(fileName);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    while(true)
                    {
                        try {
                            readBytes = fis.read(buffer);
                            if(readBytes <= 0)
                                break;
                            else
                            {
                                try {
                                    mBufferOut.write(buffer, 0, readBytes);
                                    mBufferOut.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
*/
        }

        /**
         * Close the connection and release the members
         */
        public void stopClient() throws IOException {
            if(mBufferOut!=null) {
                mBufferOut.flush();
                mBufferOut.close();
            }

            //mMessageListener = null;
            mBufferIn = null;
            mBufferOut = null;
            mServerMessage = null;
            mRun = false;
        }

        public void run() {

            mRun = true;

            try {
                //here you must put your computer's IP address.
                InetAddress serverAddr = InetAddress.getByName(server_ip);

                if(LOG)
                    Log.e("TCP Client", "C: Connecting...");

                //create a socket to make the connection with the server
                Socket socket = new Socket(serverAddr, server_port);

                try {

                    //sends the message to the server
                    mBufferOut = socket.getOutputStream();
//                    mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
//                    out =soc.getOutputStream();


                    //receives the message which the server sends back
                    mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    int charsRead = 0;
                    char[] buffer = new char[1024]; //choose your buffer size if you need other than 1024

                    //in this while the client listens for the messages sent by the server
                    while (mRun) {
                        charsRead = mBufferIn.read(buffer);
                        if(charsRead>0) {
                            mServerMessage = new String(buffer).substring(0, charsRead);

                            if (mServerMessage != null && mMessageListener != null) {
                                //call the method messageReceived from MyActivity class
                                mMessageListener.messageReceived(mServerMessage);
                            }
                        }

                    }

                    if(LOG)
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

                } catch (Exception e) {

                    Log.e("TCP", "S: Error", e);

                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                }

            } catch (Exception e) {

                Log.e("TCP", "C: Error", e);

            }

        }

        //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
        //class at on asynckTask doInBackground
        public interface OnMessageReceived {
            public void messageReceived(String message);
        }

    }
    public static class UDP_Client
    {

        private InetAddress IPAddress = null;
        //private String message = "Hello Android!" ;
        private AsyncTask<Void, Void, Void> async_cient;
        public String Message;


        @SuppressLint("NewApi")
        public void NachrichtSenden()
        {
            async_cient = new AsyncTask<Void, Void, Void>()
            {
                @Override
                protected Void doInBackground(Void... params)
                {
                    DatagramSocket ds = null;

                    try
                    {
                        byte[] ipAddr = new byte[]{ (byte) 192, (byte) 168,43, (byte) 157};
                        InetAddress addr = InetAddress.getByAddress(ipAddr);
                        ds = new DatagramSocket(5000);
                        DatagramPacket dp;
                        dp = new DatagramPacket(Message.getBytes(), Message.getBytes().length, addr, 50000);
                        ds.setBroadcast(true);
                        ds.send(dp);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        if (ds != null)
                        {
                            ds.close();
                        }
                    }
                    return null;
                }

                protected void onPostExecute(Void result)
                {
                    super.onPostExecute(result);
                }
            };

            if (Build.VERSION.SDK_INT >= 11) async_cient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else async_cient.execute();
        }
    }
    public static String UrlEncode(String str)
    {
        if (str == null)
        {
            return "";
        }
        byte[] bytes = new byte[0];
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int count = bytes.length;
        int offset = 0;

        int num = 0;
        int num2 = 0;
        for (int i = 0; i < count; i++)
        {
            char ch = (char)bytes[offset + i];
            if (ch == ' ')
            {
                num++;
            }
            else if (!isSafe(ch))
            {
                num2++;
            }
        }

        byte[] buffer = new byte[count + (num2 * 2)];
        int num4 = 0;
        for (int j = 0; j < count; j++)
        {
            byte num6 = bytes[offset + j];
            char ch2 = (char)num6;
            if (isSafe(ch2))
            {
                buffer[num4++] = num6;
            }
            else if (ch2 == ' ')
            {
                buffer[num4++] = 0x2b;
            }
            else
            {
                buffer[num4++] = 0x25;
                buffer[num4++] = (byte)IntToHex((num6 >> 4) & 15);
                buffer[num4++] = (byte)IntToHex(num6 & 15);
            }
        }
        String ret = null;
        try {
            ret = new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //return Encoding.ASCII.GetString(buffer, 0, buffer.length);
        return ret;
    }

    private static char IntToHex(int n)
    {
        if (n <= 9)
        {
            return (char)(n + 0x30);
        }
        return (char)((n - 10) + 0x61);
    }
    private static boolean isSafe(char ch)
    {
        if ((((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))) || ((ch >= '0') && (ch <= '9')))
        {
            return true;
        }
        switch (ch)
        {
            case '\'':
            case '(':
            case ')':
            case '*':
            case '-':
            case '.':
            case '_':
            case '!':
                return true;
        }
        return false;
    }
    public static boolean isConnected(Context c)
    {
        ConnectivityManager manager =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);


        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo etherNet = manager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        NetworkInfo wimax = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        if(wifi!=null && wifi.isConnected())
            return true;
        if((etherNet!=null)&&(etherNet.isConnected()))
            return true;
        if((wimax!=null)&&(wimax.isConnected()))
            return true;
        return false;
    }
    public static class TcpClientSend {

        private static String server_ip;
        private static int server_port;
        // message to send to the server
        private String mServerMessage;
        // sends message received notifications
        private OnMessageReceived mMessageListener = null;
        // while this is true, the server will continue running
        private boolean mRun = false;
        // used to send messages
        private PrintWriter mBufferOut;
        // used to read messages from the server
        private BufferedReader mBufferIn;
        private String mTransferFile = null;
        private String mCommand = null;

        /**
         * Constructor of the class. OnMessagedReceived listens for the messages received from server
         */
        public TcpClientSend(OnMessageReceived listener, String ip, int port) {
            mMessageListener = listener;
            server_ip = ip;
            server_port = port;
        }

        public TcpClientSend(OnMessageReceived listener, String ip, int port, String cmd, String srcFile) {
            mMessageListener = listener;
            server_ip = ip;
            server_port = port;
            mCommand = cmd;
            mTransferFile = srcFile;
        }


        /**
         * Sends the message entered by client to the server
         *
         * @param message text entered by client
         */
        public void sendMessage(final String message) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mBufferOut != null) {
                        if(LOG)
                            Log.d(TAG, "Sending: " + message);
                        mBufferOut.println(message + "\r\n");
                        mBufferOut.flush();
                    }
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

        /**
         * Close the connection and release the members
         */
        public void stopClient() {

            mRun = false;

            if (mBufferOut != null) {
                mBufferOut.flush();
                mBufferOut.close();
            }

            mMessageListener = null;
            mBufferIn = null;
            mBufferOut = null;
            mServerMessage = null;
        }

        public void run() {

            mRun = true;

            try {
                //here you must put your computer's IP address.
                InetAddress serverAddr = InetAddress.getByName(server_ip);

                if(LOG)
                    Log.d("TCP Client", "C: Connecting...");

                //create a socket to make the connection with the server
                Socket socket = new Socket(serverAddr, server_port);

                File trFile = new File(mTransferFile);
                if((trFile==null)|| !trFile.exists() || !trFile.isFile())
                {
                    Utils.LOG("Capture file is not exists");
                    Utils.LOG("");
                    return;
                }

                try {
                    FileInputStream fis = new FileInputStream(trFile);

                    //sends the message to the server
                    mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                    //receives the message which the server sends back
                    mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    int charsRead = 0;
                    char[] inputbuffer = new char[1024]; //choose your buffer size if you need other than 1024
                    byte[] outbuffer = new byte[1024]; //choose your buffer size if you need other than 1024

                    int read;
                    while(mRun)
                    {
                        charsRead = mBufferIn.read(inputbuffer);
                        mServerMessage =new String(inputbuffer).substring(0, charsRead);

                        if (mServerMessage != null && mMessageListener != null) {
                            //call the method messageReceived from MyActivity class
                            mMessageListener.messageReceived(mServerMessage);
                        }
/*
                        while ((read = fis.read(outbuffer, 0, 1024)) != -1) {
                            mBufferOut.write(String.valueOf(outbuffer), 0, read);
                            mBufferOut.flush();
                        }
*/
                    }

                    if(LOG)
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

                } catch (Exception e) {

                    Log.e("TCP", "S: Error", e);
                    {
                        String log = String.format("TCP() s:err=%s", e.toString());
                        FileUtils.writeDebug(log, "PayCastAgent");
                    }

                } finally {
                    //the socket must be closed. It is not possible to reconnect to this socket
                    // after it is closed, which means a new socket instance has to be created.
                    socket.close();
                }

            } catch (Exception e) {

                Log.e("TCP", "C: Error", e);
                {
                    String log = String.format("tcp() err=%s", e.toString());
                    FileUtils.writeDebug(log, "PayCastAgent");
                }

            }

        }

        //Declare the interface. The method messageReceived(String message) will must be implemented in the MyActivity
        //class at on asynckTask doInBackground
        public interface OnMessageReceived {
            public void messageReceived(String message);
        }
    }
    public boolean getNetworkInfoFromServer(Context context, PropUtil propUtil, StbOptionEnv stbOpt,  List<CommandObject> commandList, List<DownFileInfo> downloadList, List<CommandObject> newcommandList) {
        // start timer...
        String reqUrl = null;
        String macAddr  = Utils.getMacAddrOnConnect(context);
        String queryString = null;
        XmlOptionParser xmlParser = new XmlOptionParser();
        boolean result = false;

        if (stbOpt.serverPort == 80) {
            reqUrl = String.format("http://%s/info/stb?" , stbOpt.serverHost);
            queryString = String.format("macAddress=%s&site=%s", macAddr , stbOpt.serverUkid);
        } else {
            reqUrl = String.format("http://%s:%d/info/stb?" , stbOpt.serverHost, stbOpt.serverPort);
            queryString = String.format("macAddress=%s&site=", macAddr ,stbOpt.serverUkid);
        }
        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(context.getString(R.string.serverSSLEnabled), context);
        if(LOG)
            Log.d(TAG, "startTimerTask() ssl=" + ssl);

        if (Boolean.valueOf(ssl)) {
            reqUrl = reqUrl.replace("http://", "https://");
        }
        //-
        if(LOG) {
            Log.d(TAG, "reqUrl=" + reqUrl);
            Log.d(TAG, "queryString=" + queryString);
        }

        String encodedQueryString = null;
        try {
            encodedQueryString = URLEncoder.encode(queryString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            {
                String log = String.format("getNetworkInfoFromServer() 1 err=%s", e.toString());
                FileUtils.writeDebug(log, "PayCastAgent");
            }
        }
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(reqUrl + queryString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return false;
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return false;
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return false;
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return false;
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return false;
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return false;
                }
                builder.append(line);
            }
            responseString = builder.toString();
            result =xmlParser.ParseXML(responseString, context, propUtil, stbOpt, commandList, downloadList, newcommandList);
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
            return false;
        }
        return result;
    }
    public static String HttpKioskResponseString(String url, KioskPayDataInfo kioskPayDataInfo) throws JSONException {
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setRequestProperty("content-type", "application/json");

        try {
            conn.setRequestMethod("POST"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoOutput(true); //쓰기모드 지정
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;
        OutputStreamWriter wr = null;
        try {
            wr = new OutputStreamWriter(conn.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONArray json1 = new JSONArray();
        JSONObject k = new JSONObject();


        k.put("tid", kioskPayDataInfo.tid);  //거래고유번호
        k.put("mid", kioskPayDataInfo.mid);  //가맹점 번호
        k.put("fnCd", kioskPayDataInfo.fnCd);    //발급사 코드
        k.put("fnName", kioskPayDataInfo.fnName);   //발급사명
        k.put("fnCd1", kioskPayDataInfo.fnCd1);    //매입사 코드
        k.put("fnName1", kioskPayDataInfo.fnName1);   //매입사명


        k.put("storeIdpay", kioskPayDataInfo.storeIdpay);
        k.put("totalindex", kioskPayDataInfo.totalindex);
        k.put("goodsAmt", kioskPayDataInfo.goodsAmt);
        k.put("goodsTotal", kioskPayDataInfo.goodsTotal);
        k.put("orderNumber", kioskPayDataInfo.orderNumber);
        k.put("orderDate", kioskPayDataInfo.orderDate);
        k.put("AuthCode", kioskPayDataInfo.authCode);
        //k.put("fnName", kioskPayDataInfo.fnName);
        k.put("CATID", kioskPayDataInfo.catID);

        JSONArray json = new JSONArray();
        for(int i = 0; i<kioskPayDataInfo.orderMenuList.size(); i++) {
            JSONObject j = new JSONObject();
            KioskOrderMenuItem menu = kioskPayDataInfo.orderMenuList.get(i);
            try {
                j.put("productID", menu.productID);
            } catch (JSONException e) {
                e.printStackTrace();
                {
                    String log = String.format("HttpKioskResponseString() productID err=%s", e.toString());
                    FileUtils.writeDebug(log, "PayCastAgent");
                }
            }
            try {
                j.put("productName", menu.productName);
            } catch (JSONException e) {
                e.printStackTrace();
                {
                    String log = String.format("HttpKioskResponseString() productName err=%s", e.toString());
                    FileUtils.writeDebug(log, "PayCastAgent");
                }
                return "";
            }
            try {
                j.put("orderCount", menu.orderCount);
            } catch (JSONException e) {
                e.printStackTrace();
                {
                    String log = String.format("HttpKioskResponseString() orderCount err=%s", e.toString());
                    FileUtils.writeDebug(log, "PayCastAgent");
                }
                return "";

            }
            int price = Integer.valueOf(menu.orderPrice);
            int count = Integer.valueOf(menu.orderCount);
            if((price!=0)&&(count!=0)) {
                try {
                    j.put("orderPrice", String.valueOf(price / count));
                } catch (JSONException e) {
                    e.printStackTrace();
                    {
                        String log = String.format("HttpKioskResponseString() orderPrice err=%s", e.toString());
                        FileUtils.writeDebug(log, "PayCastAgent");
                    }
                    return "";
                }
            }
            String isPackage = "0";
            if(menu.orderPackage)
                isPackage = "1";
            try {
                j.put("orderMenuPacking", isPackage);
            } catch (JSONException e) {
                e.printStackTrace();
                {
                    String log = String.format("HttpKioskResponseString() orderMenuPacking err=%s", e.toString());
                    FileUtils.writeDebug(log, "PayCastAgent");
                }
            }
            json.put(j);
        }
        k.put("orderMenu", json);
        json1.put(k);

        try {
            wr.write(json1.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        try {
            wr.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        try {
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }
    public static String onGetOrderPrintStringFrServer(String urlString)
    {
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);

        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
            if(LOG)
                Log.d(TAG, "MonitorAsynTask() resp="+responseString);
        }
        else
        {
            conn.disconnect();
        }
        return responseString;

    }
    public static String onGetStoreChgSync(String urlString)
    {
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }

    public static String getStoreInfoChgFromServer(String rcCommandid, Context c, StbOptionEnv stb) {
        String reqUrl = getServerStoreInfoChgUrl(stb, c);
        String resStr = "";

        if(LOG)
            Log.d(TAG, "getStoreInfoChgFromServer() reqUrl:" + reqUrl);
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(reqUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }
    private static String getServerStoreInfoChgUrl(StbOptionEnv stbOpt, Context c) {
        String serverUrl = "";
        if (stbOpt.serverPort == 80) {
            serverUrl = String.format("http://%s%s", stbOpt.serverHost , "/info/store");
        } else {
            serverUrl = String.format("http://%s:%d%s" , stbOpt.serverHost , stbOpt.serverPort , "/info/store");
        }
        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);

        if (Boolean.valueOf(ssl)) {
            serverUrl = serverUrl.replace("http://", "https://");
        }
        //-
        String encodedQueryString = String.format("storeId=%s" , stbOpt.storeId);
        return String.format("%s?%s", serverUrl, encodedQueryString);
    }
    public static String getNetworkInfoFromServer(Context c, StbOptionEnv stbOpt) {
        // start timer...
        String reqUrl = null;
        String queryString = null;

        if (stbOpt.serverPort == 80) {
            reqUrl = String.format("http://%s/info/stb?", stbOpt.serverHost);
            queryString = String.format("site=%s&deviceId=%s" , stbOpt.serverUkid, stbOpt.deviceId);
        } else {
            reqUrl = String.format("http://%s:%d/info/stb?" , stbOpt.serverHost , stbOpt.serverPort);
            queryString = String.format("site=%s&deviceId=%s" , stbOpt.serverUkid, stbOpt.deviceId);
        }
        // jason:serverssl: 서버 https 프로토콜 옵션(2017/10/12)
        String ssl = PropUtil.configValue(c.getString(R.string.serverSSLEnabled), c);
        URI url = null;

        if (Boolean.valueOf(ssl)) {
            reqUrl = reqUrl.replace("http://", "https://");

        }
        if(LOG) {
            Log.d(TAG, "reqUrl=" + reqUrl);
            Log.d(TAG, "queryString=" + queryString);
        }
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(reqUrl + queryString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }
    public static String sendFCMTokenToAuthServer(String reqUrl, String queryString) {
        URL Url = null; // URL화 한다.
        try {
            Url = new URL(reqUrl + "?"+queryString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
        HttpURLConnection conn = null; // URL을 연결한 객체 생성.
        try {
            conn = (HttpURLConnection) Url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        try {
            conn.setRequestMethod("GET"); // get방식 통신
        } catch (ProtocolException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        conn.setDoInput(true); // 읽기모드 지정
        conn.setConnectTimeout(MAX_CONNECTION_TIMEOUT);        // 통신 타임아웃
        conn.setReadTimeout(MAX_SOCKET_TIMEOUT);
        int resCode = -1;

        try {
            resCode = conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            conn.disconnect();
            return "";
        }
        String responseString = "";
        if(resCode==HttpURLConnection.HTTP_OK || resCode == HttpURLConnection.HTTP_CREATED)
        {

            InputStream is = null; //input스트림 개방
            try {
                is = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }

            StringBuilder builder = new StringBuilder(); //문자열을 담기 위한 객체
            BufferedReader reader = null; //문자열 셋 세팅
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                conn.disconnect();
                return "";
            }
            String line = null;

            while (true) {
                try {
                    if (!((line = reader.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                    conn.disconnect();
                    return "";
                }
                builder.append(line);
            }
            responseString = builder.toString();
            conn.disconnect();
        }
        else
        {
            conn.disconnect();
        }
        return responseString;
    }

    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;


    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        String status = null;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = "Wifi enabled";
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }
    public static int getWifiRssi(Context c)
    {
        int conn = NetworkUtil.getConnectivityStatus(c);
        int rssi = 0xfff;
        if (conn == NetworkUtil.TYPE_WIFI) {
            WifiManager wm = (WifiManager) c.getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wm.getConnectionInfo();
            rssi = wifiInfo.getRssi();
        }
        return 0;

    }
}
