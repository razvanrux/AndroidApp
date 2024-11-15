package com.rux.first_test

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.rux.first_test.ui.theme.SecondEverTestTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.prefs.Preferences

val Context.dataStore by preferencesDataStore(name = "messages")

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecondEverTestTheme {
                MainContent()
            }
        }
    }

    @Composable
    fun MainContent() {
        var textState by remember { mutableStateOf("") }
        val messages = remember { mutableStateListOf<Message>() }
        var sortOption by remember { mutableStateOf(SortOption.NONE) }


        LaunchedEffect(Unit) {
            loadMessages().collect { savedMessages ->
                messages.clear()
                messages.addAll(savedMessages)
            }
        }

        // Update sorting when sort option changes
        LaunchedEffect(sortOption) {
            sortMessages(messages, sortOption)
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Add the SortDropdown here
                SortDropdown(sortOption = sortOption) { selectedOption ->
                    sortOption = selectedOption
                }

                TextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(this@MainActivity, SecondActivity::class.java)
                            intent.putExtra("EXTRA_TEXT", textState)
                            startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Open in New Window")
                    }

                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            val message = Message(textState, timestamp)
                            saveMessage(message)
                            messages.add(message)
                            textState = ""
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Add Chat")
                    }

                    // Move SortDropdown here
                    SortDropdown(
                        sortOption = sortOption,
                        onSortChange = { newSortOption ->
                            sortOption = newSortOption
                            sortMessages(messages, newSortOption)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(messages) { message ->
                        MessageCard(
                            message = message,
                            //messages = messages,
                            onPin = { msg ->
                                msg.pinned = true
                                // Re-sort to ensure pinned messages stay at the top
                                sortMessages(messages, sortOption)
                            },
                            onDelete = { msg ->
                                messages.remove(msg)
                                deleteMessage(msg) // Remove the message from DataStore
                            }
                        )
                    }
                }

            }
        }
    }


    @Composable
    fun SortDropdown(sortOption: SortOption, onSortChange: (SortOption) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Box {
            TextButton(onClick = { expanded = !expanded }) {
                Text("Sort by: ${sortOption.displayName}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onSortChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MessageCard(
        message: Message,
        //messages: MutableList<Message>,
        onPin: (Message) -> Unit,
        onDelete: (Message) -> Unit
    ) {
        var showMenu by remember { mutableStateOf(false) }

        Surface(
            color = if (message.pinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* Regular click (if needed) */ },
                    onLongClick = { showMenu = true } // Open menu on long press
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = message.text,
                    color = if (message.pinned) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = if (message.pinned) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = message.timestamp,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown Menu for Pin and Delete
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            onPin(message)
                        },
                        text = { Text(if (message.pinned) "Unpin" else "Pin") }
                    )
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            onDelete(message)
                        },
                        text = { Text("Delete") }
                    )
                }
            }
        }
    }





    private fun saveMessage(message: Message) {
        lifecycleScope.launch {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey("message_${System.currentTimeMillis()}")
                preferences[key] = "${message.text}|${message.timestamp}|${message.pinned}"
            }
        }
    }

    private fun loadMessages(): Flow<List<Message>> {
        return dataStore.data.map { preferences ->
            preferences.asMap().mapNotNull { entry ->
                val parts = entry.value.toString().split("|")
                if (parts.size == 3) {
                    val (text, timestamp, pinned) = parts
                    Message(text, timestamp, pinned.toBoolean())
                } else null
            }
        }
    }


    private fun sortMessages(messages: MutableList<Message>, option: SortOption) {
        // Separate pinned and unpinned messages explicitly
        val pinnedMessages = messages.filter { it.pinned }.toMutableList()
        val unpinnedMessages = messages.filter { !it.pinned }.toMutableList()

        // Sort the unpinned messages based on the selected sort option
        when (option) {
            SortOption.DATE_ASC -> unpinnedMessages.sortBy { it.timestamp }
            SortOption.DATE_DESC -> unpinnedMessages.sortByDescending { it.timestamp }
            SortOption.NAME_ASC -> unpinnedMessages.sortBy { it.text }
            SortOption.NAME_DESC -> unpinnedMessages.sortByDescending { it.text }
            SortOption.NONE -> { /* No sorting needed */ }
        }

        // Combine pinned messages on top and sorted unpinned below
        messages.clear()
        messages.addAll(pinnedMessages)
        messages.addAll(unpinnedMessages)
    }


    private fun deleteMessage(message: Message) {
        lifecycleScope.launch {
            dataStore.edit { preferences ->
                // Iterate through preferences to find the correct key to delete
                val entryToRemove = preferences.asMap().entries.find { entry ->
                    val parts = entry.value.toString().split("|")
                    parts.size == 3 && parts[0] == message.text && parts[1] == message.timestamp
                }

                // Remove the entry if it exists
                entryToRemove?.let { preferences.remove(it.key) }
            }
        }
    }


    private fun showContextMenu(message: Message, messages: MutableList<Message>) {
        lifecycleScope.launch {
            // Example: toggle pin
            message.pinned = !message.pinned
            sortMessages(messages, SortOption.DATE_DESC) // Example: Sort to keep pinned on top
        }
    }

}

data class Message(val text: String, val timestamp: String, var pinned: Boolean = false)


// Enum for sorting options
enum class SortOption(val displayName: String) {
    NONE("None"),
    DATE_ASC("Date Ascending"),
    DATE_DESC("Date Descending"),
    NAME_ASC("Name Ascending"),
    NAME_DESC("Name Descending")
}
