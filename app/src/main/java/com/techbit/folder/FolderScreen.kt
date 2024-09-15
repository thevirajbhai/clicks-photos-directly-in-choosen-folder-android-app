package com.techbit.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    folderList: List<String>,
    currentPath: File,
    onCreateFolder: (String) -> Unit,
    requestStorageAccess: () -> Unit,
    onFolderClick: (String) -> Unit,
    onBackPress: () -> Unit,
    onOpenInGallery: (String) -> Unit
) {
    val showFolderDialog = remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        requestStorageAccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentPath.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (currentPath.parentFile != null) {
                        IconButton(onClick = onBackPress) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0x949C27B0)) // Customize color
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showFolderDialog.value = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create Folder")
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
                    FolderItem(
                        folderName = folder,
                        onClick = { onFolderClick(folder) },
                        onOpenInGallery = { onOpenInGallery(folder) }
                    )
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
                        label = { Text("Folder Name") },
                        singleLine = true
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

@Composable
fun FolderItem(folderName: String, onClick: () -> Unit, onOpenInGallery: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .background(Color(0xFFDDDDDD))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = folderName,
            fontSize = 18.sp,
            color = Color(0xFF000000)
        )
        Button(
            onClick = { onOpenInGallery() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text(text = "Open in Gallery", color = Color.White)
        }
    }
}
