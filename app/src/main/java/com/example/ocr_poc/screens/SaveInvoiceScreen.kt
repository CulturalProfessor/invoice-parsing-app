package com.example.ocr_poc.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

data class ReceiptItem(
    val title: String,
    val date: String,
    val category: String
)

@Composable
fun SavedInvoiceScreen() {
    val invoiceList = listOf(
        ReceiptItem("Refrigerator", "24 Aug 2024", "Appliances"),
        ReceiptItem("iPhone", "14 Jul 2024", "Electronics"),
        ReceiptItem("Bike", "25 May 2024", "Miscellaneous"),
        ReceiptItem("Couch", "04 May 2024", "Furniture"),
        ReceiptItem("Television", "07 Apr 2024", "Appliances")
    )

    // Display the list using LazyColumn
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(invoiceList) { invoice ->
            ReceiptCard(invoice)
        }
    }
}

@Composable
fun ReceiptCard(item: ReceiptItem) {
    // Use shadow for elevation effect
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Purchased on ${item.date}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Badge for category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF4CAF50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.category,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SavedInvoiceScreenPreview() {
    SavedInvoiceScreen()
}
