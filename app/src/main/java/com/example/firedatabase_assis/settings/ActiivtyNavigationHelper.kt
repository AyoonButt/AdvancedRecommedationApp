package com.example.firedatabase_assis.settings

class ActivityNavigationHelper {
    companion object {
        // Store the last opened settings-related activity
        private var lastOpenedSettingsActivity: Class<*>? = null

        // Store the origin activity for back navigation
        private var settingsOriginActivity: Class<*>? = null

        fun setLastOpenedSettingsActivity(activityClass: Class<*>) {
            lastOpenedSettingsActivity = activityClass
        }

        fun getLastOpenedSettingsActivity(): Class<*>? {
            return lastOpenedSettingsActivity
        }

        fun setSettingsOriginActivity(activityClass: Class<*>) {
            settingsOriginActivity = activityClass
        }

        fun getSettingsOriginActivity(): Class<*>? {
            return settingsOriginActivity
        }

        fun clearSettingsOriginActivity() {
            settingsOriginActivity = null
        }
    }
}