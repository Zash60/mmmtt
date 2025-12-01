package com.nes.android

import android.graphics.Canvas
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.nes.emulator.Console

/**
 * Renderizador de frames do emulador em SurfaceView.
 */
class EmulatorRenderer(val surfaceView: SurfaceView) : SurfaceHolder.Callback {
    
    private var renderThread: RenderThread? = null
    private var console: Console? = null
    private var isRunning = false
    
    init {
        surfaceView.holder.addCallback(this)
    }
    
    fun setConsole(console: Console) {
        this.console = console
    }
    
    fun start() {
        isRunning = true
        renderThread = RenderThread(surfaceView.holder, this)
        renderThread?.start()
    }
    
    fun stop() {
        isRunning = false
        renderThread?.join()
        renderThread = null
    }
    
    fun render(canvas: Canvas) {
        val console = console ?: return
        
        val frameBuffer = console.getFrameBuffer()
        val width = 256
        val height = 240
        
        // Calcular escala para preencher a tela
        val surfaceWidth = surfaceView.width
        val surfaceHeight = surfaceView.height
        
        val scaleX = surfaceWidth.toFloat() / width
        val scaleY = surfaceHeight.toFloat() / height
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (surfaceWidth - width * scale) / 2
        val offsetY = (surfaceHeight - height * scale) / 2
        
        // Renderizar frame buffer
        val paint = Paint()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = frameBuffer[y * width + x]
                paint.color = pixel
                canvas.drawRect(
                    offsetX + x * scale,
                    offsetY + y * scale,
                    offsetX + (x + 1) * scale,
                    offsetY + (y + 1) * scale,
                    paint
                )
            }
        }
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isRunning) {
            start()
        }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stop()
    }
    
    private inner class RenderThread(
        val holder: SurfaceHolder,
        val renderer: EmulatorRenderer
    ) : Thread() {
        
        override fun run() {
            while (isRunning) {
                var canvas: Canvas? = null
                try {
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        // Limpar canvas
                        canvas.drawColor(android.graphics.Color.BLACK)
                        
                        // Renderizar
                        renderer.render(canvas)
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }
                
                // Limitar a 60 FPS
                Thread.sleep(16)
            }
        }
    }
}
