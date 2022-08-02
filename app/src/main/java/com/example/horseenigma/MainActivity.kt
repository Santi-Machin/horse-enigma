package com.example.horseenigma

import android.content.res.TypedArray
import com.stripe.android.PaymentConfiguration
import android.graphics.Point
import android.icu.text.CaseMap
import androidx.appcompat.app.AppCompatActivity
import android.util.TypedValue
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.horseenigma.R
import org.w3c.dom.Text
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import junit.runner.Version.id
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    var unloadedAd = true

    private var resume = 0

    private var bitmap: Bitmap? = null

    private var mHandler: Handler? = null
    private var timeInSeconds: Long = 0
    private var gaming = true
    private var string_share = ""

    private var cellSelected_x = 0
    private var cellSelected_y = 0

    private var lastlevel = 2
    private var nextLevel = false
    private var level = 1
    private var scoreLevel = 1
    private var levelMoves = 0
    private var movesRequired = 0
    private var moves = 0
    private var lives = 1
    private var score_lives = 1

    private var options = 0
    private var bonus = 0

    private var checkMovement = true

    private var nameColorBlack = "cell_black"
    private var nameColorWhite = "cell_white"

    private lateinit var board: Array<IntArray>

    private var premium: Boolean = false

    private var optionBlack = R.drawable.option_black
    private var optionWhite = R.drawable.option_white

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var mpMovement: MediaPlayer
    private lateinit var mpBonus: MediaPlayer
    private lateinit var mpGameOver: MediaPlayer
    private lateinit var mpWin: MediaPlayer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSound()
        initScreengame()
        initPreferences()
    }

    override fun onResume() {
        super.onResume()
        checkPremium()
        startGame()
        resume++
    }

    private fun initSound() {
        mpMovement = MediaPlayer.create(this, R.raw.piece_movement)
        mpMovement.isLooping = false

        mpGameOver = MediaPlayer.create(this, R.raw.game_over)
        mpGameOver.isLooping = false

        mpWin = MediaPlayer.create(this, R.raw.win)
        mpWin.isLooping = false

        mpBonus = MediaPlayer.create(this, R.raw.bonus)
        mpBonus.isLooping = false

        }

    private fun initPreferences() {
        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }


    private fun checkPremium() {
        premium = sharedPreferences.getBoolean("PREMIUM", false)
        if (premium) {
            lastlevel = 5
            level = sharedPreferences.getInt("LEVEL", 1)

            var lyPremium = findViewById<LinearLayout>(R.id.lyPremium)
            lyPremium.removeAllViews()

            var lyAdsBanner = findViewById<LinearLayout>(R.id.lyAdsBanner)
            lyAdsBanner.removeAllViews()

            var svGame = findViewById<ScrollView>(R.id.svGame)
            svGame.setPadding(0,0,0,0)

            var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
            tvLiveData.setTextColor(resources.getColor(R.color.gold))

            var tvLive = findViewById<TextView>(R.id.tvLives)
            tvLive.setTextColor(resources.getColor(R.color.gold))

            var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
            tvBonusData.setTextColor(resources.getColor(R.color.gold))

            nameColorBlack = "black_cell_premium"
            nameColorWhite = "white_cell_premium"
            optionBlack = R.drawable.option_black_premium
            optionWhite = R.drawable.option_white_premium
        }
        else if(resume == 0) {
           initAds()
        }
    }

    fun launchPaymentCard(v: View) {
        callPayment()
    }
    private fun callPayment() {
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_Dt4ZBItXSZT1EzmOd8yCxonL"
        )
        val intent = Intent(this, CheckoutActivity::class.java)
        intent.putExtra("level", level)
        startActivity(intent)
    }
    private fun initAds() {
        MobileAds.initialize(this) {}
        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"

        var lyAdsBanner = findViewById<LinearLayout>(R.id.lyAdsBanner)
        lyAdsBanner.addView(adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
    private fun showInterstitial() {
        unloadedAd = true
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    mInterstitialAd = null
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    mInterstitialAd = null
                }
                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                }
            }
            mInterstitialAd?.show(this)
        }
    }
    private fun getReadyAds() {
        var adRequest = AdRequest.Builder().build()
        unloadedAd = false

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                println("hola")

            }
        })
    }

    private fun initScreengame() {
        setSizeBoard()
        hide_message(false)
    }
    private fun setSizeBoard() {
        var iv: ImageView

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x

        var width_dp = (width / resources.displayMetrics.density)

        var lateralMarginsDP = 0
        val width_cell = (width_dp - lateralMarginsDP) / 8
        var height_cell = width_cell

        for(i in 0..7) {
            for(j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id",packageName))

                var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height_cell, resources.displayMetrics).toInt()
                var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width_cell, resources.displayMetrics).toInt()
                iv.setLayoutParams(TableRow.LayoutParams(width, height))
            }
        }
    }
    private fun hide_message(start: Boolean) {
        val lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.INVISIBLE

        if (start) startGame()
    }

    fun launchAction(v: View) {
        if(premium == false && level > lastlevel) callPayment()
        hide_message(true)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun launchShareGame(v: View) {
        shareGame()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun shareGame() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        var ssc: ScreenCapture = capture(this)
        bitmap = ssc.getBitmap()
        if(bitmap != null) {
            var idGame = SimpleDateFormat("yyy/MM/dd").format(Date())
            idGame = idGame.replace(":", "")
            idGame = idGame.replace("/", "")

            val path = saveImage(bitmap, "${idGame}.jpg")
            var bmpUri = Uri.parse(path)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri)
            shareIntent.putExtra(Intent.EXTRA_TEXT, string_share)
            shareIntent.type = "image/png"

            val finalShareIntent = Intent.createChooser(shareIntent, "Select were you want share")
            finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(finalShareIntent)
        }
    }
    private fun saveImage(bitmap: Bitmap?, fileName: String): String? {
        if(bitmap == null)
            return null

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            }
            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                this.contentResolver.openOutputStream(uri).use {
                    if (it == null)
                        return@use

                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, it)
                    it.flush()
                    it.close()

                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
                }
            }
            return uri.toString()
        }

        val filePath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES + "/Screenshots"
        ).absolutePath

        val dir = File(filePath)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        var fOut = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
        fOut.flush()
        fOut.close()

        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null)
        return filePath
    }

    fun checkCellClicked(v: View) {
        var name = v.tag.toString()
        var x = name.subSequence(1,2).toString().toInt()
        var y = name.subSequence(2,3).toString().toInt()
        checkCell(x, y)
    }
    private fun checkCell(x: Int, y: Int) {
        var checkTrue = true
        if(checkMovement){
            var dif_x = x - cellSelected_x
            var dif_y = y - cellSelected_y

            checkTrue = false
            if (dif_x == 1 && dif_y == 2) checkTrue = true // right - top long
            if (dif_x == 1 && dif_y == -2) checkTrue = true //right - bottom long
            if (dif_x == 2 && dif_y == 1) checkTrue = true // right long - top
            if (dif_x == 2 && dif_y == -1) checkTrue = true // right long - bottom
            if (dif_x == -1 && dif_y == 2) checkTrue = true // left - top long
            if (dif_x == -1 && dif_y == -2) checkTrue = true // left - bottom long
            if (dif_x == -2 && dif_y == 1) checkTrue = true // left long - long
            if (dif_x == -2 && dif_y == -1) checkTrue = true // left long - bottom
        }
        else {
            if (board[x][y] != 1 ) {
                bonus--
                var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
                tvBonusData.text = " + $bonus"
                if (bonus == 0) {
                    tvBonusData.text = ""
                }
            }
        }

        if (board[x][y] == 1) checkTrue = false
        if(checkTrue) selectCell(x, y)

    }
    private fun selectCell(x: Int, y: Int) {
        moves--
        var tvMovesNumber = findViewById<TextView>(R.id.tvMovesNumber)
        tvMovesNumber.text = moves.toString()

        if(board[x][y] == 2) {
            bonus++
            var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
            tvBonusData.text = " + $bonus"
            mpBonus.start()
        }
        else {
            mpMovement.start()
        }

        board[x][y] = 1
        paintHorseCell(cellSelected_x, cellSelected_y, "previous_cell")

        cellSelected_x = x
        cellSelected_y = y

        clearOptions()

        paintHorseCell(x, y, "selected_cell")
        checkMovement = true
        checkOption(x, y)

        if(moves > 0) {
            checkNewBonus()
            checkGameOver()
        }
        else {
            showMessage("You win", "next level", false)
        }
    }

    private fun resetBoard() {

        // 0 -> The cell is free
        // 1 -> The cell is checked
        // 2 -> The cell is a bonus
        // 9 -> The cell is a possibility of movement

        board = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        )
    }
    private fun clearBoard() {
        var iv: ImageView

        var colorBlack = ContextCompat.getColor(this, resources.getIdentifier(nameColorBlack, "color", packageName))
        var colorWhite = ContextCompat.getColor(this, resources.getIdentifier(nameColorWhite, "color", packageName))

        for (i in 0..7) {
            for (j in 0..7) {
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                iv.setImageResource(0)

                if (checkColorCell(i,j) == "black") iv.setBackgroundColor(colorBlack)
                else iv.setBackgroundColor(colorWhite)
            }
        }
    }
    private fun setFirstPosition() {
        var x = 0
        var y = 0

        var firstPosition = false
        while (firstPosition == false) {
            x = (0..7).random()
            y = (0..7).random()
            if(board[x][y] == 0) firstPosition = true
            checkOption(x, y)
            if(options == 0) firstPosition = false
        }

        cellSelected_x = x
        cellSelected_y = y

        selectCell(x, y)
    }

    private fun setLevel() {
        if(nextLevel) {
            level++
            setLives()
        }
        else {
            if(!premium) {
                lives--
                if (lives < 1)
                    level = 1
                lives = 1
            }
        }
    }
        /*if(nextLevel) {
            level++
            if(!premium) setLives()
            else {
                editor.apply {
                    putInt("LEVEL", level!!)
                }.apply
            }
        } else {
            if(!premium) {
                lives--
                if (lives <1)
                    level = 1
                    lives = 1
            }
        }
    }*/

    private fun setLevelParameters() {
        var tvLiveData = findViewById<TextView>(R.id.tvLiveData)
        tvLiveData.text = lives.toString()
        if(premium) tvLiveData.text = "∞"

        var tvLevelNumber = findViewById<TextView>(R.id.tvLevelNumber)
        tvLevelNumber.text = level.toString()

        bonus = 0
        var tvBonusData = findViewById<TextView>(R.id.tvBonusData)
        tvBonusData.text = ""

        setLevelMoves()
        moves = levelMoves

        movesRequired = setMovesRequired()
    }

    private fun setLevelMoves() {
        when (level){
            1 -> levelMoves = 64
            2 -> levelMoves = 56
            3 -> levelMoves = 32
            4 -> levelMoves = 16
            5 -> levelMoves = 48
        }
    }
    private fun setLives() {
        when (level) {
            1 -> lives = 1
            2 -> lives = 4
            3 -> lives = 3
            4 -> lives = 3
            5 -> lives = 4
        }
        if (premium) lives = 9999999
    }
    private fun setMovesRequired():Int {
        var movesRequired = 0

        when(level) {
            1 -> movesRequired = 8
            2 -> movesRequired = 10
            3 -> movesRequired = 12
            4 -> movesRequired = 10
            5 -> movesRequired = 10
        }
        return movesRequired
    }
    private fun setBoardLevel() {
        when(level) {
            2 -> paintLevel_2()
            3 -> paintLevel_3()
            4 -> paintLevel_4()
            5 -> paintLevel_5()
        }
    }

    private fun paint_column(column: Int) {
        for(i in 0..7) {
            board[column][i] = 1
            paintHorseCell(column, i, "previous_cell")
        }
    }

    private fun paintLevel_2() {
        paint_column(6)
    }
    private fun paintLevel_3() {
        for (i in 0..7) {
            for (j in 4..7) {
                board[j][i] = 1
                paintHorseCell(j, i, "previous_cell")
            }
        }
    }
    private fun paintLevel_4() {
        paintLevel_3(); paintLevel_5()
    }
    private fun paintLevel_5() {
        for (i in 0..3) {
            for (j in 0..3) {
                board[j][i] = 1
                paintHorseCell(j, i, "previous_cell")
            }
        }
    }

    private fun checkNewBonus() {
        if(moves%movesRequired == 0) {
            var bonusCell_x = 0
            var bonusCell_y = 0
            var bonusCell = false
            while (bonusCell == false) {
                bonusCell_x = (0..7).random()
                bonusCell_y = (0..7).random()

                if(board[bonusCell_x][bonusCell_y] == 0) bonusCell = true
            }
            board[bonusCell_x][bonusCell_y] = 2
            paintBonusCell(bonusCell_x, bonusCell_y)
        }
    }
    private fun paintBonusCell(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id",packageName))
        iv.setImageResource(R.drawable.bonus)
    }

    private fun clearOptions() {
        for(i in 0..7) {
            for(j in 0..7) {
                if(board[i][j] == 9 || board[i][j] == 2) {
                    if(board[i][j] == 9) board[i][j] = 0
                    clearOption(i, j)
                }
            }
        }
    }
    private fun clearOption(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id",packageName))
        if(checkColorCell(x, y) == "black")
            iv.setBackgroundColor(ContextCompat.getColor(this,
                resources.getIdentifier(nameColorBlack, "color", packageName)))
        else
            iv.setBackgroundColor(ContextCompat.getColor(this,
                resources.getIdentifier(nameColorWhite, "color", packageName)))

        if(board[x][y] == 1) {
            iv.setBackgroundColor(ContextCompat.getColor(this,
            resources.getIdentifier("previous_cell", "color", packageName)))
            }
    }
    private fun paintOptions(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id",packageName))
        if(checkColorCell(x, y) == "black") iv.setBackgroundResource(optionBlack)
        else iv.setBackgroundResource(optionWhite)
    }
    private fun paintAllOptions() {
        for (i in 0..7) {
            for(j in 0..7) {
                if (board[i][j] != 1) paintOptions(i, j)
                if (board[i][j] == 0) board[i][j] = 9
            }
        }
    }

    private fun checkGameOver() {
        if(options == 0) {
            if(bonus > 0) {
                checkMovement = false
                paintAllOptions()
            }
            else {
                showMessage("Game over", "Try again!", true)
            }
        }
    }
    private fun showMessage(title: String, action: String, gameOver: Boolean, endGame: Boolean = false) {
        gaming = false
        nextLevel = !gameOver

        var lyMessage = findViewById<LinearLayout>(R.id.lyMessage)
        lyMessage.visibility = View.VISIBLE

        var tvTitleMessage = findViewById<TextView>(R.id.tvTitleMessage)
        tvTitleMessage.text = title

        var tvTimeData = findViewById<TextView>(R.id.tvTimeNumber)
        var score: String = ""
        if (gameOver) {
            mpGameOver.start()

            if (premium == false) {
                showInterstitial()
            }

            score = "Puntuacion " + (levelMoves-moves) + "/" + levelMoves
            string_share = "No puedo resolverlo! "+ score +""

        }
        else {
            mpWin.start()
            score = tvTimeData.text.toString()
            string_share = "Lo complete!, Nivel: $level ("+ score +")"
        }

        if (endGame) score = ""
        var tvScoreMessage = findViewById<TextView>(R.id.tvScoreMessage)
        tvScoreMessage.text = score

        var tvAction = findViewById<TextView>(R.id.tvAction)
        tvAction.text = action

    }

    private fun checkOption(x: Int, y: Int) {
        options = 0

        checkMove(x, y, 1, 2) //check move right - top long
        checkMove(x, y, 2, 1) //check move right long - top
        checkMove(x, y, 1, -2) // check move right - bottom long
        checkMove(x, y, 2, -1) //check move right long - bottom
        checkMove(x, y, -1, 2) //check move left - top long
        checkMove(x, y, -2, 1) //check move left long - top
        checkMove(x, y, -1, -2) //check move left - bottom long
        checkMove(x, y, -2, -1) //check move left long - bottom

        var tvOptionsData = findViewById<TextView>(R.id.tvOptionsData)
        tvOptionsData.text = options.toString()
    }
    private fun checkMove(x: Int, y: Int, mov_x: Int, mov_y: Int) {
        var option_x = x + mov_x
        var option_y = y + mov_y


        if(option_x < 8 && option_y < 8 && option_x >= 0 && option_y >= 0) {
            if(board[option_x][option_y] != 1) {
                if ((board[option_x][option_y] == 0) || (board[option_x][option_y] == 2))
                    options++
                paintOptions(option_x, option_y)
                if (board[option_x][option_y] == 0) board[option_x][option_y] = 9
            }
        }

    }
    private fun checkColorCell(x: Int, y: Int): String {
        var color = ""
        var blackColumn_x = arrayOf(0,2,4,6)
        var blackRow_x = arrayOf(1,3,5,7)
        if((blackColumn_x.contains(x) && blackColumn_x.contains(y)) || (blackRow_x.contains(x) && blackRow_x.contains(y))) {
            color = "black"
        }
        else {
            color = "white"
        }
        return color
    }

    private fun paintHorseCell(x: Int, y: Int, color: String) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(color, "color", packageName)))
        if (color == "selected_cell") {
            iv.setImageResource(R.drawable.horse)
        }
        else {
            iv.setImageResource(R.drawable.flag)
        }
    }

    private fun resetTime() {
        mHandler?.removeCallbacks(chronometer)
        timeInSeconds = 0

        var tvTimeNumber = findViewById<TextView>(R.id.tvTimeNumber)
        tvTimeNumber.text = "00:00"
    }
    private fun startTime() {
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }
    private var chronometer: Runnable = object: Runnable{
        override fun run() {
            try {
                if(gaming) {
                    timeInSeconds++
                    updateStopWatchView(timeInSeconds)
                }
            } finally {
                mHandler!!.postDelayed(this, 1000L)
            }
        }
    }
    private fun updateStopWatchView(timeInSeconds: Long) {
        val formattedTime = getFormattedStopWatch((timeInSeconds * 1000))
        var tvTimeNumber = findViewById<TextView>(R.id.tvTimeNumber)
        tvTimeNumber.text = formattedTime
    }
    private fun getFormattedStopWatch(ms: Long): String {
        var milliseconds = ms
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MILLISECONDS.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (minutes < 10) "0" else ""}$minutes:" + "${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun startGame() {

        if (unloadedAd == true && premium == false) getReadyAds()

        setLevel()
        if (level > lastlevel) {
            if (premium) showMessage("Terminaste el juego", "Mas niveles pronto...", false, true)
            else {
                showMessage("Accede a todos los niveles con premium", "Obten acceso premium", false, true)
            }
        }

        else {
            setLevelParameters()

            resetBoard()
            clearBoard()
            setBoardLevel()
            setFirstPosition()

            resetTime()
            startTime()
            gaming = true
        }

    }
}