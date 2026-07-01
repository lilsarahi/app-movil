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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.nio.charset.StandardCharsets

data class MensajeDB(
    val usuario: String,
    val contenido: String,
    val _id: String? = null
)

interface ApiService {
    @GET("mensajes")
    suspend fun obtenerMensajes(): List<MensajeDB>

    @POST("mensajes")
    suspend fun enviarMensaje(@Body mensaje: MensajeDB): MensajeDB
}

class MainActivity : AppCompatActivity(),
    CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    lateinit var conectar: Button
    lateinit var btnGet: Button
    lateinit var btnPost: Button
    lateinit var textoIng: EditText
    lateinit var lResultado: TextView
    var activityContext: Context? = null

    private var deviceConnected: Boolean = false
    private val payloadPath = "/APP_OPEN"
    lateinit var nodeID: String

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/") 
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activityContext = this
        
        conectar = findViewById(R.id.boton)
        btnGet = findViewById(R.id.btnGet)
        btnPost = findViewById(R.id.btnPost)
        textoIng = findViewById(R.id.ingtexto)
        lResultado = findViewById(R.id.resultado)

        // Vincular el reloj
        conectar.setOnClickListener {
            val tempAct: Activity = activityContext as MainActivity
            getNodes(tempAct)
        }

        // Mandar mensaje y guardar en base de datos
        btnPost.setOnClickListener {
            val mensaje = textoIng.text.toString().trim()
            if (mensaje.isEmpty()) {
                lResultado.text = getString(R.string.msg_write_something)
                return@setOnClickListener
            }

            if (deviceConnected) {
                enviarAlReloj(mensaje)
                guardarEnDB("Phone", mensaje)
                lResultado.text = getString(R.string.msg_sent_both)
            } else {
                guardarEnDB("Phone (Solo DB)", mensaje)
                lResultado.text = getString(R.string.msg_sent_db_only)
            }
            textoIng.setText("")
        }

        // Boton historial
        btnGet.setOnClickListener {
            traerHistorial()
        }
    }

    private fun traerHistorial() {
        launch(Dispatchers.IO) {
            try {
                val lista = apiService.obtenerMensajes()
                withContext(Dispatchers.Main) {
                    if (lista.isEmpty()) {
                        lResultado.text = getString(R.string.msg_history_empty)
                    } else {
                        val sb = StringBuilder("--- HISTORIAL ---\n")
                        lista.takeLast(10).forEach {
                            sb.append("${it.usuario}: ${it.contenido}\n")
                        }
                        lResultado.text = sb.toString()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    lResultado.text = getString(R.string.error_connect)
                }
            }
        }
    }

    private fun guardarEnDB(usuario: String, contenido: String) {
        launch(Dispatchers.IO) {
            try {
                val nuevo = MensajeDB(usuario, contenido)
                apiService.enviarMensaje(nuevo)
            } catch (e: Exception) {
                Log.e("Error", "No se guardo")
            }
        }
    }

    private fun enviarAlReloj(mensaje: String) {
        Wearable.getMessageClient(activityContext!!)
            .sendMessage(nodeID, payloadPath, mensaje.toByteArray(StandardCharsets.UTF_8))
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
                withContext(Dispatchers.Main) {
                    if (deviceConnected) {
                        lResultado.text = getString(R.string.msg_connected_watch, nodeID)
                    } else {
                        lResultado.text = getString(R.string.msg_no_nodes)
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val message = String(messageEvent.data, StandardCharsets.UTF_8)
        nodeID = messageEvent.sourceNodeId
        deviceConnected = true
        
        runOnUiThread {
            lResultado.text = getString(R.string.msg_from_watch, message)
        }
        
        guardarEnDB("Wear", message)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {}

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
