package com.techbit.folder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var storagePermissionsLauncher: ActivityResultLauncher<Array<String>>
    private val folderList = mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerActivityResultLaunchers()

        setContent {
            FolderScreen(
                folderList = folderList.value,
                onCreateFolder = { name -> createFolder(name) },
                requestStorageAccess = ::requestStorageAccess
            )
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
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
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
        val parentDir = Environment.getExternalStorageDirectory()
        return if (parentDir.isDirectory) {
            parentDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }
    private fun createFolder(folderName: String) {
        if (folderName.isNotEmpty()) {
            val parentDir = Environment.getExternalStorageDirectory()
            val newFolder = File(parentDir, folderName)
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
}
