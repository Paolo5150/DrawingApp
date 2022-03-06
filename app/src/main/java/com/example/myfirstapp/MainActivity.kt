package com.example.myfirstapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var m_drawingView: DrawingView? = null
    private var m_imageButtonCurrentPaint: ImageButton? = null
    private var m_customProgressDialog : Dialog? = null

    private val m_openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            if(result.resultCode == RESULT_OK && result.data != null) {
                val imageBnd:ImageView = findViewById(R.id.iv_background)
                imageBnd.setImageURI(result.data?.data)
            }
        }

    val m_requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){perms ->
            perms.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted) {

                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    m_openGalleryLauncher.launch(pickIntent)
                }
                else
                {
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {

                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_drawingView = findViewById(R.id.drawing_view)
        m_drawingView!!.setBrushSize(20.0f)

        var linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        m_imageButtonCurrentPaint = linearLayoutPaintColors[0] as ImageButton

        m_imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        val brushBtn = findViewById<Button>(R.id.ib_brush)
        brushBtn.setOnClickListener{
            showBrishSizeDialog()
        }

        val undoBtn = findViewById<Button>(R.id.ib_undo)
        undoBtn.setOnClickListener{
            m_drawingView!!.undoLast()
        }

        val galleryBtn = findViewById<Button>(R.id.ib_gallery)
        galleryBtn.setOnClickListener{
            requestStoragePermission()
        }

        val saveBtn = findViewById<Button>(R.id.ib_save)
        saveBtn.setOnClickListener{
            if(isReadStorageAllowed())  {             //Also true if we have write
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawingViewContainer)
                    val bmp = getBitmapFromView(flDrawingView)
                    val res = saveBmpToFile(bmp)
                }
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val res = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showRationaleDialog("Permission required", "Need the read storage permission to save files")
        }
        else {
            m_requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message)
        builder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()

        }
        builder.show()
        }

    private fun showBrishSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size")

        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            m_drawingView!!.setBrushSize(5.0f)
            brushDialog.dismiss()
        }

        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            m_drawingView!!.setBrushSize(15.0f)
            brushDialog.dismiss()
        }

        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            m_drawingView!!.setBrushSize(30.0f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBmp = Bitmap.createBitmap(view.width, view.height,Bitmap.Config.ARGB_8888)

        val canv = Canvas(returnedBmp)
        val bgDrawable = view.background
        if(bgDrawable != null) {
            bgDrawable.draw(canv)
        }
        else {
            canv.drawColor(Color.WHITE)
        }

        view.draw(canv)


        return returnedBmp
    }

    fun paintClicked(view: View)
    {
        if(view != m_imageButtonCurrentPaint) {
            var imageBtn = view as ImageButton
            var tag = imageBtn.tag.toString()
            m_drawingView!!.setColor(tag)
            imageBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            m_imageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            m_imageButtonCurrentPaint = imageBtn

        }
    }

    private fun showProgressDialog() {
        m_customProgressDialog = Dialog(this)
        m_customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        m_customProgressDialog?.show()
    }

    private fun dismissProgressDialog() {
        if(m_customProgressDialog != null) {
            m_customProgressDialog?.dismiss()
            m_customProgressDialog = null
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) {
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private suspend fun saveBmpToFile(bmp: Bitmap?) {
        var result = ""
        withContext(Dispatchers.IO) {
            if(bmp != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() +
                            File.separator + "DrawingApp" + System.currentTimeMillis() / 1000 + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        dismissProgressDialog()
                        if(result.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, "File saved: $result", Toast.LENGTH_SHORT).show()

                            //Ask for sharing
                            shareImage(result)
                        }
                        else {
                            Toast.makeText(this@MainActivity, "Failed to save: $result", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception) {
                    result = ""
                    Toast.makeText(this@MainActivity, "Exception ${e.message}", Toast.LENGTH_SHORT).show()

                }
            }
        }
    }
}