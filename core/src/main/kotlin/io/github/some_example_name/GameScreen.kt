package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Json
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.PI
import kotlinx.coroutines.*

class GameScreen(private val game: Main) : Screen, GestureDetector.GestureListener {
    // === Time & World Settings ===
    private var elapsedTime = 0f

    // Tilemap dimensions.
    private val mapWidth = 64
    private val mapHeight = 64

    // How much to move a tile vertically per unit altitude.
    private val altitudeScale = 16f

    // Seed for noise generation.
    private var noiseSeed = abs(java.util.Random().nextInt()) % 10000

    // === Data Classes ===
    // Now each tile holds altitude, temperature and humidity.
    data class TileData(
        var textureId: String? = null,      // For static tiles.
        var animationId: String? = null,      // For animated tiles.
        var animationStateTime: Float = 0f,   // Tracks the elapsed time for animations.
        var altitude: Float = 0f,             // Altitude value for the tile.
        var temperature: Float = 20f,         // Temperature value for the tile.
        var humidity: Float = 50f             // Humidity value for the tile.
    )

    // Updated GameState with default values so that a no-arg constructor is available.
    data class GameState(
        var tileMap: Array<Array<TileData>> = emptyArray(),
        var noiseSeed: Int = 0,
        var money: Float = 500f,
        var energy: Float = 0f,
        var co2: Float = 0f
    )

    // The tilemap is a 2D array of TileData.
    private lateinit var tileMap: Array<Array<TileData>>

    // === Texture & Animation Setup ===
    // Lookup map for static textures.
    // Load your texture atlas (assume you have created one using TexturePacker)
    private val atlas = TextureAtlas(Gdx.files.internal("textures.atlas"))

    // Create a mapping from texture id to TextureRegion
    private val textureRegionMap: Map<String, TextureRegion> = mapOf(
        "grass_0" to atlas.findRegion("grass", 0),
        "grass_1" to atlas.findRegion("grass", 1),
        "grass_2" to atlas.findRegion("grass", 2),
        "grass_3" to atlas.findRegion("grass", 3),
        "grass_4" to atlas.findRegion("grass", 4),
        "grass_5" to atlas.findRegion("grass", 5)
    )

    // Define tile dimensions based on one of the grass textures.
    private val tileWidth = textureRegionMap["grass_0"]!!.regionWidth.toFloat()
    private val tileHeight = textureRegionMap["grass_0"]!!.regionHeight.toFloat()


    // Animation lookup for animated tiles.
    private val animationMap: Map<String, Animation<TextureRegion>> = mapOf(
        "windTurbine" to Animation(
            0.3f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                add(atlas.findRegion("wind_turbine_small", 0))
                add(atlas.findRegion("wind_turbine_small", 1))
                add(atlas.findRegion("wind_turbine_small", 2))
                add(atlas.findRegion("wind_turbine_small", 3))
            }
        ),
        "coalPlant" to Animation(
            0.3f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                add(atlas.findRegion("coal_plant", 0))
            }
        ),
        "house" to Animation(
            0.3f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                add(atlas.findRegion("house", -1))
            }
        )
    )

    // Animation lookup for animated tiles.
    private val animationLightMap: Map<String, Animation<TextureRegion>> = mapOf(
        "coalPlant" to Animation(
            0.15f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                // Load all frames of the flames animation from your atlas.
                for (i in 1..17) {
                    add(atlas.findRegion("coal_plant_flames", i))
                }
            }
        )
    )

    private val tileScale = 6f

    // Offsets for panning the map.
    private var offsetX = Gdx.graphics.width.toFloat() / 2 - 64 * tileScale / 2
    private var offsetY = Gdx.graphics.height.toFloat() / 2 - 64 * tileScale / 2 * 32

    // Variables to track initial touch positions and offsets.
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialOffsetX = 0f
    private var initialOffsetY = 0f

    // Variables for panning velocity calculations.
    private var velocityX = 0f
    private var velocityY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L

    // For tracking which tile was clicked.
    private var clickedTileX: Int = 0
    private var clickedTileY: Int = 0

    // === UI Overlay Integration ===
    private lateinit var gameUI: GameUI
    private lateinit var testUI: Test
    private val uiBatch = SpriteBatch()
    private val testBatch = SpriteBatch()
    private val lightBatch = SpriteBatch()
    private val font = BitmapFont(Gdx.files.internal("default.fnt"))

    // Declare variables to hold your background tracks.
    private lateinit var musicPlaylist: List<Music>
    private var currentTrackIndex = 0

    private var pendingBuilding: String? = null
    private var pendingTileX: Int = 0
    private var pendingTileY: Int = 0
    private var pendingCost: Int = 0
    private var pendingValid: Boolean = false
    private var pendingEfficiency: Float = 0f
    private lateinit var overlayColor: Color
    private lateinit var whitePixel: Texture
    private lateinit var confirmTexture: Texture
    private lateinit var cancelTexture: Texture

    private var co2 = 0f // in %
    private var co2level = 0f // 0 to 1
    private var money = 500f // in $
    private var energy = 0f // in kWh
    private var energylevel = 0f // 0 to 1
    private var lastEnergyProduced = 0f // in kWh
    private var lastEnergyUsed = 0f // in kWh
    private var hasBatteries = false
    private var battery = 0f // in kWh
    private var batterylevel = 0f // 0 to 1

    // Rates for building effects (per second).
    private val windTurbineCost = 300
    private val windTurbineEnergyRate = 1.0f
    private val windTurbineCO2Rate = 0.1f

    private val coalPlantCost = 100
    private val coalPlantEnergyRate = 2.0f
    private val coalPlantCO2Rate = 2.0f

    private val solarPanelCost = 200
    private val solarPanelEnergyRate = 0.5f
    private val solarPanelCO2Rate = 0.1f

    private val houseEnergyRate = 1.0f
    private val energyPrice = 1.0f

    // Timer variables for periodic saving (avoid saving every frame).
    private val SAVE_INTERVAL = 5f // seconds
    private var saveTimer = 0f

    override fun show() {
        for (region in atlas.regions) {
            region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }

        // Load saved game state if available.
        if (Gdx.files.local("game_state.json").exists()) {
            loadGameState()
        } else {
            // Attempt to load the tilemap from a JSON file.
            val tilemapFile = Gdx.files.local("tilemap.json")
            if (tilemapFile.exists()) {
                val json = Json()
                tileMap = json.fromJson(Array<Array<TileData>>::class.java, tilemapFile.readString())
            } else {
                // Generate the island using perlin noise.
                tileMap = generateIslandTileMap(mapWidth, mapHeight)
            }
        }



        // Initialize the UI overlay.
        gameUI = GameUI()
        gameUI.onPlaceBuilding = { buildingTile ->
            startPlacingBuilding(buildingTile)
        }

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        whitePixel = Texture(pixmap)
        pixmap.dispose()
        confirmTexture = Texture(Gdx.files.internal("confirm.png"))
        cancelTexture = Texture(Gdx.files.internal("cancel.png"))

        testUI = Test()
        // Set up the GestureDetector to capture touch events.
        Gdx.input.inputProcessor = GestureDetector(this)

        loadBackgroundMusic()
        startPlaylist()

        // Save state at the end of show() to persist the initial setup.
        saveGameStateAsync()
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Update periodic saving timer.
        saveTimer += delta
        if (saveTimer >= SAVE_INTERVAL) {
            saveGameStateAsync()
            saveTimer = 0f
        }

        // Update camera offset via panning/inertia...
        if (!Gdx.input.isTouched) {
            offsetX += velocityX * delta
            offsetY += velocityY * delta
            velocityX *= 0.9f
            velocityY *= 0.9f
            if (abs(velocityX) < 0.1f) velocityX = 0f
            if (abs(velocityY) < 0.1f) velocityY = 0f
        }

        // Clamp the camera to the tilemap boundaries.
        clampCamera()

        // Compute the day/night cycle parameters outside the batch blocks.
        val cycleDuration = 60f * 4f // default: 60f * 4f (4 minutes)
        val t = (elapsedTime % cycleDuration) / cycleDuration
        val dayNightAlpha = when {
            t < 0.5f -> 0f
            t < 0.6f -> ((t - 0.5f) / 0.1f)
            t < 0.85f -> 1f
            else -> 1f - ((t - 0.85f) / 0.15f)
        }
        val dayColor = Color(1f, 1f, 1f, 1f)
        val nightColor = Color(0.4f, 0.4f, 0.7f, 1f)
        val skyColor = Color(
            (1 - dayNightAlpha) * dayColor.r + dayNightAlpha * nightColor.r,
            (1 - dayNightAlpha) * dayColor.g + dayNightAlpha * nightColor.g,
            (1 - dayNightAlpha) * dayColor.b + dayNightAlpha * nightColor.b,
            1f
        )

        if (pendingBuilding != null) {
            // Determine if the placement is valid (here valid if no building exists at the tile).

            val centerTile = screenToTile(Gdx.graphics.width.toFloat() / 2, Gdx.graphics.height.toFloat() / 2)
            pendingTileX = centerTile.x.toInt()
            pendingTileY = centerTile.y.toInt()
            val currentTile = tileMap[pendingTileX][pendingTileY]
            pendingCost = when (pendingBuilding) {
                "windTurbine" -> {windTurbineCost}
                "coalPlant" -> {coalPlantCost}
                "solarPanel" -> {solarPanelCost}
                else -> {0}
            }
            pendingValid = currentTile.animationId == null && money >= pendingCost
            overlayColor = if (pendingValid) Color(0f, 1f, 0f, 0.5f) else Color(1f, 0f, 0f, 0.5f)
        }

        game.batch.begin()

        // Iterate over the entire tileMap.
        for (x in (mapWidth - 1) downTo 0) {
            for (y in (mapHeight - 1) downTo 0) {
                val tile = tileMap[x][y]
                // Compute the per-tile tint based on temperature and humidity.
                val tint = computeTint(tile.temperature, tile.humidity)
                // Combine the tile tint with the sky color.
                val combinedTint = tint.cpy().mul(skyColor)
                // Save the current batch color.
                val originalColor = game.batch.color.cpy()
                // Set the batch color to the combined tint.
                game.batch.color = combinedTint

                if (tile.animationId != null) {
                    tile.animationStateTime += delta
                    val animation = animationMap[tile.animationId]
                    if (animation != null) {
                        drawAnimTile(animation, tile.animationStateTime, x, y, tile.altitude)
                    }
                } else if (tile.textureId != null) {
                    textureRegionMap[tile.textureId]?.let { region ->
                        drawTile(region, x, y, tile.altitude)
                    }
                }
                if (pendingBuilding != null && x == pendingTileX && y == pendingTileY) {
                    game.batch.color = overlayColor
                    animationMap[pendingBuilding]?.getKeyFrame(0f, true)?.let { region ->
                        drawTile(region, x, y, tile.altitude)
                    }
                }
                // Restore the original color.
                game.batch.color = originalColor

                when (tile.animationId) {
                    "windTurbine" -> {
                        energy += windTurbineEnergyRate * delta
                        lastEnergyProduced += windTurbineEnergyRate * delta
                        co2 += windTurbineCO2Rate * delta
                    }
                    "coalPlant" -> {
                        energy += coalPlantEnergyRate * delta
                        lastEnergyProduced += coalPlantEnergyRate * delta
                        co2 += coalPlantCO2Rate * delta
                    }
                    "solarPanel" -> {
                        energy += solarPanelEnergyRate * delta
                        lastEnergyProduced += solarPanelEnergyRate * delta
                        co2 += solarPanelCO2Rate * delta
                    }
                    "house" -> {
                        energy -= houseEnergyRate * delta * (1f - dayNightAlpha * 0.5f)
                        lastEnergyUsed += houseEnergyRate * delta * (1f - dayNightAlpha * 0.5f)
                        money += energyPrice * delta
                    }
                }
            }
        }

        game.batch.end()


        // Now adjust the flame rendering using the same day/night cycle.
        // Here, the flameAlpha is computed to be lower during the day (e.g., 0.3) and higher at night.
        val flameAlpha = 0.8f + 0.2f * dayNightAlpha

        lightBatch.begin()
        // Set the light batch's color with the computed flameAlpha.
        lightBatch.color = Color(1f, 1f, 1f, flameAlpha)
        for (x in 0 until mapWidth) {
            for (y in 0 until mapHeight) {
                val tile = tileMap[x][y]
                if (tile.animationId == "coalPlant") {
                    val flamesAnimation = animationLightMap["coalPlant"]
                    if (flamesAnimation != null) {
                        val pos = tileToScreen(x, y)
                        val frame = flamesAnimation.getKeyFrame(tile.animationStateTime, true)
                        lightBatch.draw(frame, pos.x, pos.y + tile.altitude * altitudeScale, frame.regionWidth * tileScale, frame.regionHeight * tileScale)
                    }
                }
            }
        }
        lightBatch.end()

        if (pendingBuilding != null) {
            uiBatch.begin()

            // Draw confirm and cancel buttons using texture icons.
            val screenWidth = Gdx.graphics.width.toFloat()
            val screenHeight = Gdx.graphics.height.toFloat()
            val buttonScale = tileScale
            val buttonWidth = confirmTexture.width * buttonScale
            val buttonHeight = confirmTexture.height * buttonScale
            val confirmX = screenWidth / 2 - buttonWidth - 10f
            val cancelX = screenWidth / 2 + 10f
            val buttonY = 50f
            uiBatch.draw(confirmTexture, confirmX, buttonY, buttonWidth, buttonHeight)
            uiBatch.draw(cancelTexture, cancelX, buttonY, buttonWidth, buttonHeight)

            // Draw efficiency text if placing a wind turbine.
            if (pendingBuilding == "windTurbine") {
                val tileAltitude = tileMap[pendingTileX][pendingTileY].altitude
                // Convert altitude to an efficiency percentage (e.g., 0 becomes 50%, 1 becomes 100%)
                pendingEfficiency = (tileAltitude * 50 + 50).coerceAtMost(100f).coerceAtLeast(0f)
                val efficiencyText = "Efficiency: ${pendingEfficiency.toInt()}%"
                // Use a GlyphLayout to measure the text height
                val layout = com.badlogic.gdx.graphics.g2d.GlyphLayout(font, efficiencyText)
                font.draw(uiBatch, efficiencyText, screenWidth / 2 - layout.width / 2, buttonY + buttonHeight + layout.height + 20f)
            }

            uiBatch.end()

            // Check for touch input on the confirm and cancel buttons.
            if (Gdx.input.justTouched()) {
                val touchX = Gdx.input.x.toFloat()
                val touchY = screenHeight - Gdx.input.y.toFloat()
                if (touchX in confirmX..(confirmX + buttonWidth) && touchY in buttonY..(buttonY + buttonHeight)) {
                    if (pendingValid) confirmPlacement() else cancelPlacement()
                } else if (touchX in cancelX..(cancelX + buttonWidth) && touchY in buttonY..(buttonY + buttonHeight)) {
                    cancelPlacement()
                }
            }
        }

        energylevel = lastEnergyProduced / lastEnergyUsed / 2f
        co2level = co2 * 10f

        gameUI.updateUI(co2level, money.toInt(), energylevel, hasBatteries, batterylevel, pendingBuilding)
        gameUI.render(uiBatch)
        lastEnergyProduced = 0f
        lastEnergyUsed = 0f
    }

    // Add the clampCamera function.
    private fun clampCamera() {
        // Get screen dimensions and the top-right screen corner.
        val screenWidth = Gdx.graphics.width.toFloat()
        val screenHeight = Gdx.graphics.height.toFloat()

        val topRightDistance = screenToTile(screenWidth, screenHeight).x - 64
        if (topRightDistance > 0) {
            val topRightOffset = tileToScreenNoOffset(topRightDistance, 0f)
            offsetX += topRightOffset.x
            offsetY += topRightOffset.y
        }

        val topLeftDistance = screenToTile(0f, screenHeight).y - 64
        if (topLeftDistance > 0) {
            val topLeftOffset = tileToScreenNoOffset(0f, topLeftDistance)
            offsetX += topLeftOffset.x
            offsetY += topLeftOffset.y
        }

        val bottomRightDistance = screenToTile(screenWidth, 0f).y
        if (bottomRightDistance < 0) {
            val bottomRightOffset = tileToScreenNoOffset(0f, bottomRightDistance + 1)
            offsetX += bottomRightOffset.x
            offsetY += bottomRightOffset.y
        }

        val bottomLeftDistance = screenToTile(0f, 0f).x
        if (bottomLeftDistance < 0) {
            val bottomLeftOffset = tileToScreenNoOffset(bottomLeftDistance + 1, 0f)
            offsetX += bottomLeftOffset.x
            offsetY += bottomLeftOffset.y
        }
    }


    // Generates a tilemap for an island using a perlin-like noise.
    private fun generateIslandTileMap(width: Int, height: Int): Array<Array<TileData>> {
        val map = Array(width) { Array(height) { TileData() } }
        for (x in 0 until width) {
            for (y in 0 until height) {
                var altValue = 0f
                // Combine multiple noise layers.
                for (scale in 1 until 4) {
                    val scaleFactor = 16f / (scale * scale)
                    altValue += perlin(x.toFloat() / scaleFactor, y.toFloat() / scaleFactor, noiseSeed * 10 + 0) / scale
                }
                altValue += 0.5f
                // Multiply by sine functions to form an island shape.
                altValue *= sin(PI * x / width).toFloat()
                altValue *= sin(PI * y / height).toFloat()

                // Store the computed altitude in the tile.
                map[x][y] = TileData(textureId = "grass_${(abs((altValue*100).toInt()) % 6)}", altitude = altValue)

                var tempValue = 0f
                // Combine multiple noise layers.
                for (scale in 1 until 4) {
                    val scaleFactor = 16f / (scale * scale)
                    tempValue += perlin(x.toFloat() / scaleFactor, y.toFloat() / scaleFactor, noiseSeed * 10 + 1) / scale
                }
                tempValue += 0.5f
                map[x][y].temperature = 20f - tempValue * 5f

                var humValue = 0f
                // Combine multiple noise layers.
                for (scale in 1 until 4) {
                    val scaleFactor = 16f / (scale * scale)
                    humValue += perlin(x.toFloat() / scaleFactor, y.toFloat() / scaleFactor, noiseSeed * 10 + 2) / scale
                }
                humValue += 0.5f
                map[x][y].humidity = 50f + humValue * 10f
            }
        }
        return map
    }

    // A simple pseudo Perlin noise function.
    private fun perlin(x: Float, y: Float, seed: Int): Float {
        val value = sin(x * 12.9898f + y * 78.233f + seed) * 43758.5453f
        return value - value.toInt()
    }

    // Computes a tint based on temperature and humidity.
    // Here we assume 20°C and 50% humidity as baselines.
    // Warmer temperatures slightly boost red while cooler ones boost blue.
    // Higher humidity adds a slight green tint.
    private fun computeTint(temperature: Float, humidity: Float): Color {
        val tempFactor = (temperature - 20f) / 20f  // deviation from 20°C
        val humFactor = (humidity - 50f) / 50f        // deviation from 50%

        // Adjustments are kept small (max ±0.1)
        val redAdjustment = tempFactor * 0.3f
        val blueAdjustment = -tempFactor * 0.3f
        val greenAdjustment = humFactor * 0.3f

        val r = (1f + redAdjustment).coerceIn(0f, 1f)
        val g = (1f + greenAdjustment).coerceIn(0f, 1f)
        val b = (1f + blueAdjustment).coerceIn(0f, 1f)

        return Color(r, g, b, 1f)
    }


    // Draws a tile using a Texture, applying a vertical offset from altitude.
    private fun drawTile(texture: Texture, tileX: Int, tileY: Int, altitude: Float) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(texture, pos.x, pos.y + altitude * altitudeScale, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Overloaded version for drawing using a TextureRegion.
    private fun drawTile(textureRegion: TextureRegion, tileX: Int, tileY: Int, altitude: Float) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(textureRegion, pos.x, pos.y + altitude * altitudeScale, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Draws an animated tile with the altitude offset.
    private fun drawAnimTile(animation: Animation<TextureRegion>, stateTime: Float, tileX: Int, tileY: Int, altitude: Float) {
        val frame = animation.getKeyFrame(stateTime, true)
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(frame, pos.x, pos.y + altitude * altitudeScale, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Converts tile (grid) coordinates to screen coordinates.
    private fun tileToScreen(tileX: Int, tileY: Int): Vector2 {
        val screenX = ((tileX - tileY) * (tileWidth / 2 * tileScale)) + offsetX
        val screenY = ((tileX + tileY) * (tileHeight / 4 * tileScale)) + offsetY
        return Vector2(screenX, screenY)
    }

    // Converts tile (grid) coordinates to screen coordinates without the offset.
    private fun tileToScreenNoOffset(tileX: Float, tileY: Float): Vector2 {
        val screenX = (tileX - tileY) * (tileWidth / 2 * tileScale)
        val screenY = (tileX + tileY) * (tileHeight / 4 * tileScale)
        return Vector2(screenX, screenY)
    }

    // Converts screen coordinates back to tile (grid) coordinates.
    private fun screenToTile(screenX: Float, screenY: Float): Vector2 {
        val a = tileWidth / 2 * tileScale
        val b = tileHeight / 4 * tileScale
        val adjustedX = screenX - offsetX - tileWidth / 2 * tileScale
        val adjustedY = screenY - offsetY - tileHeight / 2 * tileScale - 6 * tileScale
        var tileX = (adjustedX / a + adjustedY / b) / 2
        var tileY = (adjustedY / b - adjustedX / a) / 2
        if (tileX > -1f) { tileX += 1f }
        if (tileY > -1f) { tileY += 1f }
        return Vector2(tileX, tileY)
    }

    // New function to save game state.
    private fun saveGameState() {
        val gameState = GameState(tileMap, noiseSeed, money, energy, co2)
        val json = Json()
        val jsonString = json.prettyPrint(gameState)
        Gdx.files.local("game_state.json").writeString(jsonString, false)
        Gdx.app.log("SaveGame", "Game state saved.")
    }

    fun saveGameStateAsync() {
        GlobalScope.launch(Dispatchers.IO) {
            saveGameState() // your existing synchronous saveGameState function
        }
    }

    // New function to load the game state.
    private fun loadGameState() {
        val file = Gdx.files.local("game_state.json")
        if (file.exists()) {
            val json = Json()
            val jsonString = file.readString()
            try {
                val loadedState = json.fromJson(GameState::class.java, jsonString)
                tileMap = loadedState.tileMap
                noiseSeed = loadedState.noiseSeed
                money = loadedState.money
                energy = loadedState.energy
                co2 = loadedState.co2
                Gdx.app.log("LoadGame", "Game state loaded.")
            } catch (e: Exception) {
                Gdx.app.log("LoadGame", "Error loading game state: ${e.message}")
            }
        }
    }

    // Call this method (e.g., from your create() method) to load the tracks.
    fun loadBackgroundMusic() {
        val gardenMusic = Gdx.audio.newMusic(Gdx.files.internal("Garden.mp3"))
        val windmillMusic = Gdx.audio.newMusic(Gdx.files.internal("Windmill.mp3"))
        val coalMusic = Gdx.audio.newMusic(Gdx.files.internal("Coal.mp3"))

        // Set looping to false because we want each track to finish completely.
        gardenMusic.isLooping = false
        windmillMusic.isLooping = false
        coalMusic.isLooping = false

        // Create the playlist.
        musicPlaylist = listOf(gardenMusic, windmillMusic, coalMusic)

        // Set an on-completion listener for each track.
        musicPlaylist.forEach { music ->
            music.setOnCompletionListener { playNextTrack() }
        }
    }

    // Starts the playlist from the beginning.
    fun startPlaylist() {
        currentTrackIndex = 0
        musicPlaylist[currentTrackIndex].volume = 0.5f  // Adjust volume if needed.
        musicPlaylist[currentTrackIndex].play()
    }

    // This method is called when a track finishes playing.
    fun playNextTrack() {
        // Stop current track if it's still running.
        musicPlaylist[currentTrackIndex].stop()

        // Increment the index, looping back to 0 when at the end.
        currentTrackIndex = (currentTrackIndex + 1) % musicPlaylist.size

        // Play the next track.
        musicPlaylist[currentTrackIndex].volume = 0.5f  // Adjust volume if needed.
        musicPlaylist[currentTrackIndex].play()
    }

    private fun startPlacingBuilding(buildingTile: String) {
        pendingBuilding = buildingTile
        // For example, use the center tile as the preview location.
        val centerTile = screenToTile(Gdx.graphics.width.toFloat() / 2, Gdx.graphics.height.toFloat() / 2)
        pendingTileX = centerTile.x.toInt()
        pendingTileY = centerTile.y.toInt()
    }

    private fun confirmPlacement() {
        money -= pendingCost
        pendingCost = 0
        if (pendingBuilding != null) {
            tileMap[pendingTileX][pendingTileY].animationId = pendingBuilding
            pendingBuilding = null
        }
    }

    private fun cancelPlacement() {
        pendingBuilding = null
    }

    override fun resize(width: Int, height: Int) { }
    override fun pause() { }
    override fun resume() { }
    override fun hide() { }

    // Save the tilemap to a JSON file when the app is closed.
    override fun dispose() {
        val json = Json()
        val file = Gdx.files.local("tilemap.json")
        file.writeString(json.toJson(tileMap), false)
        musicPlaylist.forEach { it.dispose() }
    }

    // GestureDetector callbacks
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        // Only process the first touch
        if (pointer != 0) return false

        initialTouchX = x
        initialTouchY = y
        initialOffsetX = offsetX
        initialOffsetY = offsetY
        lastTouchX = x
        lastTouchY = y
        lastTouchTime = System.currentTimeMillis()

        return true
    }


    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        if (gameUI.isMenuOpen()) return false

        // Incrementally update the offset.
        offsetX += deltaX
        offsetY -= deltaY

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastTouchTime) / 1000f
        if (dt > 0) {
            velocityX = deltaX / dt
            velocityY = -deltaY / dt
        }
        lastTouchX = x
        lastTouchY = y
        lastTouchTime = currentTime
        return true
    }


    override fun fling(velX: Float, velY: Float, button: Int): Boolean = false
    override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = false
    override fun longPress(x: Float, y: Float): Boolean = false
    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false
    override fun zoom(initialDistance: Float, distance: Float): Boolean = false
    override fun pinch(initialPointer1: Vector2?, initialPointer2: Vector2?, pointer1: Vector2?, pointer2: Vector2?): Boolean = false
    override fun pinchStop() {}
}
