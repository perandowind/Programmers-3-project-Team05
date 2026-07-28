package com.team05.petmeeting.domain.donation.service

import com.team05.petmeeting.domain.donation.dto.CompleteReq
import com.team05.petmeeting.domain.donation.dto.CompleteRes
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.dto.PrepareRes
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.donation.errorcode.DonationErrorCode
import com.team05.petmeeting.domain.user.dto.profile.UserDonationRes
import com.team05.petmeeting.global.exception.BusinessException
import io.portone.sdk.server.errors.PaymentNotFoundException
import io.portone.sdk.server.payment.PaidPayment
import io.portone.sdk.server.payment.Payment
import io.portone.sdk.server.payment.PaymentAmount
import io.portone.sdk.server.payment.PaymentClient
import io.portone.sdk.server.webhook.WebhookTransaction
import io.portone.sdk.server.webhook.WebhookTransactionData
import io.portone.sdk.server.webhook.WebhookVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doAnswer
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
class DonationServiceTest {

    @Mock
    lateinit var paymentClient: PaymentClient

    @Mock
    lateinit var webhookVerifierProvider: ObjectProvider<WebhookVerifier>

    @Mock
    lateinit var webhookVerifier: WebhookVerifier

    @Mock
    lateinit var donationDbService: DonationDbService

    @InjectMocks
    lateinit var donationService: DonationService

    // ===== prepare =====

    @Test
    @DisplayName("결제 준비 - DB 서비스에 위임")
    fun prepare_success() {
        val req = PrepareReq(11L, 10000)
        val res = PrepareRes("donate-123", 10000)

        whenever(donationDbService.prepareDonation(1L, req)).thenReturn(res)

        val result = donationService.prepare(1L, req)

        assertThat(result.paymentId).isEqualTo("donate-123")
        assertThat(result.amount).isEqualTo(10000)
        verify(donationDbService).prepareDonation(1L, req)
    }

    // ===== donate =====

    @Test
    @DisplayName("결제 완료 성공")
    fun donate_success() = runTest {
        val req = CompleteReq("donate-123")
        val paidPayment = mock<PaidPayment>()
        val amount = mock<PaymentAmount>()

        whenever(paidPayment.amount).thenReturn(amount)
        whenever(amount.total).thenReturn(10000L)
        whenever(paymentClient.getPayment("donate-123")).thenReturn(paidPayment)
        whenever(donationDbService.completeDonation("donate-123", 10000, true))
            .thenReturn(CompleteRes(1L, 10000, DonationStatus.PAID, 11L))

        val result = donationService.donate(1L, req)

        assertThat(result.amount).isEqualTo(10000)
        assertThat(result.status).isEqualTo(DonationStatus.PAID)
        verify(donationDbService).completeDonation("donate-123", 10000, true)
    }

    @Test
    @DisplayName("결제 완료 실패 - 결제 정보 없음")
    fun donate_fail_paymentNotFound() = runTest {
        val req = CompleteReq("donate-123")

        doAnswer { throw mock<PaymentNotFoundException>() }
            .`when`(paymentClient).getPayment("donate-123")

        assertThrows<BusinessException> {
            donationService.donate(1L, req)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.PAYMENT_NOT_FOUND)
        }
    }

    @Test
    @DisplayName("결제 완료 실패 - 인식 불가 응답")
    fun donate_fail_unrecognizedPayment() = runTest {
        val req = CompleteReq("donate-123")
        val unrecognizedPayment = mock<Payment.Unrecognized>()

        whenever(paymentClient.getPayment("donate-123")).thenReturn(unrecognizedPayment)

        assertThrows<BusinessException> {
            donationService.donate(1L, req)
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.PAYMENT_NOT_FOUND)
        }
    }

    // ===== handleWebhook =====

    @Test
    @DisplayName("웹훅 처리 성공")
    fun handleWebhook_success() = runTest {
        val webhookTransaction = mock<WebhookTransaction>()
        val webhookData = mock<WebhookTransactionData>()
        val paidPayment = mock<PaidPayment>()
        val amount = mock<PaymentAmount>()

        whenever(webhookVerifierProvider.getObject()).thenReturn(webhookVerifier)
        whenever(webhookVerifier.verify(any(), any(), any(), any())).thenReturn(webhookTransaction)
        whenever(webhookTransaction.data).thenReturn(webhookData)
        whenever(webhookData.paymentId).thenReturn("donate-123")
        whenever(paymentClient.getPayment("donate-123")).thenReturn(paidPayment)
        whenever(paidPayment.amount).thenReturn(amount)
        whenever(amount.total).thenReturn(10000L)

        donationService.handleWebhook("body", "id", "sig", "ts")

        verify(donationDbService).processWebhookDonation("donate-123", 10000)
    }

    @Test
    @DisplayName("웹훅 처리 실패 - 유효하지 않은 웹훅")
    fun handleWebhook_fail_invalidWebhook() = runTest {
        whenever(webhookVerifierProvider.getObject()).thenReturn(webhookVerifier)
        whenever(webhookVerifier.verify(any(), any(), any(), any()))
            .thenThrow(RuntimeException("invalid"))

        assertThrows<BusinessException> {
            donationService.handleWebhook("body", "id", "sig", "ts")
        }.also {
            assertThat(it.errorCode).isEqualTo(DonationErrorCode.INVALID_WEBHOOK)
        }
    }

    // ===== getMyDonations =====

    @Test
    @DisplayName("내 후원 내역 조회 - DB 서비스에 위임")
    fun getMyDonations_success() {
        val res = UserDonationRes(2, 10000, emptyList())
        whenever(donationDbService.getMyDonations(1L)).thenReturn(res)

        val result = donationService.getMyDonations(1L)

        assertThat(result.donationCount).isEqualTo(2)
        assertThat(result.donationTotalAmount).isEqualTo(10000)
        verify(donationDbService).getMyDonations(1L)
    }
}