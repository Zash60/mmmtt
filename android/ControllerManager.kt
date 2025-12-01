package com.nes.android

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.nes.emulator.Console

/**
 * Gerencia controles virtuais e Bluetooth do emulador.
 */
class ControllerManager(val context: Context) {
    
    companion object {
        // Botões NES
        const val BUTTON_A = 0x01
        const val BUTTON_B = 0x02
        const val BUTTON_SELECT = 0x04
        const val BUTTON_START = 0x08
        const val BUTTON_UP = 0x10
        const val BUTTON_DOWN = 0x20
        const val BUTTON_LEFT = 0x40
        const val BUTTON_RIGHT = 0x80
    }
    
    private var console: Console? = null
    private var controller1State = 0
    private var controller2State = 0
    
    // Mapeamento de controles customizável
    private val keyMap = mutableMapOf<Int, Int>(
        KeyEvent.KEYCODE_DPAD_UP to BUTTON_UP,
        KeyEvent.KEYCODE_DPAD_DOWN to BUTTON_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT to BUTTON_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT to BUTTON_RIGHT,
        KeyEvent.KEYCODE_Z to BUTTON_A,
        KeyEvent.KEYCODE_X to BUTTON_B,
        KeyEvent.KEYCODE_SHIFT_RIGHT to BUTTON_SELECT,
        KeyEvent.KEYCODE_ENTER to BUTTON_START
    )
    
    fun setConsole(console: Console) {
        this.console = console
    }
    
    fun handleKeyDown(keyCode: Int): Boolean {
        val button = keyMap[keyCode] ?: return false
        controller1State = controller1State or button
        updateConsole()
        return true
    }
    
    fun handleKeyUp(keyCode: Int): Boolean {
        val button = keyMap[keyCode] ?: return false
        controller1State = controller1State and button.inv()
        updateConsole()
        return true
    }
    
    fun handleGamepadInput(keyCode: Int, event: KeyEvent): Boolean {
        val button = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> BUTTON_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> BUTTON_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> BUTTON_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> BUTTON_RIGHT
            KeyEvent.KEYCODE_BUTTON_A -> BUTTON_B
            KeyEvent.KEYCODE_BUTTON_B -> BUTTON_A
            KeyEvent.KEYCODE_BUTTON_X -> BUTTON_A
            KeyEvent.KEYCODE_BUTTON_Y -> BUTTON_B
            KeyEvent.KEYCODE_BUTTON_SELECT -> BUTTON_SELECT
            KeyEvent.KEYCODE_BUTTON_START -> BUTTON_START
            KeyEvent.KEYCODE_BUTTON_L1 -> BUTTON_SELECT
            KeyEvent.KEYCODE_BUTTON_R1 -> BUTTON_START
            else -> return false
        }
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            controller1State = controller1State or button
        } else if (event.action == KeyEvent.ACTION_UP) {
            controller1State = controller1State and button.inv()
        }
        
        updateConsole()
        return true
    }
    
    fun handleTouchInput(view: View, event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        val width = view.width
        val height = view.height
        
        // Dividir tela em zonas
        val dpadZoneWidth = width / 4
        val dpadZoneHeight = height / 2
        
        val buttonZoneWidth = width / 4
        val buttonZoneHeight = height / 2
        
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // D-pad (esquerda)
                if (x < dpadZoneWidth) {
                    controller1State = controller1State and (BUTTON_UP or BUTTON_DOWN or BUTTON_LEFT or BUTTON_RIGHT).inv()
                    
                    if (y < dpadZoneHeight / 2) {
                        controller1State = controller1State or BUTTON_UP
                    } else if (y > dpadZoneHeight / 2) {
                        controller1State = controller1State or BUTTON_DOWN
                    }
                    
                    if (x < dpadZoneWidth / 2) {
                        controller1State = controller1State or BUTTON_LEFT
                    } else {
                        controller1State = controller1State or BUTTON_RIGHT
                    }
                }
                
                // Botões (direita)
                if (x > width - buttonZoneWidth) {
                    val relX = x - (width - buttonZoneWidth)
                    val relY = y
                    
                    controller1State = controller1State and (BUTTON_A or BUTTON_B or BUTTON_SELECT or BUTTON_START).inv()
                    
                    if (relY < buttonZoneHeight / 2) {
                        if (relX < buttonZoneWidth / 2) {
                            controller1State = controller1State or BUTTON_SELECT
                        } else {
                            controller1State = controller1State or BUTTON_START
                        }
                    } else {
                        if (relX < buttonZoneWidth / 2) {
                            controller1State = controller1State or BUTTON_B
                        } else {
                            controller1State = controller1State or BUTTON_A
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                controller1State = 0
            }
        }
        
        updateConsole()
        return true
    }
    
    fun setKeyMapping(keyCode: Int, button: Int) {
        keyMap[keyCode] = button
    }
    
    fun getKeyMapping(keyCode: Int): Int? {
        return keyMap[keyCode]
    }
    
    fun getController1State(): Int {
        return controller1State
    }
    
    fun setController1State(state: Int) {
        controller1State = state
        updateConsole()
    }
    
    private fun updateConsole() {
        console?.setController1(controller1State)
    }
}
