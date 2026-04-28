package com.nubiaferr.pospayment.data.local


import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.local.entity.TransactionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [PaymentDao] using an in-memory Room database.
 *
 * No mocks — the real Room implementation runs on the JVM via Robolectric,
 * so these tests prove that data is actually persisted, updated and retrieved
 * correctly without adding any production code.
 *
 * Add to androidTest source set and run with:
 *   ./gradlew connectedAndroidTest
 *
 * Or as a local JVM test with Robolectric (requires testOptions.unitTests.isIncludeAndroidResources = true):
 *   ./gradlew test
 */
@RunWith(AndroidJUnit4::class)
class PaymentDaoTest {

    private lateinit var database: PaymentDatabase
    private lateinit var dao: PaymentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PaymentDatabase::class.java)
            .allowMainThreadQueries()   // acceptable in tests only
            .build()
        dao = database.paymentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ── upsert — insert ────────────────────────────────────────────────────────

    @Test
    fun `given new transaction, when upsert, then it can be retrieved by id`() = runTest {
        val entity = makeEntity(id = "txn_001", status = "APPROVED")

        dao.upsert(entity)

        val result = dao.getById("txn_001")
        assertNotNull(result)
        assertEquals("txn_001", result?.id)
    }

    @Test
    fun `given new transaction, when upsert, then all fields are persisted correctly`() = runTest {
        val entity = makeEntity(
            id = "txn_002",
            amount = 15_000L,
            paymentMethod = "PIX",
            installments = 1,
            status = "APPROVED",
            authCode = "AUTH_PIX"
        )

        dao.upsert(entity)

        val result = dao.getById("txn_002")!!
        assertEquals(15_000L, result.amount)
        assertEquals("PIX", result.paymentMethod)
        assertEquals("AUTH_PIX", result.authCode)
    }

    // ── upsert — update (REPLACE on conflict) ─────────────────────────────────

    @Test
    fun `given existing PENDING transaction, when upsert with APPROVED status, then status is updated`() = runTest {
        dao.upsert(makeEntity(id = "txn_003", status = "PENDING"))

        dao.upsert(makeEntity(id = "txn_003", status = "APPROVED"))

        val result = dao.getById("txn_003")
        assertEquals("APPROVED", result?.status)
    }

    @Test
    fun `given existing transaction, when upsert with CANCELLED status, then status is updated`() = runTest {
        dao.upsert(makeEntity(id = "txn_004", status = "APPROVED"))

        dao.upsert(makeEntity(id = "txn_004", status = "CANCELLED"))

        val result = dao.getById("txn_004")
        assertEquals("CANCELLED", result?.status)
    }

    @Test
    fun `given existing transaction, when upsert with same id, then only one record exists`() = runTest {
        dao.upsert(makeEntity(id = "txn_005", status = "PENDING"))
        dao.upsert(makeEntity(id = "txn_005", status = "APPROVED"))

        val all = dao.getAll()
        assertEquals(1, all.count { it.id == "txn_005" })
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    fun `given no transactions, when getById, then returns null`() = runTest {
        val result = dao.getById("non_existent")
        assertNull(result)
    }

    @Test
    fun `given multiple transactions, when getById, then returns only the matching one`() = runTest {
        dao.upsert(makeEntity(id = "txn_010", status = "APPROVED"))
        dao.upsert(makeEntity(id = "txn_011", status = "DECLINED"))

        val result = dao.getById("txn_011")
        assertEquals("DECLINED", result?.status)
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    fun `given empty database, when getAll, then returns empty list`() = runTest {
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `given multiple transactions, when getAll, then returns all of them`() = runTest {
        dao.upsert(makeEntity(id = "txn_020", status = "APPROVED"))
        dao.upsert(makeEntity(id = "txn_021", status = "DECLINED"))
        dao.upsert(makeEntity(id = "txn_022", status = "CANCELLED"))

        assertEquals(3, dao.getAll().size)
    }

    @Test
    fun `given multiple transactions with different timestamps, when getAll, then returns most recent first`() = runTest {
        dao.upsert(makeEntity(id = "txn_030", timestamp = 1_000L))
        dao.upsert(makeEntity(id = "txn_031", timestamp = 3_000L))
        dao.upsert(makeEntity(id = "txn_032", timestamp = 2_000L))

        val result = dao.getAll()
        assertEquals("txn_031", result[0].id)
        assertEquals("txn_032", result[1].id)
        assertEquals("txn_030", result[2].id)
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun makeEntity(
        id: String = "txn_test",
        amount: Long = 10_000L,
        paymentMethod: String = "CREDIT",
        installments: Int = 1,
        status: String = "APPROVED",
        authCode: String = "AUTH_TEST",
        timestamp: Long = System.currentTimeMillis()
    ) = TransactionEntity(
        id = id,
        amount = amount,
        paymentMethod = paymentMethod,
        installments = installments,
        description = "Test transaction",
        status = status,
        authCode = authCode,
        timestamp = timestamp
    )
}