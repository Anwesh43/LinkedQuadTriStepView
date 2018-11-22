package com.anwesh.uiprojects.linkedquadtristepview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.quadtristepview.QuadTriStepView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view : QuadTriStepView = QuadTriStepView.create(this)
    }
}
