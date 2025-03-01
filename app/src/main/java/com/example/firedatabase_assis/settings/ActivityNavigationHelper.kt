package com.example.firedatabase_assis.settings

class ActivityNavigationHelper {
    companion object {
        // Store the stack of visited settings activities
        private val settingsActivityStack = mutableListOf<Class<*>>()

        fun setLastOpenedSettingsActivity(activityClass: Class<*>) {
            // If this activity is already at the top of the stack, don't add it again
            if (settingsActivityStack.isNotEmpty() && settingsActivityStack.last() == activityClass) {
                return
            }

            // Add to the stack
            settingsActivityStack.add(activityClass)

            // Keep the stack at a reasonable size (optional)
            if (settingsActivityStack.size > 10) {
                settingsActivityStack.removeAt(0)
            }
        }

        fun getLastOpenedSettingsActivity(): Class<*>? {
            return if (settingsActivityStack.isNotEmpty()) {
                settingsActivityStack.last()
            } else {
                null
            }
        }

        fun removeLastOpenedSettingsActivity() {
            if (settingsActivityStack.isNotEmpty()) {
                settingsActivityStack.removeAt(settingsActivityStack.size - 1)
            }
        }

        fun clearSettingsActivityStack() {
            settingsActivityStack.clear()
        }
    }
}