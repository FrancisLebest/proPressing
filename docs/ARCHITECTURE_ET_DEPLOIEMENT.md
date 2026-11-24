# GESTION PRO AFRICA — Pressing Pro Mobile
## Architecture Technique, Sécurité RBAC+ABAC & Guide de Déploiement

---

## 1. Source de Vérité & Stack Technique

- **Client mobile** : Android natif **Kotlin + Jetpack Compose + Material Design 3** (Clean Architecture + MVVM + StateFlow).
- **Base de données & temps réel** : **Cloud Firestore** (isolation multi-tenant par `users/{uid}/...` et `agencyName`).
- **Cache hors-ligne & reprise réseau** : **Room** (`PressingLocalDatabase`, `OfflineSyncRepository`) avec file d'attente locale pour les commandes créées en coupure Internet et synchronisation automatique au retour réseau (`NetworkConnectivityObserver`).
- **Notifications Push** : **Firebase Cloud Messaging (FCM)** (`PressingFirebaseMessagingService` + `PressingNotificationManager`) avec canaux Android dédiés (nouvelles commandes caissiers, vêtements prêts clients, stocks bas, validations).
- **Sécurité applicative** : **Firebase Auth**, **Firebase App Check**, `firestore.rules`, `storage.rules`, et moteur RBAC+ABAC (`AuthorizationEngine` + `AtomicPermissionRegistry` v2026.1.0 — 143 permissions atomiques sur 19 modules).
- **Vision & QR** : **CameraX + ML Kit Barcode Scanning** (`QrBarcodeScannerDialog`) + **Firebase AI Logic (Gemini)** (`AiRepository`) pour l'analyse de taches/tissus et le conseil pressing.

---

## 2. Modèle de Sécurité Multi-Tenant (RBAC + ABAC)

### Rôles système & périmètres
1. **SUPER_ADMIN** : Administration globale SaaS (forfaits, supervision, sessions d'assistance en lecture seule avec ticket, motif obligatoire, expiration et révocation immédiate). Aucun accès silencieux aux données commerciales des pressings.
2. **PRESSING_ADMIN (Directeur Général)** : Contrôle complet de son entreprise multi-agences, comptabilité SYSCOHADA, RH/salaires, configuration des profils gestionnaires, validation administrative.
3. **PRESSING_MANAGER (Gérant, Caissier, Opérateur Atelier, Contrôle Qualité, Livreur)** : Permissions granulaires par profil (6 profils prédéfinis + Personnalisé), restriction stricte aux agences assignées, horaires d'accès, plafonds financiers (remise 0–30 %, remboursement, dépense, décaissement, plafond cumulé journalier anti-fractionnement) et **DENY explicite prioritaire**.
4. **CLIENT_PORTAL** : Accès isolé par jeton non prédictible (`TRK-XXXX-XXXX-XXXX`) sans exposition des notes internes ni des données des autres clients.

### Workflow de validation administrative
- Toute opération dépassant un plafond gestionnaire crée une demande `ApprovalRequestRecord` scellée par empreinte cryptographique (`payloadHash`).
- **Séparation des responsabilités** : le demandeur ne peut jamais approuver sa propre demande.
- **Exécution unique** : une demande exécutée ne peut pas être rejouée.

---

## 3. Intégrations Externes Nécessitant des Identifiants Commerçants

Les encaissements **Wave, Orange Money, MTN MoMo et Moov Money** sont prêts côté modèle comptable, reçu thermique et suivi de référence de transaction. Pour activer l'encaissement automatisé avec vérification par webhook serveur en production :

| Prestataire | Variables serveur (Cloud Functions / Secrets) | Action requise |
|---|---|---|
| **Wave Business** | `WAVE_API_KEY`, `WAVE_WEBHOOK_SECRET` | Configurer l'URL webhook dans le portail Wave Business |
| **Orange Money CI/SN** | `OM_CLIENT_ID`, `OM_CLIENT_SECRET`, `OM_MERCHANT_KEY` | Activer l'API WebPayment Orange Developer |
| **MTN MoMo** | `MOMO_SUBSCRIPTION_KEY`, `MOMO_API_USER`, `MOMO_API_KEY` | Générer les identifiants Collection API |
| **Moov Money** | `MOOV_MERCHANT_ID`, `MOOV_API_SECRET` | Demander l'accès passerelle marchand Moov |

> **Règle de sécurité** : Ne jamais intégrer les secrets marchands dans l'APK Android. Toute confirmation de paiement mobile en production doit être validée par webhook signé côté serveur.

---

## 4. Compilation, Tests & Publication Google Play (APK / AAB)

### Vérification locale & CI
- Compilation Debug : `gradle :app:assembleDebug`
- Tests de sécurité RBAC+ABAC (30 scénarios) :
  `gradle :app:testDebugUnitTest --tests "com.example.ExampleUnitTest" --tests "com.example.RbacAbacSecurityTest"`
- Bundle Google Play Release (AAB) : `gradle :app:bundleRelease`
- Déploiement des règles Firestore : `firebase deploy --only firestore:rules,firestore:indexes,storage`

### Checklist Google Play
- [x] Icône adaptative personnalisée GESTION PRO AFRICA
- [x] Edge-to-edge activé (`enableEdgeToEdge()`) + respect des `WindowInsets`
- [x] Photo Picker sans permission stockage large (`PickVisualMedia`)
- [x] Permission `POST_NOTIFICATIONS` demandée uniquement à la demande sur Android 13+
- [x] Thèmes Clair, Sombre et Système + zones tactiles >= 48 dp
- [x] Zéro secret ou clé privée dans l'APK
