package com.musornibak.korvus

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.musornibak.korvus.data.model.ModelRegistry
import com.musornibak.korvus.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KorvusApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        instance = this
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val prefs = UserPrefs(this@KorvusApp)
            ModelRegistry.setCustom(prefs.customModels.first())
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()

    companion object {
        lateinit var instance: KorvusApp
            private set
    }
}
