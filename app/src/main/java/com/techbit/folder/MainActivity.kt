package com.techbit.folder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var storagePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var cameraResultLauncher: ActivityResultLauncher<Intent>

    private val folderList = mutableStateOf<List<String>>(emptyList())
    private val rootPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "MyAlbums")
    private val currentPath = mutableStateOf(rootPath)
    private var currentMediaPath: String? = null

    private val REQUEST_CODE_CAPTURE_MEDIA = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerActivityResultLaunchers()

        setContent {
            FolderScreen(
                folderList = folderList.value,
                currentPath = currentPath.value,
                onCreateFolder = { name -> createFolder(name) },
                requestStorageAccess = ::requestStorageAccess,
                onFolderClick = { folder -> updateCurrentPath(folder) },
                onBackPress = { navigateToParentDirectory() },
                onOpenInGallery = { folderName -> openFolderInFileManager(File(currentPath.value, folderName)) },
                onOpenCamera = { folderName -> openCameraInFolder(File(currentPath.value, folderName)) } // Updated
            )
        }
    }

    override fun onBackPressed() {
        if (!navigateToParentDirectory()) {
            super.onBackPressed()
        }
    }

    private fun registerActivityResultLaunchers() {
        manageAllFilesPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    onPermissionGranted()
                } else {
                    onPermissionDenied()
                }
            }
        }

        storagePermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) onPermissionGranted() else onPermissionDenied()
        }

        cameraResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                currentMediaPath?.let { mediaPath ->
                    val mediaFile = File(mediaPath)
                    if (mediaFile.exists()) {
                        scanFile(mediaFile) // Scan the file after capture
                        Toast.makeText(this, "Media saved to: $mediaPath", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to find media file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun requestStorageAccess() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (!Environment.isExternalStorageManager()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    manageAllFilesPermissionLauncher.launch(intent)
                } else {
                    onPermissionGranted()
                }
            }
            else -> {
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA // Request CAMERA permission
                )
                if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
                    onPermissionGranted()
                } else {
                    storagePermissionsLauncher.launch(permissions)
                }
            }
        }
    }

    private fun onPermissionGranted() {
        Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
        folderList.value = getListOfFolders()
    }

    private fun onPermissionDenied() {
        Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
    }

    private fun getListOfFolders(): List<String> {
        val parentDir = currentPath.value
        val folderList = mutableListOf<String>()
        if (parentDir.isDirectory) {
            parentDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    folderList.add(file.name)
                }
            }
        }
        return folderList
    }

    private fun createFolder(folderName: String) {
        if (folderName.isNotEmpty()) {
            val newFolder = File(currentPath.value, folderName)
            if (!newFolder.exists()) {
                if (newFolder.mkdirs()) {
                    Toast.makeText(this, "Folder created successfully", Toast.LENGTH_SHORT).show()
                    folderList.value = getListOfFolders() // Refresh the folder list
                } else {
                    Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentPath(folderName: String) {
        val newPath = File(currentPath.value, folderName)
        if (newPath.isDirectory) {
            currentPath.value = newPath
            folderList.value = getListOfFolders() // Refresh the folder list
        }
    }

    private fun navigateToParentDirectory(): Boolean {
        val parentDir = currentPath.value.parentFile
        return if (parentDir != null && parentDir.exists() && currentPath.value.canonicalPath != rootPath.canonicalPath) {
            currentPath.value = parentDir
            folderList.value = getListOfFolders() // Refresh the folder list
            true // Indicate that navigation to the parent directory was successful
        } else {
            false // Indicate that there is no parent directory or can't navigate beyond root
        }
    }

    private fun openFolderInFileManager(folder: File) {
        if (folder.exists() && folder.isDirectory) {
            folder.listFiles()?.forEach { file ->
                MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null) { path, uri ->
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.fileprovider",
                folder
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(intent, "Open folder with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                startActivity(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No suitable app found to open this folder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Folder does not exist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCameraInFolder(folder: File) {
        val imageFile = createMediaFile(folder, "jpg")
        val videoFile = createMediaFile(folder, "mp4")

        val imageUri: Uri = FileProvider.getUriForFile(
            this,
            "com.techbit.folder.fileprovider",
            imageFile
        )

        val videoUri: Uri = FileProvider.getUriForFile(
            this,
            "com.techbit.folder.fileprovider",
            videoFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(cameraIntent, "Capture Media").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(videoIntent))
        }

        cameraResultLauncher.launch(chooserIntent)
    }

    private fun createMediaFile(folder: File, extension: String): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MEDIA_${timeStamp}.$extension"
        return File(folder, fileName)
    }

    private fun scanFile(file: File) {
        MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null) { path, uri ->
        }
    }
}
