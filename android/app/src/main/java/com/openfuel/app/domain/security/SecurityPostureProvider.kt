package com.openfuel.app.domain.security

import com.openfuel.app.domain.model.SecurityPosture
import kotlinx.coroutines.flow.Flow

interface SecurityPostureProvider {
    fun posture(): Flow<SecurityPosture>
}
