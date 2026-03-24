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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

// Data for user kvp.
data class LeaderboardEntry(
    val username: String,
    val timeMs: Long
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
    var startTimeMs by remember { mutableLongStateOf(0L) }
    var currentTimeMs by remember { mutableLongStateOf(0L) }

    // leaderboard variables, decided to go with
    // Long for time, will be converted
    // to xx:xx.xx format later.
    var username by remember { mutableStateOf("") }
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    val leaderboard = remember { // leaderboard does not have inf. persistance
        mutableStateListOf(
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L),
            LeaderboardEntry("---", 9999999L)
        )
    }

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

//    // Thankfully, AS does threading on its own.
//    LaunchedEffect(gameStarted) {
//        while (gameStarted) {
//            delay(10L)
//            elapsedTimeMs += 10L
//        }
//    } WAS NOT ACCURATE

    fun resetGame() {
        for (i in dice.indices) {
            dice[i] = Die(value = (1..6).random(), held = false)
        }
        timeLeft = 30
        startTimeMs = System.currentTimeMillis()
        gameStarted = false
        showGameOverDialog = false
    }

    // some weird scope thing, both of the below
    // had to be used right here to work with
    // invoking with the other funcs...

    // note this requires all to be held
    fun hasWon(): Boolean {
        val firstValue = dice.first().value
        return dice.all { it.held && it.value == firstValue }
    }

    // simple logic to update leaderboard.
    fun updateLeaderboard(name: String, timeMs: Long) {
        val safeName = if (name.isBlank()) "Player" else name.trim()

        leaderboard.add(LeaderboardEntry(safeName, timeMs))

        val sorted = leaderboard
            .filter { it.username != "---" }// cuts out blank user
            .sortedBy { it.timeMs } // shoutout sortedby
            .take(10) // truncates

        leaderboard.clear()
        leaderboard.addAll(sorted)

        while (leaderboard.size < 10) {
            leaderboard.add(LeaderboardEntry("---", 9999999L))
        }

        resetGame() // flush after win
    }

    fun rollAll() {
        if (!gameStarted) return
        for (i in dice.indices) {
            if (!dice[i].held) {
                dice[i] = dice[i].copy(value = (1..6).random())
            }
        }
        if (hasWon()) {
            gameStarted = false
            val finalTime = System.currentTimeMillis() - startTimeMs
            updateLeaderboard(username, finalTime)
        }
    }

    fun toggleHold(index: Int) {
        if (!gameStarted) return
        dice[index] = dice[index].copy(held = !dice[index].held)
        if (hasWon()) { // NOTE -- possible race condition
            gameStarted = false
            val finalTime = System.currentTimeMillis() - startTimeMs
            updateLeaderboard(username, finalTime)
        }
    }

    // simple conversion to standard format
    fun formatTime(timeMs: Long): String {
        val totalHundredths = timeMs / 10
        val minutes = totalHundredths / 6000
        val seconds = (totalHundredths % 6000) / 100
        val hundredths = totalHundredths % 100
        return "%d:%02d:%02d".format(minutes, seconds, hundredths)
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

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
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
            Button(onClick = {
                resetGame()
                gameStarted = true
            }) {
                Text(text = "Start Game", fontSize = 18.sp)
            }
        }

        // LEADERBOARD

        Spacer(modifier = Modifier.height(12.dp))

        // subrow to separate column
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            // LEFT COLUMN (1–5)
            Column {
                leaderboard.take(5).forEachIndexed { index, entry ->
                    val rank = index + 1
                    val time = if (entry.username == "---") "--:--:--" else formatTime(entry.timeMs)

                    Text("$rank. ${entry.username} - $time")
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // RIGHT COLUMN (6–10)
            Column {
                leaderboard.drop(5).take(5).forEachIndexed { index, entry ->
                    val rank = index + 6
                    val time = if (entry.username == "---") "--:--:--" else formatTime(entry.timeMs)

                    Text("$rank. ${entry.username} - $time")
                }
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