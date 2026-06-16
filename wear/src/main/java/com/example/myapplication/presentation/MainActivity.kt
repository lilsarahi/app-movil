package com.example.myapplication.presentation

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.R
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(),
    CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    lateinit var conectar: Button
    lateinit var textoIng: EditText
    lateinit var lResultado: TextView
    var activityContext: Context? = null

    private var deviceConnected: Boolean = false
    private val PAYLOAD_PATH = "/APP_OPEN"
    lateinit var nodeID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        activityContext = this
        conectar = findViewById(R.id.boton)
        textoIng = findViewById(R.id.ingtexto)
        lResultado = findViewById(R.id.resultado)

        conectar.setOnClickListener {
            if (!deviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                getNodes(tempAct)
            } else {
                val mensaje = textoIng.text.toString()
                if (mensaje.isNotEmpty()) {
                    sendMessage(mensaje)
                }
            }
        }
    }

    private fun getNodes(context: Context) {
        launch(Dispatchers.Default) {
            val nodeList = Wearable.getNodeClient(context).connectedNodes
            try {
                val nodes = Tasks.await(nodeList)
                for (node in nodes) {
                    nodeID = node.id
                    deviceConnected = true
                }
                if (deviceConnected) {
                    withContext(Dispatchers.Main) {
                        lResultado.text = "Conectado al Teléfono: $nodeID"
                        conectar.text = "Enviar"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        lResultado.text = "No se encontró teléfono"
                    }
                }
            } catch (exception: Exception) {
                Log.d("Error en el nodo", exception.toString())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendMessage(mensaje: String) {
        if (!deviceConnected) return
        
        val sendMessageTask = Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, PAYLOAD_PATH, mensaje.toByteArray(StandardCharsets.UTF_8))
            
        sendMessageTask.addOnSuccessListener {
            Log.d("sendMessage", "Mensaje enviado desde reloj")
            textoIng.setText("")
        }.addOnFailureListener { e ->
            Log.d("sendMessage", "Error en reloj: ${e.message}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data, StandardCharsets.UTF_8)
        
        nodeID = messageEvent.sourceNodeId
        deviceConnected = true

        runOnUiThread {
            lResultado.text = "Teléfono: $message"
            conectar.text = "Enviar"
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {}

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
