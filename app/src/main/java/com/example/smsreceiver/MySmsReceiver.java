package com.example.smsreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MySmsReceiver extends BroadcastReceiver {
    private static final String TAG = MySmsReceiver.class.getSimpleName();
    private static final String pdu_type = "pdus";

    private static MyMessageReceiver mListener;


    @Override
    public void onReceive(Context context, Intent intent) {
        //Getting the SMS Message
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String strMessage = "";

        String format = bundle.getString("format");

        //Retrieve the SMS message received
        Object[] pdus = (Object[]) bundle.get(pdu_type);

        if(pdus != null){
            msgs = new SmsMessage[pdus.length];

            for(int i = 0; i < msgs.length; i++){
                msgs[i] =SmsMessage.createFromPdu((byte[] ) pdus[i]);

                if(msgs[i].getOriginatingAddress() == "MTNMobileMoney") {
                    strMessage += "Sender :" + trimSender(msgs[i].getMessageBody());
                    strMessage += " Amount Sent :" + trimAmount(msgs[i].getMessageBody()) + "\n";
                    strMessage += " Time Stamp :" + TimeStamp(msgs[i].getMessageBody()) + "\n";
                }

                mListener.messageRecieved(strMessage);
            }
        }
    }

    public String trimSender(String message){
        String sender = "";
        Boolean begin = false;

        for(int i = 0; i < message.length(); i++){
            if(message.charAt(i) == '('){
                begin = true;
                continue;
            }

            if(begin){
                sender += "" + message.charAt(i) + "";
            }

            if(message.charAt(i + 1) == ')'){
                break;
            }

        }

        return sender;
    }

    public String trimAmount(String message){
        String amount = "";
        Boolean begin = false;

        for(int i = 1; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == 'd'){
                begin = true;
                continue;
            }

            if(begin){
                amount += "" + message.charAt(i) + "";
            }

            if(message.charAt(i + 1) == ' '){
                break;
            }

        }

        return amount;

    }

    public String TimeStamp(String message){
        String timestamp = "";
        Boolean begin = false;

        for(int i = 2; i < message.length(); i++){
            if(message.charAt(i) == ' ' && message.charAt(i - 1) == 't' && message.charAt(i - 2) == 'a'){
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

    public static void bindListener(MyMessageReceiver listener){
        mListener = listener;
    }
}