package com.example.spjappapk.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spjappapk.data.local.entities.PaymentEntity
import com.example.spjappapk.data.local.entities.SalesMaster
import com.example.spjappapk.data.repository.PaymentRepository
import com.example.spjappapk.SPJApplication
import com.example.spjappapk.data.local.entities.SalesOrderItem
import com.example.spjappapk.util.BusinessRevenueHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val app: SPJApplication,
    private val saleId: String
) : ViewModel() {

    private val repository = app.paymentRepository

    val sale: StateFlow<SalesMaster?> = flow {
        val s = repository.getSaleById(saleId)
        if (s != null) {
            val customerList = app.customerRepository.allCustomers.firstOrNull() ?: emptyList()
            val resolvedName = BusinessRevenueHelper.resolveCustomerName(s.customerName, s.customerId, customerList)
            emit(s.copy(customerName = resolvedName))
        } else {
            emit(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val items: StateFlow<List<SalesOrderItem>> = flow {
        val details = app.salesRepository.getSalesDetails(saleId)
        val productsMap = app.inventoryRepository.allInventory.firstOrNull()?.associateBy { it.remoteId } ?: emptyMap()
        val orderItems = details.map { detail ->
            val prodName = detail.itemName ?: productsMap[detail.productId]?.name ?: "Produk tidak ditemukan"
            SalesOrderItem(
                productId = detail.productId ?: "",
                itemName = prodName,
                quantity = detail.quantity,
                price = detail.price,
                priceCategory = detail.priceCategory ?: "R"
            )
        }
        emit(orderItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.getPaymentsForSale(saleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPayment(
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
            repository.addPayment(payment)
        }
    }
}
