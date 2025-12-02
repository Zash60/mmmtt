package com.nes.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Fragment para exibir a tela de emulação.
 */
class EmulatorFragment : Fragment() {
    
    private lateinit var surfaceView: android.view.SurfaceView
    private lateinit var fpsText: TextView
    private lateinit var speedSeekBar: SeekBar
    private var fpsCounter = 0
    private var lastTime = System.currentTimeMillis()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_emulator, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        surfaceView = view.findViewById(R.id.emulator_surface)
        fpsText = view.findViewById(R.id.fps_text)
        speedSeekBar = view.findViewById(R.id.speed_seekbar)
        
        // Configurar controles
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress / 100f) * 1.5f
                (activity as? MainActivity)?.console?.speed = speed
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Botão de pausa
        view.findViewById<Button>(R.id.btn_pause).setOnClickListener {
            val console = (activity as? MainActivity)?.console
            if (console != null) {
                console.paused = !console.paused
            }
        }
        
        // Botão de reset
        view.findViewById<Button>(R.id.btn_reset).setOnClickListener {
            val console = (activity as? MainActivity)?.console
            console?.reset()
        }
        
        // Botão de voltar
        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            (activity as? MainActivity)?.stopEmulation()
            parentFragmentManager.popBackStack()
        }
        
        // Iniciar contador de FPS
        startFPSCounter()
    }
    
    private fun startFPSCounter() {
        view?.post(object : Runnable {
            override fun run() {
                fpsCounter++
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastTime
                
                if (deltaTime >= 1000) {
                    fpsText.text = "FPS: $fpsCounter"
                    fpsCounter = 0
                    lastTime = currentTime
                }
                
                view?.postDelayed(this, 16)
            }
        })
    }
}
