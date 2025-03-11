package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * GameUI creates a simple overlay drawn directly to the screen.
 * It displays:
 * - A CO₂ section with an icon and a progress bar (the fill tinted from red to green based on the CO₂ value),
 * - An Energy section with an icon and a slider (with a movable knob), and
 * - A Money section with an icon and its value rendered as text.
 *
 * The UI is arranged horizontally at the top center of the screen.
 */
class GameUI {
    // Scale factor.
    private val scale = 6f

    // Load textures from the assets folder.
    private val statsBackground = Texture("stats_background.png")
    private val co2Bar = Texture("co2_bar.png")

    // Load the default font.
    private val font = BitmapFont(Gdx.files.internal("default.fnt"))

    // UI state values.
    private var co2: Float = 1f      // 0 (good) to 1 (bad)
    private var money: Int = 0
    private var energy: Float = 1f   // 0 (too low) to 0.5 (perfect) to 1 (too high)

    /**
     * Updates the UI state.
     *
     * @param co2    The current CO₂ level (0 to 100). A higher value indicates better conditions.
     * @param money  The current money value.
     * @param energy The current energy level (0 to 100).
     */
    fun updateUI(co2: Float, money: Int, energy: Float) {
        if (co2 < 0f) {this.co2 = 0f}
        else if (co2 > 1f) {this.co2 = 1f}
        else {this.co2 = co2}
        this.money = money
        this.energy = energy
    }

    /**
     * Renders the UI overlay.
     *
     * Call this method after rendering your game world. It takes a SpriteBatch
     * and handles its own begin()/end() calls.
     */
    fun render(batch: SpriteBatch) {
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        val srcWidth = (co2Bar.width * co2).toInt()
        val drawWidth = srcWidth * scale

        val moneyText = "$money$"
        val moneyLayout = GlyphLayout(font, moneyText)

        batch.begin()

        batch.draw(statsBackground, screenWidth / 2 - statsBackground.width * scale / 2, screenHeight - statsBackground.height * scale, statsBackground.width * scale, statsBackground.height * scale)
        batch.draw(
            co2Bar,
            screenWidth / 2 - statsBackground.width * scale / 2 + 27 * scale, // x-position
            screenHeight - (statsBackground.height - 9) * scale,              // y-position
            drawWidth,                                                       // drawn width
            co2Bar.height * scale,                                             // drawn height
            0,                                                               // srcX: start at left edge of the texture
            0,                                                               // srcY: start at the top (or bottom if your origin is different)
            srcWidth,                                                        // srcWidth: the portion to draw
            co2Bar.height,                                                   // srcHeight: full height of the texture
            false,                                                           // flipX
            false                                                            // flipY
        )

        val textX = screenWidth / 2 - 22 * scale
        val textY = screenHeight - 12 * scale + moneyLayout.height / 2
        font.draw(batch, moneyText, textX, textY)

        batch.end()
    }

    /**
     * Disposes of the UI resources.
     */
    fun dispose() {
        statsBackground.dispose()
        font.dispose()
    }
}
