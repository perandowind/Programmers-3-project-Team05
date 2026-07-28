package com.team05.petmeeting.domain.donation.service

import com.team05.petmeeting.domain.campaign.entity.Campaign
import com.team05.petmeeting.domain.campaign.repository.CampaignRepository
import com.team05.petmeeting.domain.donation.dto.PrepareReq
import com.team05.petmeeting.domain.donation.enums.DonationStatus
import com.team05.petmeeting.domain.donation.repository.DonationRepository
import com.team05.petmeeting.domain.shelter.dto.ShelterCommand
import com.team05.petmeeting.domain.shelter.entity.Shelter
import com.team05.petmeeting.domain.shelter.repository.ShelterRepository
import com.team05.petmeeting.domain.user.entity.User
import com.team05.petmeeting.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest
@ActiveProfiles("test")
class DonationDbServiceConcurrencyTest {

    @Autowired lateinit var donationDbService: DonationDbService
    @Autowired lateinit var campaignRepository: CampaignRepository
    @Autowired lateinit var donationRepository: DonationRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var shelterRepository: ShelterRepository

    @AfterEach
    fun tearDown() {
        donationRepository.deleteAllInBatch()
        campaignRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
        shelterRepository.deleteAllInBatch()
    }

    @Test
    fun `concurrent donations should safely accumulate the campaign total amount and measure performance`() {
        val shelter = shelterRepository.save(
            Shelter.create(
                ShelterCommand(
                    careRegNo = "TEST-001",
                    careNm = "test shelter",
                    careTel = "010-0000-0000",
                    careAddr = "Seoul",
                    careOwnerNm = "owner",
                    orgNm = "org",
                    updTm = LocalDateTime.now()
                )
            )
        )
        val user = userRepository.save(User.create("test@test.com", "nick", "real"))
        val campaign = campaignRepository.save(
            Campaign.create(
                shelter = shelter,
                title = "Emergency Surgery for Max",
                description = "Need funds",
                targetAmount = 100000
            )
        )

        val threadCount = 100
        val donationAmount = 1000
        val executor = Executors.newFixedThreadPool(threadCount)

        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(threadCount)

        val exceptions = Collections.synchronizedList(mutableListOf<Throwable>())

        val stopWatch = StopWatch()
        val totalThreadExecutionTime = AtomicLong(0)

        val paymentIds = (1..threadCount).map {
            donationDbService.prepareDonation(
                userId = user.id!!,
                req = PrepareReq(campaign.id!!, donationAmount)
            ).paymentId
        }

        try {
            for (paymentId in paymentIds) {
                executor.submit {
                    readyLatch.countDown()
                    try {
                        startLatch.await()

                        val threadStart = System.currentTimeMillis()

                        donationDbService.completeDonation(
                            paymentId = paymentId,
                            paidAmount = donationAmount,
                            isPaid = true
                        )

                        val threadEnd = System.currentTimeMillis()
                        totalThreadExecutionTime.addAndGet(threadEnd - threadStart)

                    } catch (e: Exception) {
                        exceptions.add(e)
                    } finally {
                        finishLatch.countDown()
                    }
                }
            }

            readyLatch.await()

            stopWatch.start("100 Concurrent Donations")
            startLatch.countDown()

            val completedInTime = finishLatch.await(10, TimeUnit.SECONDS)

            stopWatch.stop()

            assertThat(completedInTime)
                .`as`("Threads did not finish within 10 seconds (Possible Deadlock)")
                .isTrue()

        } finally {
            executor.shutdown()
        }

        assertThat(exceptions).isEmpty()

        val updatedCampaign = campaignRepository.findById(campaign.id!!).get()
        assertThat(updatedCampaign.currentAmount).isEqualTo(threadCount * donationAmount)

        // --- Print Performance Metrics ---
        val avgThreadTime = totalThreadExecutionTime.get() / threadCount
        println("\n================ PERFORMANCE REPORT ================")
        println("Strategy              : DB-Level Atomic Update (Row Lock)")
        println("Total Requests        : $threadCount")
        println("Active Connections    : 32 (Thread Pool Size)")
        println("Total Wall-Clock Time : ${stopWatch.totalTimeMillis} ms")
        println("Average Time/Request  : $avgThreadTime ms")
        println("Throughput            : ${threadCount / (stopWatch.totalTimeSeconds.takeIf { it > 0 } ?: 1.0)} req/sec")
        println("====================================================\n")
    }

    @Test
    fun `when UI and Webhook complete the same donation concurrently, it should not double count the amount`() {
        val shelter = shelterRepository.save(
            Shelter.create(
                ShelterCommand(
                    careRegNo = "TEST-002",
                    careNm = "test shelter 2",
                    careTel = "010-0000-0000",
                    careAddr = "Seoul",
                    careOwnerNm = "owner",
                    orgNm = "org",
                    updTm = LocalDateTime.now()
                )
            )
        )
        val user = userRepository.save(User.create("user2@test.com", "nick2", "real"))
        val campaign = campaignRepository.save(
            Campaign.create(
                shelter = shelter,
                title = "Vaccines for Puppies",
                description = "Need funds",
                targetAmount = 50000
            )
        )

        val donationAmount = 1000

        // donation 하나 준비 (Status = PENDING)
        val prepareRes = donationDbService.prepareDonation(
            userId = user.id!!,
            req = PrepareReq(campaign.id!!, donationAmount)
        )
        val paymentId = prepareRes.paymentId

        // 스레드 2개
        val executor = Executors.newFixedThreadPool(2)
        val readyLatch = CountDownLatch(2)
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(2)

        // 스레드 1: ui로 완료 처리
        executor.submit {
            readyLatch.countDown()
            startLatch.await()
            try {
                donationDbService.completeDonation(paymentId, donationAmount, true)
            } catch (e: Exception) {
                println("UI Thread Exception: ${e.message}")
            } finally {
                finishLatch.countDown()
            }
        }

        // 스레드 2: 포트원 웹훅으로 완료 처리
        executor.submit {
            readyLatch.countDown()
            startLatch.await()
            try {
                donationDbService.processWebhookDonation(paymentId, donationAmount)
            } catch (e: Exception) {
                println("Webhook Thread Exception: ${e.message}")
            } finally {
                finishLatch.countDown()
            }
        }

        // 동시에 스레드 실행
        readyLatch.await()
        startLatch.countDown()
        finishLatch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        // Assertions
        val updatedCampaign = campaignRepository.findById(campaign.id!!).get()
        val finalDonation = donationRepository.findByPaymentId(paymentId)!!

        // 한번만 증가 시켜야함
        assertThat(updatedCampaign.currentAmount)
            .`as`("Campaign amount should only increase by $donationAmount once")
            .isEqualTo(donationAmount)

        assertThat(finalDonation.status)
            .`as`("Donation status should be PAID")
            .isEqualTo(DonationStatus.PAID)
    }
}