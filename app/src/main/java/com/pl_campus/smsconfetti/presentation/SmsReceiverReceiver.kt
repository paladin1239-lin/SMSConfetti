package com.pl_campus.smsconfetti.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.pl_campus.smsconfetti.utils.Utils

class SmsReceiverReceiver(
    private val onProperSmsReceived: () -> Unit
): BroadcastReceiver() {
    override fun onReceive(p0: Context?, intent: Intent?) {
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent?.action)){
            val msg = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            msg.forEach {  smsMsg->
                val sender = smsMsg.displayOriginatingAddress
                val body = smsMsg.messageBody
                if(sender.equals(Utils.CHOSEN_SENDER) && body.equals(Utils.TEMPLATE)){
                    onProperSmsReceived.invoke()
                }
            }
        }

    }
}