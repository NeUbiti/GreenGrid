package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

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
    private val atlas = TextureAtlas(Gdx.files.internal("textures.atlas"))
    private val statsBackground = Texture("stats_background.png")
    private val co2Bar = Texture("co2_bar.png")
    private val energySlider = Texture("energy_slider.png")
    private val batteryLevel = Texture("battery_level.png")
    private val craneButton = Texture("crane.png")
    private val cranePushed = Texture("crane_pushed.png")

    init {
        // Set filtering on each atlas region.
        for (i in 0 until atlas.regions.size) {
            val region = atlas.regions[i]
            region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }

    // Load the default font.
    private val font = BitmapFont(Gdx.files.internal("default.fnt"))

    // UI state values.
    private var co2: Float = 1f      // 0 (good) to 1 (bad)
    private var money: Int = 0
    private var energy: Float = 1f   // 0 (too low) to 0.5 (perfect) to 1 (too high)
    private var hasBatteries: Boolean = false
    private var battery: Float = 1f   // 0 (empty) to 1 (full)

    // Callback that GameScreen can set so that when a building is selected, it is placed.
    var onPlaceBuilding: ((buildingTile: String) -> Unit)? = null

    // For example, in your building menu, each tile holds name, description and cost.
    data class BuildingOptions(
        var texture: TextureAtlas.AtlasRegion? = null,
        var name: String? = null,
        var description: String? = null,
        var cost: Float = 0f
    )

    // Create a list of building options.
    private val buildingOptionsList = listOf(
        BuildingOptions(texture = atlas.findRegion("wind_turbine_small", 0), name = "windTurbine", description = "Wind turbine", cost = 200f),
        BuildingOptions(texture = atlas.findRegion("solar_panel_small", 0), name = "SolarPanel", description = "Solar panel", cost = 150f),
        BuildingOptions(texture = atlas.findRegion("coal_plant", 0), name = "coalPlant", description = "Coal plant", cost = 100f)
    )
    private var buildingMenuOpen = false

    // Crane button pressed timer variables.
    private var cranePressedTimer = 0f
    private val cranePressDuration = 0.2f // Duration in seconds to show the pushed texture

    /**
     * Updates the UI state.
     *
     * @param co2    The current CO₂ level (0 to 1). A higher value indicates worse conditions.
     * @param money  The current money value.
     * @param energy The current energy level (0 to 1).
     * @param hasBatteries  If the player has batteries (default false).
     * @param battery The current battery level (0 to 1).
     */
    fun updateUI(co2: Float, money: Int, energy: Float, hasBatteries: Boolean, battery: Float) {
        this.co2 = when {
            co2 < 0f -> 0f
            co2 > 1f -> 1f
            else -> co2
        }
        this.money = money
        this.energy = when {
            energy < 0f -> 0f
            energy > 1f -> 1f
            else -> energy  // Fixed: assign energy value correctly.
        }
        this.hasBatteries = hasBatteries
        this.battery = when {
            battery < 0f -> 0f
            battery > 1f -> 1f
            else -> battery
        }
    }

    fun isMenuOpen(): Boolean {
        return buildingMenuOpen
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
        val delta = Gdx.graphics.deltaTime

        // Update crane pressed timer.
        if (cranePressedTimer > 0) {
            cranePressedTimer -= delta
            if (cranePressedTimer < 0) {
                cranePressedTimer = 0f
            }
        }

        val srcWidth = (co2Bar.width * co2).toInt()
        val drawWidth = srcWidth * scale

        val energySliderIcon: TextureRegion = when {
            energy < 0.25f -> TextureRegion(energySlider, 0, 0, 11, 11)
            energy < 0.75f -> TextureRegion(energySlider, 11, 0, 11, 11)
            else          -> TextureRegion(energySlider, 22, 0, 11, 11)
        }

        val batteryIcon: TextureRegion = when {
            battery < 0.25f -> TextureRegion(batteryLevel, 0, 0, 10, 6)
            battery < 0.5f -> TextureRegion(batteryLevel, 10, 0, 10, 6)
            battery < 0.75f -> TextureRegion(batteryLevel, 20, 0, 10, 6)
            else          -> TextureRegion(batteryLevel, 30, 0, 10, 6)
        }

        val moneyText = "$money$"
        val moneyLayout = GlyphLayout(font, moneyText)

        batch.begin()

        // Draw stats background and CO₂ bar.
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

        // Draw energy slider and battery icon.
        batch.draw(energySliderIcon, screenWidth / 2 + (85 + energy * 81) * scale, screenHeight - 17 * scale, 11 * scale, 11 * scale)
        batch.draw(batteryIcon, screenWidth / 2 + (86 + energy * 81) * scale, screenHeight - 24 * scale, 10 * scale, 6 * scale)

        // Draw money text.
        val textX = screenWidth / 2 - 22 * scale
        val textY = screenHeight - 12 * scale + moneyLayout.height / 2
        font.draw(batch, moneyText, textX, textY)

        // Draw the crane button in the bottom right corner.
        val buttonWidth = craneButton.width * scale
        val buttonHeight = craneButton.height * scale
        val buttonX = screenWidth - buttonWidth - 20f  // 20 pixel margin from right edge
        val buttonY = 20f  // 20 pixel margin from bottom edge

        // Choose the texture based on whether the button is pressed.
        val currentCraneTexture = if (cranePressedTimer > 0) cranePushed else craneButton
        batch.draw(currentCraneTexture, buttonX, buttonY, buttonWidth, buttonHeight)

        // Check for button click.
        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val touchY = screenHeight - Gdx.input.y.toFloat() // convert input Y coordinate
            if (touchX in buttonX..(buttonX + buttonWidth) &&
                touchY in buttonY..(buttonY + buttonHeight)
            ) {
                // Set the pressed timer so that the pushed texture is shown.
                cranePressedTimer = cranePressDuration

                // Toggle building menu.
                buildingMenuOpen = !buildingMenuOpen
            }
        }

        // === Draw the building menu if it's open ===
        if (buildingMenuOpen) {
            // Define the size of each building option tile.
            val iconSize = 64f * scale
            // We'll position the building menu at the bottom left.
            val menuStartX = 20f  // 20 pixel margin from left edge
            val menuY = 20f       // align with bottom margin

            // For each building option, draw its tile and then overlay its description at the bottom of the tile.
            val overlayHeight = 20f * scale  // Height of the description overlay
            for ((index, option) in buildingOptionsList.withIndex()) {
                val optionX = menuStartX + index * (iconSize + 10f)
                // Draw the building tile texture.
                option.texture?.let { region ->
                    batch.draw(region, optionX, menuY, iconSize, iconSize)
                }
                // Draw the description text over the overlay, centered horizontally.
                val description = option.description ?: ""
                val layout = GlyphLayout(font, description)
                val textXOption = optionX + (iconSize - layout.width) / 2
                // Draw the text a few pixels above the bottom edge of the overlay.
                val textYOption = menuY + overlayHeight - (overlayHeight - layout.height) / 2
                font.draw(batch, description, textXOption, textYOption)
            }

            // Check for touches on the building menu area.
            if (Gdx.input.justTouched()) {
                val touchX = Gdx.input.x.toFloat()
                val touchY = screenHeight - Gdx.input.y.toFloat() // convert Y coordinate
                for ((index, option) in buildingOptionsList.withIndex()) {
                    val optionX = menuStartX + index * (iconSize + 10f)
                    if (touchX >= optionX && touchX <= optionX + iconSize &&
                        touchY >= menuY && touchY <= menuY + iconSize
                    ) {
                        onPlaceBuilding?.invoke(option.name ?: "")
                        Gdx.app.log("buildingMenu", option.name ?: "unknown")
                        buildingMenuOpen = false
                        break
                    }
                }
            }
        }

        batch.end()
    }

    /**
     * Disposes of the UI resources.
     */
    fun dispose() {
        statsBackground.dispose()
        co2Bar.dispose()
        energySlider.dispose()
        batteryLevel.dispose()
        craneButton.dispose()
        cranePushed.dispose()
        font.dispose()
    }
}
