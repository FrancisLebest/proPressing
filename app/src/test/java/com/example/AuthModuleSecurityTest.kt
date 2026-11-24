package com.example

import com.example.data.AfricanDialCodes
import com.example.data.AuthErrorMapper
import com.example.data.AuthInputValidator
import com.example.data.AuthRateLimiter
import com.example.data.EnterpriseRegistrationDraft
import com.example.data.PasswordStrengthLevel
import com.example.data.SystemRoleTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated security and validation test suite for GESTION PRO AFRICA Authentication module (Section 18).
 */
class AuthModuleSecurityTest {

    @Test
    fun emailValidation_acceptsValidAndRejectsInvalid() {
        assertNull(AuthInputValidator.validateEmail("direction@pressing-elite.ci"))
        assertNotNull(AuthInputValidator.validateEmail(""))
        assertNotNull(AuthInputValidator.validateEmail("sans-arobase.ci"))
        assertNotNull(AuthInputValidator.validateEmail("esp ace@pressing.ci"))
    }

    @Test
    fun phoneNormalization_handlesIvoryCoastAndAfricanDialCodesToE164() {
        assertEquals("+225", AfricanDialCodes.defaultCountry.dialCode)
        assertNull(AuthInputValidator.validatePhone("+225", "07 58 42 19 63"))
        assertEquals("+2250758421963", AuthInputValidator.normalizePhoneToE164("+225", "07 58 42 19 63"))
        assertEquals("+2250758421963", AuthInputValidator.normalizePhoneToE164("+225", "+225 07 58 42 19 63"))
        assertEquals("+221771234567", AuthInputValidator.normalizePhoneToE164("+221", "77 123 45 67"))
        assertNotNull(AuthInputValidator.validatePhone("+225", "1234"))
    }

    @Test
    fun usernameValidation_acceptsCleanLoginAndRejectsMalformed() {
        assertNull(AuthInputValidator.validateUsername("pressing.cocody"))
        assertNull(AuthInputValidator.validateUsername("Admin_CI-01"))
        assertEquals("usr.pressing.cocody@pressingpro.africa", AuthInputValidator.usernameToAuthIdentifier("Pressing.Cocody"))
        assertNotNull(AuthInputValidator.validateUsername("ab"))
        assertNotNull(AuthInputValidator.validateUsername("bad user"))
        assertNotNull(AuthInputValidator.validateUsername(".leadingdot"))
    }

    @Test
    fun passwordStrength_enforcesComplexityAndConfirmation() {
        val weak = AuthInputValidator.evaluatePassword("abc")
        assertEquals(PasswordStrengthLevel.WEAK, weak.level)
        assertFalse(weak.isAcceptable)

        val strong = AuthInputValidator.evaluatePassword("Pressing2026@CI")
        assertTrue(strong.isAcceptable)
        assertTrue(strong.level == PasswordStrengthLevel.STRONG || strong.level == PasswordStrengthLevel.VERY_STRONG)

        val mismatchErrors = AuthInputValidator.validateRegistration(
            fullName = "Kouassi Yao",
            email = "kouassi@pressing.ci",
            password = "Pressing2026@CI",
            confirmPassword = "DifferentPassword1!",
            acceptedTerms = true
        )
        assertTrue(mismatchErrors.any { it.contains("correspondent pas", ignoreCase = true) })
    }

    @Test
    fun publicRegistration_requiresLegalConsentAndLocksRoleToPressingAdmin() {
        val draft = EnterpriseRegistrationDraft(
            companyName = "Pressing Élite CI",
            adminFullName = "Kouassi Yao",
            adminEmail = "kouassi@pressing.ci"
        )
        assertEquals(SystemRoleTier.PRESSING_ADMIN, draft.enforcedSystemRole)

        val noConsentErrors = AuthInputValidator.validateUsernameRegistration(
            fullName = "Kouassi Yao",
            rawUsername = "kouassi.ci",
            password = "Pressing2026@CI",
            confirmPassword = "Pressing2026@CI",
            acceptedTerms = false
        )
        assertTrue(noConsentErrors.any { it.contains("Conditions", ignoreCase = true) })
    }

    @Test
    fun antiEnumerationAndRateLimiter_protectLoginEndpoints() {
        val wrongPwdMsg = AuthErrorMapper.mapFirebaseErrorCode("ERROR_WRONG_PASSWORD")
        val notFoundMsg = AuthErrorMapper.mapFirebaseErrorCode("ERROR_USER_NOT_FOUND")
        assertEquals(wrongPwdMsg, notFoundMsg)

        val limiter = AuthRateLimiter(maxFailures = 5, cooldownMillis = 30_000L)
        val t0 = 1_000_000L
        repeat(4) { limiter.recordFailure(t0) }
        assertEquals(0, limiter.remainingCooldownSeconds(t0))
        limiter.recordFailure(t0)
        assertEquals(30, limiter.remainingCooldownSeconds(t0))
        assertEquals(0, limiter.remainingCooldownSeconds(t0 + 31_000L))
        limiter.recordSuccess()
        assertEquals(0, limiter.remainingCooldownSeconds(t0))
    }
}
