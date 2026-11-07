package com.example.spjappapk.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.Customer
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.local.entities.SalesPersonEntity
import com.example.spjappapk.ui.auth.SalesPerson
import com.example.spjappapk.util.BusinessRevenueHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaymentDisplayItem(
    val payment: PaymentEntity,
    val sale: SalesMaster?,
    val customerName: String,
    val salesName: String,
    val nota: String
)

data class AllPaymentsUiState(
    val payments: List<PaymentDisplayItem> = emptyList(),
    val unpaidSales: List<SalesMaster> = emptyList(),
    val totalCollected: Double = 0.0,
    val cashTotal: Double = 0.0,
    val transferTotal: Double = 0.0,
    val isManagerOrOwner: Boolean = false,
    val isLoading: Boolean = false
)

class AllPaymentsViewModel(
    private val app: SPJApplication,
    private val currentUser: SalesPerson?
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val isManagerOrOwner = currentUser?.role?.trim()?.lowercase() in listOf("manager", "owner", "admin")

    val uiState: StateFlow<AllPaymentsUiState> = combine(
        app.paymentRepository.allPayments,
        app.salesRepository.allSalesMaster,
        app.database.salesPersonDao().getAllSalesPersons(),
        app.customerRepository.allCustomers,
        _searchQuery
    ) { flows ->
        val payments = flows[0] as List<PaymentEntity>
        val salesMasterList = flows[1] as List<SalesMaster>
        val staffList = flows[2] as List<SalesPersonEntity>
        val customerList = flows[3] as List<Customer>
        val query = flows[4] as String

        val salesMap = salesMasterList.associateBy { it.id }

        // Filter sales where status is TEMPO / remaining > 0 for new payment dialog
        val unpaid = salesMasterList.filter { sale ->
            sale.isDeleted != true &&
            sale.remaining > 0 &&
            (isManagerOrOwner || sale.salesPersonId?.equals(currentUser?.id, ignoreCase = true) == true ||
            BusinessRevenueHelper.resolveSalesPersonName(sale, staffList).equals(currentUser?.name, ignoreCase = true))
        }.map { sale ->
            sale.copy(customerName = BusinessRevenueHelper.resolveCustomerName(sale.customerName, sale.customerId, customerList))
        }.sortedByDescending { it.remaining }

        // Build display items
        val displayItems = payments.map { payment ->
            val sale = salesMap[payment.saleId]
            val custName = BusinessRevenueHelper.resolveCustomerName(sale?.customerName, sale?.customerId, customerList)
            val salesName = if (sale != null) BusinessRevenueHelper.resolveSalesPersonName(sale, staffList) else "Sales"
            val nota = sale?.nota ?: "Nota #${payment.saleId}"

            PaymentDisplayItem(
                payment = payment,
                sale = sale,
                customerName = custName,
                salesName = salesName,
                nota = nota
            )
        }

        // Filter by Role
        val roleFiltered = if (isManagerOrOwner) {
            displayItems
        } else {
            displayItems.filter { item ->
                item.sale?.salesPersonId?.equals(currentUser?.id, ignoreCase = true) == true ||
                item.salesName.equals(currentUser?.name, ignoreCase = true)
            }
        }

        // Filter by Search Query
        val filtered = if (query.isBlank()) {
            roleFiltered
        } else {
            roleFiltered.filter { item ->
                item.nota.contains(query, ignoreCase = true) ||
                item.customerName.contains(query, ignoreCase = true) ||
                item.salesName.contains(query, ignoreCase = true) ||
                item.payment.paymentMethod.contains(query, ignoreCase = true)
            }
        }

        val total = filtered.sumOf { it.payment.amount }
        val cash = filtered.filter { it.payment.paymentMethod.uppercase() == "CASH" }.sumOf { it.payment.amount }
        val transfer = filtered.filter { it.payment.paymentMethod.uppercase() == "TRANSFER" }.sumOf { it.payment.amount }

        AllPaymentsUiState(
            payments = filtered,
            unpaidSales = unpaid,
            totalCollected = total,
            cashTotal = cash,
            transferTotal = transfer,
            isManagerOrOwner = isManagerOrOwner,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AllPaymentsUiState(isLoading = true))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addPayment(
        saleId: String,
        amount: Double,
        method: String,
        notes: String?,
        date: Long
    ) {
        viewModelScope.launch {
            val payment = PaymentEntity(
                saleId = saleId,
                paymentDate = date,
                amount = amount,
                paymentMethod = method,
                notes = notes,
                isSynced = false
            )
            app.paymentRepository.addPayment(payment)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            app.paymentRepository.refreshPayments()
        }
    }
}
