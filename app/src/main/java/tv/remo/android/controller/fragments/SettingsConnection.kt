package tv.remo.android.controller.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.preference.EditTextPreference
import okhttp3.*
import org.json.JSONObject
import tv.remo.android.controller.R
import tv.remo.android.controller.sdk.RemoSettingsUtil
import tv.remo.android.controller.sdk.utils.EndpointBuilder
import tv.remo.android.settingsutil.fragments.BasePreferenceFragmentCompat
import java.io.IOException


/**
 * Connection Settings
 *
 * Contains robotId, cameraId, streamKey
 */

class SettingsConnection : BasePreferenceFragmentCompat(
    R.xml.settings_connection
){
    var refreshNeeded = false
    lateinit var handler : Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        handler = Handler(Looper.getMainLooper())
    }

    override fun onResume() {
        super.onResume()
        maybeRefresh()
        preferenceManager.findPreference<EditTextPreference>(getString(R.string.connectionApiTokenKey))?.
            setOnPreferenceChangeListener { _, data ->
                AlertDialog.Builder(context).also {
                    it.setTitle("Robot key updated")
                    it.setMessage("Would you like to verify and auto fill the other fields? \n" +
                            "This will validate your token with remo.tv")
                    it.setPositiveButton("yes"){ dialog, which ->
                        validateRobotJWT(data as? String)
                    }
                    it.setNegativeButton("no"){ dialog, which ->
                        //do nothing. It will dismiss automagically
                    }
                }.create().show()
                true
            }
    }

    private fun forceRefresh() {
        refreshNeeded = true
        maybeRefresh()
    }

    private fun maybeRefresh() {
        if(refreshNeeded){
            preferenceScreen = null
            addPreferencesFromResource(R.xml.settings_connection)
            refreshNeeded = false
        }
    }

    private fun validateRobotJWT(data : String?) {
        context?.let {
            data ?: run{
                Toast.makeText(it, "Unable to validate empty field", Toast.LENGTH_SHORT).show()
                return
            }
            val endpoint = "${EndpointBuilder.getBaseApiEndpoint(it)}/robot/auth"
            val client = OkHttpClient().newBuilder()
                .build()
            val mediaType = MediaType.parse("application/json")
            val body = RequestBody.create(
                mediaType,
                "{\"token\": \"${data}\"}"
            )
            val request: Request = Request.Builder()
                .url(endpoint)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    handler.post {
                        Toast.makeText(it, "Network error or server error", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try{
                        var message = response.body()?.string()
                        Log.d("MESSAGE", message)
                        JSONObject(message).also { json ->
                            handler.post {
                                if(json["status"].toString() == "success!"){
                                    Toast.makeText(it, "Token validated", Toast.LENGTH_SHORT).show()
                                    setConnectionInfo(json)
                                }
                                else{
                                    Toast.makeText(it, "Token invalid", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                        handler.post {
                            Toast.makeText(it, "Token invalid", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    /**
     * Auto update any fields we can from the authorization
     */
    private fun setConnectionInfo(json: JSONObject) {
        RemoSettingsUtil.with(requireContext()).channelId.savePref(json.getJSONObject("robot")["id"].toString())
        forceRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.connection_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.webServerSelectMenuItem -> {
                refreshNeeded = true
                Navigation.findNavController(view!!)
                    .navigate(R.id.action_settingsConnection_to_webServerSettingsPage)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}