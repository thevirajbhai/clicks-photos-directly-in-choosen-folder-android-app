package com.techbit.folder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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

    private val rootPath =
        File(Environment.getExternalStorageDirectory(), "MyAlbum") // New root path

    private val currentPath = mutableStateOf(rootPath)
    private val folderList = mutableStateOf<List<String>>(emptyList())
    private val imageList = mutableStateOf<List<String>>(emptyList())

    private lateinit var currentPhotoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        registerActivityResultLaunchers()

        setContent {
            FolderScreen(
                currentPath = currentPath.value,
                rootPath = rootPath, // Pass the rootPath
                folderList = folderList.value,
                imageList = imageList.value,
                onCreateFolder = ::createFolder,
                requestStorageAccess = ::requestStorageAccess,
                onFolderClick = ::updateCurrentPath,
                onBackPress = ::navigateToParentDirectory,
                onOpenCamera = { folderName ->
                    openCameraInFolder(
                        File(
                            currentPath.value,
                            folderName
                        )
                    )
                },
                onOpenCamera2 = { openCameraInFolder(currentPath.value) },
                onDeleteFolder = ::deleteFolder,
                onDeleteImage = { imagePath -> onDelteImage(imagePath) },
                openFolder = { folderPath -> openFileManager(folderPath) }
            )
        }
    }


    override fun onBackPressed() {
        if (!navigateToParentDirectory()) {
            super.onBackPressed()
        }
    }

    private fun onDelteImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            file.delete()
            imageList.value = getImagesFromFolder()
            scanFile(this, file = file)
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

        cameraResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                    imageList.value = getImagesFromFolder()
                    scanFile(this, file = currentPhotoFile)
                }
            }
    }
    private fun openFileManager(folderPath: String) {
        val file = File(folderPath)
        val uri = FileProvider.getUriForFile(this, "com.techbit.folder.fileprovider", file)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "resource/folder")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
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
                if (permissions.all {
                        ContextCompat.checkSelfPermission(
                            this,
                            it
                        ) == PackageManager.PERMISSION_GRANTED
                    }) {
                    onPermissionGranted()
                } else {
                    storagePermissionsLauncher.launch(permissions)
                }
            }
        }
    }

    private fun scanFile(context: Context, file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null
        ) { path, uri ->
            println("Scanned $path:")
            println("-> uri=$uri")
        }
    }

    private fun onPermissionGranted() {
        Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
        getList()
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

    private fun getImagesFromFolder(): List<String> {
        return currentPath.value.listFiles { file ->
            file.extension in listOf(
                "jpg",
                "jpeg",
                "png",
                "gif"
            )
        }?.map { it.absolutePath } ?: emptyList()
    }

    private fun getList() {
        folderList.value = getListOfFolders()
        imageList.value = getImagesFromFolder()
    }

    private fun createFolder(folderName: String) {
        if (folderName.isNotEmpty()) {
            val newFolder = File(currentPath.value, folderName)
            if (!newFolder.exists()) {
                if (newFolder.mkdirs()) {
                    Toast.makeText(this, "Folder created successfully", Toast.LENGTH_SHORT).show()
                    folderList.value = getListOfFolders() // Refresh the folder list
                }
            }
        } else {
            Toast.makeText(this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFolder(folderName: String) {
        val folderToDelete = File(currentPath.value, folderName)
        if (folderToDelete.exists() && folderToDelete.isDirectory) {
            folderToDelete.deleteRecursively()
            Toast.makeText(this, "Folder deleted successfully", Toast.LENGTH_SHORT).show()
            folderList.value = getListOfFolders() // Refresh the folder list
        } else {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCurrentPath(folderName: String) {
        val newPath = File(currentPath.value, folderName)
        if (newPath.isDirectory) {
            currentPath.value = newPath
            folderList.value = getListOfFolders() // Refresh the folder list
            imageList.value = getImagesFromFolder() // Update the image list
        }
    }

    private fun navigateToParentDirectory(): Boolean {
        val parentDir = currentPath.value.parentFile
        return if (parentDir != null && parentDir.exists() && currentPath.value.canonicalPath != rootPath.canonicalPath) {
            currentPath.value = parentDir
            folderList.value = getListOfFolders() // Refresh the folder list
            imageList.value = getImagesFromFolder() // Update the image list
            true // Indicate that navigation to the parent directory was successful
        } else {
            false // Indicate that there is no parent directory or can't navigate beyond root
        }
    }

    private fun openCameraInFolder(folder: File) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            currentPhotoFile = createImageFile(folder)
            val photoUri: Uri = FileProvider.getUriForFile(
                this@MainActivity,
                "com.techbit.folder.fileprovider",
                currentPhotoFile
            )
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        cameraResultLauncher.launch(cameraIntent)
    }


    private fun createImageFile(folder: File): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(folder, "IMG_$timeStamp.jpg")
    }

}