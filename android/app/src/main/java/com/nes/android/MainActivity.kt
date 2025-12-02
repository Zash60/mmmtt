package com.nes.android

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nes.emulator.Console
import com.nes.emulator.ROMLoader

/**
 * Activity principal do emulador NES.
 */
class MainActivity : AppCompatActivity() {
    
    private var console: Console? = null
    private var emulatorRenderer: EmulatorRenderer? = null
    private var audioManager: AudioManager? = null
    private var controllerManager: ControllerManager? = null
    private var emulatorThread: EmulatorThread? = null
    private var isEmulating = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inicializar componentes
        audioManager = AudioManager()
        audioManager?.initialize()
        
        controllerManager = ControllerManager(this)
        
        // Mostrar tela inicial (lista de ROMs)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ROMListFragment())
                .commit()
        }
    }
    
    fun startEmulation(romPath: String) {
        // Carregar ROM
        val romFile = java.io.File(romPath)
        console = ROMLoader.loadROM(romFile)
        
        if (console == null) {
            showError("Falha ao carregar ROM")
            return
        }
        
        // Inicializar renderizador
        val surfaceView = findViewById<android.view.SurfaceView>(R.id.emulator_surface)
        emulatorRenderer = EmulatorRenderer(surfaceView)
        emulatorRenderer?.setConsole(console!!)
        
        // Inicializar áudio
        audioManager?.setConsole(console!!)
        audioManager?.start()
        
        // Inicializar controles
        controllerManager?.setConsole(console!!)
        
        // Iniciar thread de emulação
        isEmulating = true
        emulatorThread = EmulatorThread(console!!)
        emulatorThread?.start()
        
        // Mostrar tela de emulador
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EmulatorFragment())
            .addToBackStack(null)
            .commit()
    }
    
    fun stopEmulation() {
        isEmulating = false
        emulatorThread?.join()
        emulatorThread = null
        
        audioManager?.stop()
        emulatorRenderer?.stop()
        
        console = null
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isEmulating && controllerManager != null) {
            // Tentar processar como gamepad primeiro
            if (event?.source?.and(android.view.InputDevice.TOOL_UNKNOWN) != 0) {
                return controllerManager!!.handleGamepadInput(keyCode, event)
            }
            // Depois como teclado
            return controllerManager!!.handleKeyDown(keyCode)
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isEmulating && controllerManager != null) {
            if (event?.source?.and(android.view.InputDevice.TOOL_UNKNOWN) != 0) {
                return controllerManager!!.handleGamepadInput(keyCode, event)
            }
            return controllerManager!!.handleKeyUp(keyCode)
        }
        return super.onKeyUp(keyCode, event)
    }
    
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (isEmulating && event != null && controllerManager != null) {
            val surfaceView = findViewById<android.view.SurfaceView>(R.id.emulator_surface)
            return controllerManager!!.handleTouchInput(surfaceView, event)
        }
        return super.onGenericMotionEvent(event)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isEmulating) {
            stopEmulation()
        }
        audioManager?.release()
    }
    
    private fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private inner class EmulatorThread(val console: Console) : Thread() {
        override fun run() {
            while (isEmulating) {
                try {
                    console.runFrame()
                    sleep(16)  // ~60 FPS
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}
