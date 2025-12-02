package com.nes.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.nes.emulator.Console

/**
 * Renderizador otimizado usando Bitmap para alta performance.
 */
class EmulatorRenderer(val surfaceView: SurfaceView) : SurfaceHolder.Callback {
    
    private var renderThread: RenderThread? = null
    private var console: Console? = null
    private var isRunning = false
    
    // Bitmap buffer para desenhar a tela inteira de uma vez
    private val frameBitmap = Bitmap.createBitmap(256, 240, Bitmap.Config.ARGB_8888)
    private val srcRect = Rect(0, 0, 256, 240)
    private val dstRect = Rect()
    private val paint = Paint().apply { 
        isFilterBitmap = false // Mantém o pixel art nítido (sem borrao)
        isDither = false
    }
    
    init {
        surfaceView.holder.addCallback(this)
    }
    
    fun setConsole(console: Console) {
        this.console = console
    }
    
    fun start() {
        if (isRunning) return
        isRunning = true
        renderThread = RenderThread(surfaceView.holder, this)
        renderThread?.start()
    }
    
    fun stop() {
        isRunning = false
        try {
            renderThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        renderThread = null
    }
    
    fun render(canvas: Canvas) {
        val console = console ?: return
        
        // Pega os pixels brutos do emulador (IntArray)
        val frameBuffer = console.getFrameBuffer()
        
        // Transfere os pixels para o Bitmap (MUITO mais rápido que drawRect)
        // O frameBuffer tem tamanho 256x240 = 61440 ints
        if (frameBuffer.size >= 61440) {
            frameBitmap.setPixels(frameBuffer, 0, 256, 0, 0, 256, 240)
        }
        
        // Calcular escala para preencher a tela mantendo proporção
        val surfaceWidth = surfaceView.width
        val surfaceHeight = surfaceView.height
        
        val scale = minOf(
            surfaceWidth.toFloat() / 256f,
            surfaceHeight.toFloat() / 240f
        )
        
        val scaledWidth = (256 * scale).toInt()
        val scaledHeight = (240 * scale).toInt()
        
        val offsetX = (surfaceWidth - scaledWidth) / 2
        val offsetY = (surfaceHeight - scaledHeight) / 2
        
        // Define a área de destino na tela
        dstRect.set(offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight)
        
        // Limpa o fundo (bordas pretas)
        canvas.drawColor(Color.BLACK)
        
        // Desenha o jogo
        canvas.drawBitmap(frameBitmap, srcRect, dstRect, paint)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isRunning && console != null) {
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
                    // Tenta bloquear o canvas para desenho
                    canvas = holder.lockCanvas()
                    if (canvas != null) {
                        renderer.render(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                // Limitar a ~60 FPS para não gastar bateria à toa
                try {
                    sleep(16)
                } catch (e: InterruptedException) {
                    // Ignora
                }
            }
        }
    }
}
