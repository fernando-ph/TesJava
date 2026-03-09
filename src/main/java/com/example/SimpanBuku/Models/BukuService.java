package com.example.SimpanBuku.Models;

import com.example.SimpanBuku.dto.BukuRequest;
import com.example.SimpanBuku.dto.BukuResponse;
import com.example.SimpanBuku.dto.StatistikGenreResponse;
import com.example.SimpanBuku.exception.ResourceNotFoundException;
import com.example.SimpanBuku.model.Buku;
import com.example.SimpanBuku.repository.BukuRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class BukuService {

   
    private final BukuRepository bukuRepository;

   
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

    /**
     * Stream #2: Filter buku tersedia dan urutkan berdasarkan harga
     * Operasi: filter → sorted → map → collect
     *
     * Stream Pipeline:
     *   List<Buku> → [filter tersedia] → [sorted by harga] → [map] → List<BukuResponse>
     */
    public List<BukuResponse> getBukuTersediaSortedByHarga() {
        return bukuRepository.findAll()
                .stream()
                .filter(buku -> buku.getTersedia() && buku.getStok() > 0) // intermediate: filter
                .sorted(Comparator.comparingDouble(Buku::getHarga))       // intermediate: sort ascending
                .map(this::toResponse)                                     // intermediate: transform
                .collect(Collectors.toList());                             // terminal
    }

    /**
     * Stream #3: Kelompokkan buku berdasarkan genre
     * Operasi: collect dengan Collectors.groupingBy
     *
     * Stream Pipeline:
     *   List<Buku> → [groupingBy genre] → Map<String, List<BukuResponse>>
     */
    public Map<String, List<BukuResponse>> getBukuGroupByGenre() {
        return bukuRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        Buku::getGenre,                              // terminal: kelompokkan by genre
                        Collectors.mapping(this::toResponse,         // transform tiap item dalam grup
                                Collectors.toList())
                ));
    }

    /**
     * Stream #4: Hitung total nilai stok (harga × stok) per genre
     * Operasi: filter → collect groupingBy dengan summingDouble
     *
     * Stream Pipeline:
     *   List<Buku> → [filter tersedia] → [group by genre, sum nilai] → Map<String, Double>
     */
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

    /**
     * Stream #5: Cari buku via Native SQL, lalu proses hasilnya dengan Stream
     * Gabungan Native SQL Query + Java Stream
     */
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

      
        Map<String, Object[]> hargaMap = hargaPerGenre
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // key: genre
                        row -> row               // value: seluruh row
                ));

        return jumlahPerGenre
                .stream()
                .map(row -> {
                    String genre        = (Strig) row[0];
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
                .collect(Collectors.toList()); 
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
