package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "PressingAiRepo"

data class GarmentAiAnalysis(
    val garmentType: String,
    val detectedColor: String,
    val detectedFabric: String,
    val soilLevel: String,
    val visibleDefects: String,
    val recommendedTreatment: String,
    val careWarning: String
)

class AiRepository {
    private val garmentSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "garmentType" to Schema.string("Type de vêtement détecté en français (ex: Chemise, Veste, Robe, Pantalon)"),
                "detectedColor" to Schema.string("Couleur principale en français"),
                "detectedFabric" to Schema.string("Tissu probable en français (ex: Coton, Laine, Soie, Polyester, Lin)"),
                "soilLevel" to Schema.enumeration(
                    values = listOf("Léger", "Normal", "Élevé"),
                    description = "Niveau de salissure visible"
                ),
                "visibleDefects" to Schema.string("Taches, déchirures, boutons manquants ou usure visibles, ou 'Aucun défaut visible détecté (vérification humaine requise)'"),
                "recommendedTreatment" to Schema.string("Prestation conseillée (ex: Lavage + Repassage, Nettoyage à sec, Détachage spécial)"),
                "careWarning" to Schema.string("Précaution textile éventuelle pour l'opérateur pressing")
            )
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = garmentSchema
            }
        )
    }

    private val advisorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    suspend fun analyzeGarmentPhoto(bitmap: Bitmap): Result<GarmentAiAnalysis> {
        return try {
            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            "Tu es un assistant d'inspection textile pour un pressing professionnel en Afrique francophone. " +
                                "Analyse cette photo de vêtement de manière indicative. Ne certifie jamais qu'un vêtement est sans défaut " +
                                "ou qu'un traitement est sans risque : rappelle qu'une validation humaine est obligatoire."
                        )
                    }
                )
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Réponse vide du modèle IA."))
            val obj = JSONObject(rawJson)
            Result.success(
                GarmentAiAnalysis(
                    garmentType = obj.optString("garmentType", "Vêtement"),
                    detectedColor = obj.optString("detectedColor", "Non précisée"),
                    detectedFabric = obj.optString("detectedFabric", "Textile mixte"),
                    soilLevel = obj.optString("soilLevel", "Normal"),
                    visibleDefects = obj.optString("visibleDefects", "À inspecter manuellement"),
                    recommendedTreatment = obj.optString("recommendedTreatment", "Lavage + Repassage"),
                    careWarning = obj.optString("careWarning", "Validation humaine obligatoire avant traitement.")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Garment vision analysis timed out", e)
            Result.failure(Exception("L'analyse IA a dépassé le délai de 60 secondes. Réessayez."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Garment vision analysis failed", e)
            Result.failure(e)
        }
    }

    fun streamAdvisorAnswer(
        businessContextSummary: String,
        userQuestion: String
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine("Tu es l'assistant IA opérationnel et financier de PRESSING PRO (Gestion Pro Africa).")
            appendLine("Réponds en français professionnel, structuré et concis avec des recommandations actionnables.")
            appendLine("Rappelle toujours que toute décision financière ou de traitement textile critique nécessite validation humaine.")
            appendLine()
            appendLine("=== DONNÉES EN TEMPS RÉEL DE L'ENTREPRISE ===")
            appendLine(businessContextSummary)
            appendLine()
            appendLine("=== QUESTION DU GÉRANT ===")
            appendLine(userQuestion)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                advisorModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("L'assistant IA n'a renvoyé aucun texte.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Advisor stream timed out", e)
            throw Exception("Délai dépassé (60s) lors de la génération IA.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Advisor stream failed", e)
            throw e
        }
    }
}
