package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.sin

class GameScreen(private val game: Main) : Screen, GestureDetector.GestureListener {
    private var elapsedTime = 0f

    data class TileData(
        val textureId: String? = null,      // For static tiles.
        val animationId: String? = null,    // For animated tiles.
        val animationStateTime: Float = 0f  // Tracks the elapsed time for the animation.
    )

    private val worldWidth = 1
    private val worldHeight = 1

    // Animation lookup for animated tiles.
    private val animationMap: Map<String, Animation<TextureRegion>> = mapOf(
        "windTurbineAnim" to Animation(
            0.1f,
            com.badlogic.gdx.utils.Array<TextureRegion>().apply {
                add(TextureRegion(Texture("wind_turbine_small_0.png")))
                add(TextureRegion(Texture("wind_turbine_small_1.png")))
                add(TextureRegion(Texture("wind_turbine_small_2.png")))
                add(TextureRegion(Texture("wind_turbine_small_3.png")))
            }
        )
    )

    private val font = BitmapFont().apply { data.setScale(3f) }
    private var clickedTileX: Int = 0
    private var clickedTileY: Int = 0

    private val tileScale = 6f

    // 2D array of tile data that you can later serialize as JSON.
    val tileMap: Array<Array<TileData>> = Array(worldWidth) { x: Int ->
        Array(worldHeight) { y: Int ->
            // For now, every tile is a "grass" tile.
            TileData("grass_0")
        }
    }

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

    // Offsets for panning the map.
    private var offsetX = 0f
    private var offsetY = 0f

    // Variables to store the initial touch position and the offset at that moment.
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialOffsetX = 0f
    private var initialOffsetY = 0f

    // Variables for calculating velocity.
    private var velocityX = 0f
    private var velocityY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTouchTime = 0L
    private var worldTouchX = 0f
    private var worldTouchY = 0f

    // Preload the demo texture (using PNG instead of GIF for better compatibility)
    private val demoTile: Texture = Texture("wind_turbine_small_0.png")

    override fun show() {
        // Set up the GestureDetector to capture touch events.
        Gdx.input.inputProcessor = GestureDetector(this)
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Cycle between day and night over 3 minutes.
        val cycleDuration = 60f * 3f
        val alpha = 0.5f + 0.5f * sin((2 * Math.PI * (elapsedTime / cycleDuration))).toFloat()

        // Define two colors (Day and Night) and interpolate.
        val dayColor = Color(1f, 1f, 1f, 1f)
        val nightColor = Color(0.4f, 0.4f, 0.7f, 1f)
        val skyColor = Color(
            (1 - alpha) * dayColor.r + alpha * nightColor.r,
            (1 - alpha) * dayColor.g + alpha * nightColor.g,
            (1 - alpha) * dayColor.b + alpha * nightColor.b,
            1f
        )

        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
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

        // Update animation state times for animated tiles.
        val deltaTime = Gdx.graphics.deltaTime
        for (x in tileMap.indices) {
            for (y in tileMap[x].indices) {
                val tileData = tileMap[x][y]
                if (tileData.animationId != null) {
                    tileMap[x][y] = tileData.copy(animationStateTime = tileData.animationStateTime + deltaTime)
                }
            }
        }

        if (clickedTileX in 0 until worldWidth && clickedTileY in 0 until worldHeight) {
            tileMap[clickedTileX][clickedTileY] = TileData(textureId = "coal_plant_small")
        } else {
            // Handle clicks outside the tile map bounds, e.g., ignore or clamp the indices.
        }


        // Draw the isometric tilemap.
        game.batch.begin()
        for (x in tileMap.indices.reversed()) {
            for (y in tileMap[x].indices.reversed()) {
                // Compute on-screen positions using tile dimensions.
                val posX = ((x - y) * (tileWidth / 2 * tileScale) + offsetX)
                val posY = ((x + y) * (tileHeight / 4 * tileScale) + offsetY)
                println("Tilemap Position X Y: $x $y")
                val tileData = tileMap[x][y]

                // Draw animated tile if applicable.
                if (tileData.animationId != null) {
                    val animation = animationMap[tileData.animationId]
                    animation?.let {
                        val frame = it.getKeyFrame(tileData.animationStateTime, true)
                        game.batch.draw(frame, posX, posY, tileWidth * tileScale, tileHeight * tileScale)
                    }
                } else {
                    // Otherwise, draw the static texture.
                    val texture = textureMap[tileData.textureId]
                    texture?.let { game.batch.draw(it, posX, posY, tileWidth * tileScale, tileHeight * tileScale) }
                }
            }
        }

        game.batch.color = skyColor
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) { }
    override fun pause() { }
    override fun resume() { }
    override fun hide() { }
    override fun dispose() {
        font.dispose()
        demoTile.dispose()
        // Dispose any other textures if necessary.
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
