package com.anwesh.uiprojects.quadtristepview

/**
 * Created by anweshmishra on 22/11/18.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Path
import android.app.Activity
import android.content.Context

val nodes : Int = 5
val lines : Int = 4
val scDiv : Double = 0.51
val scGap : Float = 0.05f
val sizeFactor : Int = 3
val strokeFactor : Int = 80

fun Int.getInverse() : Float = 1f / this

fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.getInverse(), Math.max(0f, this - i * n.getInverse())) * n

fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()

fun Float.mirrorValue(a : Int, b : Int) : Float = (1f - this) * a.getInverse() + this * b.getInverse()

fun Float.updateScale(dir : Float, a : Int, b : Int) : Float = dir * scGap * scaleFactor().mirrorValue(lines, 1)

fun Canvas.drawQTSNode(i : Int, scale : Float, paint : Paint) {
    paint.color = Color.parseColor("#1565C0")
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    val gap : Float = w / (nodes + 1)
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val size : Float = gap / sizeFactor
    save()
    translate(gap * (i + 1), h/2)
    rotate(90f * sc2)
    for (j in 0..(lines - 1)) {
        val sc : Float = sc1.divideScale(j, lines)
        save()
        val path : Path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(size * ( 1 - 2 *(j % 2)), 0f)
        path.lineTo(0f, size * sc * (1 - 2 * (j / 2)))
        path.lineTo(0f, 0f)
        drawLine(0f, 0f, (size - paint.strokeWidth) * ( 1 - 2 *(j % 2)), 0f, paint)
        paint.style = Paint.Style.FILL
        drawPath(path, paint)
        restore()
    }
    restore()
}

class QuadTriStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateScale(dir, lines, 1)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class QTSNode(var i : Int, val state : State = State()) {

        private var next : QTSNode? = null

        private var prev : QTSNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = QTSNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawQTSNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : QTSNode {
            var curr : QTSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class QuadTriStep(var i : Int) {

        private var root : QTSNode = QTSNode(0)
        private var curr : QTSNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : QuadTriStepView) {

        private val animator : Animator = Animator(view)

        private val qts : QuadTriStep = QuadTriStep(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            qts.draw(canvas, paint)
            animator.animate {
                qts.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            qts.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity)  : QuadTriStepView {
            val view : QuadTriStepView = QuadTriStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}