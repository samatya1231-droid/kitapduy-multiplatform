package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.koin.koinScreenModel
import com.semtinazizi.kitapduy.ui.viewmodel.GuessAuthorState
import com.semtinazizi.kitapduy.ui.viewmodel.GuessAuthorScreenModel

class GuessAuthorScreen : Screen {
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<GuessAuthorScreenModel>()

        val gameState by screenModel.gameState.collectAsState()
        val currentQuestion by screenModel.currentQuestion.collectAsState()
        val score by screenModel.score.collectAsState()
        val lives by screenModel.lives.collectAsState()
        val selectedAnswer by screenModel.selectedAnswer.collectAsState()
        val showResultColor by screenModel.showResultColor.collectAsState()

        val BrandGold = Color(0xFFFFD700)
        
        if (gameState == GuessAuthorState.GAME_OVER) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💔", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Text("Oyun Bitti!", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Text(
                        text = "Tekrar denemek ister misin?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { screenModel.startGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Baştan Başla", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Vazgeç ve Çık", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kimin Kaleminden?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("Bilgi Yarışması", color = BrandGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            if (gameState == GuessAuthorState.LOADING) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGold)
                }
            } else {
                val question = currentQuestion

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    
                    // ?? MODERN SCORE & LIVES PANEL ??
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PUAN", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("$score", color = BrandGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { index ->
                                val isLost = index >= lives
                                val heartScale by animateFloatAsState(
                                    targetValue = if (isLost) 0.8f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = if (isLost) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f) else Color.Red,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .scale(heartScale)
                                        .padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    if (question != null) {
                        // ?? GLASSMORPHISM QUESTION CARD ??
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)), RoundedCornerShape(32.dp))
                        ) {
                            // Dekoratif arka plan
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .offset(x = (-50).dp, y = (-50).dp)
                                    .background(BrandGold.copy(alpha = 0.05f), CircleShape)
                                    .blur(40.dp)
                            )

                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✦",
                                    fontSize = 80.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = BrandGold.copy(alpha = 0.2f),
                                    modifier = Modifier.height(50.dp).offset(y = 10.dp)
                                )
                                Text(
                                    text = question.quote,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 20.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 30.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "✦",
                                    fontSize = 80.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = BrandGold.copy(alpha = 0.2f),
                                    modifier = Modifier.height(50.dp).offset(y = (-10).dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(40.dp))

                        // ?? MODERN ANSWER BUTTONS ??
                        question.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswer == option
                            val isCorrect = option == question.correctAnswer

                            val buttonBaseColor = if (showResultColor) {
                                if (isCorrect) Color(0xFF2E7D32)
                                else if (isSelected) Color(0xFFC62828)
                                else MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                            
                            val borderAlpha by animateFloatAsState(targetValue = if (isSelected) 0.8f else 0.1f)

                            Surface(
                                onClick = { screenModel.submitAnswer(option) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(64.dp)
                                    .testTag("guess-author-option-$index"),
                                shape = RoundedCornerShape(20.dp),
                                color = buttonBaseColor,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = borderAlpha)),
                                tonalElevation = if (isSelected) 8.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        color = if (showResultColor && (isSelected || isCorrect)) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected || (showResultColor && isCorrect)) FontWeight.Bold else FontWeight.Medium
                                    )
                                    
                                    if (showResultColor) {
                                        if (isCorrect) Icon(Icons.Filled.Whatshot, null, tint = Color.White)
                                        else if (isSelected) Icon(Icons.Filled.Info, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}
