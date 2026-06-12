package com.example.myapplication


import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textoIng = findViewById<EditText>(R.id.ingtexto)
        val btnAceptar = findViewById<Button>(R.id.btnAceptar)
        val lResultado = findViewById<TextView>(R.id.resultado)

        btnAceptar.setOnClickListener {

            val textoIngresado = textoIng.text.toString()

            lResultado.text = textoIngresado

            AlertDialog.Builder(this)
                .setTitle("¡Éxito!")
                .setMessage("El texto \"$textoIngresado\" se guardó correctamente.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}