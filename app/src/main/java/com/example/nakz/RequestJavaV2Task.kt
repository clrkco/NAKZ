package com.example.nakz

import android.app.Activity
import android.os.AsyncTask

import com.google.cloud.dialogflow.v2beta1.DetectIntentRequest
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse
import com.google.cloud.dialogflow.v2beta1.QueryInput
import com.google.cloud.dialogflow.v2beta1.SessionName
import com.google.cloud.dialogflow.v2beta1.SessionsClient


class RequestJavaV2Task internal constructor(internal var activity: Activity, private val session: SessionName, private val sessionsClient: SessionsClient, private val queryInput: QueryInput) : AsyncTask<Void, Void, DetectIntentResponse>() {

    override fun doInBackground(vararg voids: Void): DetectIntentResponse? {
        try {
            val detectIntentRequest = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .build()
            return sessionsClient.detectIntent(detectIntentRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    override fun onPostExecute(response: DetectIntentResponse) {
        if(BluetoothConnect.mode)
            (activity as HotwordMain).callbackV2(response)
        else
            (activity as MAIN).callbackV2(response)
    }
}
