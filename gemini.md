# Panduan Proyek & Arsitektur - Aplikasi Android Sales SPJ

Dokumen ini adalah panduan teknis dan arsitektur untuk pengembangan aplikasi Android Sales SPJ. Tujuannya adalah untuk memastikan aplikasi mobile berfungsi sebagai klien yang efektif dalam ekosistem SPJ, di mana aplikasi desktop bertindak sebagai *single source of truth*.

## Prinsip Arsitektur Fundamental

1.  **Aplikasi Desktop adalah Master Data:** Pemegang data utama. Aplikasi Android mengirim "permintaan perubahan" (Change Requests) alih-alih memodifikasi tabel utama secara langsung.
2.  **Offline-First & Auto Sync:** Data disimpan secara lokal di Room DB. Aplikasi melakukan sinkronisasi otomatis (Periodic Sync) di latar belakang setiap jam untuk memperbarui Master Data dan mengirim antrian yang tertunda.
3.  **Atomisitas Data (Rollback):** Jika ada kegagalan penyimpanan data sebagian, seluruh transaksi tersebut harus di-*rollback* untuk menjaga integritas data.
4.  **Branding & UI Profesional:** Menggunakan tema terang (Light Mode) yang bersih dan profesional dengan navigasi *Sidebar Menu* (Drawer) untuk memaksimalkan ruang layar.

## Alur Kerja Utama

### 1. Keamanan & Login
- **Verifikasi Nama:** Login menggunakan Nama Lengkap dan Password terhadap tabel `sales_persons`.
- **Kebijakan Password:** Jika password masih default ("ganti123"), user **wajib** melakukan perubahan password sebelum dapat mengakses fitur aplikasi.

### 2. Modul Penjualan (Sales Order - SO)
- **Multi-Item:** Sales dapat memasukkan lebih dari satu item produk untuk satu pelanggan dalam satu nomor pesanan.
- **Data Master:** Pelanggan dan Produk dipilih dari data yang sudah di-cache secara lokal.
- **Antrian:** Data dikirim ke tabel `sales_orders_queue` di Supabase dengan status `pending` untuk diproses oleh aplikasi desktop.

### 3. Modul Kunjungan (Visit)
- **Bukti Digital:** Setiap kunjungan wajib merekam lokasi GPS dan foto user di lokasi.
- **Entri Pelanggan Baru:** Jika nama pelanggan tidak ditemukan saat kunjungan, user diperbolehkan membuat data pelanggan baru secara instan di lapangan.
- **Antrian:** Data dikirim ke `visit_logs_queue`.

### 4. Dashboard Berbasis Peran (Role-Based)
- **Role Sales:** Hanya menampilkan ringkasan pencapaian pribadi (total order, omzet, kunjungan hari ini).
- **Role Manager:** Dapat melihat hasil pencapaian dari seluruh user lainnya (akumulasi tim).

## Standar Teknis

- **Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room DB, Supabase (Postgrest, Auth, Storage), WorkManager.
- **Versi Aplikasi:** Selalu tampilkan nomor versi di layar Login dan Dashboard untuk memudahkan pelacakan pembaruan.
- **Penanganan Error:** Setiap operasi jaringan harus dibungkus dalam blok try-catch dengan mekanisme retry atau rollback yang jelas.
