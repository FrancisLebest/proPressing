package com.example

import com.example.data.AdminBootstrap
import com.example.data.AdminBootstrapCredentialSource
import com.example.data.AdminBootstrapResult
import com.example.data.AdminProvisioningManager
import com.example.data.AdminRecordBootstrapResult
import com.example.data.AfricanDialCodes
import com.example.data.AuthErrorMapper
import com.example.data.AuthInputValidator
import com.example.data.AuthRateLimiter
import com.example.data.CompanyConfig
import com.example.data.EnterpriseRegistrationDraft
import com.example.data.GeniusPayRuntimeConfig
import com.example.data.GeniusPayUrlValidator
import com.example.data.GeniusPayWebhookSignature
import com.example.data.PasswordStrengthLevel
import com.example.data.PlatformSuperAdminConfig
import com.example.data.RoleSessionResolver
import com.example.data.SubscriptionPaymentStatus
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionRenewalPricing
import com.example.data.SystemRoleTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Automated security and validation test suite for GESTION PRO AFRICA Authentication & GeniusPay module.
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
        assertEquals(5, strong.score)
        assertEquals(PasswordStrengthLevel.VERY_STRONG, strong.level)

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

    @Test
    fun platformSuperAdmin_isExclusivelyBoundToConfiguredEmail() {
        assertTrue(PlatformSuperAdminConfig.isPlatformSuperAdminEmail("lebest2london@gmail.com"))
        assertTrue(PlatformSuperAdminConfig.isPlatformSuperAdminEmail("best2london@gmail.com"))
        assertTrue(PlatformSuperAdminConfig.isPlatformSuperAdminEmail("  LEBEST2LONDON@GMAIL.COM "))
        assertFalse(PlatformSuperAdminConfig.isPlatformSuperAdminEmail("directeur@pressing.ci"))

        val company = CompanyConfig(companyName = "Pressing Élite", email = "directeur@pressing.ci")
        val superAdminPrimary = RoleSessionResolver.resolveForUser(
            userEmail = "lebest2london@gmail.com",
            userPhone = "",
            userDisplayName = "Super Admin",
            company = company,
            employees = emptyList()
        )
        assertTrue(superAdminPrimary.isSuperAdmin)

        val superAdminSecondary = RoleSessionResolver.resolveForUser(
            userEmail = "best2london@gmail.com",
            userPhone = "",
            userDisplayName = "Super Admin",
            company = company,
            employees = emptyList()
        )
        assertTrue(superAdminSecondary.isSuperAdmin)

        val regularOwner = RoleSessionResolver.resolveForUser(
            userEmail = "directeur@pressing.ci",
            userPhone = "",
            userDisplayName = "Kouassi Yao",
            company = company,
            employees = emptyList()
        )
        assertFalse(regularOwner.isSuperAdmin)
    }

    @Test
    fun geniusPayPricingUrlAllowlistAndWebhookHmac_enforceSecurityAndTotals() {
        val monthlyBiz = SubscriptionRenewalPricing.computeQuote(SubscriptionPlan.BUSINESS, "MONTHLY")
        assertEquals(10_000L, monthlyBiz.finalAmountFcfa)

        val quarterlyBiz = SubscriptionRenewalPricing.computeQuote(SubscriptionPlan.BUSINESS, "QUARTERLY")
        assertEquals(27_000L, quarterlyBiz.finalAmountFcfa)

        val annualEnterprise = SubscriptionRenewalPricing.computeQuote(SubscriptionPlan.ENTERPRISE, "ANNUAL")
        assertEquals(240_000L, annualEnterprise.finalAmountFcfa)

        assertTrue(GeniusPayUrlValidator.isAllowedBaseUrl("https://geniuspay.ci/api/v1/merchant"))
        assertFalse(GeniusPayUrlValidator.isAllowedBaseUrl("http://geniuspay.ci/api/v1/merchant"))
        assertFalse(GeniusPayUrlValidator.isAllowedBaseUrl("https://evil.example.com/api"))
        assertTrue(GeniusPayUrlValidator.isSafeCheckoutUrl("https://pay.genius.ci/checkout/abc123"))
        assertFalse(GeniusPayUrlValidator.isSafeCheckoutUrl("javascript:alert(1)"))

        val placeholderCfg = GeniusPayRuntimeConfig(
            apiKey = "GENIUSPAY_API_KEY_PLACEHOLDER",
            apiSecret = "GENIUSPAY_API_SECRET_PLACEHOLDER",
            mode = "live",
            baseUrl = "https://geniuspay.ci/api/v1/merchant"
        )
        assertFalse(placeholderCfg.isConfigured)

        val samplePayload = """{"event":"payment.completed","data":{"reference":"CHK_12345","status":"completed"}}"""
        val sampleSecret = "test_webhook_secret_only"
        val validSig = GeniusPayWebhookSignature.computeHmacSha256Hex(samplePayload, sampleSecret)
        assertTrue(GeniusPayWebhookSignature.verifyHmacSha256(samplePayload, "sha256=$validSig", sampleSecret))
        assertFalse(GeniusPayWebhookSignature.verifyHmacSha256(samplePayload, "sha256=deadbeef", sampleSecret))
        assertEquals(SubscriptionPaymentStatus.COMPLETED, SubscriptionPaymentStatus.fromGeniusPayRaw("paid"))
    }

    @Test
    fun adminProvisioningPolicy_blocksReleaseCredentialsAndRejectsUnauthorizedEmails() {
        val releaseSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = false
            override fun superAdminEmail(): String = "best2london@gmail.com"
            override fun superAdminPassword(): String = "SyntheticStrongTest#2026!"
        }
        assertEquals(
            AdminBootstrapResult.SkippedInProductionBuild,
            AdminProvisioningManager(credentialSource = releaseSource).evaluatePreFlightPolicy()
        )

        val missingDebugCredsSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = true
            override fun superAdminEmail(): String = ""
            override fun superAdminPassword(): String = ""
        }
        assertEquals(
            AdminBootstrapResult.SkippedMissingDebugCredentials,
            AdminProvisioningManager(credentialSource = missingDebugCredsSource).evaluatePreFlightPolicy()
        )

        val unauthorizedEmailSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = true
            override fun superAdminEmail(): String = "attacker@example.com"
            override fun superAdminPassword(): String = "SyntheticStrongTest#2026!"
        }
        val unauthorizedResult = AdminProvisioningManager(credentialSource = unauthorizedEmailSource).evaluatePreFlightPolicy()
        assertTrue(unauthorizedResult is AdminBootstrapResult.UnauthorizedEmailRejected)

        val weakPasswordSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = true
            override fun superAdminEmail(): String = "lebest2london@gmail.com"
            override fun superAdminPassword(): String = "weak"
        }
        val weakResult = AdminProvisioningManager(credentialSource = weakPasswordSource).evaluatePreFlightPolicy()
        assertTrue(weakResult is AdminBootstrapResult.Error)

        val validPrimaryAdminSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = true
            override fun superAdminEmail(): String = "lebest2london@gmail.com"
            override fun superAdminPassword(): String = "SyntheticStrongTest#2026!"
        }
        assertNull(AdminProvisioningManager(credentialSource = validPrimaryAdminSource).evaluatePreFlightPolicy())

        val validSecondaryAdminSource = object : AdminBootstrapCredentialSource {
            override fun isClientBootstrapAllowed(): Boolean = true
            override fun superAdminEmail(): String = "best2london@gmail.com"
            override fun superAdminPassword(): String = "SyntheticStrongTest#2026!"
        }
        assertNull(AdminProvisioningManager(credentialSource = validSecondaryAdminSource).evaluatePreFlightPolicy())
    }

    @Test
    fun adminBootstrap_enforcesDebugFlagAndBuildsSchemaAlignedPayload() {
        // Production release flag (false) must always skip execution even for an authorized Super-Admin
        assertEquals(
            AdminRecordBootstrapResult.SkippedInProductionBuild,
            AdminBootstrap.evaluatePreFlightGuard(
                allowClientBootstrap = false,
                currentUserUid = "super_uid_1",
                currentUserEmail = "best2london@gmail.com"
            )
        )

        // Unauthenticated or non-super-admin session must be skipped even in debug mode
        assertEquals(
            AdminRecordBootstrapResult.SkippedUnauthorizedSession,
            AdminBootstrap.evaluatePreFlightGuard(
                allowClientBootstrap = true,
                currentUserUid = null,
                currentUserEmail = null
            )
        )
        assertEquals(
            AdminRecordBootstrapResult.SkippedUnauthorizedSession,
            AdminBootstrap.evaluatePreFlightGuard(
                allowClientBootstrap = true,
                currentUserUid = "tenant_uid",
                currentUserEmail = "directeur@pressing.ci"
            )
        )

        // Authorized Super-Admin in debug build passes pre-flight guard
        assertNull(
            AdminBootstrap.evaluatePreFlightGuard(
                allowClientBootstrap = true,
                currentUserUid = "super_uid_1",
                currentUserEmail = "best2london@gmail.com"
            )
        )

        // Verify metadata payload keys align 1:1 with firestore.rules /admins/{adminId}
        val payload = AdminBootstrap.buildAdminDocumentPayload(createdByUid = "super_uid_1")
        assertEquals("best2london@gmail.com", payload["email"])
        assertEquals("SUPER_ADMIN", payload["role"])
        assertEquals(true, payload["active"])
        assertEquals("CLIENT_DEBUG_BOOTSTRAP", payload["provider"])
        assertEquals("super_uid_1", payload["createdByUid"])
        assertEquals(
            setOf(
                "email", "displayName", "role", "active", "provider",
                "bootstrapSource", "createdByUid", "createdAt", "updatedAt"
            ),
            payload.keys
        )
    }
}

