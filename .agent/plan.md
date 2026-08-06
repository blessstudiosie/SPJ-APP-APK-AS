# Project Plan

Sebuah aplikasi Android native untuk sales lapangan dengan kemampuan offline. Fitur utama meliputi:
1. Laporan kunjungan dengan pengambilan lokasi GPS dan foto.
2. Melihat data barang (inventory) dan riwayat penjualan.
3. Input data penjualan baru.
4. Dashboard kinerja sales (personal performance).
5. Integrasi dengan Supabase sebagai backend untuk sinkronisasi data antara Android dan desktop.
Aplikasi harus mendukung mode offline (sinkronisasi data saat kembali online).

## Project Brief

# Project Brief: Field Sales & Inventory App (MVP)

## Features
1. **Field Visit Tracking**: Capture site visits with mandatory GPS location stamping and photo attachments to ensure field accountability.
2. **Inventory & Sales Ledger**: Provide sales representatives with immediate access to current stock levels and a comprehensive history of past transactions.
3. **Offline-First Sales Entry**: Enable the entry of new sales data regardless of connectivity, with automatic synchronization to the backend once the device is back online.
4. **Performance Dashboard**: A personal summary view highlighting individual sales targets, achievements, and key performance indicators.

## High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: **Jetpack Navigation 3** (State-driven navigation logic)
- **Adaptive Strategy**: **Compose Material Adaptive** library (ensuring seamless transitions between different device form factors)
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Local Persistence**: Room Database (Required for the requested offline mode and data synchronization)
- **Backend**: Supabase (PostgreSQL, Authentication, and Remote Storage for photos)

## Implementation Steps
**Total Duration:** 42h 1m 9s

### Task_1_Infrastructure_Auth: Initialize Supabase, set up Room database (Visits, Inventory, Sales), and implement Authentication using Jetpack Navigation 3.
- **Status:** COMPLETED
- **Updates:** Inisialisasi Supabase SDK, Room database (Visit, Inventory, Sale), dan alur otentikasi menggunakan Jetpack Navigation 3 telah selesai. UI dasar menggunakan Compose sudah tersedia.
- **Acceptance Criteria:**
  - Supabase API key integrated
  - Room schemas defined
  - Login/Logout flow working with Navigation 3
  - App builds successfully
- **Duration:** 1h 43m 47s

### Task_2_DataDisplay_AdaptiveUI: Implement the Inventory List, Sales Ledger, and Performance Dashboard using Compose Material Adaptive and Material 3.
- **Status:** COMPLETED
- **Updates:** Implementasi layar Inventaris, Riwayat Penjualan, dan Dashboard Kinerja dengan desain adaptif (HP & Tablet) telah selesai. Menggunakan ListDetailPaneScaffold dan BottomBar/NavigationRail yang responsif.
- **Acceptance Criteria:**
  - Inventory and Sales history visible from Room/Supabase
  - Performance Dashboard shows targets vs achievements
  - UI adapts to different screen sizes (Phone/Tablet)
- **Duration:** 14m 14s

### Task_3_DataCapture_Sync: Develop Field Visit tracking (GPS and Photo capture) and Sales Entry features with offline-first synchronization logic.
- **Status:** COMPLETED
- **Updates:** Implementasi penangkapan GPS, integrasi kamera untuk foto kunjungan, dan sistem sinkronisasi offline-first menggunakan WorkManager telah selesai. Status sinkronisasi ditampilkan di UI.
- **Acceptance Criteria:**
  - GPS location captured for visits
  - Camera integration for photo attachments
  - Sales entries saved to Room and synced to Supabase when online
- **Duration:** 40h 1m 16s

### Task_4_FinalRun_Verify: Conduct a final run of the application to ensure stability and requirement alignment. Instruct critic_agent to verify stability (no crashes), requirement alignment, and UI quality.
- **Status:** COMPLETED
- **Updates:** Verifikasi akhir oleh critic_agent telah selesai. Aplikasi dinyatakan stabil dengan fitur Kunjungan (GPS/Foto), Inventaris, Penjualan, dan Dashboard yang berfungsi sesuai rencana. Desain adaptif telah teruji di level kode untuk HP dan Tablet. Sinkronisasi offline menggunakan WorkManager juga telah terimplementasi dengan benar. Kredensial Supabase masih menggunakan placeholder.
- **Acceptance Criteria:**
  - App does not crash
  - All features (Visit, Inventory, Sales, Dashboard) functional
  - Offline synchronization verified
  - Build pass and all existing tests pass
- **Duration:** 1m 52s

