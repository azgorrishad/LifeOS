package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiAnimation(modifier: Modifier = Modifier, onAnimFinished: () -> Unit) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    val colors = listOf(Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFC107))

    LaunchedEffect(Unit) {
        val newParticles = List(150) {
            val angle = Random.nextDouble(PI * 1.1, PI * 1.9)
            val speed = Random.nextDouble(20.0, 60.0)
            Particle(
                x = 0.5f,
                y = 1.0f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                color = colors.random(),
                size = Random.nextFloat() * 15f + 10f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 20f - 10f
            )
        }
        particles = newParticles

        while (isActive) {
            val updated = particles.map { p ->
                p.copy(
                    x = p.x + p.vx / 1000f,
                    y = p.y + p.vy / 1000f,
                    vy = p.vy + 1.5f, // Gravity
                    rotation = p.rotation + p.rotationSpeed
                )
            }.filter { it.y < 1.2f }
            
            particles = updated
            if (particles.isEmpty()) break
            delay(16)
        }
        onAnimFinished()
    }

    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                withTransform({
                    translate(p.x * width, p.y * height)
                    rotate(p.rotation)
                }) {
                    drawRect(
                        color = p.color,
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}
