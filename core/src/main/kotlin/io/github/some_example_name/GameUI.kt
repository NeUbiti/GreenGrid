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

    // Load the default font.
    private val font = BitmapFont(Gdx.files.internal("default.fnt"))

    // UI state values.
    private var co2: Float = 50f      // 0 (good) to 100 (bad)
    private var money: Int = 0
    private var energy: Float = 50f   // 0 (low) to 100 (high)

    /**
     * Updates the UI state.
     *
     * @param co2    The current CO₂ level (0 to 100). A higher value indicates better conditions.
     * @param money  The current money value.
     * @param energy The current energy level (0 to 100).
     */
    fun updateUI(co2: Float, money: Int, energy: Float) {
        this.co2 = co2
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
        val moneyText = "$money$"
        val moneyLayout = GlyphLayout(font, moneyText)

        batch.begin()

        batch.draw(statsBackground, screenWidth / 2 - statsBackground.width * scale / 2, screenHeight - statsBackground.height * scale, statsBackground.width * scale, statsBackground.height * scale)

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
