package com.khalti.checkOut.api


import com.khalti.checkOut.EBanking.helper.BaseListPojo
import com.khalti.checkOut.Wallet.helper.WalletConfirmPojo
import com.khalti.checkOut.Wallet.helper.WalletInitPojo
import com.khalti.checkOut.helper.MerchantPreferencePojo
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*

interface KhaltiApi {

    @GET
    suspend fun getBanks(@Url url: String, @QueryMap queryMap: Map<String, Any>): Deferred<Response<BaseListPojo>>

    @GET
    suspend fun getPreference(@Url url: String, @Header("Authorization") publicKey: String): Deferred<Response<MerchantPreferencePojo>>

    @POST
    @FormUrlEncoded
    suspend fun initiatePayment(@Url url: String, @FieldMap dataMap: Map<String, Any>): Deferred<Response<WalletInitPojo>>

    @POST
    @FormUrlEncoded
    suspend fun confirmPayment(@Url url: String, @FieldMap dataMap: Map<String, Any>): Deferred<Response<WalletConfirmPojo>>
}