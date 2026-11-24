package io.github.sdnick484.heyclaude.spike.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.validation.HostValidator

/** The entry point Android Auto binds to when "Claude T" is opened on the car screen. */
class SpikeCarAppService : CarAppService() {
    // Accepts any host. Acceptable for a throwaway debug build; the real app
    // must only accept the known Android Auto hosts.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(sessionInfo: SessionInfo): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = ClaudeScreen(carContext)
    }
}
