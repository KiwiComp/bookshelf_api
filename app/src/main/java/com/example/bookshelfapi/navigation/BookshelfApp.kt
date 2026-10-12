package com.example.bookshelfapi.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The app's root composable. It will hold the Scaffold, the bottom bar and the NavHost. */
@Composable
fun BookshelfApp(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {}
}
