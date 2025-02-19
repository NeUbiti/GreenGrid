package io.github.some_example_name

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch

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
