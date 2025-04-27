package qzwx.app.qtodo.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import qzwx.app.qtodo.R

class WidgetSettingsActivity : Activity() {
    private val TAG = "WidgetSettingsActivity"
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置结果为取消，如果用户按了返回键，则不会更新小组件
        setResult(RESULT_CANCELED)
        
        // 设置布局
        setContentView(R.layout.activity_widget_settings)
        
        // 获取小组件ID
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        
        // 如果没有有效的小组件ID，则直接结束
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "无效的小组件ID")
            finish()
            return
        }
        
        // 获取当前刷新频率
        val currentInterval = getRefreshInterval(this, appWidgetId)
        
        // 设置RadioGroup
        val radioGroup = findViewById<RadioGroup>(R.id.refresh_interval_group)
        
        // 根据当前设置选中相应的RadioButton
        when (currentInterval) {
            15 -> findViewById<RadioButton>(R.id.rb_15min).isChecked = true
            30 -> findViewById<RadioButton>(R.id.rb_30min).isChecked = true
            60 -> findViewById<RadioButton>(R.id.rb_60min).isChecked = true
            else -> findViewById<RadioButton>(R.id.rb_30min).isChecked = true
        }
        
        // 保存按钮点击事件
        findViewById<Button>(R.id.btn_save).setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val minutes = when (selectedId) {
                R.id.rb_15min -> 15
                R.id.rb_30min -> 30
                R.id.rb_60min -> 60
                else -> 30
            }
            
            // 保存刷新频率
            saveRefreshInterval(this, appWidgetId, minutes)
            
            // 更新小组件
            val appWidgetManager = AppWidgetManager.getInstance(this)
            SimpleAppWidget().updateAppWidget(this, appWidgetManager, appWidgetId)
            
            // 显示提示
            Toast.makeText(this, "已设置刷新间隔为 $minutes 分钟", Toast.LENGTH_SHORT).show()
            
            // 设置结果并结束
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "qzwx.app.qtodo.widgets.SimpleAppWidget"
        private const val PREF_PREFIX_KEY = "appwidget_refresh_"
        
        // 保存刷新频率
        fun saveRefreshInterval(context: Context, appWidgetId: Int, minutes: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putInt(PREF_PREFIX_KEY + appWidgetId, minutes)
            prefs.apply()
            Log.d("WidgetSettings", "已保存刷新间隔: $minutes 分钟")
        }
        
        // 获取刷新频率
        fun getRefreshInterval(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getInt(PREF_PREFIX_KEY + appWidgetId, 30)
        }
    }
}