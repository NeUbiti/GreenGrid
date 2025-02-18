package io.github.some_example_name

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.sin

class Main : Game() {
    lateinit var batch: SpriteBatch

    override fun create() {
        batch = SpriteBatch()
        // Start with the loading screen
        setScreen(LoadingScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}

class LoadingScreen(private val game: Main) : Screen {
    private val welcomeImage = Texture("greengrid.png")
    private var elapsedTime = 0f
    // Introduce an initial delay of 0.5 seconds (black screen)
    private val initialDelay = 0.5f
    // Duration for the fade-in effect after the initial delay
    private val fadeInDuration = 1f
    // Total display duration for the loading screen
    private val displayDuration = 3f

    override fun show() {
        // You can add input listeners here if needed
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Calculate the alpha value for fade-in after the initial delay.
        val alpha = when {
            elapsedTime < initialDelay -> 0f
            elapsedTime < initialDelay + fadeInDuration -> (elapsedTime - initialDelay) / fadeInDuration
            else -> 1f
        }

        // Clear the screen. Use a solid black screen during the initial delay.
        if (elapsedTime < initialDelay) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        } else {
            // For the fade-in, we can slightly adjust the clear color based on alpha.
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Draw the welcome image only after the initial delay has passed.
        if (elapsedTime >= initialDelay) {
            game.batch.begin()
            game.batch.color = Color(1f, 1f, 1f, alpha)
            val x = (Gdx.graphics.width - welcomeImage.width) / 2f
            val y = (Gdx.graphics.height - welcomeImage.height) / 2f
            game.batch.draw(welcomeImage, x, y)
            game.batch.end()
            // Reset the batch color to opaque white for future drawings.
            game.batch.color = Color.WHITE
        }

        // Transition to the game screen after the displayDuration or if the screen is touched.
        if (elapsedTime > displayDuration) {
            game.screen = GameScreen(game)
            dispose()  // Dispose of the loading screen resources.
        }
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        welcomeImage.dispose()
    }
}

class GameScreen(private val game: Main) : Screen, GestureDetector.GestureListener {
    private var elapsedTime = 0f
    private val tileScale = 6f
    private val grassTiles = arrayOf(
        Texture("grass_0.png"),
        Texture("grass_1.png"),
        Texture("grass_2.png"),
        Texture("grass_3.png"),
        Texture("grass_4.png"),
        Texture("grass_5.png"),
        Texture("coal_plant_0.png"),
        Texture("wind_turbine_0.gif")
    )
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

    override fun show() {
        // Set up the GestureDetector to capture touch events.
        Gdx.input.inputProcessor = GestureDetector(this)
    }

    override fun render(delta: Float) {
        elapsedTime += delta

        // Define cycle duration for full transition
        val cycleDuration = 60f * 3f // 60 seconds * 3 = 3 minutes
        val alpha = 0.5f + 0.5f * Math.sin((2 * Math.PI * (elapsedTime / cycleDuration))).toFloat()

        // Define two colors (Day and Night)
        val dayColor = Color(1f, 1f, 1f, 1f)  // White (Day)
        val nightColor = Color(0.4f, 0.4f, 0.7f, 1f)  // Dark blue (Night)

        // Interpolate between Day and Night colors
        val skyColor = Color(
            (1 - alpha) * dayColor.r + alpha * nightColor.r,
            (1 - alpha) * dayColor.g + alpha * nightColor.g,
            (1 - alpha) * dayColor.b + alpha * nightColor.b,
            1f
        )

        // Clear the screen.
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Update the offset based on velocity if the screen is not touched.
        if (!Gdx.input.isTouched) {
            offsetX += velocityX * delta
            offsetY += velocityY * delta

            // Apply friction to gradually slow down the velocity.
            velocityX *= 0.9f
            velocityY *= 0.9f
            if (Math.abs(velocityX) < 0.1f) velocityX = 0f
            if (Math.abs(velocityY) < 0.1f) velocityY = 0f
        }

        // Draw the isometric tilemap.
        game.batch.begin()
        val mapSize = 32
        for (tileX in mapSize / 2 downTo mapSize / -2) {
            for (tileY in mapSize / 2 downTo mapSize / -2) {
                val random = java.util.Random(tileX.toLong() * java.util.Random(tileY.toLong()).nextLong())
                val tile = grassTiles[random.nextInt(grassTiles.size)]
                val posX = ((tileX - tileY) * (tile.width.toFloat() / 2 * tileScale)
                    + Gdx.graphics.width / 2 - tile.width.toFloat() / 2 * tileScale
                    + offsetX)
                val posY = ((tileX + tileY) * (tile.height.toFloat() / 4 * tileScale)
                    + Gdx.graphics.height / 2 - (tile.height.toFloat() / 2 - 10) * tileScale
                    + offsetY)
                game.batch.draw(tile, posX, posY, tile.width * tileScale, tile.height * tileScale)
            }
        }
        game.batch.color = Color(skyColor.r, skyColor.g, skyColor.b, 1f)
        println(elapsedTime)
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        // Dispose of tile textures if necessary.
    }

    // GestureDetector callbacks

    override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
        // Record the initial touch and offset.
        initialTouchX = x
        initialTouchY = y
        initialOffsetX = offsetX
        initialOffsetY = offsetY
        // Also record the last touch event for velocity calculation.
        lastTouchX = x
        lastTouchY = y
        lastTouchTime = System.currentTimeMillis()
        // Reset any existing velocity.
        velocityX = 0f
        velocityY = 0f
        return true
    }

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
        // Update the offset based on the difference from the initial touch.
        offsetX = initialOffsetX + (x - initialTouchX)
        offsetY = initialOffsetY - (y - initialTouchY)

        // Calculate velocity based on movement since the last event.
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
