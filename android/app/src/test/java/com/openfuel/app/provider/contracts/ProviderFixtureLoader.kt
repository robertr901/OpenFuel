package com.openfuel.app.provider.contracts

import com.google.gson.Gson
import java.nio.charset.StandardCharsets

internal data class ProviderFixtureManifest(
    val version: Int,
    val providers: Map<String, List<String>>,
)

internal object ProviderFixtureLoader {
    private val gson = Gson()

    fun readFixture(path: String): String {
        val classLoader = checkNotNull(ProviderFixtureLoader::class.java.classLoader) {
            "Unable to resolve test classloader for fixture loading."
        }
        val stream = checkNotNull(classLoader.getResourceAsStream(path)) {
            "Missing fixture resource: $path"
        }
        return stream.use { input ->
            String(input.readBytes(), StandardCharsets.UTF_8)
        }
    }

    fun <T> parseFixture(path: String, clazz: Class<T>): T {
        return gson.fromJson(readFixture(path), clazz)
    }

    fun manifest(): ProviderFixtureManifest {
        return parseFixture(
            path = "provider_fixtures/manifest.json",
            clazz = ProviderFixtureManifest::class.java,
        )
    }
}
