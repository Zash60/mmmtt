package com.nes.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

/**
 * Gerenciador de banco de dados SQLite para o emulador NES.
 */
class DatabaseManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    
    companion object {
        private const val DATABASE_NAME = "nes_emulator.db"
        private const val DATABASE_VERSION = 1
        
        // Tabelas
        private const val TABLE_ROMS = "roms"
        private const val TABLE_SAVE_STATES = "save_states"
        private const val TABLE_SETTINGS = "settings"
        private const val TABLE_CONTROLLER_MAPPINGS = "controller_mappings"
        private const val TABLE_GAME_LIBRARY = "game_library"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // Tabela de ROMs
        db.execSQL("""
            CREATE TABLE $TABLE_ROMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                file_path TEXT NOT NULL UNIQUE,
                md5_hash TEXT NOT NULL UNIQUE,
                mapper_number INTEGER NOT NULL,
                prg_size INTEGER NOT NULL,
                chr_size INTEGER NOT NULL,
                mirroring INTEGER NOT NULL,
                battery_backed INTEGER DEFAULT 0,
                thumbnail_url TEXT,
                last_played_at INTEGER,
                play_time_seconds INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        
        // Tabela de Save States
        db.execSQL("""
            CREATE TABLE $TABLE_SAVE_STATES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rom_id INTEGER NOT NULL,
                slot_number INTEGER NOT NULL,
                state_data BLOB NOT NULL,
                screenshot_path TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(rom_id) REFERENCES $TABLE_ROMS(id),
                UNIQUE(rom_id, slot_number)
            )
        """)
        
        // Tabela de Configurações
        db.execSQL("""
            CREATE TABLE $TABLE_SETTINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                emulation_speed TEXT DEFAULT 'normal',
                video_filter TEXT DEFAULT 'none',
                audio_volume INTEGER DEFAULT 100,
                show_fps INTEGER DEFAULT 0,
                auto_save INTEGER DEFAULT 1,
                controller_layout TEXT DEFAULT 'default',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
        
        // Tabela de Mapeamento de Controles
        db.execSQL("""
            CREATE TABLE $TABLE_CONTROLLER_MAPPINGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                controller_type TEXT NOT NULL,
                button_name TEXT NOT NULL,
                key_code TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                UNIQUE(controller_type, button_name)
            )
        """)
        
        // Tabela de Biblioteca de Jogos
        db.execSQL("""
            CREATE TABLE $TABLE_GAME_LIBRARY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rom_id INTEGER NOT NULL,
                is_favorite INTEGER DEFAULT 0,
                rating INTEGER DEFAULT 0,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(rom_id) REFERENCES $TABLE_ROMS(id),
                UNIQUE(rom_id)
            )
        """)
        
        // Inserir configurações padrão
        val defaultSettings = ContentValues().apply {
            put("emulation_speed", "normal")
            put("video_filter", "none")
            put("audio_volume", 100)
            put("show_fps", 0)
            put("auto_save", 1)
            put("controller_layout", "default")
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        db.insert(TABLE_SETTINGS, null, defaultSettings)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Implementar migrações conforme necessário
    }
    
    // ===== ROM Functions =====
    
    fun addROM(name: String, filePath: String, md5Hash: String, mapperNumber: Int, 
               prgSize: Int, chrSize: Int, mirroring: Int, batteryBacked: Boolean = false): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("file_path", filePath)
            put("md5_hash", md5Hash)
            put("mapper_number", mapperNumber)
            put("prg_size", prgSize)
            put("chr_size", chrSize)
            put("mirroring", mirroring)
            put("battery_backed", if (batteryBacked) 1 else 0)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        return writableDatabase.insert(TABLE_ROMS, null, values)
    }
    
    fun getAllROMs(): List<ROMData> {
        val roms = mutableListOf<ROMData>()
        val cursor = readableDatabase.query(TABLE_ROMS, null, null, null, null, null, "name ASC")
        
        cursor.use {
            while (it.moveToNext()) {
                roms.add(ROMData(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                    md5Hash = it.getString(it.getColumnIndexOrThrow("md5_hash")),
                    mapperNumber = it.getInt(it.getColumnIndexOrThrow("mapper_number")),
                    prgSize = it.getInt(it.getColumnIndexOrThrow("prg_size")),
                    chrSize = it.getInt(it.getColumnIndexOrThrow("chr_size")),
                    mirroring = it.getInt(it.getColumnIndexOrThrow("mirroring")),
                    batteryBacked = it.getInt(it.getColumnIndexOrThrow("battery_backed")) != 0,
                    lastPlayedAt = it.getLong(it.getColumnIndexOrThrow("last_played_at")),
                    playTimeSeconds = it.getInt(it.getColumnIndexOrThrow("play_time_seconds"))
                ))
            }
        }
        return roms
    }
    
    fun getROMById(id: Int): ROMData? {
        val cursor = readableDatabase.query(TABLE_ROMS, null, "id = ?", arrayOf(id.toString()), null, null, null)
        
        return cursor.use {
            if (it.moveToFirst()) {
                ROMData(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                    md5Hash = it.getString(it.getColumnIndexOrThrow("md5_hash")),
                    mapperNumber = it.getInt(it.getColumnIndexOrThrow("mapper_number")),
                    prgSize = it.getInt(it.getColumnIndexOrThrow("prg_size")),
                    chrSize = it.getInt(it.getColumnIndexOrThrow("chr_size")),
                    mirroring = it.getInt(it.getColumnIndexOrThrow("mirroring")),
                    batteryBacked = it.getInt(it.getColumnIndexOrThrow("battery_backed")) != 0,
                    lastPlayedAt = it.getLong(it.getColumnIndexOrThrow("last_played_at")),
                    playTimeSeconds = it.getInt(it.getColumnIndexOrThrow("play_time_seconds"))
                )
            } else {
                null
            }
        }
    }
    
    fun updateROMLastPlayed(id: Int) {
        val values = ContentValues().apply {
            put("last_played_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_ROMS, values, "id = ?", arrayOf(id.toString()))
    }
    
    fun deleteROM(id: Int) {
        writableDatabase.delete(TABLE_ROMS, "id = ?", arrayOf(id.toString()))
        writableDatabase.delete(TABLE_SAVE_STATES, "rom_id = ?", arrayOf(id.toString()))
        writableDatabase.delete(TABLE_GAME_LIBRARY, "rom_id = ?", arrayOf(id.toString()))
    }
    
    // ===== Save State Functions =====
    
    fun saveSaveState(romId: Int, slotNumber: Int, stateData: ByteArray): Long {
        val values = ContentValues().apply {
            put("rom_id", romId)
            put("slot_number", slotNumber)
            put("state_data", stateData)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        
        val existing = readableDatabase.query(TABLE_SAVE_STATES, null, 
            "rom_id = ? AND slot_number = ?", arrayOf(romId.toString(), slotNumber.toString()), 
            null, null, null)
        
        return if (existing.moveToFirst()) {
            existing.close()
            writableDatabase.update(TABLE_SAVE_STATES, values, 
                "rom_id = ? AND slot_number = ?", arrayOf(romId.toString(), slotNumber.toString())).toLong()
        } else {
            existing.close()
            writableDatabase.insert(TABLE_SAVE_STATES, null, values)
        }
    }
    
    fun loadSaveState(romId: Int, slotNumber: Int): ByteArray? {
        val cursor = readableDatabase.query(TABLE_SAVE_STATES, null, 
            "rom_id = ? AND slot_number = ?", arrayOf(romId.toString(), slotNumber.toString()), 
            null, null, null)
        
        return cursor.use {
            if (it.moveToFirst()) {
                it.getBlob(it.getColumnIndexOrThrow("state_data"))
            } else {
                null
            }
        }
    }
    
    fun getSaveStates(romId: Int): List<SaveStateData> {
        val states = mutableListOf<SaveStateData>()
        val cursor = readableDatabase.query(TABLE_SAVE_STATES, null, "rom_id = ?", 
            arrayOf(romId.toString()), null, null, "slot_number ASC")
        
        cursor.use {
            while (it.moveToNext()) {
                states.add(SaveStateData(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    romId = it.getInt(it.getColumnIndexOrThrow("rom_id")),
                    slotNumber = it.getInt(it.getColumnIndexOrThrow("slot_number")),
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at")),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                ))
            }
        }
        return states
    }
    
    fun deleteSaveState(romId: Int, slotNumber: Int) {
        writableDatabase.delete(TABLE_SAVE_STATES, "rom_id = ? AND slot_number = ?", 
            arrayOf(romId.toString(), slotNumber.toString()))
    }
    
    // ===== Settings Functions =====
    
    fun getSettings(): SettingsData? {
        val cursor = readableDatabase.query(TABLE_SETTINGS, null, null, null, null, null, null, "1")
        
        return cursor.use {
            if (it.moveToFirst()) {
                SettingsData(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    emulationSpeed = it.getString(it.getColumnIndexOrThrow("emulation_speed")),
                    videoFilter = it.getString(it.getColumnIndexOrThrow("video_filter")),
                    audioVolume = it.getInt(it.getColumnIndexOrThrow("audio_volume")),
                    showFPS = it.getInt(it.getColumnIndexOrThrow("show_fps")) != 0,
                    autoSave = it.getInt(it.getColumnIndexOrThrow("auto_save")) != 0,
                    controllerLayout = it.getString(it.getColumnIndexOrThrow("controller_layout"))
                )
            } else {
                null
            }
        }
    }
    
    fun updateSettings(emulationSpeed: String? = null, videoFilter: String? = null, 
                      audioVolume: Int? = null, showFPS: Boolean? = null, 
                      autoSave: Boolean? = null, controllerLayout: String? = null) {
        val values = ContentValues()
        emulationSpeed?.let { values.put("emulation_speed", it) }
        videoFilter?.let { values.put("video_filter", it) }
        audioVolume?.let { values.put("audio_volume", it) }
        showFPS?.let { values.put("show_fps", if (it) 1 else 0) }
        autoSave?.let { values.put("auto_save", if (it) 1 else 0) }
        controllerLayout?.let { values.put("controller_layout", it) }
        values.put("updated_at", System.currentTimeMillis())
        
        writableDatabase.update(TABLE_SETTINGS, values, null, null)
    }
    
    // ===== Controller Mapping Functions =====
    
    fun setControllerMapping(controllerType: String, buttonName: String, keyCode: String) {
        val values = ContentValues().apply {
            put("controller_type", controllerType)
            put("button_name", buttonName)
            put("key_code", keyCode)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        
        writableDatabase.insertWithOnConflict(TABLE_CONTROLLER_MAPPINGS, null, values, 
            SQLiteDatabase.CONFLICT_REPLACE)
    }
    
    fun getControllerMappings(): List<ControllerMappingData> {
        val mappings = mutableListOf<ControllerMappingData>()
        val cursor = readableDatabase.query(TABLE_CONTROLLER_MAPPINGS, null, null, null, null, null, null)
        
        cursor.use {
            while (it.moveToNext()) {
                mappings.add(ControllerMappingData(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    controllerType = it.getString(it.getColumnIndexOrThrow("controller_type")),
                    buttonName = it.getString(it.getColumnIndexOrThrow("button_name")),
                    keyCode = it.getString(it.getColumnIndexOrThrow("key_code"))
                ))
            }
        }
        return mappings
    }
    
    // ===== Game Library Functions =====
    
    fun addToLibrary(romId: Int) {
        val values = ContentValues().apply {
            put("rom_id", romId)
            put("created_at", System.currentTimeMillis())
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(TABLE_GAME_LIBRARY, null, values, 
            SQLiteDatabase.CONFLICT_IGNORE)
    }
    
    fun removeFromLibrary(romId: Int) {
        writableDatabase.delete(TABLE_GAME_LIBRARY, "rom_id = ?", arrayOf(romId.toString()))
    }
    
    fun setGameRating(romId: Int, rating: Int) {
        val values = ContentValues().apply {
            put("rating", rating)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_GAME_LIBRARY, values, "rom_id = ?", arrayOf(romId.toString()))
    }
    
    fun setGameFavorite(romId: Int, isFavorite: Boolean) {
        val values = ContentValues().apply {
            put("is_favorite", if (isFavorite) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update(TABLE_GAME_LIBRARY, values, "rom_id = ?", arrayOf(romId.toString()))
    }
}

// Data Classes
data class ROMData(
    val id: Int,
    val name: String,
    val filePath: String,
    val md5Hash: String,
    val mapperNumber: Int,
    val prgSize: Int,
    val chrSize: Int,
    val mirroring: Int,
    val batteryBacked: Boolean,
    val lastPlayedAt: Long,
    val playTimeSeconds: Int
)

data class SaveStateData(
    val id: Int,
    val romId: Int,
    val slotNumber: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class SettingsData(
    val id: Int,
    val emulationSpeed: String,
    val videoFilter: String,
    val audioVolume: Int,
    val showFPS: Boolean,
    val autoSave: Boolean,
    val controllerLayout: String
)

data class ControllerMappingData(
    val id: Int,
    val controllerType: String,
    val buttonName: String,
    val keyCode: String
)
