package com.example.myapplication.presentation

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.myapplication.R
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(),
    CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    lateinit var conectar: Button
    lateinit var textoIng: EditText
    lateinit var lResultado: TextView
    var activityContext: Context? = null

    private var deviceConnected: Boolean = false
    private val payloadPath = "/APP_OPEN"
    lateinit var nodeID: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // No apagar pantalla
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        activityContext = this
        conectar = findViewById(R.id.boton)
        textoIng = findViewById(R.id.ingtexto)
        lResultado = findViewById(R.id.resultado)

        // Boton para vincular o mandar mensaje
        conectar.setOnClickListener {
            if (!deviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                getNodes(tempAct)
            } else {
                val mensaje = textoIng.text.toString().trim()
                if (mensaje.isNotEmpty()) {
                    sendMessage(mensaje)
                    textoIng.setText("")
                }
            }
        }
    }

    // Buscar celular
    private fun getNodes(context: Context) {
        launch(Dispatchers.Default) {
            val nodeList = Wearable.getNodeClient(context).connectedNodes
            try {
                val nodes = Tasks.await(nodeList)
                for (node in nodes) {
                    nodeID = node.id
                    deviceConnected = true
                }
                withContext(Dispatchers.Main) {
                    if (deviceConnected) {
                        lResultado.text = getString(R.string.msg_connected_phone, nodeID)
                        conectar.text = getString(R.string.btn_send)
                    } else {
                        lResultado.text = getString(R.string.msg_no_phone)
                    }
                }
            } catch (e: Exception) {
                Log.e("Error", "No hay nodos")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {}
    }

    // Mandar mensaje al telefono
    private fun sendMessage(mensaje: String) {
        if (!deviceConnected) return
        
        Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, payloadPath, mensaje.toByteArray(StandardCharsets.UTF_8))
    }

    // Recibir mensaje del telefono
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data, StandardCharsets.UTF_8)
        nodeID = messageEvent.sourceNodeId
        deviceConnected = true

        runOnUiThread {
            lResultado.text = getString(R.string.msg_from_phone, message)
            conectar.text = getString(R.string.btn_send)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {}

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
