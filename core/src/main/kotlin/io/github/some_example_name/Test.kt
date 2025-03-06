package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.sin
import kotlin.math.PI

class Test {
    // Define screen dimensions and noise parameters.
    private val screenWidth = Gdx.graphics.width
    private val screenHeight = Gdx.graphics.height
    private val seed = 1233

    // Create a 1x1 white pixel texture.
    private val pixelTexture: Texture

    init {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        pixelTexture = Texture(pixmap)
        pixmap.dispose()
    }

    // Render method: iterates over every pixel on the screen,
    // computes a noise value, and draws a tinted pixel.
    fun render(batch: SpriteBatch) {
        batch.begin()
        val mapSize = 64
        for (x in 0 until mapSize) {
            for (y in 0 until mapSize) {
                var noiseValue = 0f
                for (scale in 1 until 4) {
                    val scaleFactor = 16f / (scale.toFloat() * scale.toFloat())
                    noiseValue += perlin(x.toFloat() / scaleFactor, y.toFloat() / scaleFactor, seed) / scale
                }
                //println(noiseValue)
                noiseValue += 0.5f
                noiseValue *= sin(PI * x / mapSize).toFloat()
                noiseValue *= sin(PI * y / mapSize).toFloat()
                // Use the noise value to set a grayscale color.
                batch.setColor(noiseValue, noiseValue, noiseValue, 1f)
                batch.draw(pixelTexture, x.toFloat()*16, y.toFloat()*16, 16f, 16f)
            }
        }
        batch.end()
        // Reset the batch color to white for subsequent draw calls.
        batch.setColor(1f, 1f, 1f, 1f)
    }

    // Clean up the texture when done.
    fun dispose() {
        pixelTexture.dispose()
    }
}
