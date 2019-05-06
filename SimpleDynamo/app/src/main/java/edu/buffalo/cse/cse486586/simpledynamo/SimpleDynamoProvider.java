package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
//
//import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoActivity.sharedPref;
//import static edu.buffalo.cse.cse486586.simpledynamo.SimpleDynamoActivity.portNumber;


public class SimpleDynamoProvider extends ContentProvider {

    private final Object lockObj = new Object();
    private volatile boolean flag = true;

    Map<String, String> newData = new HashMap<String, String>();

    static volatile String portNumber = "";
    static volatile SharedPreferences sharedPref;
    static volatile String node_id = "";
    static volatile Map<String, String> hashToPort = new HashMap<String, String>();
    static final String TAG = SimpleDynamoActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;

    static HashMap<String, String> successor = new HashMap<String, String>();
    static HashMap<String, String> predecessor = new HashMap<String, String>();
    static volatile List<String> nodeList = new ArrayList<String>();

    static final Map<String, Integer> counterMap = new HashMap<String, Integer>();

    Uri uri;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        synchronized (this) {
            try {


                if (selection.equals("*")) {
                    Log.i("DELETE FOR * at ", portNumber);
                    modifyEditor("*");
                    return 0;

                } else if (selection.equals("@")) {
                    modifyEditor("@");
                    return 0;
                } else {

                    String nodeWithKey = getNode(selection);

                    for (int i = 0; i < 3; i++) {

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), getPort(nodeWithKey));
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("DELETE|" + selection);

                        nodeWithKey = getSuccessor(nodeWithKey);

                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


        return 0;
    }

    public synchronized void modifyEditor(String selection) {
        int port = 11108;
        int p = port / 2;


        if (selection.equals("*")) {

            for (int i = 0; i < 5; i++) {

                if (portNumber.equals(p + "")) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.clear();
                    editor.commit();

                } else {
                    try {
                        Socket socket = null;

                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("DELETE_LOCAL|" + "@");
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
                port += 4;
                p = port / 2;
            }


        } else if (selection.equals("@")) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.commit();

        } else {

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(selection);
            editor.commit();

        }

    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        try {

            synchronized (lockObj) {

                if (flag) {
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (uri == null) {
                    uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
                }


                String key = (String) values.get("key");
                String value = (String) values.get("value");


                int mapVal = 0;

                String nodeToInsert = getNode(key);

                for (int i = 0; i < 3; i++) {

                    if (counterMap.containsKey(key)) {
                        mapVal = counterMap.get(key);
                    }

                    Log.i("INSERT CALLED for", key + " " + value + " insert at " + nodeToInsert);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), getPort(nodeToInsert));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("INSERT|" + key + "|" + value + "," + mapVal);

                    BufferedReader in;

                    String ack = "";

                    try {
                        socket.setSoTimeout(1000);
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.i("%%%%%% INSERT %%%%", " Expecting response for " + key + " " + value + " from port " + nodeToInsert);
                        ack = in.readLine();

                        if(ack != null) {
                            String acks[] = ack.split(",");

                            Log.i("Received_ACKS ", Arrays.toString(acks));

                            if (Integer.parseInt(acks[1]) > mapVal) {
                                counterMap.put(key, Integer.parseInt(acks[1]));
                            }
                        }

                    } catch (Exception e) {
                        Log.i("MILA " , "%%%%%%%%%%%%%%%%%%%%% " + ack);
                        e.printStackTrace();
                    }

                    nodeToInsert = getSuccessor(nodeToInsert);

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private synchronized String getSuccessor(String nodeToInsert) {

        try {
            return hashToPort.get(successor.get(genHash(nodeToInsert)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private synchronized String getNode(String key) {
        Log.i("GET_NODE ", key + " at port " + portNumber);

        try {

            String keyHash = genHash(key);

            if (keyHash.compareTo("177ccecaec32c54b82d5aaafc18a2dadb753e3b1") < 0)
                return "5562";
            else if (keyHash.compareTo("208f7f72b198dadd244e61801abe1ec3a4857bc9") < 0)
                return "5556";
            else if (keyHash.compareTo("33d6357cfaaf0f72991b0ecd8c56da066613c089") < 0)
                return "5554";
            else if (keyHash.compareTo("abf0fd8db03e5ecb199a9b82929e9db79b909643") < 0)
                return "5558";
            else if (keyHash.compareTo("c25ddd596aa7c81fa12378fa725f706d54325d12") < 0)
                return "5560";
            else if (keyHash.compareTo("c25ddd596aa7c81fa12378fa725f706d54325d12") > 0)
                return "5562";

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*String node = "";
        try {
            String keyHash = genHash(key);
            if(!nodeList.contains(keyHash)) {
                Log.i("Adding key to list, ",key + " " + nodeList.toString());
                nodeList.add(keyHash);
            }
            Collections.sort(nodeList);
            int index = 0;
            for (index = 0; index < nodeList.size(); index++) {
                if (nodeList.get(index).equals(keyHash))
                    break;
            }
            Log.i("NODE LIST IS ", nodeList.toString() + " index " + index);
            if (index == 0 || index == 5)
                node = "5562";
            else
                node = hashToPort.get(nodeList.get(index + 1));
            nodeList.remove(index);
            Log.i("Removing key from list ",key + " " + nodeList.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        return "";
    }

    @Override
    public boolean onCreate() {

        TelephonyManager tel = (TelephonyManager) getContext().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        portNumber = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        sharedPref = getContext().getApplicationContext().getSharedPreferences("FILE", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();

        try {
            hashToPort.put(genHash("5554"), "5554");
            hashToPort.put(genHash("5556"), "5556");
            hashToPort.put(genHash("5558"), "5558");
            hashToPort.put(genHash("5560"), "5560");
            hashToPort.put(genHash("5562"), "5562");


            successor.put(genHash("5562"), genHash("5556"));
            successor.put(genHash("5556"), genHash("5554"));
            successor.put(genHash("5554"), genHash("5558"));
            successor.put(genHash("5558"), genHash("5560"));
            successor.put(genHash("5560"), genHash("5562"));

            predecessor.put(genHash("5562"), genHash("5560"));
            predecessor.put(genHash("5556"), genHash("5562"));
            predecessor.put(genHash("5554"), genHash("5556"));
            predecessor.put(genHash("5558"), genHash("5554"));
            predecessor.put(genHash("5560"), genHash("5558"));


            nodeList.add(genHash("5562"));
            nodeList.add(genHash("5556"));
            nodeList.add(genHash("5554"));
            nodeList.add(genHash("5558"));
            nodeList.add(genHash("5560"));

            Collections.sort(nodeList);

            //if(sharedPref.getAll().size() != 0) {
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "RECOVER");
            //}


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
        }

        return false;
    }


    public void recover() {

        flag = true;
        synchronized (lockObj) {
            Log.i("RECOVER CALLED *** ", portNumber + " ***************************");
            MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
            int port = 11108;
            int p = port / 2;

            try {

                int k = 0;
                for (int i = 0; i < 5; i++) {

                    if (portNumber.equals(p + "")) {
                        port += 4;
                        p = port / 2;
                        continue;

                    } else {

                        Socket socket = null;

                        String cnt = null;
                        BufferedReader in = null;
                        try {


                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);

                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("QUERY_LOCAL|" + "@");

                            socket.setSoTimeout(300);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            cnt = in.readLine();

                        } catch (Exception e) {
                            e.printStackTrace();
//                            port = port - 4;
//                            p = port / 2;
                            k++;
                            if (k < 3) {
                                Thread.sleep(50);
                                i--;
                                continue;
                            }
                            //else
                                //k = 0;

                        }

                        if (cnt == null)
                            cnt = "0";

                        //Log.i("RECOVER COUNT: loop", cnt);


                        for (int j = 0; j < Integer.parseInt(cnt); j++) {
                            String keyval = in.readLine();
                            Log.i("RECOVER KEYVAL ", keyval);
                            String input[] = keyval.split("\\|");
                            // Log.i("RECOVER KEY VAL IS ", keyval);
                            if (belongsToNode(input[0])) {
                                Log.i("PASSED:RECOVER INSERT ", keyval);


                                String existingValue = sharedPref.getString(input[0], "NOT_FOUND");
                                Log.i("EXISTING_VALUE ", existingValue);

                                if (existingValue.equals("NOT_FOUND")) {
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(input[0], input[1]);
                                    editor.commit();
                                    Log.i("RECOVER_INSERT IF", input[0] + " " + input[1]);
                                } else {
                                    //int currentVal = Integer.parseInt(existingValue.substring(existingValue.indexOf(",") + 1)) + 1;
                                    int oldCnt = Integer.parseInt(existingValue.substring(existingValue.indexOf(",") + 1));
                                    int newCnt = Integer.parseInt(input[1].substring(input[1].indexOf(",") + 1));
                                    int newCounter = Math.max(oldCnt, newCnt) + 1;

                                    String newVal = existingValue.substring(0, existingValue.indexOf(",")) + "," + newCounter;
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString(input[0], newVal);
                                    editor.commit();
                                    Log.i("RECOVER_INSERT ELSE", input[0] + " " + newVal);
                                }

                            }
                        }

                    }
                    port += 4;
                    p = port / 2;
                }

//                if(newData.size() != 0) {
//                    for(Map.Entry<String, String> entry: newData.entrySet()){
//                        String val = sharedPref.getString(entry.getKey(), "NOT_FOUND");
//                        SharedPreferences.Editor editor = sharedPref.edit();
//                        if(val.equals("NOT_FOUND")){
//                            editor.putString(entry.getKey(), entry.getValue()+","+0);
//                            editor.commit();
//                        } else {
//                            int newCnt = Integer.parseInt(val.substring(val.indexOf(",") + 1)) + 1;
//                            String newVal = val.substring(0, val.indexOf(",")) + newCnt;
//                            editor.putString(entry.getKey(), newVal);
//                            editor.commit();
//                        }
//                    }
//                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i("RECOVER ENDED *** ", portNumber + " ***************************");
        }
        flag = false;
    }

    private boolean belongsToNode(String key) {

        String keyNode = "";
        String prevNode = "";

        try {

            if ((keyNode = getNode(key)).equals(portNumber))
                return true;
            else if ((prevNode = hashToPort.get(predecessor.get(genHash(portNumber)))).equals(keyNode)) {
                return true;
            } else if ((prevNode = hashToPort.get(predecessor.get(genHash(prevNode)))).equals(keyNode)) {
                return true;
            } else
                return false;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }


    volatile List<String> queryresults = new ArrayList<String>();

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {


        synchronized (lockObj) {

            if (flag) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});
            String value = "";


            if (selection.equals("RECOVER")) {
                for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
                    String key = entry.getKey();
                    value = (String) entry.getValue();

                    cursor.addRow(new Object[]{key, value});
                }
                return cursor;
            }

            if (selection.equals("@")) {
                for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
                    String key = entry.getKey();
                    value = (String) entry.getValue();

                    value = value.substring(0, value.indexOf(","));

                    cursor.addRow(new Object[]{key, value});
                }
                return cursor;
            }

            if (selection.equals("*")) {

                Log.i(" ***** CALLED ", portNumber);
                return getAllCursor();
            }


            String nodeWithkey = getNode(selection);

            for (int i = 0; i < 3; i++) {

                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), getPort(nodeWithkey));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("SELECTION|" + selection);

                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    Log.i("%%%%%% SELECTION %%%%", " Expecting response for " + selection + " from port " + nodeWithkey);

                    socket.setSoTimeout(300);
                    value = in.readLine();

                    if (value == null || value == "" || value.equals("NOT_FOUND")) {
                        nodeWithkey = getSuccessor(nodeWithkey);
                        continue;
                    }

                    queryresults.add(value);
                    Log.i("ADDING TO LIST ", queryresults.toString() + " VALUE**** " + value);
                    nodeWithkey = getSuccessor(nodeWithkey);

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("****EXCEPTION ", nodeWithkey);
                    nodeWithkey = getSuccessor(nodeWithkey);
                    continue;
                }

            }

            Log.i("QUERIES LIST ", queryresults.toString() + " selection is " + selection);
            String latestVersion = getLatestVersion(queryresults);
            cursor.addRow(new Object[]{selection, latestVersion});
            queryresults.clear();
            return cursor;

        }
    }

    private synchronized String getLatestVersion(List<String> queryresults) {

        Log.i(" getlatestVersion ", queryresults.toString());

        int max = -1;
        String ret = "";

        for (String value : queryresults) {
            String val[] = value.split(",");
            if (Integer.parseInt(val[1]) > max) {
                max = Integer.parseInt(val[1]);
                ret = val[0];
            }
        }

        Log.i("Return value ", ret);
        return ret;

    }

    private synchronized Cursor getAllCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});


        for (int i = 0; i < nodeList.size(); i++) {
            String node = hashToPort.get(nodeList.get(i));

            if (node.equals(portNumber)) {//Handle local calls to avoid deadlock as there is only one server thread.
                int c = 0;
                for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
                    String key = entry.getKey();
                    String value = (String) entry.getValue();

                    value = value.substring(0, value.indexOf(","));

                    cursor.addRow(new Object[]{key, value});
                    c++;
                }
                Log.i("COUNT_LOADED frm LOCAL ", c + "");
                continue;
            }
            String remote = String.valueOf(Integer.parseInt(node) * 2);
            try {

                Log.i("getAllCursor called ", remote);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remote));

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("QUERY_LOCAL");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String cnt = in.readLine();

                Log.i("COUNT: loop", cnt);
                for (int j = 0; j < Integer.parseInt(cnt); j++) {
                    String keyval = in.readLine();
                    String input[] = keyval.split("\\|");
                    String vals[] = input[1].split(",");
                    Log.i("getAllCursor returned ", keyval);
                    cursor.addRow(new Object[]{input[0], vals[0]});
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("****Exception at ", remote);
                continue;
            }
        }


        return cursor;

    }

    private synchronized String getLastNode(String nodeWithkey) {
        String lastNode = "";

        String next = getSuccessor(nodeWithkey);

        lastNode = getSuccessor(next);

        /*if(lastNode.equals("DEAD")) // TODO: Implement during failure handling
            return next;*/

        return lastNode;

    }

    private int getPort(String node) {

        if (node.equals("5554"))
            return 11108;
        else if (node.equals("5556"))
            return 11112;
        else if (node.equals("5558"))
            return 11116;
        else if (node.equals("5560"))
            return 11120;
        else if (node.equals("5562"))
            return 11124;

        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private synchronized String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];


            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    String inputLine;
                    if ((inputLine = in.readLine()) != null) {
                        String msg[] = inputLine.split("\\|");

                        if (msg[0].equals("STORE")) {
                            Log.i("STORE CALLED ", inputLine);
                            ContentValues values = new ContentValues();
                            values.put("key", msg[2]);
                            values.put("value", msg[3]);

                            uri = buildUri("content", "edu.buffalo.cse.cse486586.simpledynamo.provider");
                            insert(uri, values);

                        } else if (msg[0].equals("REPLICA")) {

                            Log.i("REPLICA CALLED ", inputLine);


                            String keyData = sharedPref.getString(msg[2], "NOT_FOUND");

                            // Versioning

                            if (keyData.equals("NOT_FOUND")) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(msg[2], msg[3]);
                                editor.commit();
                            } else {
                                String finalValue = keyData + "|" + msg[3];
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(msg[2], finalValue);
                                editor.commit();
                            }


                        } else if (msg[0].equals("SELECTION")) {

                            Log.i("SELECTION CALLED ", inputLine);
                            String val = sharedPref.getString(msg[1], "NOT_FOUND");

                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            out.println(val);

                        } else if (msg[0].equals("QUERY_LOCAL")) {
                            Log.i("QUERY_LOCAL CALLED ", portNumber);
                            Cursor cursor = query(uri, null, "RECOVER", null, null);

                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            int cnt = cursor.getCount();
                            out.println(cnt);

                            Log.i("COUNT_SENT ", cnt + "");
                            if (cursor.moveToFirst()) {
                                do {
                                    String key = cursor.getString(cursor.getColumnIndex("key"));
                                    String value = cursor.getString(cursor.getColumnIndex("value"));

                                    //value = value.substring(0, value.indexOf(","));

                                    Log.i(" RET QUERY_CALLED ", key + " " + value + " at " + portNumber);
                                    out.println(key + "|" + value);
                                } while (cursor.moveToNext());
                            }
                            cursor.close();
                        } else if (msg[0].equals("DELETE")) {

                            Log.i("DELETE CALLED ", inputLine);

                            modifyEditor(msg[1]);

                        } else if (msg[0].equals("DELETE_LOCAL")) {

                            Log.i("DELETE_LOCAL CALLED ", inputLine);

                            modifyEditor("@");

                        } else if (msg[0].equals("INSERT")) {

                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            //out.println("DELETE|" + selection);

                            Log.i("INSERT CALLED ", inputLine);

                            String existingValue = sharedPref.getString(msg[1], "NOT_FOUND");

                            String newValue = "";

                            if (existingValue.equals("NOT_FOUND")) {
                                newValue = msg[2];
                            } else {

                                int oldCnt = Integer.parseInt(existingValue.substring(existingValue.indexOf(",") + 1));
                                int newCnt = Integer.parseInt(msg[2].substring(msg[2].indexOf(",") + 1));
                                int newCounter = Math.max(oldCnt, newCnt) + 1;

                                String v = msg[2].substring(0, msg[2].indexOf(","));
                                newValue = v + "," + newCounter;
                            }

                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(msg[1], newValue);
                            editor.commit();

                            ////
                            Log.i("Value inserted ", sharedPref.getString(msg[1], "NOT_INSERTED"));

                            out.println(newValue);

                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    protected void onProgressUpdate(String... strings) {

        return;
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String msg = msgs[0];

            try {
                String input[] = msg.split("\\|");

                if (input[0].equals("RECOVER")) {
                    recover();
                } else if (input[0].equals("STORE") || input[0].equals("REPLICA")) {


                    Log.i("STORE|REPLICA CALLED ", msg + " server called at port " + getPort(input[1]));
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), getPort(input[1]));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(msg);

                }


            } catch (Exception e) {
                Log.e("EXCEPTION CAUGHT AT ", portNumber);
                e.printStackTrace();
            }

            return null;
        }

        private int getPort(String node) {

            if (node.equals("5554"))
                return 11108;
            else if (node.equals("5556"))
                return 11112;
            else if (node.equals("5558"))
                return 11116;
            else if (node.equals("5560"))
                return 11120;
            else if (node.equals("5562"))
                return 11124;

            return 0;
        }


    }
}