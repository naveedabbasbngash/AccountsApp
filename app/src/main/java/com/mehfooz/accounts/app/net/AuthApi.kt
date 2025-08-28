package com.mehfooz.accounts.app.net

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/* -----------------------------
   Replace this with your domain
   ----------------------------- */
private const val BASE_URL =
    "https://kheloaurjeeto.net/Apps/pairing-api/sync/"

/* =========================================================
   Request & Response models (match your PHP JSON)
   ========================================================= */

/** Request body for verify_and_sync.php */
data class VerifyAndSyncRequest(
    val email: String,
    @SerializedName("mobile_app_id") val mobileAppId: String,
    val idToken: String? = null,
    val appVersion: String? = null,
    val name: String? = null
)

/** `user` block */
data class ApiUser(
    val id: Long?,
    val email: String?,
    @SerializedName("is_enabled") val isEnabled: Int? = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("api_token") val apiToken: String? = null
)

/** A small subscription object as returned by PHP (active/latest) */
data class ApiSubscriptionLite(
    val id: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("is_active") val isActive: String?
)

/** `subscription` block */
data class ApiSubscriptionSummary(
    @SerializedName("has_any") val hasAny: Boolean = false,
    @SerializedName("has_active") val hasActive: Boolean = false,
    val active: ApiSubscriptionLite? = null,
    val latest: ApiSubscriptionLite? = null
)

/** `next` block: what UI should do next */
data class ApiNext(
    val redirect: String? = null   // e.g. "activation" or "dashboard"
)

/** `client` echo block (optional debug info) */
data class ApiClientEcho(
    val appVersion: String? = null,
    @SerializedName("mobile_app_id") val mobileAppId: String? = null,
    val name: String? = null
)

/** Success response shape */
data class VerifyAndSyncResponse(
    val ok: Boolean,
    val user: ApiUser? = null,
    val subscription: ApiSubscriptionSummary? = null,
    val next: ApiNext? = null,
    val client: ApiClientEcho? = null
)

/** Error response shape from PHP (defensive) */
data class ApiErrorResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/* =========================================================
   Retrofit service
   ========================================================= */

interface AuthApiService {
    @POST("verify_and_sync.php")
    @Headers("Content-Type: application/json")
    suspend fun verifyAndSync(
        @Body body: VerifyAndSyncRequest
    ): Response<VerifyAndSyncResponse>
}

/* =========================================================
   Retrofit provider (singleton)
   ========================================================= */

object AuthApi {

    private val json = "application/json; charset=utf-8".toMediaType()

    private val logging: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            // Use BODY in debug builds; INFO/HEADERS in release if you prefer
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val req = original.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "MehfoozAccounts/Android")
            .method(original.method, original.body)
            .build()
        chain.proceed(req)
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}

/* =========================================================
   Small helper result wrapper (optional)
   ========================================================= */

sealed class ApiResult<out T> {
    data class Success<T>(val body: T) : ApiResult<T>()
    data class Failure(
        val code: Int? = null,
        val message: String? = null
    ) : ApiResult<Nothing>()
}

/** Call wrapper: converts Retrofit Response → ApiResult */
suspend fun verifyAndSyncCall(req: VerifyAndSyncRequest): ApiResult<VerifyAndSyncResponse> {
    return try {
        val resp = AuthApi.service.verifyAndSync(req)
        if (resp.isSuccessful) {
            val body = resp.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Failure(resp.code(), "Empty response body")
            }
        } else {
            // Try to parse error JSON softly
            ApiResult.Failure(resp.code(), resp.errorBody()?.string())
        }
    } catch (t: Throwable) {
        ApiResult.Failure(message = t.message)
    }
}