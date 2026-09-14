package com.savemoney.app.data

import android.app.Application
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SessionDto(
    val token: String,
    val householdCode: String,
    val role: String,
    val requesterName: String,
    val approverName: String,
)

data class JoinBody(
    val name: String,
    val role: String,
    val code: String? = null,
)

data class ReviewBody(val approve: Boolean, val comment: String)

data class ProfileBody(val role: String, val requesterName: String, val approverName: String)

data class BudgetBody(val amountCents: Long)

interface SaveMoneyApi {
    @POST("api/households")
    suspend fun createHousehold(@Body body: JoinBody): SessionDto

    @POST("api/households/join")
    suspend fun joinHousehold(@Body body: JoinBody): SessionDto

    @GET("api/session")
    suspend fun session(@Header("Authorization") authorization: String): SessionDto

    @PUT("api/session")
    suspend fun updateSession(@Header("Authorization") authorization: String, @Body body: ProfileBody): SessionDto

    @GET("api/requests")
    suspend fun listRequests(@Header("Authorization") authorization: String): List<PurchaseRequest>

    @Multipart
    @POST("api/requests")
    suspend fun createRequest(
        @Header("Authorization") authorization: String,
        @Part("itemName") itemName: okhttp3.RequestBody,
        @Part("category") category: okhttp3.RequestBody,
        @Part("unitPriceCents") unitPriceCents: okhttp3.RequestBody,
        @Part("quantity") quantity: okhttp3.RequestBody,
        @Part("reason") reason: okhttp3.RequestBody,
        @Part image: MultipartBody.Part?,
    ): PurchaseRequest

    @POST("api/requests/{id}/review")
    suspend fun review(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body body: ReviewBody,
    ): PurchaseRequest

    @DELETE("api/requests/{id}")
    suspend fun withdraw(@Header("Authorization") authorization: String, @Path("id") id: Long)

    @GET("api/expenses")
    suspend fun listExpenses(
        @Header("Authorization") authorization: String,
        @Query("yearMonth") yearMonth: String,
    ): List<ExpenseRecord>

    @GET("api/budget")
    suspend fun getBudget(
        @Header("Authorization") authorization: String,
        @Query("yearMonth") yearMonth: String,
    ): MonthlyBudget?

    @PUT("api/budget")
    suspend fun setBudget(
        @Header("Authorization") authorization: String,
        @Query("yearMonth") yearMonth: String,
        @Body body: BudgetBody,
    ): MonthlyBudget
}

class HouseholdRepository(private val app: Application) {
    private val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val text = "text/plain".toMediaType()

    private fun api(): SaveMoneyApi {
        val base = prefs.getString("serverUrl", "http://10.0.2.2:8080")!!.trim().trimEnd('/') + "/"
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SaveMoneyApi::class.java)
    }

    private fun bearer(): String = "Bearer ${prefs.getString("token", "")}"

    suspend fun createHousehold(name: String, role: String): SessionDto =
        api().createHousehold(JoinBody(name = name, role = role))

    suspend fun joinHousehold(code: String, name: String, role: String): SessionDto =
        api().joinHousehold(JoinBody(name = name, role = role, code = code.trim()))

    suspend fun session(): SessionDto = api().session(bearer())

    suspend fun updateSession(role: String, requesterName: String, approverName: String): SessionDto =
        api().updateSession(bearer(), ProfileBody(role, requesterName, approverName))

    suspend fun listRequests(): List<PurchaseRequest> = api().listRequests(bearer())

    suspend fun createRequest(
        itemName: String,
        category: String,
        unitPriceCents: Long,
        quantity: Int,
        reason: String,
        imageUri: Uri?,
    ): PurchaseRequest {
        val imagePart = imageUri?.let { uri ->
            val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@let null
            val body = bytes.toRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("image", "photo.jpg", body)
        }
        return api().createRequest(
            bearer(),
            itemName.toRequestBody(text),
            category.toRequestBody(text),
            unitPriceCents.toString().toRequestBody(text),
            quantity.toString().toRequestBody(text),
            reason.toRequestBody(text),
            imagePart,
        )
    }

    suspend fun review(id: Long, approve: Boolean, comment: String): PurchaseRequest =
        api().review(bearer(), id, ReviewBody(approve, comment))

    suspend fun withdraw(id: Long) {
        api().withdraw(bearer(), id)
    }

    suspend fun listExpenses(yearMonth: String): List<ExpenseRecord> = api().listExpenses(bearer(), yearMonth)

    suspend fun getBudget(yearMonth: String): MonthlyBudget? = api().getBudget(bearer(), yearMonth)

    suspend fun setBudget(yearMonth: String, amountCents: Long): MonthlyBudget =
        api().setBudget(bearer(), yearMonth, BudgetBody(amountCents))
}
