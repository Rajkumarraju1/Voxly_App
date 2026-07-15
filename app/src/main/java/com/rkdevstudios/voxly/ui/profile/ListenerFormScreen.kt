package com.rkdevstudios.voxly.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ListenerFormScreen(
    initialGender: String,
    initialLanguages: List<String>,
    onBackClick: () -> Unit,
    onSubmit: (String, String, List<String>) -> Unit
) {
    var gender by remember { mutableStateOf(initialGender) }
    var phoneNumber by remember { mutableStateOf("") }
    
    // Languages - matches LanguageScreen list as requested
    val allLanguages = listOf("English", "Hindi", "Telugu", "Tamil", "Kannada", "Malayalam") 
    
    // Single selection state
    var selectedLanguage by remember { mutableStateOf(initialLanguages.firstOrNull()) }

    // Removed local dialog. We will rely on navigation change to show success state in parent screen.
    // Or we keep simple dialog and then navigate.
    // User requested: "press on ok then user should navigate to Become a Listiner screen"
    
    var showSuccessDialog by remember { mutableStateOf(false) }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { /* Prevent dismiss */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✓", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Application Submitted",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you! Our team will verify your details and reach out to you within 24 hours.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onSubmit(gender, phoneNumber, listOfNotNull(selectedLanguage))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = "OK",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF05050A),
        topBar = {
            TopAppBar(
                title = { Text("Listener Application", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF05050A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // 1. Step Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step 1 Details
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Details", color = Color(0xFFE91E63), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Dotted Connector
                Text(" - - - - - - - ", color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 8.dp))

                // Step 2 Verification
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color(0xFF2A2A35), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("2", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Verification", color = Color.Gray, fontSize = 11.sp)
                }

                // Dotted Connector
                Text(" - - - - - - - ", color = Color(0xFF2A2A35), modifier = Modifier.padding(horizontal = 8.dp))

                // Step 3 Review
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color(0xFF2A2A35), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("3", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Review", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Subheader Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFDE3B75).copy(alpha = 0.2f), Color(0xFF8F00FF).copy(alpha = 0.2f))
                                )
                            )
                            .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Let's get you started",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please confirm your details to proceed.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 3. Gender Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♂", color = Color(0xFFDE3B75), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gender",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val genders = listOf("Male", "Female", "Other")
                        val symbols = listOf("♂ Male", "♀ Female", "⚦ Other")
                        genders.forEachIndexed { idx, option ->
                            val isSelected = gender == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFDE3B75).copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFFDE3B75) else Color(0xFF2A2A35),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { gender = option },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = symbols[idx],
                                    color = if (isSelected) Color(0xFFDE3B75) else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 4. Phone Number Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Phone Number",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We'll use this to verify your account.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone input container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flag selector mock
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { }
                        ) {
                            Text("🇮🇳", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+91", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.6f)
                                .background(Color(0xFF2A2A35))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Outlined Text Field (Styled borderless to match mockup input container)
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { input ->
                                var digits = input.filter { it.isDigit() }
                                if (digits.startsWith("91") && digits.length > 10) {
                                    digits = digits.substring(2)
                                }
                                phoneNumber = if (digits.length > 10) digits.substring(0, 10) else digits
                            },
                            placeholder = { Text("Enter your phone number", color = Color.Gray, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Language Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100F14))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFFDE3B75),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Primary Language",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your primary language (Only one allowed)",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        allLanguages.forEach { lang ->
                            val isSelected = (lang == selectedLanguage)
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFDE3B75), Color(0xFF8F00FF))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color.Transparent, Color.Transparent)
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else Color(0xFF2A2A35),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedLanguage = lang }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lang,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 6. Action Button Continue
            val buttonEnabled = gender.isNotEmpty() && phoneNumber.length == 10 && selectedLanguage != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (buttonEnabled) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE91E63), Color(0xFF8F00FF))
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2A2A35), Color(0xFF2A2A35))
                            )
                        }
                    )
                    .clickable(enabled = buttonEnabled) {
                        showSuccessDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "Continue",
                        color = if (buttonEnabled) Color.White else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (buttonEnabled) Color.White else Color.Gray
                    )
                }
            }

            // Lock Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your information is safe and secure",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
