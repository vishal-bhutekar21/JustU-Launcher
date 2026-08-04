package com.justu.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.justu.launcher.data.model.ThemeSettings
import com.justu.launcher.data.repository.SettingsRepository
import com.justu.launcher.ui.apps.AppsScreen
import com.justu.launcher.ui.home.HomeScreen
import com.justu.launcher.ui.home.LeftScreen
import com.justu.launcher.ui.search.SearchScreen
import com.justu.launcher.ui.theme.JustUTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeSettings by settingsRepository.themeSettings.collectAsState(initial = ThemeSettings())
            val coroutineScope = rememberCoroutineScope()

            JustUTheme(
                themeMode = themeSettings.themeMode,
                fontFamily = themeSettings.fontFamily,
                fontScale = themeSettings.fontScale
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Layout:
                    //   Vertical pages:
                    //     0 = Main Horizontal Pager (Home row)
                    //     1 = SearchScreen (Quick Google search)
                    //
                    //   Horizontal pages (within Vertical page 0):
                    //     0 = LeftScreen  (Intentions)
                    //     1 = HomeScreen  (Main)
                    //     2 = AppsScreen  (Drawer)

                    val verticalPagerState = rememberPagerState(initialPage = 0) { 2 }
                    val horizontalPagerState = rememberPagerState(initialPage = 1) { 3 }

                    BackHandler(enabled = true) {
                        coroutineScope.launch {
                            if (verticalPagerState.currentPage != 0) {
                                verticalPagerState.animateScrollToPage(0)
                            } else if (horizontalPagerState.currentPage != 1) {
                                horizontalPagerState.animateScrollToPage(1)
                            }
                        }
                    }

                    VerticalPager(
                        state = verticalPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { verticalPage ->
                        when (verticalPage) {
                            0 -> {
                                HorizontalPager(
                                    state = horizontalPagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { horizontalPage ->
                                    when (horizontalPage) {
                                        0 -> LeftScreen()
                                        1 -> HomeScreen()
                                        2 -> AppsScreen()
                                    }
                                }
                            }
                            1 -> SearchScreen()
                        }
                    }
                }
            }
        }
    }
}
