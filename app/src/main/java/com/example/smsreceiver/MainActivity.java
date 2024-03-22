package com.example.smsreceiver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MyMessageReceiver {

    private ArrayList<String> smsList = new ArrayList<>();
    private ListView listView;

    private Boolean mynetworkstate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        checkPermissions();
        mynetworkstate = networkState();
        MySmsReceiver.bindListener(this);
    }

    public void sendData(View view){

        //Getting URL path
        EditText urlElement = (EditText) findViewById(R.id.url);
        EditText urlPathELement = (EditText) findViewById(R.id.urlpath);

        String url = urlElement.getText().toString() + urlPathELement.getText().toString();

        //Creating JSON Object
        ArrayList<String> my_Data = new ArrayList<String>();
        my_Data.add(0, "Rafe Aaron");
        my_Data.add(1, "256778673874");
        my_Data.add(2, "150000");
        my_Data.add(3, "2024-03-18 07:25:21");
        my_Data.add(4, "23454323534");

        JSONObject preparedData = dataToSendToServer(my_Data);

        Toast.makeText(this, "The data has been prepared", Toast.LENGTH_SHORT).show();

        PostDataToServer post = new PostDataToServer(this, url);

        Toast.makeText(this, "Post class created", Toast.LENGTH_SHORT).show();

        post.doInBackground(preparedData.toString());
    }

    public void sendSMS(View view){

        EditText number = (EditText) findViewById(R.id.url);
        String Destination_Address = number.getText().toString();

        EditText message = (EditText) findViewById(R.id.urlpath);
        String Destination_message = message.getText().toString();

        //Setting the service center address if needed
        String scAddress = null;

        //Set pending intent to broadcast
        //When message is sent and when delivered
        PendingIntent sentIntent = null, deliveryIntent = null;

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(Destination_Address, scAddress,Destination_message,sentIntent,deliveryIntent);
    }

    public void checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
            }

            else{
                Toast.makeText(this, "SMS sending permissions already enabled. Enjoy", Toast.LENGTH_SHORT).show();
            }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 1);
        }

        else{
            Toast.makeText(this, "SMS receiving permissions already enabled. Enjoy", Toast.LENGTH_SHORT).show();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, 1);
        }

        else{
            Toast.makeText(this, "SMS read permissions already enabled. Enjoy", Toast.LENGTH_SHORT).show();
            readSms();
        }
    }

    @Override
    public void messageRecieved(String message) {
        ArrayAdapter<String> newAdapter = (ArrayAdapter<String>) listView.getAdapter();
        newAdapter.notifyDataSetChanged();
    }

    public void readSms(){
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null);

        if(cursor != null && cursor.moveToFirst()){
            do{
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                if(address.equals("MTNMobMoney") && body.contains("received") && body.contains("Reason:")) {
                    smsList.add("Sender: " + trimName(body) + "\nNumber: " + trimNumber(body)
                            + "\nAmount: UGX " + trimAmount(body) + "\nTimeStamp: " + TimeStamp(body)
                            + "\nID: " + trimID(body)
                    );
                    //smsList.add(address);
                }

            }while(cursor.moveToNext());

            if(cursor != null){
                cursor.close();
            }
        }
    }

    public String trimID(String message){
        String id = "";
        Boolean begin = false;

        for(int i = 2; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == ':' && message.charAt(i - 2) == 'D'){
                begin = true;
                continue;
            }

            if(begin){
                id += "" + message.charAt(i) + "";
            }

            /*if(message.charAt(i + 1) == '.' && i == message.length() - 1 ){
                break;
            }*/

        }

        return id;
    }

    public String trimName(String message){
        String name = "";
        Boolean begin = false;
        int number = 0;

        for(int i = 2; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == 'm'){
                begin = true;
                continue;
            }

            if(begin){
                name += "" + message.charAt(i) + "";
                number++;
            }

            if(message.charAt(i) == ',' && message.charAt(i + 1) == ' '){
                break;
            }

        }

        return name;
    }

    public String trimNumber(String message){
        String sender = "";
        Boolean begin = false;
        int number = 0;

        for(int i = 2; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == ','){
                begin = true;
                continue;
            }

            if(begin){
                sender += "" + message.charAt(i) + "";
                number++;
            }

            if(number == 12){
                break;
            }

        }

        return sender;
    }

    public String trimAmount(String message){
        String amount = "";
        Boolean begin = false;

        for(int i = 1; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == 'X'){
                begin = true;
                continue;
            }

            if(begin){
                amount += "" + message.charAt(i) + "";
            }

            if(message.charAt(i + 1) == ' ' && message.charAt(i + 2) == 'f'){
                break;
            }

        }

        return amount;

    }

    public String TimeStamp(String message){
        String timestamp = "";
        Boolean begin = false;

        for(int i = 2; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == 'n' && message.charAt(i - 2) == 'o'){
                begin = true;
                continue;
            }

            if(begin){
                timestamp += "" + message.charAt(i) + "";
            }

            if(message.charAt(i + 1) == '.'){
                break;
            }

        }

        return timestamp;
    }

    public boolean networkState(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isWifiConn = false;
        boolean isMobileConn = false;

        for (Network network : connectivityManager.getAllNetworks()){
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);

            if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                isWifiConn |= networkInfo.isConnected();
                Toast.makeText(this, "You are connected to a wifi network", Toast.LENGTH_SHORT).show();
            }

            if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                isMobileConn |= networkInfo.isConnected();
                Toast.makeText(this, "You are connected to a mobile network", Toast.LENGTH_SHORT).show();
            }
        }

        return isWifiConn || isMobileConn;
    }

    public JSONObject dataToSendToServer(ArrayList mydata) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("Name", mydata.get(0));
            jsonObject.put("Number", mydata.get(1));
            jsonObject.put("Amount", mydata.get(2));
            jsonObject.put("Time Stamp", mydata.get(3));
            jsonObject.put("ID", mydata.get(4));
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return jsonObject;
    }

}

class PostDataToServer extends AsyncTask<String, Void, String>{

    private Context myContext;
    private String url;

    public PostDataToServer(Context context, String urlToPostTo){
        this.myContext = context;
        this.url = urlToPostTo;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            //Creating Url to post the data
            URL url = new URL(this.url);

            //Create an HTTPUrlConnection
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");

            //Setting content type and accept type
            client.setRequestProperty("Content-Type", "application/json");
            client.setRequestProperty("Accept", "application/json");

            //Set client to be able to output
            client.setDoOutput(true);

            //Creating an output stream and posting the data.

            try (OutputStream os = client.getOutputStream()) {
                byte[] input = strings[0].getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();

                //create a variable for the response
                String responseLine = null;

                //writing the response
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                //Displaying success message
                Toast.makeText(this.myContext, "Data has been posted to the API", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this.myContext, "Failed to push the data to the server: " + e.getMessage(), Toast.LENGTH_SHORT).show();

        }

        return null;
    }
}