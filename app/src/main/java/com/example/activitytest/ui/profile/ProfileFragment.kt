package com.example.activitytest.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.activitytest.util.MoreMenuItem
import com.example.activitytest.util.MoreMenuPopup
import com.example.activitytest.R
import com.example.activitytest.databinding.FragmentProfileBinding
import com.example.activitytest.service.SyncService
import com.example.activitytest.util.MenuDialogHelper
import com.example.activitytest.util.ThemeHelper
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_NICKNAME = "nickname"
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val syncReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            if (
                intent?.action ==
                SyncService.ACTION_SYNC_FINISHED
            ) {
                val message = intent.getStringExtra(
                    SyncService.EXTRA_SYNC_MESSAGE
                ) ?: getString(R.string.sync_completed)

                binding.progressSync.visibility = View.GONE
                binding.btnSync.isEnabled = true
                binding.tvSyncStatus.text = message

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SyncService.ACTION_SYNC_FINISHED)
        ContextCompat.registerReceiver(
            requireContext(), syncReceiver, filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(syncReceiver)
    }


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        com.example.activitytest.util.WindowInsetsHelper.applySystemBarPadding(
            binding.root,
            applyTop = false,
            applyBottom = false
        )

        setupMoreMenu()

        val preferences = requireContext()
            .getSharedPreferences(
                PREF_NAME,
                android.content.Context.MODE_PRIVATE
            )

        val nickname = preferences.getString(
            KEY_NICKNAME,
            getString(R.string.default_nickname)
        )

        binding.etNickname.setText(nickname)

        // 显示欢迎语
        binding.tvWelcome.text = getString(R.string.welcome_message, nickname)

        binding.btnSync.setOnClickListener {
            binding.progressSync.visibility = View.VISIBLE
            binding.tvSyncStatus.text = getString(R.string.syncing)
            binding.btnSync.isEnabled = false

            val intent = Intent(requireContext(), SyncService::class.java)
            requireContext().startService(intent)
        }

        binding.btnSaveNickname.setOnClickListener {
            val newNickname = binding.etNickname.text
                ?.toString()
                ?.trim()
                .orEmpty()

            if (newNickname.isBlank()) {
                binding.etNickname.error =
                    getString(R.string.nickname_empty_error)
                return@setOnClickListener
            }

            preferences.edit()
                .putString(KEY_NICKNAME, newNickname)
                .apply()

            // 保存后更新欢迎语
            binding.tvWelcome.text =
                getString(R.string.welcome_message, newNickname)

            Toast.makeText(
                requireContext(),
                R.string.nickname_saved,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupMoreMenu() {
        binding.btnMore.setOnClickListener { anchor ->
            MoreMenuPopup.show(
                anchor,
                listOf(
                    MoreMenuItem(
                        R.id.menu_theme,
                        R.string.menu_theme,
                        R.drawable.ic_theme
                    ),
                    MoreMenuItem(
                        R.id.menu_settings,
                        R.string.menu_settings,
                        R.drawable.ic_settings
                    ),
                    MoreMenuItem(
                        R.id.menu_about,
                        R.string.menu_about,
                        R.drawable.ic_about
                    )
                )
            ) { itemId ->
                when (itemId) {
                    R.id.menu_theme -> showThemePicker()
                    R.id.menu_settings -> showSettingsDialog()
                    R.id.menu_about -> showAboutDialog()
                }
            }
        }
    }

    private fun showThemePicker() {
        val contentView = layoutInflater.inflate(
            R.layout.dialog_theme_picker,
            null,
            false
        )
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.theme_title)
            .setView(contentView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        fun selectTheme(theme: String) {
            ThemeHelper.saveTheme(requireContext(), theme)
            dialog.dismiss()
            requireActivity().recreate()
        }

        contentView.findViewById<MaterialButton>(R.id.btnThemeNavy)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_NAVY) }
        contentView.findViewById<MaterialButton>(R.id.btnThemeGreen)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_GREEN) }
        contentView.findViewById<MaterialButton>(R.id.btnThemePurple)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_PURPLE) }
        contentView.findViewById<MaterialButton>(R.id.btnThemePink)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_PINK) }
        contentView.findViewById<MaterialButton>(R.id.btnThemeSkyBlue)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_SKYBLUE) }
        contentView.findViewById<MaterialButton>(R.id.btnThemeYellow)
            .setOnClickListener { selectTheme(ThemeHelper.THEME_YELLOW) }

        dialog.show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_title)
            .setItems(
                arrayOf(
                    getString(R.string.settings_language),
                    getString(R.string.settings_clear_data),
                    getString(R.string.settings_version)
                )
            ) { _, which ->
                when (which) {
                    0 -> showLanguagePicker()
                    1 -> confirmClearData()
                    2 -> showVersionDialog()
                }
            }
            .show()
    }

    private fun showVersionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_version)
            .setMessage(R.string.current_version)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showLanguagePicker() {
        MenuDialogHelper.showLanguageDialog(
            requireActivity()
        ) {
            // 通过 MainActivity 彻底重启，确保导航栏与当前页面都按新语言重建
            (requireActivity() as? com.example.activitytest.ui.main.MainActivity)
                ?.restartForLanguage()
        }
    }

    private fun confirmClearData() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_clear_data)
            .setMessage(R.string.settings_clear_data_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Toast.makeText(
                    requireContext(),
                    R.string.data_cleared,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}