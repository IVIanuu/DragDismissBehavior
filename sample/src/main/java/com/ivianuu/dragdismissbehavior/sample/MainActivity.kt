package com.ivianuu.dragdismissbehavior.sample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.ivianuu.dragdismissbehavior.DragDismissBehavior
import com.ivianuu.dragdismissbehavior.DragDismissCallback

class MainActivity : AppCompatActivity() {

    private val elastic by lazy(LazyThreadSafetyMode.NONE) { findViewById<View>(R.id.elastic) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DragDismissBehavior.from(elastic)?.run {
            callback = object : DragDismissCallback {
                override fun onDrag(offset: Float) {
                }

                override fun onDragDismissed() {
                    finish()
                }
            }
        }
    }
}
