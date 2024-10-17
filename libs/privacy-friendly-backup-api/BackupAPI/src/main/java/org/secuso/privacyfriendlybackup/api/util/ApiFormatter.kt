package org.secuso.privacyfriendlybackup.api.util

import android.content.Intent

/**
 * @author Christopher Beckmann
 */
object ApiFormatter {

    fun formatIntent(intent: Intent?) : String {
        if(intent == null) {
            return "\n[null]"
        }

        val sb = StringBuilder("\n[Intent:\n")

        // action
        sb.append("\tAction:\t")
        if(intent.action.isNullOrEmpty()) {
            sb.append("No Action.")
        } else {
            sb.append("${intent.action}")
        }
        sb.append('\n')

        // extras
        val keySet = intent.extras?.keySet()
        if(!keySet.isNullOrEmpty()) {
            for (key in keySet) {
                sb.append("\tExtra:\t$key:\t${intent.extras!!.get(key)}\n")
            }
        }

        sb.append("]")
        return sb.toString()
    }

}