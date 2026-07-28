package com.team05.petmeeting.domain.donation.service
import com.team05.petmeeting.domain.donation.dto.CompleteReq
import com.team05.petmeeting.domain.donation.dto.CompleteRes
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.dto.PrepareRes
import com.team05.petmeeting.domain.donation.errorcode.DonationErrorCode
import com.team05.petmeeting.domain.user.dto.profile.UserDonationRes
import com.team05.petmeeting.global.exception.BusinessException
import io.portone.sdk.server.errors.ForbiddenException
import io.portone.sdk.server.errors.GetPaymentException
import io.portone.sdk.server.errors.InvalidRequestException
import io.portone.sdk.server.errors.PaymentNotFoundException
import io.portone.sdk.server.errors.PortOneException
import io.portone.sdk.server.errors.UnauthorizedException
import io.portone.sdk.server.errors.UnknownException
import io.portone.sdk.server.payment.PaidPayment
import io.portone.sdk.server.payment.Payment
import io.portone.sdk.server.payment.PaymentClient
import io.portone.sdk.server.webhook.WebhookTransaction
import io.portone.sdk.server.webhook.WebhookVerifier
import org.springframework.stereotype.Service
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

@Service
class DonationService(
    private val paymentClient: PaymentClient,
    private val webhookVerifierProvider: ObjectProvider<WebhookVerifier>,
    private val donationDbService: DonationDbService // Inject the DB service
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${portone.store-id}")
    private val storeId: String? = null

    // Just delegate to the DB service
    fun prepare(userId: Long, req: PrepareReq): PrepareRes {
        return donationDbService.prepareDonation(userId, req)
    }

    suspend fun donate(userId: Long, req: CompleteReq): CompleteRes {
        // 1. Execute Network Call FIRST (Non-blocking)
        val payment = try {
            paymentClient.getPayment(req.paymentId)
        } catch (error: PortOneException) {
            if (error is GetPaymentException) {
                when (error) {
                    is PaymentNotFoundException -> throw BusinessException(DonationErrorCode.PAYMENT_NOT_FOUND)
                    is UnauthorizedException -> throw BusinessException(DonationErrorCode.UNAUTHORIZED)
                    is ForbiddenException -> throw BusinessException(DonationErrorCode.FORBIDDEN)
                    is InvalidRequestException -> throw BusinessException(DonationErrorCode.INVALID_REQUEST)
                    is UnknownException -> throw BusinessException(DonationErrorCode.UNKNOWN)
                }
            }
            throw BusinessException(DonationErrorCode.PAYMENT_NOT_FOUND)
        }

        if (payment !is Payment.Recognized) {
            throw BusinessException(DonationErrorCode.PAYMENT_NOT_FOUND)
        }

        val isPaid = payment is PaidPayment
        val paidAmount = payment.amount.total.toInt()

        // 2. Offload the blocking DB logic to the IO thread pool
        return withContext(Dispatchers.IO) {
            donationDbService.completeDonation(req.paymentId, paidAmount, isPaid)
        }
    }

    suspend fun handleWebhook(
        body: String,
        webhookId: String,
        webhookSignature: String,
        webhookTimestamp: String
    ) {
        val webhook = try {
            webhookVerifierProvider.getObject().verify(
                msgBody = body,
                msgId = webhookId,
                msgSignature = webhookSignature,
                msgTimestamp = webhookTimestamp
            )
        } catch (e: Exception) {
            throw BusinessException(DonationErrorCode.INVALID_WEBHOOK)
        }

        when (webhook) {
            is WebhookTransaction -> {
                val paymentId = webhook.data.paymentId

                // PortOne API Call (Network)
                val payment = try {
                    paymentClient.getPayment(paymentId)
                } catch (e: PaymentNotFoundException) {
                    // PortOne webhook test or invalid payment
                    logger.info("Ignoring unknown paymentId: {}", paymentId)
                    return
                } catch (e: Exception) {
                    throw BusinessException(DonationErrorCode.PAYMENT_NOT_FOUND)
                }

                if (payment is Payment.Recognized && payment is PaidPayment) {
                    val paidAmount = payment.amount.total.toInt()

                    // DB updates safely wrapped in IO
                    withContext(Dispatchers.IO) {
                        donationDbService.processWebhookDonation(paymentId, paidAmount)
                    }
                }
            }
            else -> { /* Ignore */ }
        }
    }

    // Just delegate to the DB service
    fun getMyDonations(userId: Long): UserDonationRes {
        return donationDbService.getMyDonations(userId)
    }
}
