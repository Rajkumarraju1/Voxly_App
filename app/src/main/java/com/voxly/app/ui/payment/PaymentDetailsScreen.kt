package com.voxly.app.ui.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxly.app.data.model.User
import java.util.regex.Pattern

fun deriveBankFromIfsc(ifsc: String): Pair<String, String> {
    if (ifsc.length < 4) return Pair("", "")
    val prefix = ifsc.substring(0, 4).uppercase()
    val bankName = when (prefix) {
        "SBIN" -> "State Bank of India"
        "ICIC" -> "ICICI Bank"
        "HDFC" -> "HDFC Bank"
        "BARB" -> "Bank of Baroda"
        "UTIB" -> "Axis Bank"
        "PUNB" -> "Punjab National Bank"
        "CNRB" -> "Canara Bank"
        "KKBK" -> "Kotak Mahindra Bank"
        "YESB" -> "Yes Bank"
        "IBKL" -> "IDBI Bank"
        "IDIB" -> "Indian Bank"
        "UCOB" -> "UCO Bank"
        "UBIN" -> "Union Bank of India"
        "IOBA" -> "Indian Overseas Bank"
        "JAKA" -> "Jammu & Kashmir Bank"
        "MAHB" -> "Bank of Maharashtra"
        "PSIB" -> "Punjab & Sind Bank"
        "DLXB" -> "Dhanlaxmi Bank"
        "TMBL" -> "Tamilnad Mercantile Bank"
        "KVBL" -> "Karur Vysya Bank"
        "FSFB" -> "Fincare Small Finance Bank"
        "PAYT" -> "Paytm Payments Bank"
        "AIRP" -> "Airtel Payments Bank"
        else -> "Unknown Bank"
    }
    val branch = if (ifsc.length >= 11) "Branch Code: ${ifsc.substring(5)}" else "Main Branch"
    return Pair(bankName, branch)
}

@Composable
fun PaymentDetailsScreen(
    user: User,
    onBack: () -> Unit,
    onSave: (User) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(if (user.paymentMethod.isNotEmpty()) user.paymentMethod else "UPI") }
    
    // UPI State
    var upiId by remember { mutableStateOf(user.upiId) }
    var upiError by remember { mutableStateOf<String?>(null) }
    
    // Bank State
    var accountHolderName by remember { mutableStateOf(user.displayName) }
    var accountNumber by remember { mutableStateOf(user.bankAccountNumber) }
    var ifscCode by remember { mutableStateOf(user.ifscCode) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var ifscError by remember { mutableStateOf<String?>(null) }
    var holderError by remember { mutableStateOf<String?>(null) }

    val isUpiSelected = selectedMethod == "UPI"

    // Custom Double Arrow Icon (>>) for UPI ID
    val doubleArrowIcon = remember {
        ImageVector.Builder(
            name = "DoubleArrow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color(0xFFDE3B75))) {
            moveTo(6.41f, 6f)
            lineTo(5f, 7.41f)
            lineTo(9.58f, 12f)
            lineTo(5f, 16.59f)
            lineTo(6.41f, 18f)
            lineTo(12.41f, 12f)
            close()
            moveTo(13f, 6f)
            lineTo(11.59f, 7.41f)
            lineTo(16.17f, 12f)
            lineTo(11.59f, 16.59f)
            lineTo(13f, 18f)
            lineTo(19f, 12f)
            close()
        }.build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)) // Solid dark black
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Navigation Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Payment Details",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage where your earnings will be sent",
                    color = Color(0xFF8E8E93),
                    fontSize = 14.sp
                )
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 2. Custom Method Tabs Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PaymentMethodTab(
                    title = "UPI ID",
                    icon = Icons.Rounded.Smartphone,
                    isSelected = isUpiSelected,
                    onClick = { selectedMethod = "UPI" },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodTab(
                    title = "Bank Account",
                    icon = Icons.Rounded.AccountBalance,
                    isSelected = !isUpiSelected,
                    onClick = { selectedMethod = "Bank" },
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Middle Description Card (Dynamic based on selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141419))
                    .border(1.dp, Color(0xFF2A2A35), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Purple Circle Badge with White Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C132B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUpiSelected) doubleArrowIcon else Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF8F00FF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (isUpiSelected) "UPI ID" else "Bank Account",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isUpiSelected) "Fast withdrawals using your UPI ID" else "Withdraw directly to your bank account",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp
                    )
                }
            }

            // 4. Input Fields Content
            if (isUpiSelected) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter UPI ID",
                        color = Color(0xFFB8B6D5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    PaymentInputField(
                        value = upiId,
                        onValueChange = {
                            upiId = it
                            upiError = null
                        },
                        placeholder = "yourname@bank",
                        leadingIcon = doubleArrowIcon,
                        isError = upiError != null
                    )

                    Text(
                        text = if (upiError != null) upiError!! else "Example: username@okaxis",
                        color = if (upiError != null) Color.Red else Color(0xFF8E8E93),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Account Holder Name field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Account Holder Name",
                            color = Color(0xFFB8B6D5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        PaymentInputField(
                            value = accountHolderName,
                            onValueChange = {
                                accountHolderName = it
                                holderError = null
                            },
                            placeholder = "Enter account holder name",
                            leadingIcon = Icons.Rounded.Person,
                            isError = holderError != null
                        )

                        Text(
                            text = if (holderError != null) holderError!! else "Recommended (matching official records)",
                            color = if (holderError != null) Color.Red else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Account Number field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Account Number",
                            color = Color(0xFFB8B6D5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        PaymentInputField(
                            value = accountNumber,
                            onValueChange = {
                                accountNumber = it
                                accountError = null
                            },
                            placeholder = "Enter account number",
                            leadingIcon = Icons.Rounded.CreditCard,
                            isError = accountError != null,
                            keyboardType = KeyboardType.Number
                        )

                        Text(
                            text = if (accountError != null) accountError!! else "Minimum 9 digits",
                            color = if (accountError != null) Color.Red else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // IFSC Code field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "IFSC Code",
                            color = Color(0xFFB8B6D5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        PaymentInputField(
                            value = ifscCode,
                            onValueChange = {
                                ifscCode = it.uppercase()
                                ifscError = null
                            },
                            placeholder = "Enter IFSC code",
                            leadingIcon = Icons.Rounded.Lock,
                            isError = ifscError != null
                        )

                        Text(
                            text = if (ifscError != null) ifscError!! else "Example: HDFC0001234",
                            color = if (ifscError != null) Color.Red else Color(0xFF8E8E93),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Dynamic Read-Only Bank & Branch Information Card
                    val derivedBankDetails = remember(ifscCode) { deriveBankFromIfsc(ifscCode) }
                    val derivedBankName = derivedBankDetails.first
                    val derivedBranchName = derivedBankDetails.second

                    if (derivedBankName.isNotEmpty() && derivedBankName != "Unknown Bank") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF141419))
                                .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDE3B75).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFFDE3B75),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = derivedBankName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = derivedBranchName,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Secure & Verified Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2E1225).copy(alpha = 0.4f)) // Translucent dark pink
                    .border(1.dp, Color(0xFFDE3B75).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDE3B75).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFFDE3B75),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Secure & Verified",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your payment details are encrypted and securely stored.",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 6. Bottom Sticky Save button & Security text
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isUpiSelected) {
                        if (isValidUPI(upiId)) {
                            onSave(user.copy(upiId = upiId, paymentMethod = "UPI"))
                        } else {
                            upiError = "Invalid UPI ID Format"
                        }
                    } else {
                        var isValid = true
                        if (accountHolderName.trim().isEmpty()) {
                            holderError = "Holder Name is required"
                            isValid = false
                        }
                        if (accountNumber.length < 9) {
                            accountError = "Invalid Account Number"
                            isValid = false
                        }
                        if (!isValidIFSC(ifscCode)) {
                            ifscError = "Invalid IFSC Code"
                            isValid = false
                        }
                        
                        if (isValid) {
                            val derived = deriveBankFromIfsc(ifscCode).first
                            onSave(user.copy(
                                bankAccountNumber = accountNumber,
                                ifscCode = ifscCode,
                                bankName = if (derived.isNotEmpty() && derived != "Unknown Bank") derived else "Bank Transfer",
                                paymentMethod = "Bank"
                            ))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDE3B75) // Pill pink
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Verify & Save Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your data is 100% secure",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PaymentMethodTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFFDE3B75) else Color.Transparent
    val textColor = if (isSelected) Color(0xFFDE3B75) else Color(0xFF8E8E93)
    val containerBg = if (isSelected) Color(0xFF2E1225).copy(alpha = 0.3f) else Color(0xFF141419)

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isSelected) borderColor else Color(0xFF2A2A35),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.Gray,
                fontSize = 15.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isError) Color.Red else Color(0xFFDE3B75),
                modifier = Modifier.size(20.dp)
            )
        },
        singleLine = true,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF141419),
            unfocusedContainerColor = Color(0xFF141419),
            errorContainerColor = Color(0xFF141419),
            focusedBorderColor = Color(0xFFDE3B75),
            unfocusedBorderColor = Color(0xFF2A2A35),
            errorBorderColor = Color.Red
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

// Validation Logic
fun isValidUPI(upi: String): Boolean {
    return Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$").matcher(upi).matches()
}

fun isValidIFSC(ifsc: String): Boolean {
    return Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$").matcher(ifsc).matches()
}
