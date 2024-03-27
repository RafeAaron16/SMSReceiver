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
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MyMessageReceiver {

    private ArrayList<String> smsList = new ArrayList<>();
    private ArrayList<String> jsonSMS = new ArrayList<>();
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        checkPermissions();
        MySmsReceiver.bindListener(this);
    }

    public void sendData(View view){

        //Getting URL path
        EditText urlElement = (EditText) findViewById(R.id.url);

        String url = urlElement.getText().toString();

        if(networkState()){
            SendDataToServer(url, jsonSMS);
        }else{
            Toast.makeText(this, "Please connect to wifi or turn on your mobile data", Toast.LENGTH_SHORT).show();
        }


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

                    try {
                        JSONObject mysms= new JSONObject();
                        mysms.put("Number", trimNumber(body));
                        mysms.put("Amount", trimAmount(body));
                        mysms.put("ID", trimID(body));

                        jsonSMS.add(mysms.toString());
                    }
                    catch(Exception ex){
                        ex.printStackTrace();
                    }
                }

            }while(cursor.moveToNext());

            Log.d("Messages", jsonSMS.toString());

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
            }

            if(networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
                isMobileConn |= networkInfo.isConnected();
            }
        }

        return isWifiConn || isMobileConn;
    }

    public void SendDataToServer(String urlToPostTo, ArrayList<String> mydata){

        TalkToServer tts = new TalkToServer(urlToPostTo, this);
        tts.execute(mydata);
    }

}