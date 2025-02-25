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
import kotlin.math.abs

class GameScreen(private val game: Main) : Screen, GestureDetector.GestureListener {
    // === Time & World Settings ===
    private var elapsedTime = 0f
    private val worldWidth = 3
    private val worldHeight = worldWidth

    // === Data Classes ===
    data class TileData(
        val textureId: String? = null,      // For static tiles.
        val animationId: String? = null,      // For animated tiles.
        val animationStateTime: Float = 0f    // Tracks the elapsed time for animations.
    )

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
    private var clickedTileX: Int = 0
    private var clickedTileY: Int = 0

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
    private var worldTouchX = 0f
    private var worldTouchY = 0f

    // === UI Overlay Integration ===
    private lateinit var gameUI: GameUI
    private val uiBatch = SpriteBatch()

    override fun show() {
        // Initialize the UI overlay.
        gameUI = GameUI()
        // Set up the GestureDetector to capture touch events.
        Gdx.input.inputProcessor = GestureDetector(this)
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Clear the screen.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Update the offset based on velocity if the screen is not touched.
        if (!Gdx.input.isTouched) {
            offsetX += velocityX * delta
            offsetY += velocityY * delta

            velocityX *= 0.9f
            velocityY *= 0.9f
            if (abs(velocityX) < 0.1f) velocityX = 0f
            if (abs(velocityY) < 0.1f) velocityY = 0f
        }


        // Draw the isometric tilemap.
        game.batch.begin()


        val centerX = Gdx.graphics.width / 2f
        val centerY = Gdx.graphics.height / 2f


        // Look up the animation (ensure the key exists in your animationMap)
        val animation = animationMap["windTurbineAnim"]
        if (animation != null) {drawAnimTile(animation, elapsedTime, 0, 0)}

        val texture = textureMap["grass_0"]
        val pos: Vector2 = screenToTile(centerX,centerY)
        if (texture != null) {drawTile(texture, pos.x.toInt(), pos.y.toInt())}

        // --- Day / Night Cycle ---
        // Full cycle duration (e.g., 4 minutes)
        val cycleDuration = 60 * 4f
        // Normalize elapsed time to [0,1]
        val t = (elapsedTime % cycleDuration) / cycleDuration

        val alpha = when {
            t < 0.5f -> 0f                      //   t in [0, 0.5)   -> day (alpha = 0)
            t < 0.6f -> ((t - 0.5f) / 0.1f)     //   t in [0.5, 0.6) -> transition from day to night (alpha increases from 0 to 1)
            t < 0.85f -> 1f                     //   t in [0.6, 0.85) -> night (alpha = 1)
            else -> 1f - ((t - 0.85f) / 0.15f)  //   t in [0.85, 1]   -> transition from night to day (alpha decreases from 1 to 0)
        }

        // Define day and night colors.
        val dayColor = Color(1f, 1f, 1f, 1f)       // Full brightness for day.
        val nightColor = Color(0.4f, 0.4f, 0.7f, 1f) // Darker blue tint for night.

        // Interpolate the sky color using alpha.
        val skyColor = Color(
            (1 - alpha) * dayColor.r + alpha * nightColor.r,
            (1 - alpha) * dayColor.g + alpha * nightColor.g,
            (1 - alpha) * dayColor.b + alpha * nightColor.b,
            1f
        )
        game.batch.color = skyColor

        game.batch.end()

        // Update the UI with current game state values (dummy values here; update as needed).
        gameUI.updateUI(co2 = 60f, money = 1200, energy = 40f)
        // Render the UI overlay.
        gameUI.render(uiBatch)
    }

    // Draw a tile given a Texture and tile (grid) coordinates.
    private fun drawTile(texture: Texture, tileX: Int, tileY: Int) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(texture, pos.x, pos.y, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Overload for drawing using a TextureRegion.
    private fun drawTile(textureRegion: TextureRegion, tileX: Int, tileY: Int) {
        val pos = tileToScreen(tileX, tileY)
        game.batch.draw(textureRegion, pos.x, pos.y, tileWidth * tileScale, tileHeight * tileScale)
    }

    // Draw a tile given an Animation and current stateTime and tile (grid) coordinates.
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
        if (tileX > -1f) {tileX += 1f}
        if (tileY > -1f) {tileY += 1f}
        return Vector2(tileX, tileY)
    }


    override fun resize(width: Int, height: Int) { }
    override fun pause() { }
    override fun resume() { }
    override fun hide() { }
    override fun dispose() {
    }

    // GestureDetector callbacks
    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        // Record the initial touch and offset.
        initialTouchX = x
        initialTouchY = y
        initialOffsetX = offsetX
        initialOffsetY = offsetY
        lastTouchX = x
        lastTouchY = y
        lastTouchTime = System.currentTimeMillis()
        velocityX = 0f
        velocityY = 0f

        worldTouchX = initialTouchX - offsetX
        worldTouchY = initialTouchY - offsetY

        val cellSize = Vector2(64f * tileScale, 32f * tileScale)
        val tileY = (worldTouchX / cellSize.x + worldTouchY / cellSize.y) * -1 + 0.5f
        val tileX = (worldTouchY / cellSize.y - worldTouchX / cellSize.x) * -1 + 0.5f

        // Store clicked tile coordinates.
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
