package com.example.ui.datemate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// 3. SPLASH SCREEN
// ============================================================================
@Composable
fun DateMateSplashScreen(
    onSplashFinished: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.65f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 750),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(2200)
        onSplashFinished()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DateMateWhite,
                        DateMateBackground,
                        DateMateSoftBlush,
                    )
                )
            )
            .clickable { onSplashFinished() }
            .testTag("splash_screen_root")
    ) {
        Floating3DHeartsBackground(heartCount = 12)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                }
        ) {
            Animated3DHeartLogo(sizeDp = 128.dp, showDmMonogram = true)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DateMate",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "Heart",
                    tint = DateMatePrimaryPink,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            LinearProgressIndicator(
                color = DateMatePrimaryPink,
                trackColor = DateMateSoftBlush,
                modifier = Modifier
                    .width(140.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
            )
        }
    }
}

// ============================================================================
// 4. ONBOARDING SCREENS (3 Pages)
// ============================================================================
private data class OnboardingPageData(
    val titleRes: Int,
    val descRes: Int,
    val imageRes: Int,
    val badgeText: String,
)

@Composable
fun DateMateOnboardingScreen(
    onFinishOnboarding: () -> Unit,
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                titleRes = R.string.onboard_1_title,
                descRes = R.string.onboard_1_desc,
                imageRes = R.drawable.img_datemate_onboard_1_1795008183837,
                badgeText = "3D ROMANTIC MATCHING"
            ),
            OnboardingPageData(
                titleRes = R.string.onboard_2_title,
                descRes = R.string.onboard_2_desc,
                imageRes = R.drawable.img_datemate_onboard_2_1795008184919,
                badgeText = "REAL-TIME CHAT & CALLS"
            ),
            OnboardingPageData(
                titleRes = R.string.onboard_3_title,
                descRes = R.string.onboard_3_desc,
                imageRes = R.drawable.img_datemate_onboard_3_1795008186477,
                badgeText = "MEMORABLE DATE EXPERIENCES"
            ),
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val infinite = rememberInfiniteTransition(label = "onboard_float")
    val floatOffset by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "illustration_float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Floating3DHeartsBackground(heartCount = 8)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Brand & Skip Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = DateMateSoftBlush,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "DateMate",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DateMate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_btn")
                ) {
                    Text(
                        text = stringResource(R.string.btn_skip),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = DateMateSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(34.dp),
                        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .size(290.dp)
                            .offset(y = floatOffset.dp)
                            .border(1.5.dp, DateMateSoftBorder, RoundedCornerShape(34.dp))
                    ) {
                        Image(
                            painter = painterResource(id = page.imageRes),
                            contentDescription = stringResource(page.titleRes),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                    ) {
                        Text(
                            text = page.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(page.titleRes),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(page.descRes),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = DateMateSecondaryText
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Page Indicators & Action Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { index ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (selected) 28.dp else 8.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    if (selected) DateMatePrimaryPink else DateMateSoftBorder
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                val isLastPage = pagerState.currentPage == pages.lastIndex
                DateMateGradientButton(
                    text = if (isLastPage) {
                        stringResource(R.string.btn_get_started)
                    } else {
                        stringResource(R.string.btn_continue)
                    },
                    onClick = {
                        if (isLastPage) {
                            onFinishOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_primary_btn"
                )
            }
        }
    }
}

// ============================================================================
// 5. AUTHENTICATION SCREENS
// Supports: Mobile OTP, Email Login, Create Account, Forgot Password,
// Date of Birth verification, 18+ confirmation, Terms & Privacy acceptance,
// and Google Sign-In
// ============================================================================
private enum class AuthTabMode {
    MOBILE_OTP,
    EMAIL,
    CREATE_ACCOUNT,
}

@Composable
fun DateMateAuthenticationScreen(
    onAuthenticated: (fullName: String, emailOrMobile: String, dob: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AuthTabMode.MOBILE_OTP) }
    var fullName by rememberSaveable { mutableStateOf("Aarav Khanna") }
    var mobileNumber by rememberSaveable { mutableStateOf("+91 98204 51280") }
    var otpCode by rememberSaveable { mutableStateOf("482910") }
    var otpSent by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("aarav.khanna@datemate.app") }
    var password by rememberSaveable { mutableStateOf("DateMate2025!") }
    var dateOfBirth by rememberSaveable { mutableStateOf("14 May 1998") }
    var age18Confirmed by rememberSaveable { mutableStateOf(true) }
    var termsAccepted by rememberSaveable { mutableStateOf(true) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        Floating3DHeartsBackground(heartCount = 7)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Animated3DHeartLogo(sizeDp = 82.dp, showDmMonogram = true)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Welcome to DateMate",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "A Better Way to Connect ❤️",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DateMatePrimaryPink,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Switcher Pill
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateSoftBlush,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(
                        AuthTabMode.MOBILE_OTP to "Mobile OTP",
                        AuthTabMode.EMAIL to "Email Login",
                        AuthTabMode.CREATE_ACCOUNT to "Create Account",
                    ).forEach { (tabMode, label) ->
                        val selected = mode == tabMode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) DateMateWhite else Color.Transparent)
                                .clickable { mode = tabMode }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selected) DateMatePrimaryPink else DateMateSecondaryText,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Form Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DateMateWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (mode == AuthTabMode.CREATE_ACCOUNT) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Full Name",
                                    tint = DateMatePrimaryPink
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = dateMateTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_name_input")
                        )
                    }

                    if (mode == AuthTabMode.MOBILE_OTP) {
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            label = { Text("Mobile Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneIphone,
                                    contentDescription = "Mobile",
                                    tint = DateMatePrimaryPink
                                )
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = {
                                        otpSent = true
                                        onShowMessage("6-digit verification OTP sent to $mobileNumber")
                                    }
                                ) {
                                    Text(
                                        text = if (otpSent) "Resend OTP" else "Send OTP",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = DateMatePrimaryPink,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = dateMateTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_mobile_input")
                        )

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it.take(6) },
                            label = { Text("6-Digit OTP Verification") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "OTP",
                                    tint = DateMatePrimaryPink
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = dateMateTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_otp_input")
                        )
                    } else {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = DateMatePrimaryPink
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = dateMateTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password",
                                    tint = DateMatePrimaryPink
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = dateMateTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )

                        if (mode == AuthTabMode.EMAIL) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DateMatePrimaryPink,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier
                                        .clickable { showForgotPasswordDialog = true }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Date of Birth Verification (Required for 18+ compliance)
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date of Birth (18+ Verification)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Cake,
                                contentDescription = "Date of Birth",
                                tint = DateMatePrimaryPink
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = dateMateTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_dob_input")
                    )

                    // 18+ Age Confirmation Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { age18Confirmed = !age18Confirmed }
                    ) {
                        Checkbox(
                            checked = age18Confirmed,
                            onCheckedChange = { age18Confirmed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DateMatePrimaryPink,
                                uncheckedColor = DateMateSecondaryText
                            )
                        )
                        Text(
                            text = "I confirm that I am 18 years of age or older.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Terms & Privacy Acceptance Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { termsAccepted = !termsAccepted }
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DateMatePrimaryPink,
                                uncheckedColor = DateMateSecondaryText
                            )
                        )
                        Text(
                            text = "I accept DateMate's Terms & Conditions and Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText
                            ),
                            modifier = Modifier.clickable { showTermsDialog = true }
                        )
                    }

                    DateMateGradientButton(
                        text = when (mode) {
                            AuthTabMode.MOBILE_OTP -> "Verify OTP & Continue"
                            AuthTabMode.EMAIL -> "Sign In to DateMate"
                            AuthTabMode.CREATE_ACCOUNT -> "Create DateMate Account"
                        },
                        enabled = age18Confirmed && termsAccepted,
                        onClick = {
                            val identifier = if (mode == AuthTabMode.MOBILE_OTP) mobileNumber else email
                            onAuthenticated(fullName, identifier, dateOfBirth)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "auth_submit_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Login Button
            OutlinedButton(
                onClick = onGoogleSignIn,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(DateMateWhite, RoundedCornerShape(999.dp))
                    .testTag("auth_google_signin_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Google Sign-In",
                    tint = DateMatePrimaryPink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Reset Your Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your registered email address to receive an instant password reset link.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotPasswordDialog = false
                        onShowMessage("Password reset instructions sent to $email")
                    }
                ) {
                    Text("Send Reset Link", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "DateMate Terms & Safety Pledge",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "DateMate is an adults-only (18+) dating and companionship community. All profiles are verified for authenticity, respectful conduct is mandatory, and your personal data is encrypted and protected.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("I Understand", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun dateMateTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DateMateWhite,
    unfocusedContainerColor = DateMateBackground,
    focusedBorderColor = DateMatePrimaryPink,
    unfocusedBorderColor = DateMateSoftBorder,
    focusedLabelColor = DateMatePrimaryPink,
    unfocusedLabelColor = DateMateSecondaryText,
    focusedTextColor = DateMateDarkText,
    unfocusedTextColor = DateMateDarkText,
    cursorColor = DateMatePrimaryPink,
)
