package com.example.SimpanBuku.service;

i
import com.example.SimpanBuku.Models.Buku;
import com.example.SimpanBuku.repository.BukuRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SERVICE — Spring IoC + Java Stream
 *
 * === SPRING IoC (Constructor Injection) ===
 * @Service memberitahu Spring untuk mendaftarkan class ini sebagai Bean.
 * Kita menggunakan CONSTRUCTOR INJECTION (bukan @Autowired di field)
 * karena:
 *   1. Field bisa final → immutable dan thread-safe
 *   2. Dependency terlihat jelas
 *   3. Mudah di-unit test tanpa Spring context
 *   4. Spring 4.3+ otomatis inject tanpa perlu @Autowired jika constructor tunggal
 *
 * === JAVA STREAM ===
 * Digunakan untuk mengolah data dari database dengan cara deklaratif
 * (menggantikan for-loop manual). Setiap method di bawah menunjukkan
 * penggunaan intermediate operation (filter, map, sorted, etc.)
 * dan terminal operation (collect, count, etc.).
 */
@Service
public class BukuService {

    // final → immutable setelah constructor — best practice IoC
    private final BukuRepository bukuRepository;

    /**
     * Constructor Injection — Spring otomatis mendeteksi dan meng-inject
     * BukuRepository Bean yang sudah terdaftar via @Repository.
     */
    public BukuService(BukuRepository bukuRepository) {
        this.bukuRepository = bukuRepository;
    }

    // ================================================================
    // CRUD DASAR
    // ================================================================

    public BukuResponse simpanBuku(BukuRequest request) {
        Buku buku = new Buku();
        buku.setJudul(request.getJudul());
        buku.setPenulis(request.getPenulis());
        buku.setGenre(request.getGenre());
        buku.setTahunTerbit(request.getTahunTerbit());
        buku.setHarga(request.getHarga());
        buku.setStok(request.getStok());
        buku.setTanggalMasuk(
            request.getTanggalMasuk() != null ? request.getTanggalMasuk() : LocalDate.now()
        );
        buku.setTersedia(request.getStok() > 0);

        Buku saved = bukuRepository.save(buku);
        return toResponse(saved);
    }

    public BukuResponse getBukuById(Long id) {
        Buku buku = bukuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Buku", id));
        return toResponse(buku);
    }

    public BukuResponse updateBuku(Long id, BukuRequest request) {
        Buku buku = bukuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Buku", id));

        buku.setJudul(request.getJudul());
        buku.setPenulis(request.getPenulis());
        buku.setGenre(request.getGenre());
        buku.setTahunTerbit(request.getTahunTerbit());
        buku.setHarga(request.getHarga());
        buku.setStok(request.getStok());
        buku.setTersedia(request.getStok() > 0);

        return toResponse(bukuRepository.save(buku));
    }

    public void hapusBuku(Long id) {
        if (!bukuRepository.existsById(id)) {
            throw new ResourceNotFoundException("Buku", id);
        }
        bukuRepository.deleteById(id);
    }

   
    public List<BukuResponse> getAllBuku() {
        return bukuRepository.findAll()
                .stream()
                .map(this::toResponse)          // intermediate: transform tiap Buku → BukuResponse
                .collect(Collectors.toList());  // terminal: kumpulkan hasil
    }

    public List<BukuResponse> getBukuTersediaSortedByHarga() {
        return bukuRepository.findAll()
                .stream()
                .filter(buku -> buku.getTersedia() && buku.getStok() > 0) // intermediate: filter
                .sorted(Comparator.comparingDouble(Buku::getHarga))       // intermediate: sort ascending
                .map(this::toResponse)                                     // intermediate: transform
                .collect(Collectors.toList());                             // terminal
    }

   
    public Map<String, List<BukuResponse>> getBukuGroupByGenre() {
        return bukuRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        Buku::getGenre,                              // terminal: kelompokkan by genre
                        Collectors.mapping(this::toResponse,         // transform tiap item dalam grup
                                Collectors.toList())
                ));
    }

 
    public Map<String, Double> getNilaiStokPerGenre() {
        return bukuRepository.findAll()
                .stream()
                .filter(Buku::getTersedia)                          // intermediate: hanya yang tersedia
                .collect(Collectors.groupingBy(
                        Buku::getGenre,                             // group by genre
                        Collectors.summingDouble(                   // terminal: jumlah harga × stok
                                b -> b.getHarga() * b.getStok()
                        )
                ));
    }

   
    public List<BukuResponse> cariBuku(String keyword) {
        return bukuRepository.cariByJudulAtauPenulis(keyword) // Native SQL
                .stream()
                .map(this::toResponse)                         // intermediate: transform
                .sorted(Comparator.comparing(BukuResponse::getJudul)) // intermediate: sort A-Z
                .collect(Collectors.toList());                 // terminal
    }

    /**
     * Stream #6: Proses hasil Native SQL agregasi (Object[]) ke DTO
     * Menggabungkan dua hasil native query menggunakan Stream
     */
    public List<StatistikGenreResponse> getStatistikGenre() {
        // Ambil data dari dua Native SQL query
        List<Object[]> jumlahPerGenre = bukuRepository.hitungBukuPerGenre();
        List<Object[]> hargaPerGenre  = bukuRepository.statistikHargaPerGenre();

        // Buat Map dari harga per genre untuk lookup cepat
        // Stream: Object[] → Map<genre, Object[]>
        Map<String, Object[]> hargaMap = hargaPerGenre
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // key: genre
                        row -> row               // value: seluruh row
                ));

        // Gabungkan dua hasil query menggunakan Stream
        return jumlahPerGenre
                .stream()
                .map(row -> {
                    String genre        = (String) row[0];
                    Long   jumlah       = ((Number) row[1]).longValue();
                    Object[] hargaRow   = hargaMap.getOrDefault(genre, new Object[]{genre, 0, 0, 0});

                    return new StatistikGenreResponse(
                            genre,
                            jumlah,
                            ((Number) hargaRow[1]).doubleValue(),
                            ((Number) hargaRow[2]).doubleValue(),
                            ((Number) hargaRow[3]).doubleValue()
                    );
                })
                .collect(Collectors.toList()); // terminal
    }

    
    public BukuResponse tambahStok(Long id, Integer jumlah) {
        int updated = bukuRepository.tambahStok(id, jumlah); // Native SQL @Modifying
        if (updated == 0) {
            throw new ResourceNotFoundException("Buku", id);
        }
        Buku buku = bukuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Buku", id));
        buku.setTersedia(buku.getStok() > 0);
        return toResponse(bukuRepository.save(buku));
    }

    
    private BukuResponse toResponse(Buku buku) {
        BukuResponse response = new BukuResponse();
        response.setId(buku.getId());
        response.setJudul(buku.getJudul());
        response.setPenulis(buku.getPenulis());
        response.setGenre(buku.getGenre());
        response.setTahunTerbit(buku.getTahunTerbit());
        response.setHarga(buku.getHarga());
        response.setStok(buku.getStok());
        response.setTanggalMasuk(buku.getTanggalMasuk());
        response.setTersedia(buku.getTersedia());

        // Logika status stok — dihitung di Java, bukan di database
        String statusStok;
        if (buku.getStok() == 0) {
            statusStok = "HABIS";
        } else if (buku.getStok() <= 5) {
            statusStok = "HAMPIR HABIS";
        } else {
            statusStok = "TERSEDIA";
        }
        response.setStatusStok(statusStok);

        return response;
    }
}
