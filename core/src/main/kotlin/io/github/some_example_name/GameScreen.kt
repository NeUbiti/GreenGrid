package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Json
import kotlin.math.abs

class GameScreen(private val game: Main) : Screen, GestureDetector.GestureListener {
    // === Time & World Settings ===
    private var elapsedTime = 0f

    // New tilemap dimensions
    private val mapWidth = 32
    private val mapHeight = 32

    // === Data Classes ===
    // Made the fields mutable so we can update animationStateTime.
    data class TileData(
        var textureId: String? = null,      // For static tiles.
        var animationId: String? = null,      // For animated tiles.
        var animationStateTime: Float = 0f    // Tracks the elapsed time for animations.
    )

    // The tilemap is a 2D array of TileData.
    private lateinit var tileMap: Array<Array<TileData>>

    // === Texture & Animation Setup ===
    // Lookup map for static textures.
    private val textureMap: Map<String, Texture> = mapOf(
        "grass_0" to Texture("grass_0.png"),
        "grass_1" to Texture("grass_1.png"),
        "grass_2" to Texture("grass_2.png"),
        "grass_3" to Texture("grass_3.png"),
        "grass_4" to Texture("grass_4.png"),
        "grass_5" to Texture("grass_5.png"),
        "coal_plant_small" to Texture("coal_plant_0.png")
    )
    // Define tile dimensions based on one of the grass textures.
    private val tileWidth = textureMap["grass_0"]!!.width.toFloat()
    private val tileHeight = textureMap["grass_0"]!!.height.toFloat()

    // Animation lookup for animated tiles.
    private val animationMap: Map<String, Animation<TextureRegion>> = mapOf(
        "windTurbineAnim" to Animation(
            0.3f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                add(TextureRegion(Texture("wind_turbine_small_0.png")))
                add(TextureRegion(Texture("wind_turbine_small_1.png")))
                add(TextureRegion(Texture("wind_turbine_small_2.png")))
                add(TextureRegion(Texture("wind_turbine_small_3.png")))
            }
        )
    )

    private val tileScale = 6f

    // Offsets for panning the map.
    private var offsetX = Gdx.graphics.width.toFloat() / 2 - 64 * tileScale / 2
    private var offsetY = Gdx.graphics.height.toFloat() / 2 - 64 * tileScale / 2

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

    override fun show() {
        // Attempt to load the tilemap from a JSON file.
        val file = Gdx.files.local("tilemap.json")
        if (file.exists()) {
            val json = Json()
            tileMap = json.fromJson(Array<Array<TileData>>::class.java, file.readString())
        } else {
            // Generate a tilemap using Perlin noise.
            //tileMap = generateTileMapWithPerlinNoise(64, 62, scale = 0.01f)
            tileMap = Array(mapWidth) { Array(mapHeight) { TileData(textureId = "grass_0") } }
        }

        // Initialize the UI overlay.
        gameUI = GameUI()
        testUI = Test()
        // Set up the GestureDetector to capture touch events.
        Gdx.input.inputProcessor = GestureDetector(this)
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Clear the screen.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Update the offset based on velocity when not touching.
        if (!Gdx.input.isTouched) {
            offsetX += velocityX * delta
            offsetY += velocityY * delta

            velocityX *= 0.9f
            velocityY *= 0.9f
            if (abs(velocityX) < 0.1f) velocityX = 0f
            if (abs(velocityY) < 0.1f) velocityY = 0f
        }

        game.batch.begin()

        // Iterate over the entire tileMap and draw each tile.
        for (x in (mapWidth - 1) downTo 0) {
            for (y in (mapHeight - 1) downTo 0) {
                val tile = tileMap[x][y]
                if (tile.animationId != null) {
                    // Update animation state time.
                    tile.animationStateTime += delta
                    val animation = animationMap[tile.animationId]
                    if (animation != null) {
                        drawAnimTile(animation, tile.animationStateTime, x, y)
                    }
                } else if (tile.textureId != null) {
                    val texture = textureMap[tile.textureId]
                    if (texture != null) {
                        drawTile(texture, x, y)
                    }
                }
            }
        }

        // --- Day / Night Cycle Overlay (example) ---
        val cycleDuration = 60 * 4f // 4-minute full cycle.
        val t = (elapsedTime % cycleDuration) / cycleDuration
        val alpha = when {
            t < 0.5f -> 0f
            t < 0.6f -> ((t - 0.5f) / 0.1f)
            t < 0.85f -> 1f
            else -> 1f - ((t - 0.85f) / 0.15f)
        }
        val dayColor = Color(1f, 1f, 1f, 1f)
        val nightColor = Color(0.4f, 0.4f, 0.7f, 1f)
        val skyColor = Color(
            (1 - alpha) * dayColor.r + alpha * nightColor.r,
            (1 - alpha) * dayColor.g + alpha * nightColor.g,
            (1 - alpha) * dayColor.b + alpha * nightColor.b,
            1f
        )
        game.batch.color = skyColor

        game.batch.end()

        // Update and render the UI overlay.
        testUI.render(testBatch)

        gameUI.updateUI(co2 = 60f, money = 1200, energy = 40f)
        gameUI.render(uiBatch)
    }

//    private fun generateTileMapWithPerlinNoise(width: Int, height: Int, scale: Float = 0.1f): Array<Array<GameScreen.TileData>> {
//        val tileMap = Array(width) { Array(height) { GameScreen.TileData() } }
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                val noiseValue = PerlinNoise.noise(x * scale, y * scale)
//                // Choose tile type based on noise thresholds
//                tileMap[x][y] = when {
//                    noiseValue < 0.3f -> GameScreen.TileData(textureId = "grass_0") // assuming you have a water texture
//                    noiseValue < 0.6f -> GameScreen.TileData(textureId = "grass_1")  // and a sand texture
//                    else -> GameScreen.TileData(textureId = "grass_2")
//                }
//            }
//        }
//        return tileMap
//    }

    // Draw a tile given a Texture and tile (grid) coordinates.
    private fun drawTile(texture: Texture, tileX: Int, tileY: Int) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(texture, pos.x, pos.y, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Overloaded version for drawing using a TextureRegion.
    private fun drawTile(textureRegion: TextureRegion, tileX: Int, tileY: Int) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(textureRegion, pos.x, pos.y, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Draw a tile given an Animation, current stateTime, and tile (grid) coordinates.
    private fun drawAnimTile(animation: Animation<TextureRegion>, stateTime: Float, tileX: Int, tileY: Int) {
        val frame = animation.getKeyFrame(stateTime, true)
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(frame, pos.x, pos.y, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Convert from tile (grid) coordinates to screen coordinates.
    private fun tileToScreen(tileX: Int, tileY: Int): Vector2 {
        val screenX = ((tileX - tileY) * (tileWidth / 2 * tileScale)) + offsetX
        val screenY = ((tileX + tileY) * (tileHeight / 4 * tileScale)) + offsetY
        return Vector2(screenX, screenY)
    }

    // Convert from screen coordinates back to tile (grid) coordinates.
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

    override fun resize(width: Int, height: Int) { }
    override fun pause() { }
    override fun resume() { }
    override fun hide() { }

    // When closing the app, save the tileMap to a JSON file.
    override fun dispose() {
        val json = Json()
        val file = Gdx.files.local("tilemap.json")
        file.writeString(json.toJson(tileMap), false)
    }

    // GestureDetector callbacks
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        initialTouchX = x
        initialTouchY = y
        initialOffsetX = offsetX
        initialOffsetY = offsetY
        lastTouchX = x
        lastTouchY = y
        lastTouchTime = System.currentTimeMillis()

        val cellSize = Vector2(64f * tileScale, 32f * tileScale)
        // Calculate which tile was touched (this logic may be adjusted based on your isometric projection).
        val tileY = (x - offsetX) / cellSize.x + (y - offsetY) / cellSize.y
        val tileX = (y - offsetY) / cellSize.y - (x - offsetX) / cellSize.x
        clickedTileX = tileX.toInt()
        clickedTileY = tileY.toInt()

        return true
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        offsetX = initialOffsetX + (x - initialTouchX)
        offsetY = initialOffsetY - (y - initialTouchY)

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastTouchTime) / 1000f
        if (dt > 0) {
            velocityX = (x - lastTouchX) / dt
            velocityY = -(y - lastTouchY) / dt
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
