package io.github.some_example_name

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture

class LoadingScreen(private val game: Main) : Screen {
    private val welcomeImage0 = Texture("greengrid_0.png")
    private val welcomeImage1 = Texture("greengrid_1.png")
    private val welcomeImage2 = Texture("greengrid_2.png")
    private val welcomeImage3 = Texture("greengrid_3.png")
    private var welcomeImage = welcomeImage0

    private var elapsedTime = 0f
    private var animStateTime = 0f
    private val animSpeed = 0.3f  // Each frame lasts 0.1 seconds.
    // Introduce an initial delay of 0.5 seconds (black screen)
    private val initialDelay = 0.5f
    // Duration for the fade-in effect after the initial delay
    private val fadeInDuration = 1f
    // Total display duration for the loading screen
    private val displayDuration = 3f

    override fun show() {
        // Optionally add input listeners here.
    }

    override fun render(delta: Float) {
        elapsedTime += delta
        animStateTime += delta

        // Calculate the fade-in alpha.
        val alpha = when {
            elapsedTime < initialDelay -> 0f
            elapsedTime < initialDelay + fadeInDuration -> (elapsedTime - initialDelay) / fadeInDuration
            else -> 1f
        }

        // Clear the screen.
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Only start animating after the initial delay.
        if (elapsedTime >= initialDelay) {
            // Cycle through the four frames.
            // Total cycle duration = 4 * animSpeed.
            val frameTime = animStateTime % (4 * animSpeed)
            welcomeImage = when {
                frameTime < animSpeed -> welcomeImage0
                frameTime < 2 * animSpeed -> welcomeImage1
                frameTime < 3 * animSpeed -> welcomeImage2
                else -> welcomeImage3
            }
        }

        // Draw the welcome image at the center with the computed alpha.
        game.batch.begin()
        game.batch.color = Color(1f, 1f, 1f, alpha)
        val x = (Gdx.graphics.width - welcomeImage.width) / 2f
        val y = (Gdx.graphics.height - welcomeImage.height) / 2f
        game.batch.draw(welcomeImage, x, y)
        game.batch.end()
        game.batch.color = Color.WHITE  // Reset color for future draws.

        // Transition to the game screen after displayDuration.
        if (elapsedTime > displayDuration) {
            game.screen = GameScreen(game)
            dispose()
        }
    }

    override fun resize(width: Int, height: Int) { }
    override fun pause() { }
    override fun resume() { }
    override fun hide() { }

    override fun dispose() {
        welcomeImage0.dispose()
        welcomeImage1.dispose()
        welcomeImage2.dispose()
        welcomeImage3.dispose()
    }
}
