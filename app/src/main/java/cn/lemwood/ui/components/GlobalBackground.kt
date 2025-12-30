package cn.lemwood.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter

@OptIn(UnstableApi::class)
@Composable
fun GlobalBackground(
    type: String,
    path: String,
    scale: String,
    volume: Float
) {
    if (type == "none" || path.isBlank()) return

    val context = LocalContext.current
    val contentScale = when (scale) {
        "fill" -> ContentScale.Crop
        "fit" -> ContentScale.Fit
        "stretch" -> ContentScale.FillBounds
        else -> ContentScale.Crop
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (type) {
            "image" -> {
                Image(
                    painter = rememberAsyncImagePainter(model = path),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            "video" -> {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        repeatMode = Player.REPEAT_MODE_ALL
                        setMediaItem(MediaItem.fromUri(Uri.parse(path)))
                        prepare()
                        playWhenReady = true
                    }
                }

                LaunchedEffect(path) {
                    exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(path)))
                    exoPlayer.prepare()
                }

                LaunchedEffect(volume) {
                    exoPlayer.volume = volume
                }

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = exoPlayer
                            resizeMode = when (scale) {
                                "fill" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                "fit" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                "stretch" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.resizeMode = when (scale) {
                            "fill" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            "fit" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            "stretch" -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    }
                )
            }
        }
    }
}
