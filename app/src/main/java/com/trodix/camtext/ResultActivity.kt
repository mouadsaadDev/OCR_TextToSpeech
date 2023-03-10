package com.trodix.camtext

import MyDrawView
import android.app.Application
import android.content.*
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.vision.Frame
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.nl.languageid.LanguageIdentification


import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_result.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class ResultActivity : AppCompatActivity() {
    lateinit var tts : TextToSpeech

    var detectLanguage: TextView? = null
    private val handler: Handler = Handler()
    private lateinit var myDrawView: MyDrawView
    private var ocrResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_result)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // This will disable the Soft Keyboard from appearing by default
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        //li zedt ana

        val resultLanguage = findViewById<TextView>(R.id.result);
        val checkLanguage = findViewById<Button>(R.id.checkLanguage);



        // Share button


        // Clipboard button
        clipboard_btn.setOnClickListener {
            // Get the clipboard system service
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OCR result", ocr_result.text)
            clipboard.setPrimaryClip(clip)

            val imgResource: Int = R.drawable.clipboard_done_24
            clipboard_btn.setCompoundDrawablesWithIntrinsicBounds(0, imgResource, 0, 0)
            clipboard_btn.text = resources.getString(R.string.copied)
            clipboard_btn.invalidate()

            Snackbar.make(result_main, resources.getString(R.string.copied_toast), Snackbar.LENGTH_SHORT).show()
            //Toast.makeText(this, resources.getString(R.string.copied_toast), Toast.LENGTH_SHORT).show()
        }

        // Translate button
        /*
        translate_btn.setOnClickListener {

        }
        */

        // Capture button
        recapture_btn.setOnClickListener {
            val myIntent = Intent(applicationContext, MainActivity::class.java)
            startActivityForResult(myIntent, 0)
        }

        // TODO: handle text selection
        /*
        val parent = image_overlay as FrameLayout
        myDrawView = MyDrawView(this)

        parent.addView(myDrawView)

        myDrawView.layoutParams.width = image_view.layoutParams.width
        myDrawView.layoutParams.height = image_view.layoutParams.height
        */

        val extras = intent.extras
        if (extras != null) {
            // There's an extra "text" when launching this activity from
            // the history list. We don't need to redo OCR
            if (extras?.getString("text") != null) {
                ocr_result.setText(extras?.getString("text"))
                loading_anim.visibility = View.GONE;
                return
            }
        }

        DoAsync {
            var imageUri: Uri? = null
            when (intent?.action) {
                // When coming from share event
                Intent.ACTION_SEND -> {
                    if (intent.type?.startsWith("image/") == true) {
                        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                            // Update UI to reflect image being shared
                            //image_view.setImageURI(it)
                            imageUri = it
                        }
                    }
                }
                // When coming from other activity
                else -> {
                    val extras = intent.extras
                    if (extras != null) {
                        imageUri = Uri.parse(extras.getString("imageUri"))
                    }
                }
            }

            if (imageUri != null) {
                //set image captured to image view
                try {
                    image_view.post {
                        image_view.setImageURI(imageUri)
                    }
                } catch (e: Throwable) {
                    image_view.post {
                        image_view.setImageResource(R.drawable.image_placeholder)
                    }
                }

                val inStream: InputStream? = applicationContext.contentResolver.openInputStream(
                    imageUri!!
                )
                val bitmap: Bitmap = BitmapFactory.decodeStream(inStream)

                val ei = ExifInterface(getPath(this, imageUri!!))
                val orientation: Int = ei.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )

                // -> Some device have the sensor in landscape mode
                // meaning the image should be rotated for OCR
                var rotatedBitmap : Bitmap? = null
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotatedBitmap = rotateImage(bitmap, 90F)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotatedBitmap = rotateImage(bitmap, 180F)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotatedBitmap = rotateImage(bitmap, 270F)
                    else -> rotatedBitmap = bitmap
                }

                val image = InputImage.fromBitmap(rotatedBitmap!!, 0)
                val recognizer = TextRecognition.getClient()

                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Task completed successfully
                        val resultText = visionText.text
                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            val blockCornerPoints = block.cornerPoints
                            val blockFrame = block.boundingBox
                            for (line in block.lines) {
                                val lineText = line.text
                                val lineCornerPoints = line.cornerPoints
                                val lineFrame = line.boundingBox
                                for (element in line.elements) {
                                    val elementText = element.text
                                    val elementCornerPoints = element.cornerPoints
                                    val elementFrame = element.boundingBox
                                }
                            }
                        }

                        ocrResult = visionText.text

                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
                        if (ocrResult != "" && sharedPreferences.getBoolean("history", true)) {
                            extras!!.getString("imageUri")?.let { addHistoryItem(it, ocrResult) }
                        }

                        ocr_result.post {
                            loading_anim.visibility = View.GONE;
                            ocr_result.setText(ocrResult)
                            //li zedt ana

                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        // ...
                    }
            }
        }

        //li zedt ana
        checkLanguage.setOnClickListener {

            val languageIdentifier = LanguageIdentification.getClient()
            languageIdentifier.identifyLanguage(ocrResult)
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        Toast.makeText(baseContext, "Can't identify language", Toast.LENGTH_SHORT).show();
                    } else {
                        when(languageCode){
                            "en" -> {
                                resultLanguage.text = "The Identified language is English"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.US
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "ru" -> resultLanguage.text = "The Identified language is Russian"
                            "ar" -> resultLanguage.text = "The Identified language is Arabic"
                            "fr" -> {
                                resultLanguage.text = "The Identified language is French"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.FRANCE
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "es" -> {
                                resultLanguage.text = "The Identified language is Spanish"


                            }

                            "hi" -> resultLanguage.text = "The Identified language is Hindi"
                            "id" -> resultLanguage.text = "The Identified language is Indonesian"
                            "it" -> {
                                resultLanguage.text = "The Identified language is Italian"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.ITALIAN
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "ja" -> {
                                resultLanguage.text = "The Identified language is Japanese"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.JAPAN
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "pt" -> {
                                resultLanguage.text = "The Identified language is Portuguese"

                            }
                            "de" -> {
                                resultLanguage.text = "The Identified language is German"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.GERMAN
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "hr" -> resultLanguage.text = "The Identified language is Croatian"
                            "sv" -> resultLanguage.text = "The Identified language is Swedish"
                            "tr" -> resultLanguage.text = "The Identified language is Turkish"
                            "uk" -> resultLanguage.text = "The Identified language is Ukrainian"
                            "vi" -> resultLanguage.text = "The Identified language is Vietnamese"
                            "zh" -> {
                                resultLanguage.text = "The Identified language is Chinese"
                                tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                                    if (it==TextToSpeech.SUCCESS){
                                        tts.language = Locale.CHINA
                                        tts.setSpeechRate(1.0f)
                                        tts.speak(ocrResult, TextToSpeech.QUEUE_ADD, null)
                                    }
                                })
                            }
                            "hu" -> resultLanguage.text = "The Identified language is Hungarian"
                            "af" -> resultLanguage.text = "The Identified language is Afrikaans"


                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(baseContext, "Can't identify language", Toast.LENGTH_SHORT).show();
                }
        }






        // TODO: handle text selection
        //handler.postDelayed(runnable, 2000); // Call the handler for the first time.
    }



    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (!TextUtils.isEmpty(id)) {
                    return if (id.startsWith("raw:")) {
                        id.replaceFirst("raw:".toRegex(), "")
                    } else try {
                        val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"),
                            java.lang.Long.valueOf(id)
                        )
                        getDataColumn(context, contentUri, null, null)
                    } catch (e: NumberFormatException) {
                        null
                    }
                }
            } else if (isMediaDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_options -> {
                Log.d("MENU", "OPTIONS")
                val myIntent = Intent(applicationContext, SettingsActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_history -> {
                Log.d("MENU", "HISTORY")
                val myIntent = Intent(applicationContext, HistoryActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_about -> {
                val myIntent = Intent(applicationContext, AboutActivity::class.java)
                startActivityForResult(myIntent, 0)
                true
            }
            R.id.action_rate -> {
                rateMyApp()
                true
            }
            else -> false
        }
    }

    private fun rateMyApp() {
        val uri: Uri = Uri.parse("market://details?id=" + applicationContext.packageName)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + applicationContext.packageName)
                )
            )
        }
    }

    private fun addHistoryItem(uri:String, text:String) {
        var json = JSONArray()

        val sharedPref = getSharedPreferences("appData", Context.MODE_PRIVATE)
        val prefEditor = sharedPref.edit()

        // Retrieve values from preferences
        val str: String? = sharedPref.getString("history", null)
        // Prefs exist, we override the json
        if (str != null) {
            json = JSONArray(str)
        }
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())

        // Create JSON entry
        val entry = JSONObject()
        entry.put("Uri", uri)
        entry.put("Text", text)
        entry.put("Date", currentDate)
        json.put(entry)
        prefEditor.putString("history", json.toString())
        prefEditor.apply() // handle writing in the background
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            // The method you want to call every now and then.
            handleSelection()
            handler.postDelayed(this, 300) // This time is in millis.
        }
    }

    private fun handleSelection() {
        val res = myDrawView.calculateBoundingBox()
        Log.d("DRAWING", res.toString())

        if(res.size == 0) return
        if(res[0] == 0.0 && res[1] == 0.0 && res[2] == 0.0 && res[3] == 0.0) return
        image_view.scaleX
        image_view.scaleY

        val drawable: BitmapDrawable? = image_view.drawable as BitmapDrawable?

        val cropped: Bitmap = Bitmap.createBitmap(
            drawable!!.bitmap,
            (res[0]*image_view.scaleX).toInt(),
            (res[1]*image_view.scaleY).toInt(),
            (res[2]*image_view.scaleX).toInt(),
            (res[3]*image_view.scaleY).toInt()
        )

        image_view.setImageBitmap(cropped)

        val imFrame = Frame.Builder().setBitmap(cropped).build()


    }

}

class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
    init {
        execute()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        handler()
        return null
    }
}
