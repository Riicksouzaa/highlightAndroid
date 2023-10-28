package com.codenome.myhighlighttest

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codenome.highlightview.HighlighterConfig
import com.codenome.highlightview.TooltipConfig
import com.codenome.highlightview.drawHighlight
import com.codenome.myhighlighttest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.hello.setOnClickListener {
            val tooltipConfig = TooltipConfig("TEXT").apply { backgroundColor = getColor(R.color.black) }
            val highlighterConfig = HighlighterConfig(tooltipConfig = tooltipConfig).apply { overlayColor = getColor(R.color.overlay) }

            it.drawHighlight(this, highlighterConfig) {
                Toast.makeText(this, "HEHE", Toast.LENGTH_SHORT).show()
            }
        }


    }
}