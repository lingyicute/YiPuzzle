package org.secuso.privacyfriendlybackup.api.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.security.NoSuchAlgorithmException

/**
 * @author Christopher Beckmann
 */
object AuthenticationHelper {

    fun getPackageName(context: Context, uid: Int) : String? {
        val names = getPackageNames(context, uid)
        return if(names.isNotEmpty()) {
            names[0]
        } else {
            null
        }
    }

    fun getPackageNames(context: Context, uid: Int) : Array<String> {
        val packageNameArray = context.packageManager.getPackagesForUid(uid)

        if (packageNameArray == null || packageNameArray.isEmpty()) {
            return emptyArray()
        }

        return packageNameArray
    }

    @Throws(IOException::class, JSONException::class)
    fun loadValidSignatures(context: Context, file : String = "Signatures.json") : List<String>  {
        val validSignatures : MutableList<String> = ArrayList()

        //var jsonArray = JSONArray(context.assets.open(context.assets.list("")!![0]).readJson())
        var signatureJson = JSONArray(context.assets.open(file).readString())
        for(i in 0 until signatureJson.length()) {
            val sig = signatureJson.get(i) as String
            validSignatures.add(sig)
        }

        return validSignatures
    }

    /**
     * Test if the given uid is in the list of valid signatures.
     *
     * @param context The context with which the packageManager is loaded and assets are loaded
     * @param uid The id of the process being authenticated
     * @param validSignaturesParam Optional - custom list of valid signatures given as hex strings (see {@link bytesToHex(ByteArray)})
     *
     * @return true if uid is authorized or false if not
     */
    fun authenticate(context: Context, uid: Int, validSignaturesParam : List<String> = emptyList()): Boolean {
        // if no uid is sent - we can not authenticate
        if (uid == -1)
            return false

        val packageNameArray = getPackageNames(context, uid)
        if(packageNameArray.isEmpty())
            return false

        // if we are binding our own service, always grant access
        if(packageNameArray[0] == context.packageName) {
            return true
        }

        val validSignatures = ArrayList(validSignaturesParam)
        if(validSignatures.isEmpty()) {
            try {
                validSignatures.addAll(loadValidSignatures(context))
            } catch (e : IOException) {
                return false
            } catch (e : JSONException) {
                return false
            }
        }

        for (packageName in packageNameArray) {
            val sigs = getSignatures(context, packageName)
            if (sigs.isEmpty())
                return false

            for(sig in sigs) {
                if(!validSignatures.contains(sig)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Returns the signatures for the given packageName encoded as Base64 or an empty list if no signatures were found
     *
     * @param context The context that is used to query the package manager
     * @param packageName the package name for which the signatures are being loaded
     */
    @SuppressLint("PackageManagerGetSignatures")
    private fun getSignatures(context: Context, packageName: String): List<String> {
        try {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val sig = context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo

                if (sig.hasMultipleSigners()) {
                    sig.apkContentsSigners.map { it.toHex() }
                } else {
                    sig.signingCertificateHistory.map { it.toHex() }
                }
            } else {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                ).signatures.map { it.toHex() }
            }

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return emptyList()
    }
}


