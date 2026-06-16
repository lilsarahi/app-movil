package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
                    Log.d("NODO", node.toString())
                    Log.d("NODO", "El id del nodo es: ${node.id}")
                    nodeID = node.id
                    deviceConnected = true
                }
                if (deviceConnected) {
                    withContext(Dispatchers.Main) {
                        lResultado.text = "Conectado al reloj: $nodeID"
                        conectar.text = "Enviar Mensaje"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        lResultado.text = "No se encontraron nodos"
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
        val sendMessageTask = Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, PAYLOAD_PATH, mensaje.toByteArray(StandardCharsets.UTF_8))
            
        sendMessageTask.addOnSuccessListener {
            Log.d("sendMessage", "Mensaje enviado correctamente")
            textoIng.setText("")
        }.addOnFailureListener { e ->
            Log.d("sendMessage", "Error al enviar mensaje ${e.message}")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("onMessageReceived", messageEvent.toString())
        val message = String(messageEvent.data, StandardCharsets.UTF_8)
        
        if (!deviceConnected) {
            nodeID = messageEvent.sourceNodeId
            deviceConnected = true
            runOnUiThread {
                conectar.text = "Enviar Mensaje"
            }
        }

        runOnUiThread {
            lResultado.text = "Reloj dice: $message"
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {}

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
