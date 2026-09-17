package com.example.activitytest.ui.main

import android.Manifest
import android.app.ComponentCaller
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.activitytest.R
import com.example.activitytest.databinding.ActivityMainBinding
import com.example.activitytest.ui.profile.ProfileFragment
import com.example.activitytest.ui.task.CompletedTaskFragment
import com.example.activitytest.ui.task.TaskListFragment
import com.example.activitytest.notification.NotificationHelper
import com.example.activitytest.util.ThemeHelper
import com.example.activitytest.util.WindowInsetsHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // 当前选中的底部导航项，用于语言切换重建后恢复到原界面
    private var selectedNavItemId: Int = R.id.menu_tasks

    // 通知权限请求器：处理用户授权/拒绝的结果
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                // 用户拒绝授权，提示一下
                Toast.makeText(
                    this,
                    R.string.notification_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyTheme(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        super.onCreate(savedInstanceState)
        Log.d(TAG, "nihao onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 顶部叠加状态栏高度；左右叠加系统栏（含横屏时的侧边导航栏），
        // 避免内容和三点菜单被状态栏或侧边导航栏遮挡
        WindowInsetsHelper.applySystemBarPadding(
            binding.fragmentContainer,
            applyTop = true,
            applyBottom = false
        )
        // 底部导航条本身要叠加底部系统导航栏高度，避免被遮挡或误触
        WindowInsetsHelper.applySystemBarPadding(
            binding.bottomNavigation,
            applyTop = false,
            applyBottom = true
        )

        // 恢复之前选中的底部导航项（语言切换重建时保持原页面）
        if (savedInstanceState != null) {
            selectedNavItemId = savedInstanceState.getInt(
                STATE_SELECTED_NAV,
                R.id.menu_tasks
            )
        } else {
            selectedNavItemId = intent.getIntExtra(
                EXTRA_SELECTED_NAV,
                R.id.menu_tasks
            )
        }

        NotificationHelper.createNotificationChannel(this)

        // 申请通知权限（仅 Android 13+ 需要）
        requestNotificationPermission()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            selectedNavItemId = item.itemId
            showFragmentForNav(item.itemId)
        }

        // 设置底部导航选中项：首次启动默认任务页；
        // 语言切换重启时，selectedNavItemId 已从 Intent 恢复为原页面
        binding.bottomNavigation.selectedItemId = selectedNavItemId

        // 若选中项与默认相同导致 listener 未触发，则手动显示对应 Fragment
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            showFragmentForNav(selectedNavItemId)
        }
        Log.d(TAG, "nihao onCreate")
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "nihao onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "nihao onResume")
    }

    override fun onPause() {
        Log.d(TAG, "nihao onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "nihao onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "nihao onDestroy")
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_NAV, selectedNavItemId)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val TAG = "MainActivity"
        const val STATE_SELECTED_NAV = "state_selected_nav"
        const val EXTRA_SELECTED_NAV = "extra_selected_nav"
    }

    /**
     * 语言切换后彻底重启 MainActivity。
     * 使用 CLEAR_TASK 清空任务栈，确保所有 Fragment 与 BottomNavigationView
     * 都基于新语言重新创建，避免恢复旧实例导致界面语言不刷新。
     */
    fun restartForLanguage() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
            putExtra(EXTRA_SELECTED_NAV, selectedNavItemId)
        }
        startActivity(intent)
        finish()
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // 显示Fragment
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()    //同步保存，会返回Boolean结果
    }

    // 根据底部导航项 id 显示对应 Fragment
    private fun showFragmentForNav(itemId: Int): Boolean {
        val fragment = when (itemId) {
            R.id.menu_tasks -> TaskListFragment()
            R.id.menu_completed -> CompletedTaskFragment()
            R.id.menu_profile -> ProfileFragment()
            else -> return false
        }
        showFragment(fragment)
        return true
    }
}