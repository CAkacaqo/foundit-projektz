package com.example.foundit

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            FoundItTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoundItApp()
                }
            }
        }
    }
}

data class LostItemWithUser( // List kumpulan lost items & nama Usernya
    val id: String,
    val itemName: String,
    val description: String,
    val dateLost: String,
    val status: String,
    val userId: String,
    val userName: String = "User",
    val imageUrl: String = ""
)

data class FoundItemWithUser( // List kumpulan found items & nama Usernya
    val id: String = "",
    val itemName: String = "",
    val description: String = "",
    val dateFound: String = "",
    val status: String = "active",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = ""
)

// Theme dari Figma
@Composable
fun FoundItTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFF4DFF),   // Neon pink
        secondary = Color(0xFFFF4DFF),
        background = Color.Black,
        surface = Color.Black,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// App Navigation
@Composable
fun FoundItApp() {
    val navController = rememberNavController()
    val auth = Firebase.auth

    // Check if user is already logged in
    LaunchedEffect(key1 = true) {
        if (auth.currentUser != null) {
            // Langsung ke Lost Items Screen (Ga perlu login ulang)
            navController.navigate("lost") {
                popUpTo("opening") { inclusive = true }
            }
        }
    }


    NavHost(navController = navController, startDestination = "opening") {
        composable("opening") {
            OpeningScreen(
                onContinue = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignUp = { navController.navigate("signin") }
            )
        }
        composable("signin") {
            SignInScreen(
                onSignIn = { navController.navigate("thankyou") }
            )
        }
        composable("thankyou") {
            ThankYouScreen(
                onNext = { navController.navigate("lost") }
            )
        }
        composable("lost") {
            LostItemsScreen(
                onReportClick = { navController.navigate("report") },
                onNavigateToFound = { navController.navigate("found") },
                onNavigateToProfile = { navController.navigate("profile") },
                onItemClick = { itemId -> navController.navigate("itemDetails/$itemId/lost") } // Navigate to details
            )
        }
        composable("found") {
            FoundItemsScreen(
                onFoundClick = { navController.navigate("missing") },
                onNavigateToLost = { navController.navigate("lost") },
                onNavigateToProfile = { navController.navigate("profile") },
                onItemClick = { itemId -> navController.navigate("itemDetails/$itemId/found") } // Navigate to details
            )
        }
        composable("profile") {
            // Get user data from Firebase
            val currentUser = auth.currentUser
            var username by remember { mutableStateOf("Loading...") }

            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    // Get user data from Firestore
                    // Untuk tampilkan nama lengkap di Profile
                    Firebase.firestore.collection("users")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                username = document.getString("fullName") ?: "User"
                            }
                        }
                }
            }

            ProfileScreen(
                onNavigateToLost = { navController.navigate("lost") },
                onNavigateToFound = { navController.navigate("found") },
                username = username,
                onItemClick = { itemId, itemType ->
                    navController.navigate("itemDetails/$itemId/$itemType")
                }
            )
        }
        //Rute Item Details
        composable(
            route = "itemDetails/{itemId}/{itemType}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("itemType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            val itemType = backStackEntry.arguments?.getString("itemType")

            if (!itemId.isNullOrEmpty() && (itemType == "lost" || itemType == "found")) {
                ItemDetailsScreen(
                    itemId = itemId,
                    itemType = itemType,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                // Handle error: Invalid item ID or type.
                Text("Error: Invalid item details", color = Color.Red) // Simple error message
            }
        }

        composable("report"){ // Add Lost Item
            AddLostItemScreen(
                onSubmit = {itemId ->
                    navController.navigate("thankYouLost/$itemId")}
            )
        }
        composable("missing"){ // Add Found Item
            FoundLostItemScreen(
                onSubmit =  {itemId ->
                    navController.navigate("thankYouFound/$itemId")}
            )
        }
        composable("thankYouLost/{itemId}") { //Report KEHILANGAN barang
            backStackEntry -> val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ThankYouLostItemScreen(itemId = itemId, onBack = {
                navController.navigate("lost")}
            )
        }
        composable("thankYouFound/{itemId}") {  // Report MENEMUKAN barang
                backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            ThankYouFoundItemScreen(itemId = itemId, onBack = {
                navController.navigate("found")
            })
        }

    }
}

// Logo Aplikasi
@Composable
fun FoundItLogo() {
    Text(
        text = "FOUND IT",
        color = Color(0xFFFF4DFF),  // Neon pink
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier
            .border(
                width = 2.dp,
                color = Color(0xFFFF4DFF),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}


// Subtitle Aplikasi
@Composable
fun CampusSubtitle() {
    Text(
        text = "Campus Lost & Found",
        color = Color(0xFFFF4DFF),  // Neon pink
        fontSize = 14.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

// Desain Text Field
@Composable
fun FoundItTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            color = Color(0xFFFF4DFF),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF4DFF),
                unfocusedBorderColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                cursorColor = Color(0xFFFF4DFF),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
        )
    }
}

// Button
@Composable
fun FoundItButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF4DFF)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


// Save Profile Picture via local storage, lebih mudah untuk dimodif
fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = "profile_image.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()

        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Tampilan saat loading
@Composable
fun LoadingBox(message: String) {
    Box(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color.White)
    }
}

// Tampilan Item di ProfileScreen Page
@Composable
fun ProfileItemCard(itemName: String, imageUrl: String = "") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Display image url, kalau tidak ada pakai No Image
            if (imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Item image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = Color(0xFFFF4DFF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = itemName,
                color = Color(0xFFFF4DFF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Tampilan Item di Lost Item & Found Item Page
@Composable
fun ItemCard(username: String, itemName: String, imageUrl: String = "") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Display image url, kalau tidak ada pakai No Image
            if (imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Item image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Image",
                        color = Color(0xFFFF4DFF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (username.isNotEmpty()) "$username's *$itemName*" else "*$itemName*",
                color = Color(0xFFFF4DFF),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Semua item card ketika di click, muncul detailsnya
@Composable
fun ItemDetailsScreen(
    itemId: String,
    itemType: String, // "lost" or "found"
    onBackClick: () -> Unit
) {
    val db = Firebase.firestore

    // State to hold the item details
    var itemDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
    var ownerDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showPhoneNumber by remember { mutableStateOf(false) }

    // Function to update the item status to inactive
    val closeItem = {
        val collectionName = if (itemType == "lost") "lostItems" else "foundItems"
        db.collection(collectionName).document(itemId)
            .update("status", "inactive")
            .addOnSuccessListener {
                // Langsung ubah status di database sehingga tidak di display di Lost Item & found Item
                itemDetails = itemDetails?.toMutableMap()?.apply {
                    this["status"] = "inactive"
                }
            }
    }

    // Fetch item details from Firestore
    LaunchedEffect(key1 = itemId) {
        val collectionName = if (itemType == "lost") "lostItems" else "foundItems"

        db.collection(collectionName).document(itemId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val data = document.data ?: emptyMap()
                    itemDetails = data

                    // Now fetch the user details
                    val userId = data["userId"] as? String
                    if (userId != null) {
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc != null && userDoc.exists()) {
                                    ownerDetails = userDoc.data
                                }
                                isLoading = false
                            }
                            .addOnFailureListener {
                                isLoading = false
                            }
                    } else {
                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top Bar with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onBackClick) {
                Text("Back", color = Color.White)
            }
            Text(
                text = if (itemType == "lost") "Lost Item Details" else "Found Item Details",
                color = Color(0xFFFF4DFF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading item details...", color = Color.White)
            }
        } else if (itemDetails == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Item not found", color = Color.White)
            }
        } else {
            // Ambil ImageUrl untuk display itemnya
            val imageUrl = itemDetails?.get("imageUrl") as? String ?: ""

            // Item Image (with AsyncImage if URL exists)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Item image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show placeholder if no image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Image Available",
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Item Details
            val itemName = itemDetails?.get("itemName") as? String ?: "Unknown Item"
            val description = itemDetails?.get("description") as? String ?: "No description available"
            val dateField = if (itemType == "lost") "dateLost" else "dateFound"
            val date = itemDetails?.get(dateField) as? String ?: "Unknown date"
            val currentStatus = itemDetails?.get("status") as? String ?: "active"

            Text(
                text = itemName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(label = "Status:", value = currentStatus)
            DetailRow(label = if (itemType == "lost") "Date Lost:" else "Date Found:", value = date)
            DetailRow(label = "Description:", value = description)

            // Owner information
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Contact Information",
                color = Color(0xFFFF4DFF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val ownerName = ownerDetails?.get("fullName") as? String ?: "Unknown User"
            val ownerEmail = ownerDetails?.get("email") as? String ?: "No email provided"
            val ownerPhone = ownerDetails?.get("phoneNumber") as? String

            DetailRow(label = "Reported by:", value = ownerName)
            DetailRow(label = "Email:", value = ownerEmail)
            if (ownerPhone != null && showPhoneNumber) {
                DetailRow(label = "Phone:", value = ownerPhone)
            } else {
                DetailRow(label = "Phone:", value = if (ownerPhone != null) "Tap to show" else "Not provided")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Button(
                    onClick = { showPhoneNumber = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(Color(0xFFFF4DFF)),
                    enabled = currentStatus == "active" // Disable if item is already inactive
                ) {
                    Text("Contact", color = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Khusus untuk user yg upload item, bisa diganti statusnya
                // Jalur contact external jadi butuh validasi user yang bikin item
                ownerPhone?.let {
                    val currentUser = Firebase.auth.currentUser?.uid
                    val isOwner = itemDetails?.get("userId") == currentUser

                    Button(
                        onClick = {
                            // Call the function to update the item status
                            closeItem()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            if (currentStatus == "active" && isOwner) Color(0xFF4DFFFF) else Color.Gray // Indicate if action is possible and user is owner
                        ),
                        enabled = currentStatus == "active" && isOwner // Enable only if item is active and current user is the owner
                    ) {
                        Text(
                            if (itemType == "lost") "I've Found This Item" else "I've returned This Item",
                            color = if (currentStatus == "active" && isOwner) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            color = Color.White
        )
    }
}

// Opening Screen
@Composable
fun OpeningScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FoundItLogo()
            Spacer(modifier = Modifier.height(16.dp))
            CampusSubtitle()
            Spacer(modifier = Modifier.height(180.dp))
            Text(
                text = "Loading...",
                color = Color(0xFFFF4DFF),
                fontSize = 14.sp
            )

            // Auto navigate after a delay
            LaunchedEffect(key1 = true) {
                delay(2000)
                onContinue()
            }
        }
    }
}

// SignUp Screen dengan Firebase Auth
@Composable
fun SignUpScreen(onSignUp: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Di save di Authentication & Firestore Database
    val auth = Firebase.auth
    val db = Firebase.firestore

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FoundItLogo()
            Spacer(modifier = Modifier.height(8.dp))
            CampusSubtitle()
            Spacer(modifier = Modifier.height(32.dp))

            FoundItTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name"
            )

            FoundItTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email"
            )

            FoundItTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Phone Number"
            )

            FoundItTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true
            )

            FoundItTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm Password",
                isPassword = true
            )

            // Display error message if any
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            FoundItButton(
                text = if (isLoading) "Creating Account..." else "Sign Up",
                onClick = {
                    if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all required fields"
                        return@FoundItButton
                    }

                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@FoundItButton
                    }

                    isLoading = true
                    errorMessage = ""

                    // Create user with email and password
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // User created successfully, now store additional info
                                val user = auth.currentUser
                                val userInfo = hashMapOf(
                                    "fullName" to fullName,
                                    "email" to email,
                                    "phoneNumber" to phoneNumber // Untuk contact
                                )

                                // Add user to Firestore
                                db.collection("users")
                                    .document(user?.uid ?: "")
                                    .set(userInfo)
                                    .addOnSuccessListener {
                                        // Navigate to sign in
                                        onSignUp()
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Failed to save user data: ${e.message}"
                                    }
                            } else {
                                // If sign up fails, display a message to the user
                                errorMessage = "Authentication failed: ${task.exception?.message ?: "Unknown error"}"
                            }
                        }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            FoundItButton(
                modifier = Modifier
                    .height(40.dp)
                    .size(250.dp),
                text = "Already have an account?",
                onClick = onSignUp
            )
        }
    }
}


// Sign In Screen
@Composable
fun SignInScreen(onSignIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val auth = Firebase.auth

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FoundItLogo()
            Spacer(modifier = Modifier.height(8.dp))
            CampusSubtitle()
            Spacer(modifier = Modifier.height(16.dp))

            // Box isi Honesty Quotes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No legacy is so rich as honesty.\n" +
                            "~William Shakespeare",
                    color = Color(0xFFFF4DFF),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FoundItTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email"
            )

            FoundItTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true
            )

            // Display error message if any
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Text(
                    text = "Be careful when inputting your password",
                    color = Color(0xFFFF4DFF),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp, end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            FoundItButton(
                text = if (isLoading) "Signing In..." else "Sign In",
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@FoundItButton
                    }

                    isLoading = true
                    errorMessage = ""

                    // Sign in with email and password
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // Sign in success, navigate to the next screen
                                onSignIn()
                            } else {
                                // If sign in fails, display a message to the user
                                errorMessage = "Authentication failed: ${task.exception?.message ?: "Invalid credentials"}"
                            }
                        }
                }
            )
        }
    }
}

// Thank You Screen
@Composable
fun ThankYouScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FoundItLogo()
            Spacer(modifier = Modifier.height(8.dp))
            CampusSubtitle()
            Spacer(modifier = Modifier.height(32.dp))

            // Thank you message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Thank you for signing in",
                    color = Color(0xFFFF4DFF),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .width(120.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "Next",
                    fontSize = 16.sp,
                    color = Color(0xFFFF4DFF)
                )
            }
        }
    }
}

// Lost Item (alias homepage)
@Composable
fun LostItemsScreen(
    onReportClick: () -> Unit,
    onNavigateToFound: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val db = Firebase.firestore

    // Nyimpen lost items & informasi user
    var lostItems by remember { mutableStateOf<List<LostItemWithUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ambil Lost Items dari Firestore
    LaunchedEffect(key1 = true) {
        db.collection("lostItems")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val tempItems = documents.documents.mapNotNull { doc ->
                    doc.data?.let { data ->
                        // Filter untuk only show active items
                        val status = data["status"] as? String ?: "active"
                        if (status == "active") {
                            LostItemWithUser(
                                id = doc.id,
                                itemName = data["itemName"] as? String ?: "Unknown Item",
                                description = data["description"] as? String ?: "",
                                dateLost = data["dateLost"] as? String ?: "Unknown date",
                                status = data["status"] as? String ?: "",
                                userId = data["userId"] as? String ?: "",
                                imageUrl = data["imageUrl"] as? String ?: ""
                            )
                        } else null
                    }
                }

                // Informasi user dari setiap item
                if (tempItems.isNotEmpty()) {
                    val itemsWithUserDetails = mutableListOf<LostItemWithUser>()
                    var completedRequests = 0

                    tempItems.forEach { item ->
                        if (item.userId.isNotEmpty()) {
                            db.collection("users").document(item.userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val userName = if (userDoc.exists()) {
                                        userDoc.getString("fullName") ?: "Unknown User"
                                    } else {
                                        "Unknown User"
                                    }

                                    itemsWithUserDetails.add(item.copy(userName = userName))
                                    completedRequests++

                                    if (completedRequests == tempItems.size) {
                                        lostItems = itemsWithUserDetails
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener {
                                    itemsWithUserDetails.add(item)
                                    completedRequests++

                                    if (completedRequests == tempItems.size) {
                                        lostItems = itemsWithUserDetails
                                        isLoading = false
                                    }
                                }
                        } else {
                            itemsWithUserDetails.add(item)
                            completedRequests++

                            if (completedRequests == tempItems.size) {
                                lostItems = itemsWithUserDetails
                                isLoading = false
                            }
                        }
                    }
                } else {
                    lostItems = emptyList()
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        FoundItLogo()
        CampusSubtitle()
        Spacer(modifier = Modifier.height(16.dp))

        // Navbar untuk aplikasi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Lost Items", color = Color(0xFFFF4DFF), fontWeight = FontWeight.Bold)
            Text(
                "Found Items",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToFound() }
            )
            Text(
                "Profile",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToProfile() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recently Reported Lost Items:",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading items...", color = Color.White)
            }
        } else if (lostItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No lost items reported yet", color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(lostItems.size) { index ->
                    val item = lostItems[index]

                    Box(modifier = Modifier.clickable { onItemClick(item.id) }) {
                        // Pindahin data ke itemCard
                        ItemCard(
                            username = item.userName,
                            itemName = item.itemName,
                            imageUrl = item.imageUrl
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        // Untuk ke upload lost items
        FoundItButton(
            text = "Report a Missing Item?",
            onClick = onReportClick,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// Found Item
@Composable
fun FoundItemsScreen(
    onFoundClick: () -> Unit,
    onNavigateToLost: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val db = Firebase.firestore

    // Nyimpen found items & informasi user
    var foundItems by remember { mutableStateOf<List<FoundItemWithUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Ambil Lost Items dari Firestore
    LaunchedEffect(key1 = true) {
        db.collection("foundItems")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val tempItems = documents.documents.mapNotNull { doc ->
                    doc.data?.let { data ->
                        // Filter untuk only show active items
                        val status = data["status"] as? String ?: "active"
                        if (status == "active") {
                            FoundItemWithUser(
                                id = doc.id,
                                itemName = data["itemName"] as? String ?: "Unknown Item",
                                description = data["description"] as? String ?: "",
                                dateFound = data["dateFound"] as? String ?: "Unknown date",
                                status = data["status"] as? String ?: "active",
                                userId = data["userId"] as? String ?: "",
                                imageUrl = data["imageUrl"] as? String ?: ""
                            )
                        } else null
                    }
                }

                // Informasi user dari setiap item
                if (tempItems.isNotEmpty()) {
                    val itemsWithUserDetails = mutableListOf<FoundItemWithUser>()
                    var completedRequests = 0

                    tempItems.forEach { item ->
                        if (item.userId.isNotEmpty()) {
                            db.collection("users").document(item.userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val userName = if (userDoc.exists()) {
                                        userDoc.getString("fullName") ?: "Unknown User"
                                    } else {
                                        "Unknown User"
                                    }

                                    itemsWithUserDetails.add(item.copy(userName = userName))
                                    completedRequests++

                                    if (completedRequests == tempItems.size) {
                                        foundItems = itemsWithUserDetails
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener {
                                    itemsWithUserDetails.add(item)
                                    completedRequests++

                                    if (completedRequests == tempItems.size) {
                                        foundItems = itemsWithUserDetails
                                        isLoading = false
                                    }
                                }
                        } else {
                            itemsWithUserDetails.add(item)
                            completedRequests++

                            if (completedRequests == tempItems.size) {
                                foundItems = itemsWithUserDetails
                                isLoading = false
                            }
                        }
                    }
                } else {
                    foundItems = emptyList()
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        FoundItLogo()
        CampusSubtitle()
        Spacer(modifier = Modifier.height(16.dp))

        // Navbar untuk aplikasi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                "Lost Items",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToLost() }
            )
            Text(
                "Found Items",
                color = Color(0xFFFF4DFF),
                fontWeight = FontWeight.Bold
            )
            Text(
                "Profile",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToProfile() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Items Found by the Community:",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading items...", color = Color.White)
            }
        } else if (foundItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No found items reported yet", color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(foundItems.size) { index ->
                    val item = foundItems[index]

                    Box(modifier = Modifier.clickable { onItemClick(item.id) }) {
                        // Pindahin data ke itemCard
                        ItemCard(
                            username = item.userName,
                            itemName = item.itemName,
                            imageUrl = item.imageUrl
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        // Untuk ke upload found items
        FoundItButton(
            text = "Found a Missing Item?",
            onClick = onFoundClick,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// Profile Page
@SuppressLint("UseKtx")
@Composable
fun ProfileScreen(
    username: String,
    onNavigateToLost: () -> Unit,
    onNavigateToFound: () -> Unit,
    onItemClick: (String, Any?) -> Unit
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // Untuk profile picture
    val context = LocalContext.current
    val sharedPref = remember {
        context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    }
    val savedPath = sharedPref.getString("profileImagePath", null)
    var imageFilePath by remember { mutableStateOf(savedPath) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { it ->
            val path = saveImageToInternalStorage(context, it)
            path?.let {
                imageFilePath = it
                sharedPref.edit().putString("profileImagePath", it).apply()
            }
        }
    }

    // User lost/found item state
    var userLostItems by remember { mutableStateOf<List<LostItemWithUser>>(emptyList()) }
    var userFoundItems by remember { mutableStateOf<List<FoundItemWithUser>>(emptyList()) }
    var isLoadingLost by remember { mutableStateOf(true) }
    var isLoadingFound by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = currentUser?.uid) {
        if (currentUser != null) {
            // Fetch user's lost items
            db.collection("lostItems")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    val tempItems = documents.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val status = data["status"] as? String ?: return@mapNotNull null
                        if (status == "active") { // filter untuk status
                            LostItemWithUser(
                                id = doc.id,
                                itemName = data["itemName"] as? String ?: "Unknown Item",
                                description = data["description"] as? String ?: "",
                                dateLost = data["dateLost"] as? String ?: "Unknown date",
                                status = status,
                                userId = data["userId"] as? String ?: "",
                                userName = "Your",
                                imageUrl = data["imageUrl"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    }
                    userLostItems = tempItems
                    isLoadingLost = false
                }
                .addOnFailureListener {
                    isLoadingLost = false
                }

            // Fetch user's found items
            db.collection("foundItems")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    val tempItems = documents.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        val status = data["status"] as? String ?: return@mapNotNull null
                        if (status == "active") { // filter untuk status
                            FoundItemWithUser(
                                id = doc.id,
                                itemName = data["itemName"] as? String ?: "Unknown Item",
                                description = data["description"] as? String ?: "",
                                dateFound = data["dateFound"] as? String ?: "Unknown date",
                                status = status,
                                userId = data["userId"] as? String ?: "",
                                userName = "Your",
                                imageUrl = data["imageUrl"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    }
                    userFoundItems = tempItems
                    isLoadingFound = false
                }
                .addOnFailureListener {
                    isLoadingFound = false
                }
        } else {
            isLoadingLost = false
            isLoadingFound = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        FoundItLogo()
        CampusSubtitle()
        Spacer(modifier = Modifier.height(16.dp))

        // Navbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Lost Items",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToLost() }
            )
            Text(
                text = "Found Items",
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToFound() }
            )
            Text(
                text = "Profile",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile image + edit icon
        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
            if (imageFilePath != null && File(imageFilePath!!).exists()) {
                Image(
                    painter = rememberAsyncImagePainter(File(imageFilePath!!)),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Magenta, CircleShape)
                        .align(Alignment.Center)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Default Profile",
                    tint = Color.White,
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.Center)
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile Picture",
                tint = Color(0xFFFF4DFF),
                modifier = Modifier
                    .offset(x = (-32).dp, y = (-8).dp)
                    .size(24.dp)
                    .background(Color.Black, shape = CircleShape)
                    .clickable { launcher.launch("image/*") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = username,
            color = Color(0xFFFF4DFF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Lost Items section
        Text("Currently Lost Items:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoadingLost) {
            LoadingBox("Loading your lost items...")
        } else if (userLostItems.isEmpty()) {
            LoadingBox("You haven't reported any lost items yet")
        } else {
            LazyColumn(modifier = Modifier.height(225.dp)) {
                items(userLostItems.size) { index ->
                    val item = userLostItems[index]
                    Box(modifier = Modifier.clickable {
                        onItemClick(item.id, "lost")
                    }) {
                        ProfileItemCard( // Pindahin data
                            itemName = item.itemName,
                            imageUrl = item.imageUrl
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Found Items section
        Text("Items Found:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoadingFound) {
            LoadingBox("Loading your found items...")
        } else if (userFoundItems.isEmpty()) {
            LoadingBox("You haven't reported any found items yet")
        } else {
            LazyColumn(modifier = Modifier.height(225.dp)) {
                items(userFoundItems.size) { index ->
                    val item = userFoundItems[index]
                    Box(modifier = Modifier.clickable {
                        onItemClick(item.id, "found")
                    }) {
                        ProfileItemCard( // Pindahin data
                            itemName = item.itemName,
                            imageUrl = item.imageUrl
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Function to upload an image to Imgur and return the URL
// karena tidak bisa upload via firebase storage tanpa upgrade billing plan
private fun uploadImageToImgur(context: Context, imageUri: Uri, onComplete: (String) -> Unit) {
    val clientId = "b204225a4a258c1" // Imgur client ID pribadi

    // Use a coroutine to handle the upload in background
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Convert URI to ByteArray using content resolver
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val byteArray = inputStream?.use { it.readBytes() }

            if (byteArray == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to read image", Toast.LENGTH_LONG).show()
                    onComplete("")
                }
                return@launch
            }

            val base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)

            // Create OkHttp client
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // Create multipart request body to handle binary data better
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", base64Image)
                .addFormDataPart("type", "base64")
                .build()

            // Create request
            val request = Request.Builder()
                .url("https://api.imgur.com/3/image")
                .header("Authorization", "Client-ID $clientId")
                .post(requestBody)
                .build()

            // Execute synchronously in IO context
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            withContext(Dispatchers.Main) {
                try {
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)

                        if (jsonObject.getBoolean("success")) {
                            val dataObject = jsonObject.getJSONObject("data")
                            val imageUrl = dataObject.getString("link")
                            onComplete(imageUrl)
                        } else {
                            Toast.makeText(context, "Upload failed: ${jsonObject.getString("data")}", Toast.LENGTH_LONG).show()
                            onComplete("")
                        }
                    } else {
                        Toast.makeText(context, "Empty response from server", Toast.LENGTH_LONG).show()
                        onComplete("")
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error parsing response: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete("")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete("")
            }
        }
    }
}

// Tambah Lost Item Page
@Composable
fun AddLostItemScreen(onSubmit: (String) -> Unit) {
    // State variables for user input
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var dateLost by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Image-related states
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUploadInProgress by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }

    // Context for image picker
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Upload image when selected
            uploadImageToImgur(context, it) { url ->
                imageUrl = url
                imageUploadInProgress = false
            }
        }
    }

    // Firebase instances
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("You lost an item?", color = Color(0xFFFF4DFF), fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        // Image picker section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(8.dp))
                .clickable { launcher.launch("image/*") }
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                if (imageUploadInProgress) {
                    CircularProgressIndicator(color = Color(0xFFFF4DFF))
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Add an overlay to show it's been selected
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )

                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Image selected",
                        tint = Color(0xFFFF4DFF),
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = "Add Image",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to add image", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Name of item") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = itemDescription,
            onValueChange = { itemDescription = it },
            label = { Text("Item Description") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = dateLost,
            onValueChange = { dateLost = it },
            label = { Text("Date of Loss (DD/MM/YYYY)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (itemName.isNotBlank() && itemDescription.isNotBlank()) {
                    isSubmitting = true

                    // Create a unique ID for this lost item
                    val itemId = db.collection("lostItems").document().id

                    // Create the item object as a HashMap
                    val lostItem = hashMapOf(
                        "id" to itemId,
                        "userId" to (currentUser?.uid ?: ""),
                        "itemName" to itemName,
                        "description" to itemDescription,
                        "dateLost" to dateLost,
                        "imageUrl" to imageUrl, // Add the Imgur image URL
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "status" to "active"
                    )

                    // Save to Firestore
                    db.collection("lostItems").document(itemId)
                        .set(lostItem)
                        .addOnSuccessListener {
                            isSubmitting = false
                            onSubmit(itemId)
                        }
                        .addOnFailureListener {
                            isSubmitting = false
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF4DFF)),
            enabled = !isSubmitting && itemName.isNotBlank() && itemDescription.isNotBlank()
        ) {
            Text(if (isSubmitting) "Submitting..." else "Report")
        }
    }
}

@Composable
fun FoundLostItemScreen(onSubmit: (String) -> Unit) {
    // State variables for user input
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }
    var dateFound by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Image-related states
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUploadInProgress by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }

    // Context for image picker
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            imageUploadInProgress = true
            // Upload image when selected
            uploadImageToImgur(context, it) { url ->
                imageUrl = url
                imageUploadInProgress = false
            }
        }
    }

    // Firebase instances
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("You Found a Missing Item!", color = Color(0xFFFF4DFF), fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))

        // Image picker box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.DarkGray.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(8.dp))
                .clickable { launcher.launch("image/*") }
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                if (imageUploadInProgress) {
                    CircularProgressIndicator(color = Color(0xFFFF4DFF))
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Add an overlay to show it's been selected
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )

                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Image selected",
                        tint = Color(0xFFFF4DFF),
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = "Add Image",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to add image", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Name of item") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = itemDescription,
            onValueChange = { itemDescription = it },
            label = { Text("Item Description") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = dateFound,
            onValueChange = { dateFound = it },
            label = { Text("Date of Finding (DD/MM/YYYY)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (itemName.isNotBlank() && itemDescription.isNotBlank()) {
                    isSubmitting = true

                    // Create a unique ID for this found item
                    val itemId = db.collection("foundItems").document().id

                    // Create the item object as a HashMap
                    val foundItem = hashMapOf(
                        "id" to itemId,
                        "userId" to (currentUser?.uid ?: ""),
                        "itemName" to itemName,
                        "description" to itemDescription,
                        "dateFound" to dateFound,
                        "imageUrl" to imageUrl, // Add the Imgur image URL
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "status" to "active"
                    )

                    // Save to Firestore
                    db.collection("foundItems").document(itemId)
                        .set(foundItem)
                        .addOnSuccessListener {
                            isSubmitting = false
                            onSubmit(itemId)
                        }
                        .addOnFailureListener {
                            isSubmitting = false
                            // Error handling could be added here
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF4DFF)),
            enabled = !isSubmitting && itemName.isNotBlank() && itemDescription.isNotBlank()
        ) {
            Text(if (isSubmitting) "Submitting..." else "Report")
        }
    }
}

@Composable
fun ThankYouLostItemScreen(itemId: String, onBack: () -> Unit) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // State to hold the latest lost item
    var itemName by remember { mutableStateOf("Loading...") }
    var itemDescription by remember { mutableStateOf("Loading...") }
    var dateLost by remember { mutableStateOf("Loading...") }
    var imageUrl by remember { mutableStateOf("") }

    // Fetch the latest lost item for this user
    LaunchedEffect(key1 = currentUser?.uid) {
        if (currentUser != null) {
            db.collection("lostItems").document(itemId) // Uses the itemId
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        itemName = doc.getString("itemName") ?: "Unknown Item"
                        itemDescription = doc.getString("description") ?: "No description"
                        dateLost = doc.getString("dateLost") ?: "Unknown date"
                        imageUrl = doc.getString("imageUrl") ?: ""
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Thank You for the Report!",
            color = Color(0xFFFF4DFF),
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            itemName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        // Display the image if available
        if (imageUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Item image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Show placeholder if no image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No image available", color = Color.White.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Description:",
            color = Color(0xFFFF4DFF),
            fontWeight = FontWeight.Bold
        )

        Text(
            itemDescription,
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Lost on: $dateLost",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Your item has been reported as lost. We'll notify you if someone finds it!",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF4DFF))
        ) {
            Text("Back to Lost Items")
        }
    }
}

@Composable
fun ThankYouFoundItemScreen(itemId: String, onBack: () -> Unit) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // State to hold the found item details
    var itemName by remember { mutableStateOf("Loading...") }
    var itemDescription by remember { mutableStateOf("Loading...") }
    var dateFound by remember { mutableStateOf("Loading...") }
    var imageUrl by remember { mutableStateOf("") }

    // Fetch the found item information
    LaunchedEffect(key1 = currentUser?.uid) {
        if (currentUser != null) {
            db.collection("foundItems").document(itemId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        itemName = doc.getString("itemName") ?: "Unknown Item"
                        itemDescription = doc.getString("description") ?: "No description"
                        dateFound = doc.getString("dateFound") ?: "Unknown date"
                        imageUrl = doc.getString("imageUrl") ?: ""
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Thank You for Reporting!",
            color = Color(0xFFFF4DFF),
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        Text(
            itemName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        // Display the image if available
        if (imageUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Item image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Show placeholder if no image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.DarkGray, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No image available", color = Color.White.copy(alpha = 0.6f))
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Description:",
            color = Color(0xFFFF4DFF),
            fontWeight = FontWeight.Bold
        )

        Text(
            itemDescription,
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Found on: $dateFound",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Your found item has been reported. We'll try to connect you with its owner!",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(Color(0xFFFF4DFF))
        ) {
            Text("Back to Found Items")
        }
    }
}