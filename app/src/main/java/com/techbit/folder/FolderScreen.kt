package com.techbit.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun FolderScreen(
    currentPath: File,
    rootPath: File,
    onCreateFolder: (String) -> Unit,
    folderList: List<String>,
    requestStorageAccess: () -> Unit,
    onFolderClick: (String) -> Unit,
    onBackPress: () -> Unit,
    onOpenCamera: (String) -> Unit,
    onOpenCamera2: () -> Unit,
    onDeleteFolder: (String) -> Unit,
    openFolder: (String) -> Unit
) {
    val showFolderDialog = remember { mutableStateOf(false) }
    // Added for text file creation
    LaunchedEffect(Unit) {
        requestStorageAccess()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = {
                    if (currentPath != rootPath) {
                        IconButton(onClick = onBackPress) {
                            Icon(painter = painterResource(id = R.drawable.back), contentDescription = "Back")
                        }
                    }
                    Text(currentPath.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 30.sp)
                }
            )
        },
        floatingActionButton = {
            Row {
                // Action buttons

                FloatingActionButton(
                    onClick = { openFolder(currentPath.absolutePath)},Modifier.padding(end = 16.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.openfolder), contentDescription = "Open Camera")
                }
                    FloatingActionButton(
                        onClick = { onOpenCamera2()},Modifier.padding(end = 16.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.camera), contentDescription = "Open Camera")
                    }

                    FloatingActionButton(
                        onClick = { showFolderDialog.value = true },Modifier.padding(end = 16.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.createfolder), contentDescription = "Create Folder") }

            }

        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight(.9f)) {
                items(folderList) { folder ->
                    FolderItem(
                        folderName = folder,
                        onClick = { onFolderClick(folder) },
                        onOpenCamera = { onOpenCamera(folder) },
                        onDeleteFolder = { onDeleteFolder(folder) },
                        openFolder = {openFolder(currentPath.absolutePath+"/$folder")}
                    )
                }


            }
        }

        if (showFolderDialog.value) {
            CreateFolderDialog(showFolderDialog, onCreateFolder)
        }

    }
}
@Composable
fun CreateFolderDialog(
    showFolderDialog: MutableState<Boolean>,
    onCreateFolder: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var folderName by remember { mutableStateOf("") }

    if (showFolderDialog.value) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        AlertDialog(
            onDismissRequest = { showFolderDialog.value = false },
            title = { Text("Create New Folder") },
            text = {
                TextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.key == Key.Enter && folderName.trim().isNotEmpty()) {
                                onCreateFolder(folderName.trim())
                                folderName = ""
                                showFolderDialog.value = false
                                keyboardController?.hide() // Optionally hide the keyboard
                                true
                            } else {
                                false
                            }
                        }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.trim().isNotEmpty()) {
                            onCreateFolder(folderName.trim())
                            folderName = ""
                            showFolderDialog.value = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showFolderDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun FolderItem(
    folderName: String,
    onClick: () -> Unit,
    onOpenCamera: () -> Unit,
    onDeleteFolder: () -> Unit,
    openFolder: () -> Unit
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
            .clickable(onClick = onClick)
            .padding(top = 10.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = folderName,
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row {
            Button(
                onClick = { openFolder() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795757))
            ) {
                Icon(painter = painterResource(id = R.drawable.openfolder), contentDescription = "camera button")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onOpenCamera() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795757))
            ) {
                Icon(painter = painterResource(id = R.drawable.camera), contentDescription = "camera button")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(painter = painterResource(id = R.drawable.delete), contentDescription = "delete button")
            }
        }
    }
}