package org.secuso.privacyfriendlybackup.api.backup

import android.content.SharedPreferences
import android.util.JsonWriter

/**
 * This is a convenience class, that provides utility methods to write preferences to json.
 * @author Christopher Beckmann (Kamuno)
 */
object PreferenceUtil {
    @JvmStatic
    @JvmOverloads
    fun writePreferences(writer: JsonWriter, pref: SharedPreferences, excludedKeys: Array<String> = emptyArray()) {
        writer.beginObject()
        for((key, value) in pref.all) {
            // Continue if key-value pair should be excluded from the backup
            if(key in excludedKeys) {
                continue
            }

            writer.name(key)

            // App should know the types of the keys when it sees them.
            // So only store the key value pairs.
            when(value) {
                is String -> writer.value(value)
                is Int -> writer.value(value)
                is Boolean -> writer.value(value)
                is Float -> writer.value(value)
                is Long -> writer.value(value)
                is Set<*> -> {
                    writer.beginArray()
                    for(v in value) {
                        writer.value(v as String)
                    }
                    writer.endArray()
                }
            }
        }
        writer.endObject()
    }
}