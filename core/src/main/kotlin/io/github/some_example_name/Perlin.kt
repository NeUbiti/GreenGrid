package io.github.some_example_name

import com.badlogic.gdx.math.Vector2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// Interpolation function using a 6t^5 - 15t^4 + 10t^3 smoothstep.
fun interpolate(a0: Float, a1: Float, w: Float): Float {
    if (w < 0.0f) return a0
    if (w > 1.0f) return a1
    // Smooth interpolation curve.
    val smoothW = ((w * (w * 6.0f - 15.0f) + 10.0f) * w * w * w)
    return (a1 - a0) * smoothW + a0
}

// Generates a pseudo-random gradient vector for grid cell (posX, posY)
// based on the given seed.
fun randomGradient(seed: Int, posX: Int, posY: Int): Vector2 {
    // Mimic: srand(posX); posX = rand();
    val rand1 = java.util.Random(posX.toLong()).nextInt()
    val newPosX = rand1

    // Mimic: srand(posY); posY = rand();
    val rand2 = java.util.Random(posY.toLong()).nextInt()
    val newPosY = rand2

    // Mimic: srand(seed); value = (newPosX + rand()) * newPosY;
    val rand3 = java.util.Random(seed.toLong()).nextInt()
    var value = (newPosX + rand3) * newPosY

    // Mimic: srand(value); value = (newPosY + rand()) * newPosX;
    val rand4 = java.util.Random(value.toLong()).nextInt()
    value = (newPosY + rand4) * newPosX

    // Mimic: srand(value); value = rand() % 256;
    val rand5 = java.util.Random(value.toLong()).nextInt()
    value = abs(rand5 % 256)

    // Use the resulting value as an angle in degrees, convert to radians.
    val angle = Math.toRadians(value.toDouble()).toFloat()
    return Vector2(cos(angle), sin(angle))
}

// Computes the dot product between the gradient at integer grid point (ix, iy)
// and the distance vector from (ix, iy) to (x, y).
fun dotGridGradient(seed: Int, ix: Int, iy: Int, x: Float, y: Float): Float {
    val gradient = randomGradient(seed, ix, iy)
    val dx = x - ix
    val dy = y - iy
    return dx * gradient.x + dy * gradient.y
}

// Perlin noise implementation.
// Returns a noise value in the range approximately -1 to 1.
// (Multiply by 0.5f and add 0.5f to convert the output to [0,1] if desired.)
fun perlin(x: Float, y: Float, seed: Int): Float {
    // Determine grid cell coordinates surrounding (x, y)
    val x0 = floor(x).toInt()
    val x1 = x0 + 1
    val y0 = floor(y).toInt()
    val y1 = y0 + 1

    // Compute interpolation weights.
    val sx = x - x0
    val sy = y - y0

    // Interpolate between grid point gradients.
    val n0 = dotGridGradient(seed, x0, y0, x, y)
    val n1 = dotGridGradient(seed, x1, y0, x, y)
    val ix0 = interpolate(n0, n1, sx)

    val n2 = dotGridGradient(seed, x0, y1, x, y)
    val n3 = dotGridGradient(seed, x1, y1, x, y)
    val ix1 = interpolate(n2, n3, sx)

    return interpolate(ix0, ix1, sy)
}
