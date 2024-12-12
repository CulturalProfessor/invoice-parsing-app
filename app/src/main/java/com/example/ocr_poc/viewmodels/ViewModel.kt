package com.example.ocr_poc.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocr_poc.data.TextEntityDao
import com.example.ocr_poc.models.EditableTextEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


class TextEntityViewModel(private val dao: TextEntityDao) : ViewModel() {
    private val _textEntities = MutableStateFlow<List<EditableTextEntity>>(emptyList())
    val textEntities: StateFlow<List<EditableTextEntity>> = _textEntities

    init {
        viewModelScope.launch {
            _textEntities.value = dao.getAllEntities()
        }
    }

    fun updateEntity(entity: EditableTextEntity) {
        viewModelScope.launch {
            dao.updateEntity(entity)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _textEntities.value = dao.getAllEntities()
        }
    }
}

@Composable
fun EditableKeyValueUI(
    textEntities: List<EditableTextEntity>,
    onEntityUpdate: (EditableTextEntity) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(textEntities) { entity ->
            var key by remember { mutableStateOf(entity.key) }
            var value by remember { mutableStateOf(entity.value) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = key,
                        onValueChange = { newKey ->
                            key = newKey
                            entity.key = newKey
                            onEntityUpdate(entity)
                        },
                        label = { Text("Key") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = value,
                        onValueChange = { newValue ->
                            value = newValue
                            entity.value = newValue
                            onEntityUpdate(entity)
                        },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onSave() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}
