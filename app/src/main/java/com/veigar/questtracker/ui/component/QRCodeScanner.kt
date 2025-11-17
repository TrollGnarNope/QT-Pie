package com.veigar.questtracker.ui.component // Or your actual package

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
// No Toast import needed here if Snackbar is the primary feedback for permission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import kotlinx.coroutines.launch

@Composable
fun QRCodeScanner(
    modifier: Modifier = Modifier, // This modifier will be key
    onScanSuccess: (String) -> Unit,
    onScanFailure: (Exception) -> Unit,
    autoFocusEnabled: Boolean = true,
    flashEnabled: Boolean = false,
    scanMode: ScanMode = ScanMode.SINGLE,
    onPermissionDenied: (() -> Unit)? = null,
    onPermissionGranted: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onPermissionGranted?.invoke()
        } else {
            onPermissionDenied?.invoke()
        }
    }

    LaunchedEffect(key1 = hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            onPermissionGranted?.invoke()
        }
    }

    if (hasCameraPermission) {
        val scannerView = remember { CodeScannerView(context) }
        val codeScanner = remember(context, scannerView, scanMode, autoFocusEnabled, flashEnabled) {
            CodeScanner(context, scannerView).apply {
                this.scanMode = scanMode
                this.isAutoFocusEnabled = autoFocusEnabled
                this.isFlashEnabled = flashEnabled
            }
        }

        DisposableEffect(codeScanner) {
            codeScanner.decodeCallback = DecodeCallback { result ->
                (context as? Activity)?.runOnUiThread { onScanSuccess(result.text) }
                    ?: onScanSuccess(result.text)
            }
            codeScanner.errorCallback = ErrorCallback { error ->
                Log.e("QRCodeScanner", "Scanner Error: ${error.message}", error)
                (context as? Activity)?.runOnUiThread { onScanFailure(error as Exception) }
                    ?: onScanFailure(error as Exception)
            }
            codeScanner.startPreview()
            onDispose { codeScanner.releaseResources() }
        }

        AndroidView(factory = { scannerView }, modifier = modifier) // Apply the passed modifier

    } else {
        LaunchedEffect(Unit) {
            if (!hasCameraPermission) { onPermissionDenied?.invoke() }
        }
    }
}