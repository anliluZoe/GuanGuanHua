package com.guanguanhua.app.data

import android.app.Application
import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MemberDto(
    val id: Long,
    val name: String,
    val avatarPreset: String? = null,
    val avatarUrl: String? = null,
)

data class SessionDto(
    val token: String,
    val memberId: Long,
    val householdCode: String,
    val name: String,
    val members: List<MemberDto>,
)

data class JoinBody(
    val name: String,
    val code: String? = null,
    val memberId: Long? = null,
)

data class ReviewBody(
    val approve: Boolean,
    val comment: String,
    val unitPriceCents: Long? = null,
    val quantity: Int? = null,
)

data class ProfileBody(val name: String? = null, val avatarPreset: String? = null)

data class BudgetBody(val amountCents: Long)

data class WidgetDto(
    val widgetImageUrl: String? = null,
    val widgetCaption: String? = null,
    val widgetCaptionColor: String? = null,
    val widgetUpdatedBy: String? = null,
    val widgetUpdatedAt: Long? = null,
)

data class WidgetCaptionBody(
    val widgetCaption: String,
    val widgetCaptionColor: String,
)

interface GuanGuanHuaApi {
    @POST("api/households")
    suspend fun createHousehold(@Body body: JoinBody): SessionDto

    @POST("api/households/join")
    suspend fun joinHousehold(@Body body: JoinBody): Response<SessionDto>

    @GET("api/session")
    suspend fun session(@Header("Authorization") authorization: String): SessionDto

    @DELETE("api/session")
    suspend fun leaveSession(@Header("Authorization") authorization: String): Response<ResponseBody>

    @PUT("api/session")
    suspend fun updateSession(@Header("Authorization") authorization: String, @Body body: ProfileBody): SessionDto

    @Multipart
    @POST("api/session/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") authorization: String,
        @Part avatar: MultipartBody.Part,
    ): SessionDto

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

    /** 没设预算时服务器返回 JSON null，用 Response 包一层才能拿到空 body。 */
    @GET("api/budget")
    suspend fun getBudget(
        @Header("Authorization") authorization: String,
        @Query("yearMonth") yearMonth: String,
    ): Response<MonthlyBudget>

    @PUT("api/budget")
    suspend fun setBudget(
        @Header("Authorization") authorization: String,
        @Query("yearMonth") yearMonth: String,
        @Body body: BudgetBody,
    ): MonthlyBudget

    @GET("api/widget")
    suspend fun getWidget(@Header("Authorization") authorization: String): WidgetDto

    @PATCH("api/widget")
    suspend fun updateWidget(
        @Header("Authorization") authorization: String,
        @Body body: WidgetCaptionBody,
    ): WidgetDto

    @Multipart
    @POST("api/widget/image")
    suspend fun uploadWidgetImage(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part,
    ): WidgetDto

    @DELETE("api/widget/image")
    suspend fun deleteWidgetImage(@Header("Authorization") authorization: String): WidgetDto
}

class HouseholdRepository(private val app: Application) {
    private val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val text = "text/plain".toMediaType()

    private fun api(): GuanGuanHuaApi {
        val base = ApiConfig.resolvedServerUrl(prefs.getString("serverUrl", null)) + "/"
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GuanGuanHuaApi::class.java)
    }

    private fun bearer(): String = "Bearer ${prefs.getString("token", "")}"

    suspend fun createHousehold(name: String): SessionDto =
        api().createHousehold(JoinBody(name = name))

    suspend fun joinHousehold(code: String, name: String, memberId: Long? = null): SessionDto {
        val response = api().joinHousehold(
            JoinBody(name = name, code = code.trim(), memberId = memberId),
        )
        if (response.isSuccessful) {
            return response.body() ?: error("服务器没有返回登录信息")
        }
        throw apiFailure(response.code(), response.errorBody()?.string().orEmpty(), "加入失败")
    }

    suspend fun leaveHousehold() {
        val response = api().leaveSession(bearer())
        if (response.isSuccessful || response.code() == 401) return
        throw apiFailure(response.code(), response.errorBody()?.string().orEmpty(), "退出失败")
    }

    suspend fun session(): SessionDto = api().session(bearer())

    suspend fun updateName(name: String): SessionDto =
        api().updateSession(bearer(), ProfileBody(name = name))

    suspend fun updateAvatarPreset(name: String, preset: String): SessionDto =
        api().updateSession(bearer(), ProfileBody(name = name, avatarPreset = preset))

    suspend fun uploadAvatar(imageUri: Uri): SessionDto =
        api().uploadAvatar(
            bearer(),
            jpegPart(imageUri, field = "avatar", filename = "avatar.jpg", maxEdge = ImageCompress.AVATAR_MAX_EDGE),
        )

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
            jpegPart(uri, field = "image", filename = "photo.jpg", maxEdge = ImageCompress.PHOTO_MAX_EDGE)
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

    suspend fun review(
        id: Long,
        approve: Boolean,
        comment: String,
        unitPriceCents: Long? = null,
        quantity: Int? = null,
    ): PurchaseRequest =
        api().review(bearer(), id, ReviewBody(approve, comment, unitPriceCents, quantity))

    suspend fun withdraw(id: Long) {
        api().withdraw(bearer(), id)
    }

    suspend fun listExpenses(yearMonth: String): List<ExpenseRecord> = api().listExpenses(bearer(), yearMonth)

    suspend fun getBudget(yearMonth: String): MonthlyBudget? {
        val response = api().getBudget(bearer(), yearMonth)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        return response.body()
    }

    suspend fun setBudget(yearMonth: String, amountCents: Long): MonthlyBudget =
        api().setBudget(bearer(), yearMonth, BudgetBody(amountCents))

    suspend fun getWidget(): WidgetDto = api().getWidget(bearer())

    suspend fun updateWidgetCaption(caption: String, captionColor: String): WidgetDto =
        api().updateWidget(
            bearer(),
            WidgetCaptionBody(
                widgetCaption = caption,
                widgetCaptionColor = captionColor,
            ),
        )

    suspend fun uploadWidgetImage(imageUri: Uri): WidgetDto =
        api().uploadWidgetImage(
            bearer(),
            jpegPart(imageUri, field = "image", filename = "widget.jpg", maxEdge = ImageCompress.PHOTO_MAX_EDGE),
        )

    suspend fun clearWidgetImage(): WidgetDto = api().deleteWidgetImage(bearer())

    private suspend fun jpegPart(uri: Uri, field: String, filename: String, maxEdge: Int): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val jpeg = ImageCompress.compress(app, uri, maxEdge, filename)
            val body = jpeg.bytes.toRequestBody(jpeg.mimeType.toMediaType())
            MultipartBody.Part.createFormData(field, jpeg.filename, body)
        }
}

class HouseholdFullException(
    val members: List<MemberDto>,
    override val message: String,
) : IOException(message)

internal data class ApiErrorBody(
    val detail: String? = null,
    val code: String? = null,
    val members: List<MemberDto>? = null,
)

internal fun apiFailure(httpCode: Int, body: String, fallback: String): Exception {
    val parsed = runCatching { Gson().fromJson(body, ApiErrorBody::class.java) }.getOrNull()
    val detail = parsed?.detail?.trim()?.takeIf { it.isNotEmpty() }
    if (httpCode == 409 && parsed?.code == "household_full") {
        return HouseholdFullException(
            members = parsed.members.orEmpty(),
            message = detail ?: "这个家庭已经有两个人了，请选择其中一个身份进入",
        )
    }
    return IOException(detail ?: fallback)
}
