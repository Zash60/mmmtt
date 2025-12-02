package com.nes.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nes.emulator.ROMLoader
import java.io.File

/**
 * Fragment para exibir lista de ROMs disponíveis.
 */
class ROMListFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ROMAdapter
    private val romList = mutableListOf<ROMItem>()
    
    // Novo sistema de resultado de atividade
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                copyROMToStorage(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rom_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.rom_grid)
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        
        adapter = ROMAdapter(romList) { romItem ->
            (activity as? MainActivity)?.startEmulation(romItem.path)
        }
        recyclerView.adapter = adapter
        
        // Carregar ROMs
        loadROMs()
        
        // Botão para adicionar ROM
        view.findViewById<android.widget.Button>(R.id.btn_add_rom).setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun loadROMs() {
        // Garantir que o contexto existe
        val context = context ?: return
        
        val romsDir = File(context.getExternalFilesDir(null), "roms")
        if (!romsDir.exists()) {
            romsDir.mkdirs()
        }
        
        romList.clear()
        romsDir.listFiles()?.forEach { file ->
            if (file.extension == "nes") {
                val info = ROMLoader.getROMInfo(file)
                if (info != null) {
                    romList.add(ROMItem(
                        name = info.name,
                        path = file.absolutePath,
                        mapperNumber = info.mapperNumber
                    ))
                }
            }
        }
        
        adapter.notifyDataSetChanged()
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        // Tentar filtrar apenas arquivos .nes se possível, mas genericamente octet-stream funciona
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream", "application/x-nes-rom"))
        filePickerLauncher.launch(intent)
    }
    
    private fun copyROMToStorage(uri: Uri) {
        try {
            val context = requireContext()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            val romsDir = File(context.getExternalFilesDir(null), "roms")
            romsDir.mkdirs()
            
            // Tentar pegar o nome real do arquivo, senão usar padrão
            var fileName = "imported_rom_${System.currentTimeMillis()}.nes"
            
            // Tenta pegar o nome do ContentResolver
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            
            val outputFile = File(romsDir, fileName)
            
            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            loadROMs()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Erro ao importar: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

data class ROMItem(
    val name: String,
    val path: String,
    val mapperNumber: Int
)

class ROMAdapter(
    val items: List<ROMItem>,
    val onItemClick: (ROMItem) -> Unit
) : RecyclerView.Adapter<ROMAdapter.ViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rom, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onItemClick)
    }
    
    override fun getItemCount() = items.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail = itemView.findViewById<android.widget.ImageView>(R.id.rom_thumbnail)
        private val title = itemView.findViewById<android.widget.TextView>(R.id.rom_title)
        
        fun bind(item: ROMItem, onItemClick: (ROMItem) -> Unit) {
            title.text = item.name
            
            // Carregar thumbnail
            try {
                val thumbnailManager = ThumbnailManager(itemView.context)
                val bitmap = thumbnailManager.getThumbnail(java.io.File(item.path), 200, 180)
                thumbnail.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Falha silenciosa no thumbnail
            }
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
