package com.example.callscreeningapp

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var requestRoleLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // Assuming a layout will be added later

        requestRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                println("Call screening role granted!")
            } else {
                println("Call screening role denied.")
            }
        }

        checkAndRequestCallScreeningRole()
    }

    private fun checkAndRequestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(ROLE_SERVICE) as RoleManager
            val isCallScreeningApp = roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

            if (!isCallScreeningApp) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                requestRoleLauncher.launch(intent)
            } else {
                println("App is already the default call screening app.")
            }
        } else {
            println("CallScreeningService is not available on this Android version.")
        }
    }
}