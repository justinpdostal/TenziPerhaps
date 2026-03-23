package com.example.dice_project_with_james

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Simple theme wrapper — teammates can expand this in ui/theme later
@Composable
fun MyDiceTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyDiceTheme {
                TenziGame()
            }
        }
    }
}

data class Die(
    val value: Int,
    val held: Boolean
)

@Composable
fun TenziGame() {
    val dieImages = arrayOf(
        R.drawable.dice1,
        R.drawable.dice2,
        R.drawable.dice3,
        R.drawable.dice4,
        R.drawable.dice5,
        R.drawable.dice6
    )

    // State vars
    var gameStarted by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    val dice = remember {
        mutableStateListOf(*Array(10) { Die(value = (1..6).random(), held = false) })
    }

    val timeColor = if (timeLeft < 10) {
        Color.Red
    } else {
        Color.Black
    }

    // Timer
    LaunchedEffect(gameStarted, timeLeft) {
        if(gameStarted) {
            if (timeLeft == 0) {
                gameStarted = false
                showGameOverDialog = true
            } else if (timeLeft > 0) {
                delay(1000L) // 1 second
                timeLeft--
            }
        }
    }

    fun resetGame() {
        for (i in dice.indices) {
            dice[i] = Die(value = (1..6).random(), held = false)
        }
        timeLeft = 30
        gameStarted = false
        showGameOverDialog = false
    }

    fun rollAll() {
        if (!gameStarted) return
        for (i in dice.indices) {
            if (!dice[i].held) {
                dice[i] = dice[i].copy(value = (1..6).random())
            }
        }
    }

    fun toggleHold(index: Int) {
        if (!gameStarted) return
        dice[index] = dice[index].copy(held = !dice[index].held)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TENZI",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Remaining Time: $timeLeft",
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            color = timeColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap a die to hold it. Roll the rest!",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        DiceRow(
            dice = dice.subList(0, 5),
            startIndex = 0,
            dieImages = dieImages,
            onToggleHold = { toggleHold(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        DiceRow(
            dice = dice.subList(5, 10),
            startIndex = 5,
            dieImages = dieImages,
            onToggleHold = { toggleHold(it) }
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (gameStarted) {
            Button(onClick = { rollAll() }) {
                Text(text = "Roll Dice", fontSize = 18.sp)
            }
        } else {
            Button(onClick = { gameStarted = true }) {
                Text(text = "Start Game", fontSize = 18.sp)
            }
        }
    }

    // Game Over
    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Game over!") },
            text = { Text(text = "Your time has run out.") },
            confirmButton = {
                TextButton(onClick = { resetGame() }) {
                    Text("Try Again")
                }
            }
        )
    }
}

@Composable
fun DiceRow(
    dice: List<Die>,
    startIndex: Int,
    dieImages: Array<Int>,
    onToggleHold: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        dice.forEachIndexed { localIndex, die ->
            val globalIndex = startIndex + localIndex
            SingleDie(
                die = die,
                imageRes = dieImages[die.value - 1],
                onClick = { onToggleHold(globalIndex) }
            )
        }
    }
}

@Composable
fun SingleDie(
    die: Die,
    imageRes: Int,
    onClick: () -> Unit
) {
    val borderColor = if (die.held) Color(0xFF4CAF50) else Color.Transparent
    val borderWidth = if (die.held) 3.dp else 0.dp

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Die showing ${die.value}",
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    )
}

@Preview(showBackground = true)
@Composable
fun TenziPreview() {
    MyDiceTheme {
        TenziGame()
    }
}