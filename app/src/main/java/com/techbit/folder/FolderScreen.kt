package com.techbit.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FolderScreen(
    folderList: List<String>,
    onCreateFolder: (String) -> Unit,
    requestStorageAccess: () -> Unit
) {
    val showFolderDialog = remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        requestStorageAccess()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showFolderDialog.value = true }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn {
                items(folderList) { folder ->
                    Text(text = folder, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showFolderDialog.value) {
            AlertDialog(
                onDismissRequest = { showFolderDialog.value = false },
                title = { Text("Create New Folder") },
                text = {
                    TextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onCreateFolder(folderName)
                            folderName = "" // Clear the input
                            showFolderDialog.value = false
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showFolderDialog.value = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
