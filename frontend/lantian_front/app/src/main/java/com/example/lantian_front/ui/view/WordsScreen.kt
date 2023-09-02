package com.example.lantian_front.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lantian_front.model.VMCollection

@Composable
fun WordsScreen(vmCollection: VMCollection) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(400.dp)
    ) {
        var textState by remember { mutableStateOf("") }
        val onTextChange = { text: String ->
            textState = text
        }


//        OutlinedTextField(
//            value = textState,
//            onValueChange = { onTextChange(it) },
//            singleLine = true,
//            label = { Text(text = "Enter message") },
//            modifier = Modifier.padding(10.dp),
//            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
//        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "单词：")
            Text(text = vmCollection.connectServerVM.wordsResult.word)
        }
        val bullet = "\u2022"
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "解释：")
            Text(text = buildAnnotatedString {
                vmCollection.connectServerVM.wordsResult.explains.forEach {
                    withStyle(ParagraphStyle(textIndent = TextIndent(restLine = 12.sp))) {
                        append(bullet)
                        append("\t\t")
                        append(it)
                    }
                }
            })
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "翻译：")
            Text(text = vmCollection.connectServerVM.wordsResult.translation)
        }

    }
}