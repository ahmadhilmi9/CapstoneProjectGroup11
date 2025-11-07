# Dokumentasi Perubahan Constraint - ScheduleGenerator

## Tanggal: 6 November 2025

### Perubahan yang Diimplementasikan:

## 1. ✅ PJOK 3 Jam - Pola 2-1 (Bukan 3 Langsung)

**Sebelum:**
- PJOK 3 jam dijadwalkan sebagai 1 blok berurutan (jam 1-2-3 atau 2-3-4)

**Sesudah:**
- PJOK 3 jam HARUS dibagi menjadi pola **2-1**:
  - **2 jam berurutan** pertama: maksimal dimulai di **jam ke-4** (selesai jam 5)
  - **1 jam sisanya**: bebas bisa di jam berapa saja (sampai jam 10)
  
**Contoh:**
- ✅ Benar: PJOK jam 3-4 (Senin) + jam 7 (Rabu)
- ✅ Benar: PJOK jam 1-2 (Selasa) + jam 9 (Kamis)
- ❌ Salah: PJOK jam 5-6 (2 jam berurutan mulai jam 5 - melanggar constraint)
- ❌ Salah: PJOK jam 1-2-3 (3 jam langsung - tidak sesuai pola)

**Implementasi:**
```java
SUBJECT_DISTRIBUTION_PATTERNS.put("PJOK_3", new int[]{2, 1});
```

---

## 2. ✅ MGMP Berdasarkan ID Guru (Bukan Hanya Mapel)

**Sebelum:**
- MGMP hanya dilihat dari mata pelajarannya (SKI, B.ARAB, AQIDAH AKHLAK, dll)

**Sesudah:**
- MGMP ditentukan berdasarkan **ID GURU**
- Jika seorang guru mengajar mata pelajaran MGMP (SKI, B.ARAB, AQIDAH AKHLAK, QURDITS, FIQIH, AL-QUR'AN HADITS, BAHASA ARAB), maka:
  - Guru tersebut masuk kategori "MGMP Teacher"
  - **SEMUA jadwal guru tersebut** (termasuk mapel non-MGMP yang diajarnya) akan terkena constraint MGMP

**Constraint MGMP:**
- Di hari **Rabu**: maksimal sampai **jam ke-4** saja
- Hari lain: bebas

**Contoh:**
- Pak Ahmad mengajar:
  - SKI (mapel MGMP) untuk kelas 7A - 2 jam
  - Bahasa Indonesia (bukan mapel MGMP) untuk kelas 7B - 4 jam
  
- Karena Pak Ahmad mengajar SKI (mapel MGMP), maka:
  - ✅ Semua jadwal Pak Ahmad di hari Rabu (termasuk B. Indonesia) hanya bisa sampai jam 4
  - ✅ Hari Senin-Selasa-Kamis-Jumat bebas tidak ada batasan

**Implementasi:**
```java
private Set<String> identifyMGMPTeachers() {
    Set<String> teachers = new HashSet<>();
    // MGMP ditentukan berdasarkan GURU yang mengajar mapel MGMP (berdasarkan ID guru)
    for (Assignment assignment : assignments) {
        if (isMGMPSubject(assignment.getSubject())) {
            teachers.add(assignment.getTeacher()); // Tambahkan guru ke set MGMP
        }
    }
    return teachers;
}
```

---

## 3. ✅ Semua Mapel Harus Berurutan (Tidak Boleh Terpisah)

**Sebelum:**
- IPS 3 jam bisa dijadwalkan: Senin jam 1, 3, 5 (tidak berurutan)

**Sesudah:**
- Semua mata pelajaran HARUS dijadwalkan **berurutan** dalam blok
- IPS 3 jam: **2-1** atau **3 berurutan** (prioritas 2-1 untuk distribusi lebih merata)

**Pola Distribusi yang Diperbarui:**

| Mata Pelajaran | Total Jam | Pola Distribusi | Keterangan |
|----------------|-----------|-----------------|------------|
| **Matematika** | 5 jam | **3 + 2** | 3 jam berurutan + 2 jam berurutan (di hari berbeda) |
| **IPA** | 5 jam | **3 + 2** | 3 jam berurutan + 2 jam berurutan |
| **B. Indonesia** | 6 jam | **2 + 2 + 2** | 3 sesi, masing-masing 2 jam berurutan |
| **B. Inggris** | 4 jam | **2 + 2** | 2 sesi, masing-masing 2 jam berurutan |
| **IPS** | 4 jam | **2 + 2** | 2 sesi, masing-masing 2 jam berurutan |
| **PJOK** | 3 jam | **2 + 1** | 2 jam berurutan (max jam 4) + 1 jam bebas |
| **Mapel lain** | 3 jam | **2 + 1** atau **3** | Prioritas 2-1, fallback ke 3 berurutan |
| **Mapel lain** | 2 jam | **2** | 2 jam berurutan dalam 1 sesi |
| **Mapel lain** | 1 jam | **1** | 1 jam |

**Contoh:**
- ✅ IPS 3 jam: Senin jam 1-2 + Rabu jam 5 (pola 2-1)
- ✅ IPS 3 jam: Senin jam 1-2-3 (pola 3 berurutan jika 2-1 tidak memungkinkan)
- ✅ SKI 3 jam: Selasa jam 2-3 + Kamis jam 4 (pola 2-1)
- ❌ IPS 3 jam: Senin jam 1, 3, 5 (tidak berurutan - tidak akan terjadi lagi)

**Implementasi:**
```java
// Untuk mapel 3 jam (selain PJOK)
SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SPLIT", new int[]{2, 1}); // Prioritas
SUBJECT_DISTRIBUTION_PATTERNS.put("DEFAULT_3_SINGLE", new int[]{3});   // Fallback

// Di getDistributionPattern():
if (totalHours == 3) {
    // Prioritas: 2-1 untuk distribusi lebih merata
    return SUBJECT_DISTRIBUTION_PATTERNS.get("DEFAULT_3_SPLIT");
}
```

---

## Ringkasan Perubahan:

### ✅ YANG SUDAH DIPERBAIKI:
1. **PJOK 3 jam** → Pola 2-1 (2 jam berurutan max jam 4, 1 jam bebas)
2. **MGMP** → Berdasarkan ID Guru (semua mapel guru MGMP terkena constraint)
3. **Berurutan** → Semua mapel dijadwalkan dalam blok berurutan
4. **Mapel 3 jam** → Prioritas pola 2-1, fallback ke 3 berurutan untuk fleksibilitas

### 🎯 HASIL YANG DIHARAPKAN:
- Jadwal lebih rapi dan terstruktur
- Semua constraint terpenuhi (PJOK, MGMP, dll)
- Jam pelajaran berurutan, tidak loncat-loncat
- Distribusi lebih merata antar hari

---

## Kompilasi:

```bash
mvn clean compile
```

Status: ✅ **BERHASIL** - Kode dikompilasi tanpa error

## Cara Test:
1. Jalankan aplikasi
2. Load file Excel
3. Generate jadwal
4. Periksa hasilnya - sekarang semua mapel akan berurutan dengan pola yang lebih rapi!

---

## Catatan Penting:

- Algoritma menggunakan **Hybrid Metaheuristic** dengan bonus scoring untuk slot berurutan
- Pola 2-1 diprioritaskan untuk mapel 3 jam (lebih merata)
- Jika 2-1 tidak memungkinkan, akan fallback ke 3 berurutan
- PJOK tetap strict 2-1 dengan constraint jam 4
- MGMP strict berdasarkan ID guru, bukan mapel saja
