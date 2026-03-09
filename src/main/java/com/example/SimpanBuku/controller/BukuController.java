package com.example.SimpanBuku.controller;

import com.example.SimpanBuku.dto.BukuRequest;
import com.example.SimpanBuku.dto.BukuResponse;
import com.example.SimpanBuku.service.BukuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/buku")
public class BukuController {

    
     
    public BukuController(BukuService bukuService) {
        this.bukuService = bukuService;
    }
=============================================

    @PostMapping
    public ResponseEntity<BukuResponse> tambahBuku(@Valid @RequestBody BukuRequest request) {
        BukuResponse response = bukuService.simpanBuku(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    
    @GetMapping("/{id}")
    public ResponseEntity<BukuResponse> getBukuById(@PathVariable Long id) {
        return ResponseEntity.ok(bukuService.getBukuById(id));
    }

  
    @PutMapping("/{id}")
    public ResponseEntity<BukuResponse> updateBuku(
            @PathVariable Long id,
            @Valid @RequestBody BukuRequest request) {
        return ResponseEntity.ok(bukuService.updateBuku(id, request));
    }

   
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hapusBuku(@PathVariable Long id) {
        bukuService.hapusBuku(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    public ResponseEntity<List<BukuResponse>> getAllBuku() {
        return ResponseEntity.ok(bukuService.getAllBuku());
    }

  
    @GetMapping("/tersedia")
    public ResponseEntity<List<BukuResponse>> getBukuTersedia() {
        return ResponseEntity.ok(bukuService.getBukuTersediaSortedByHarga());
    }

   
    @GetMapping("/by-genre")
    public ResponseEntity<Map<String, List<BukuResponse>>> getBukuByGenre() {
        return ResponseEntity.ok(bukuService.getBukuGroupByGenre());
    }

    /**
     * GET /api/buku/nilai-stok — Total nilai stok per genre
     * Service: Stream filter → groupingBy → summingDouble
     */
    @GetMapping("/nilai-stok")
    public ResponseEntity<Map<String, Double>> getNilaiStok() {
        return ResponseEntity.ok(bukuService.getNilaiStokPerGenre());
    }

    /**
     * GET /api/buku/cari?keyword=xxx — Cari buku
     * Repository: Native SQL LIKE
     * Service: Stream sort → collect
     */
    @GetMapping("/cari")
    public ResponseEntity<List<BukuResponse>> cariBuku(@RequestParam String keyword) {
        return ResponseEntity.ok(bukuService.cariBuku(keyword));
    }

    /**
     * GET /api/buku/statistik — Statistik per genre
     * Repository: Native SQL GROUP BY + agregasi
     * Service: Stream map (Object[] → DTO) → collect
     */
    @GetMapping("/statistik")
    public ResponseEntity<List<StatistikGenreResponse>> getStatistik() {
        return ResponseEntity.ok(bukuService.getStatistikGenre());
    }

    /**
     * PATCH /api/buku/{id}/stok?tambah=N — Tambah stok
     * Repository: Native SQL @Modifying UPDATE
     */
    @PatchMapping("/{id}/stok")
    public ResponseEntity<BukuResponse> tambahStok(
            @PathVariable Long id,
            @RequestParam Integer tambah) {
        return ResponseEntity.ok(bukuService.tambahStok(id, tambah));
    }
}
