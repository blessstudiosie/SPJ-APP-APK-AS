# Alur Kerja Baru: Penyesuaian dengan gemini.md

Aplikasi akan disesuaikan untuk mengikuti prinsip bahwa **Aplikasi Desktop adalah Master Data** dan Android mengirimkan **"Change Requests"** melalui antrian.

## User Review Required

> [!IMPORTANT]
> - Login akan dialihkan untuk menggunakan tabel `sales_persons` secara langsung (saat ini menggunakan username/ID dan password plain text sesuai panduan).
> - Tabel antrian pengiriman data akan diubah dari `sync_requests` menjadi `sales_orders_queue`.
> - Status nota akan dipaksa menjadi `SO` (Sales Order).

## Proposed Changes

### [Data Layer]

Pembaruan model data untuk mendukung Customer dan Product dengan UUID dari Supabase.

#### [NEW] [Customer.kt](file:///C:/Users/SPJ%20Admin/AndroidStudioProjects/SPJAPPAPK/app/src/main/java/com/example/spjappapk/data/local/entities/Customer.kt)
Entitas baru untuk menyimpan data pelanggan.

#### [MODIFY] [Inventory.kt](file:///C:/Users/SPJ%20Admin/AndroidStudioProjects/SPJAPPAPK/app/src/main/java/com/example/spjappapk/data/local/entities/Inventory.kt)
Menambahkan `remote_id` (UUID) agar sinkron dengan tabel `products` di Supabase.

#### [NEW] [SalesOrder.kt](file:///C:/Users/SPJ%20Admin/AndroidStudioProjects/SPJAPPAPK/app/src/main/java/com/example/spjappapk/data/local/entities/SalesOrder.kt)
Menggantikan `Sale.kt` untuk mendukung struktur Sales Order yang lebih kompleks (termasuk JSON details).

### [Synchronization Layer]

#### [MODIFY] [SyncWorker.kt](file:///C:/Users/SPJ%20Admin/AndroidStudioProjects/SPJAPPAPK/app/src/main/java/com/example/spjappapk/data/sync/SyncWorker.kt)
Mengubah target tabel sinkronisasi ke `sales_orders_queue` dengan format payload JSON sesuai `gemini.md`.

### [UI Layer]

#### [MODIFY] [LoginScreen.kt](file:///C:/Users/SPJ%20Admin/AndroidStudioProjects/SPJAPPAPK/app/src/main/java/com/example/spjappapk/ui/auth/LoginScreen.kt)
Menyesuaikan input dari Email ke Username/ID.

## Verification Plan

### Automated Tests
- Menjalankan build untuk memastikan tidak ada konflik tipe data.

### Manual Verification
- Verifikasi proses Login terhadap tabel `sales_persons`.
- Verifikasi pengiriman data Sales Order masuk ke tabel `sales_orders_queue` di Supabase.
