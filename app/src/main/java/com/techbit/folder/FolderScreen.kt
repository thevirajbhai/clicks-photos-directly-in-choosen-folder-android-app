package com.techbit.folder

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderScreen(
    currentPath: File,
    onCreateFolder: (String) -> Unit,
    folderList: List<String>,
    imageList: List<String>, // New parameter for images
    requestStorageAccess: () -> Unit,
    onFolderClick: (String) -> Unit,
    onBackPress: () -> Unit,
    onOpenCamera: (String) -> Unit,
    onDeleteFolder: (String) -> Unit
)  {
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
                        onOpenCamera = { onOpenCamera(folder) },
                        onDeleteFolder = { onDeleteFolder(folder) }
                    )
                }

                // Display images
                items(imageList) { imagePath ->
                    ImageItem(imagePath) // Call a new Composable function to display images
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
}   @Composable
fun FolderItem(
    folderName: String,
    onClick: () -> Unit,
    onOpenCamera: () -> Unit,
    onDeleteFolder: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this folder?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder() // Call the delete function
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmation = false }) {
                    Text("No")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFFDDDDDD))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Use a Box to wrap the text, allowing it to take available space
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = folderName,
                fontSize = 18.sp,
                color = Color(0xFF000000),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Ensures long text is truncated with ellipsis
            )
        }
        Row {
            Button(
                onClick = { onOpenCamera() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
            ) {
                Text(text = "Open Camera", color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showDeleteConfirmation = true }, // Show confirmation dialog
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Delete", color = Color.White)
            }
        }
    }
}

@Composable
fun ImageItem(imagePath: String) {
    val bitmap = BitmapFactory.decodeFile(imagePath) // Load the image file
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Set height as needed
                .padding(vertical = 4.dp)
        )
    }
}
