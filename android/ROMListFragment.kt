package com.nes.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val romsDir = File(requireContext().getExternalFilesDir(null), "roms")
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
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
        startActivityForResult(intent, REQUEST_CODE_PICK_ROM)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_PICK_ROM && resultCode == android.app.Activity.RESULT_OK) {
            val uri = data?.data ?: return
            copyROMToStorage(uri)
        }
    }
    
    private fun copyROMToStorage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            val romsDir = File(requireContext().getExternalFilesDir(null), "roms")
            romsDir.mkdirs()
            
            val fileName = uri.lastPathSegment ?: "rom.nes"
            val outputFile = File(romsDir, fileName)
            
            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            loadROMs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        private const val REQUEST_CODE_PICK_ROM = 1001
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
            val thumbnailManager = ThumbnailManager(itemView.context)
            val bitmap = thumbnailManager.getThumbnail(java.io.File(item.path), 200, 180)
            thumbnail.setImageBitmap(bitmap)
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
