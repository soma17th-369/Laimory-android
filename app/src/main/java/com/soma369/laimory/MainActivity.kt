package com.soma369.laimory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.data.helper.NavigationHelperImpl
import com.soma369.laimory.navigation.LaimoryNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var messageHelper: MessageHelperImpl

    @Inject
    lateinit var navigationHelper: NavigationHelperImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                LaimoryNavGraph(
                    messages = messageHelper.messages,
                    navigationFlow = navigationHelper.navigationFlow,
                )
            }
        }
    }
}
