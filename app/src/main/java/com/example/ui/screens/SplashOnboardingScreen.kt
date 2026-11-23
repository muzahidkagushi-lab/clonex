package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ControlPointDuplicate
import androidx.compose.material.icons.rounded.LockPerson
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.LanguageOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int
)

@Composable
fun SplashAndOnboardingScreen(
    currentLanguage: LanguageOption,
    onSelectLanguage: (LanguageOption) -> Unit,
    onComplete: () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    var showTermsWarning by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(900)
        showSplash = false
    }

    val slides = remember {
        listOf(
            OnboardingSlide(
                icon = Icons.Rounded.ControlPointDuplicate,
                titleRes = R.string.onboarding_slide_1_title,
                descRes = R.string.onboarding_slide_1_desc
            ),
            OnboardingSlide(
                icon = Icons.Rounded.LockPerson,
                titleRes = R.string.onboarding_slide_2_title,
                descRes = R.string.onboarding_slide_2_desc
            ),
            OnboardingSlide(
                icon = Icons.Rounded.AdminPanelSettings,
                titleRes = R.string.onboarding_slide_3_title,
                descRes = R.string.onboarding_slide_3_desc
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.clonex_brand_icon),
                    contentDescription = stringResource(R.string.app_full_title),
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.app_full_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar: Quick Language Switcher + Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { onSelectLanguage(LanguageOption.BENGALI) },
                            modifier = Modifier.testTag("onboarding_lang_bn")
                        ) {
                            Text(
                                text = "বাংলা",
                                fontWeight = if (currentLanguage == LanguageOption.BENGALI) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        FilledTonalButton(
                            onClick = { onSelectLanguage(LanguageOption.ENGLISH) },
                            modifier = Modifier.testTag("onboarding_lang_en")
                        ) {
                            Text(
                                text = "EN",
                                fontWeight = if (currentLanguage == LanguageOption.ENGLISH) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    TextButton(
                        onClick = { showTermsWarning = true },
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 560.dp)
                ) { page ->
                    val slide = slides[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (page == 0) {
                            Image(
                                painter = painterResource(id = R.drawable.clonex_brand_logo),
                                contentDescription = stringResource(R.string.app_full_title),
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.size(104.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = slide.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = stringResource(slide.titleRes),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(slide.descRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Pager Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(slides.size) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(if (active) 24.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isLast = pagerState.currentPage == slides.lastIndex
                Button(
                    onClick = {
                        if (isLast) {
                            showTermsWarning = true
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_primary_btn")
                ) {
                    Text(
                        text = if (isLast) stringResource(R.string.onboarding_get_started)
                        else stringResource(R.string.onboarding_next)
                    )
                }
            }
        }
    }

    if (showTermsWarning) {
        ResponsibleUseDialog(
            onReadPrivacy = { showPrivacySheet = true },
            onAccept = {
                showTermsWarning = false
                onComplete()
            }
        )
    }

    if (showPrivacySheet) {
        AlertDialog(
            onDismissRequest = { showPrivacySheet = false },
            title = { Text(stringResource(R.string.privacy_policy_title)) },
            text = {
                Text(
                    text = stringResource(R.string.privacy_policy_body),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacySheet = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
fun ResponsibleUseDialog(
    onReadPrivacy: () -> Unit,
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(text = stringResource(R.string.terms_warning_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.terms_warning_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onReadPrivacy) {
                    Text(stringResource(R.string.terms_warning_privacy_link))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("terms_accept_btn")
            ) {
                Text(stringResource(R.string.terms_warning_accept))
            }
        }
    )
}
