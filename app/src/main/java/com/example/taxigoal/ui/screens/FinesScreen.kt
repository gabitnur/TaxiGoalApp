package com.example.taxigoal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.viewmodel.TaxiViewModel

@Composable
fun FinesScreen(viewModel: TaxiViewModel) {
    var iin by remember { mutableStateOf("") }
    var grnz by remember { mutableStateOf("") }
    val fines by viewModel.finesSearchList.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("👮 Штрафы (Сергек / EGOV)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = iin,
                    onValueChange = { iin = it },
                    label = { Text("ИИН водителя") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = grnz,
                    onValueChange = { grnz = it },
                    label = { Text("ГРНЗ (Номер авто)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.searchFines(iin, grnz) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = iin.length == 12 && grnz.isNotBlank()
                ) {
                    Text("ПРОВЕРИТЬ ШТРАФЫ")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (fines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Штрафов не найдено", modifier = Modifier.alpha(0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(fines) { fine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(fine.type, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("${fine.amount.toInt()} ₸", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                            Text("Скидка 50%: ${fine.amount.toInt() / 2} ₸", color = Color(0xFF388E3C), fontSize = 12.sp)
                            Text("Осталось дней для скидки: ${fine.daysLeft}", fontSize = 12.sp, color = Color.Black)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://kaspi.kz/guide/ru/payments/fines/"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                                ) {
                                    Text("ОПЛАТИТЬ", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        viewModel.recordStandaloneFine(fine.type, fine.amount / 2) // Записываем со скидкой
                                        Toast.makeText(context, "Штраф списан с цели", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Text("СПИСАТЬ С ЦЕЛИ", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
