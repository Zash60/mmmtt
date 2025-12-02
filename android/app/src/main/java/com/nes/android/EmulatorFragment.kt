package com.nes.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
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
    
    private lateinit var surfaceView: SurfaceView
    private lateinit var fpsText: TextView
    private lateinit var speedSeekBar: SeekBar
    private var emulatorRenderer: EmulatorRenderer? = null
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
        
        // Inicializar Renderizador
        val mainActivity = activity as? MainActivity
        val console = mainActivity?.console
        
        if (console != null) {
            emulatorRenderer = EmulatorRenderer(surfaceView)
            emulatorRenderer?.setConsole(console)
            emulatorRenderer?.start()
        }
        
        // Configurar controles
        speedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = 0.5f + (progress / 100f) * 1.5f
                mainActivity?.console?.speed = speed
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Botão de pausa
        view.findViewById<Button>(R.id.btn_pause).setOnClickListener {
            val currentConsole = mainActivity?.console
            if (currentConsole != null) {
                currentConsole.paused = !currentConsole.paused
            }
        }
        
        // Botão de reset
        view.findViewById<Button>(R.id.btn_reset).setOnClickListener {
            val currentConsole = mainActivity?.console
            currentConsole?.reset()
        }
        
        // Botão de voltar
        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            mainActivity?.stopEmulation()
            parentFragmentManager.popBackStack()
        }
        
        // Iniciar contador de FPS
        startFPSCounter()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        emulatorRenderer?.stop()
    }
    
    // CORREÇÃO: Retorna SurfaceView? (nullable) e verifica inicialização
    fun getSurfaceView(): SurfaceView? {
        return if (::surfaceView.isInitialized) surfaceView else null
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
