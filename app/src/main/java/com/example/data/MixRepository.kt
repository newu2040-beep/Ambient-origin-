package com.example.data

import kotlinx.coroutines.flow.Flow

class MixRepository(private val dao: SavedMixDao) {
    val allMixes: Flow<List<SavedMix>> = dao.getAllMixes()

    suspend fun insert(mix: SavedMix) = dao.insertMix(mix)
    suspend fun deleteById(id: Int) = dao.deleteMixById(id)
}
