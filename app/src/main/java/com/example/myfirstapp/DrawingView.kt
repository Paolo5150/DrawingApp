package com.example.myfirstapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var m_drawPath: CustomPath? = null
    private var m_canvasBitmap: Bitmap? = null
    private var m_drawPaint: Paint? = null
    private var m_canvasPaint: Paint? = null
    private var m_brushSize: Float = 0.0f
    private var m_color = Color.BLACK
    private var m_canvas: Canvas? = null

    private var m_allPaths = ArrayList<CustomPath>()

    init {
        setUpDrawing()
    }

    private fun setUpDrawing() {
        m_drawPaint = Paint()
        m_drawPath = CustomPath(m_color, m_brushSize)

        m_drawPaint!!.color = m_color
        m_drawPaint!!.style = Paint.Style.STROKE
        m_drawPaint!!.strokeJoin = Paint.Join.ROUND
        m_drawPaint!!.strokeCap = Paint.Cap.ROUND

        m_canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        m_canvasBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888)
        m_canvas = Canvas(m_canvasBitmap!!)
    }

    fun setColor(newColor: String)
    {
        m_color = Color.parseColor(newColor)
    }

    fun undoLast()
    {
        if(m_allPaths.size >0)
        {
            m_allPaths.removeAt(m_allPaths.size -1)
            invalidate()
        }

    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val x = event?.x
        val y = event?.y

        when(event?.action) {
            MotionEvent.ACTION_DOWN ->{
                m_drawPath!!.color = m_color
                m_drawPath!!.brushThickness = m_brushSize

                m_drawPath!!.reset()
                if(x != null && y != null)
                {
                    m_drawPath!!.moveTo(x,y)

                }
            }

            MotionEvent.ACTION_MOVE -> {
                if(x != null && y != null)
                    m_drawPath!!.lineTo(x,y)
            }

            MotionEvent.ACTION_UP -> {
                m_allPaths.add(m_drawPath!!)

                m_drawPath = CustomPath(m_color, m_brushSize)
            }
            else -> {
                invalidate()

                return false}
        }
        invalidate()

        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas!!.drawBitmap(m_canvasBitmap!!, 0f, 0f,m_canvasPaint)
        for(dp in m_allPaths)
        {
            Log.d("Painter", "Drawing path in list")

            m_drawPaint!!.strokeWidth = dp!!.brushThickness
            m_drawPaint!!.color = dp!!.color
            canvas.drawPath(dp!!, m_drawPaint!!)
        }

        if(!m_drawPath!!.isEmpty)
        {
            Log.d("Painter", "Drawing path")

            m_drawPaint!!.strokeWidth = m_drawPath!!.brushThickness
            m_drawPaint!!.color = m_drawPath!!.color
            canvas.drawPath(m_drawPath!!, m_drawPaint!!)
        }
    }

    fun setBrushSize(size: Float) {
        m_brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, resources.displayMetrics)
        m_drawPaint!!.strokeWidth = m_brushSize

    }
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }
}