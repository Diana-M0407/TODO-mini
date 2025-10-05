/*
package com.example.todomini


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.todomini.ui.theme.TODOMiniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TODOMiniTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TODOMiniTheme {
        Greeting("Diana")
    }
}

*/


package com.example.todomini   // ← change to your package

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID


/* ---------- Model ---------- */
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val completed: Boolean = false
)

/* A Saver so our SnapshotStateList<TodoItem> survives config changes. */
private val todoListSaver: Saver<SnapshotStateList<TodoItem>, Any> =
    listSaver(
        save = { list -> list.map { listOf(it.id, it.label, it.completed) } },
        restore = { saved ->
            mutableStateListOf(
                *saved.map {
                    val id = it[0] as String
                    val label = it[1] as String
                    val completed = it[2] as Boolean
                    TodoItem(id, label, completed)
                }.toTypedArray()
            )
        }
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TodoApp() }
    }
}

/* ---------- App Root (state owner) ---------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp() {
    // Hoisted, rotation-safe state
    val items = rememberSaveable(saver = todoListSaver) {
        mutableStateListOf(
            // starter examples; you can start empty if you prefer
            TodoItem(label = "Learn Java"),
            TodoItem(label = "Complete android homework")
        )
    }
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // UI state (text field) that should also survive rotation
    var input by rememberSaveable ( stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    fun addItem(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("Task name can’t be blank.") }
            return
        }
        items.add(TodoItem(label = text))
        input = TextFieldValue("")
    }

    fun toggle(id: String, checked: Boolean) {
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) items[idx] = items[idx].copy(completed = checked)
    }

    fun delete(id: String) {
        items.removeAll { it.id == id }
    }

    val active = remember(items) { items.filter { !it.completed } }
    val completed = remember(items) { items.filter { it.completed } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("TODO List") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        TodoScreen(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            input = input,
            onInputChange = { input = it },
            onAdd = { addItem(input.text) },
            active = active,
            completed = completed,
            onToggle = ::toggle,
            onDelete = ::delete
        )
    }
}

/* ---------- Stateless UI (state hoisted) ---------- */

@Composable
fun TodoScreen(
    modifier: Modifier = Modifier,
    input: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onAdd: () -> Unit,
    active: List<TodoItem>,
    completed: List<TodoItem>,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Task name input" },
                singleLine = true,
                placeholder = { Text("Enter the task name") }
            )
            Button(
                onClick = {
                    onAdd()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .heightIn(min = 48.dp) // touch target
                    .semantics { contentDescription = "Add task" }
            ) { Text("Add") }
        }

        Section(
            title = "Items",
            items = active,
            emptyText = "No items yet",
            onToggle = onToggle,
            onDelete = onDelete
        )

        Section(
            title = "Completed Items",
            items = completed,
            emptyText = "No completed items",
            onToggle = onToggle,
            onDelete = onDelete
        )
    }
}

@Composable
private fun Section(
    title: String,
    items: List<TodoItem>,
    emptyText: String,
    onToggle: (id: String, checked: Boolean) -> Unit,
    onDelete: (id: String) -> Unit
) {
    if (items.isNotEmpty()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                TodoRow(
                    item = item,
                    onCheckedChange = { onToggle(item.id, it) },
                    onDelete = { onDelete(item.id) }
                )
            }
        }
    } else {
        // Friendly empty state
        Text(
            text = emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodoRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            )
            Checkbox(
                checked = item.completed,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.semantics {
                    contentDescription = if (item.completed) "Mark ${item.label} as active"
                    else "Mark ${item.label} as completed"
                }
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.semantics { contentDescription = "Delete ${item.label}" }
            ) {
                Icon(Icons.Filled.Close,
                    contentDescription = "Delete task")
            }
        }
    }
}



/*
/* Simple icon (avoid extra dependency just for Close) */
object Icons {
    object Default {
        @Composable
        fun Close() = Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Close,
            contentDescription = null
        )
    }
}

*/

