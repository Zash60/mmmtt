package com.nes.android

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.nes.emulator.Console
import com.nes.emulator.ROMLoader

/**
 * Activity principal do emulador NES.
 */
class MainActivity : AppCompatActivity() {
    
    var console: Console? = null
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
        
        // Inicializar áudio
        audioManager?.setConsole(console!!)
        audioManager?.start()
        
        // Inicializar controles
        controllerManager?.setConsole(console!!)
        
        // Iniciar thread de emulação (CPU/PPU/APU)
        isEmulating = true
        emulatorThread = EmulatorThread(console!!)
        emulatorThread?.start()
        
        // Mostrar tela de emulador (O Fragment cuidará do Renderizador de Vídeo)
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
        
        console = null
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isEmulating && controllerManager != null && event != null) {
            val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            
            if (isGamepad) {
                return controllerManager!!.handleGamepadInput(keyCode, event)
            }
            return controllerManager!!.handleKeyDown(keyCode)
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isEmulating && controllerManager != null && event != null) {
            val isGamepad = (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK

            if (isGamepad) {
                return controllerManager!!.handleGamepadInput(keyCode, event)
            }
            return controllerManager!!.handleKeyUp(keyCode)
        }
        return super.onKeyUp(keyCode, event)
    }
    
    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (isEmulating && event != null && controllerManager != null) {
            // O SurfaceView está dentro do Fragment agora, então passamos o evento
            // mas precisamos achar a view atual.
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (fragment is EmulatorFragment) {
                val surfaceView = fragment.getSurfaceView()
                // A verificação abaixo agora é válida pois getSurfaceView retorna nullable
                if (surfaceView != null) {
                    return controllerManager!!.handleTouchInput(surfaceView, event)
                }
            }
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
