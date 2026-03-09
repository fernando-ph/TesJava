package com.example.SimpanBuku;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SimpanBukuApplicationTests {
 import com.example.SimpanBuku.dto.BukuRequest;
import com.example.SimpanBuku.dto.BukuResponse;
import com.example.SimpanBuku.model.Buku;
import com.example.SimpanBuku.repository.BukuRepository;
import com.example.SimpanBuku.service.BukuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
 import java.time.LocalDate;
import java.util.List;
import java.util.Map;
 import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
class SimpanBukuApplicationTests {
  @Autowired
private BukuService bukuService;
  @Autowired
private BukuRepository bukuRepository;
  // ================================================================
// SETUP â€” Buat data buku sebelum setiap test
// ================================================================
  @BeforeEach    void setUp() {
bukuRepository.deleteAll();
// Buku 1
Buku b1 = new Buku();
b1.setJudul("Laskar Pelangi");
b1.setPenulis("Andrea Hirata");
b1.setGenre("Novel");
b1.setTahunTerbit(2005);
b1.setHarga(85000.0);
b1.setStok(10);
b1.setTanggalMasuk(LocalDate.now());
b1.setTersedia(true);
// Buku 2
Buku b2 = new Buku();
b2.setJudul("Bumi Manusia");
b2.setPenulis("Pramoedya Ananta Toer");
b2.setGenre("Novel");
b2.setTahunTerbit(1980);
b2.setHarga(95000.0);
b2.setStok(5);
b2.setTanggalMasuk(LocalDate.now());
b2.setTersedia(true);
// Buku 3 â€” stok habis
Buku b3 = new Buku();
b3.setJudul("Clean Code");
b3.setPenulis("Robert C. Martin");
b3.setGenre("Teknologi");
b3.setTahunTerbit(2008);
b3.setHarga(220000.0);
b3.setStok(0);
b3.setTanggalMasuk(LocalDate.now());
b3.setTersedia(false);
// Buku 4
Buku b4 = new Buku();
b4.setJudul("Design Patterns");
b4.setPenulis("Gang of Four");
b4.setGenre("Teknologi");
b4.setTahunTerbit(1994);
b4.setHarga(300000.0);
b4.setStok(3);
b4.setTanggalMasuk(LocalDate.now());
b4.setTersedia(true);
bukuRepository.saveAll(List.of(b1, b2, b3, b4));
}

  @Test
@DisplayName("Spring IoC: Context berhasil di-load dan semua Bean ter-inject")
void contextLoads() {
// Jika test ini pass â†’ Spring IoC berhasil inject semua dependency
assertThat(bukuService).isNotNull();
assertThat(bukuRepository).isNotNull();
}

  @Test
@DisplayName("Stream: getAllBuku() mengembalikan semua buku dengan statusStok yang
benar")
void testGetAllBukuDenganStatusStok() {
List<BukuResponse> semua = bukuService.getAllBuku();
  assertThat(semua).hasSize(4);
  // Verifikasi statusStok dihitung benar oleh Stream mapper
BukuResponse cleanCode = semua.stream()
.filter(b -> b.getJudul().equals("Clean Code"))
.findFirst().orElseThrow();
assertThat(cleanCode.getStatusStok()).isEqualTo("HABIS");
  BukuResponse designPatterns = semua.stream()
.filter(b -> b.getJudul().equals("Design Patterns"))
.findFirst().orElseThrow();
assertThat(designPatterns.getStatusStok()).isEqualTo("HAMPIR HABIS");
  BukuResponse laskarPelangi = semua.stream()
.filter(b -> b.getJudul().equals("Laskar Pelangi"))
.findFirst().orElseThrow();
assertThat(laskarPelangi.getStatusStok()).isEqualTo("TERSEDIA");
}
  @Test
@DisplayName("Stream: getBukuTersedia() hanya menampilkan buku dengan stok > 0,
}
@Test
@DisplayName("Stream: getBukuGroupByGenre() mengelompokkan buku by genre dengan
void testGroupByGenre() {
Map<String, List<BukuResponse>> grouped = bukuService.getBukuGroupByGenre();
}
assertThat(grouped).containsKeys("Novel", "Teknologi");
assertThat(grouped.get("Novel")).hasSize(2);
assertThat(grouped.get("Teknologi")).hasSize(2);
@Test
@DisplayName("Stream: getNilaiStokPerGenre() menghitung total nilai stok dengan benar")
void testNilaiStokPerGenre() {
Map<String, Double> nilaiStok = bukuService.getNilaiStokPerGenre();
assertThat(nilaiStok.get("Novel")).isEqualTo(1325000.0);
=====================================
@Test
@DisplayName("Native SQL: cariByJudulAtauPenulis() menemukan buku dengan keyword")
void testCariByKeyword() {
List<BukuResponse> hasil = bukuService.cariBuku("Pramoedya");
assertThat(hasil).hasSize(1);
assertThat(hasil.get(0).getJudul()).isEqualTo("BumiManusia");          }}
@Test
@DisplayName("Native SQL: cari dengan keyword judul menemukan buku yang tepat")
void testCariByJudul() {
List<BukuResponse> hasil = bukuService.cariBuku("Code");
}
assertThat(hasil).hasSize(1);
assertThat(hasil.get(0).getJudul()).isEqualTo("CleanCode");
@Test
@DisplayName("Native SQL: statistik genre mengembalikan data agregasi yang benar")
void testStatistikGenre() {
var statistik = bukuService.getStatistikGenre();
assertThat(statistik).isNotEmpty();
var novelStats = statistik.stream()
.filter(s -> s.getGenre().equals("Novel"))
.findFirst().orElseThrow();
}
assertThat(novelStats.getJumlahBuku()).isEqualTo(2L);
assertThat(novelStats.getHargaMin()).isEqualTo(85000.0);
assertThat(novelStats.getHargaMax()).isEqualTo(95000.0);
@Test
@DisplayName("Native SQL @Modifying: tambahStok() mengupdate stok dengan benar")
void testTambahStok() {
Buku laskarPelangi = bukuRepository.findAll().stream()
.filter(b -> b.getJudul().equals("Laskar Pelangi"))
.findFirst().orElseThrow();
int stokAwal = laskarPelangi.getStok(); 
BukuResponse result = bukuService.tambahStok(laskarPelangi.getId(), 5);
assertThat(result.getStok()).isEqualTo(stokAwal + 5); // 15
}}