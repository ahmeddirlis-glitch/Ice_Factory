package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.util.UUID

@Ignore("Requires local Firebase emulator daemon")
class FactoryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ownerCanCreateSaleAndObserveRealtimeUpdate() = runBlocking {
        val ownerUid = signInTestUser("owner_${UUID.randomUUID()}@brohi.test")
        val repo = FactoryRepository(firestore)

        val saleId = withTimeout(TIMEOUT_MS) {
            repo.createSale(
                factoryId = ownerUid,
                callerUid = ownerUid,
                customerId = "",
                customerName = "Walk-in",
                product = "Ice Blocks (10kg)",
                quantity = 10L,
                unitPrice = 1500.0,
                total = 15000.0,
                paid = true,
                source = "voice"
            )
        }
        assertTrue(saleId.isNotBlank())

        val sales = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSales(ownerUid, ownerUid).first { list -> list.any { it.id == saleId } }
        }
        assertTrue(sales.any { it.id == saleId && it.total == 15000.0 })
    }

    @Test
    fun ownerCanAddFundingAndFactoryBuildExpense() = runBlocking {
        val ownerUid = signInTestUser("owner_fund_${UUID.randomUUID()}@brohi.test")
        val repo = FactoryRepository(firestore)

        val fundId = withTimeout(TIMEOUT_MS) {
            repo.addFunding(
                factoryId = ownerUid,
                source = "Friend",
                name = "Dost",
                type = "Loan",
                amount = 20_000_000.0
            )
        }
        assertTrue(fundId.isNotBlank())

        val expId = withTimeout(TIMEOUT_MS) {
            repo.addExpense(
                factoryId = ownerUid,
                callerUid = ownerUid,
                description = "Factory ki tameer",
                amount = 5_000_000.0,
                category = "Factory Build",
                source = "voice"
            )
        }
        assertTrue(expId.isNotBlank())

        val expenses = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeExpenses(ownerUid, ownerUid, isOwner = true).first { list -> list.any { it.id == expId } }
        }
        assertTrue(expenses.any { it.id == expId && it.capitalInvestment })
    }

    @Test
    fun nonOwnerCannotAccessOwnerFundingOrPayroll() = runBlocking {
        val ownerUid = signInTestUser("owner_sec_${UUID.randomUUID()}@brohi.test")
        val repo = FactoryRepository(firestore)
        withTimeout(TIMEOUT_MS) {
            repo.addFunding(
                factoryId = ownerUid,
                source = "Bank",
                name = "Meezan Bank",
                type = "Loan",
                amount = 1_000_000.0
            )
        }

        signInTestUser("staff_${UUID.randomUUID()}@brohi.test")
        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeFunding(ownerUid).first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    @Test
    fun unauthenticatedCallerCannotObserveSales() = runBlocking {
        val ownerUid = signInTestUser("owner_unauth_${UUID.randomUUID()}@brohi.test")
        auth.signOut()
        val repo = FactoryRepository(firestore)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeSales(ownerUid, ownerUid).first()
            }
        } catch (e: Exception) {
            caught = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
