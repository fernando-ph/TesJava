package com.example.SimpanBuku.repository;

import com.example.SimpanBuku.Models.BukuService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


 
@Repository
public interface BukuRepository extends JpaRepository<Buku, Long> {

  

    List<Buku> findByGenre(String genre);

    List<Buku> findByTersedia(Boolean tersedia);

   
    @Query(
        value = "SELECT * FROM buku WHERE judul LIKE %:keyword% OR penulis LIKE %:keyword%",
        nativeQuery = true
    )
    List<Buku> cariByJudulAtauPenulis(@Param("keyword") String keyword);

  
    @Query(
        value = "SELECT * FROM buku WHERE harga BETWEEN :hargaMin AND :hargaMax AND stok > 0 ORDER BY harga ASC",
        nativeQuery = true
    )
    List<Buku> findByRangeHargaDanStokTersedia(
        @Param("hargaMin") Double hargaMin,
        @Param("hargaMax") Double hargaMax
    );

   
    @Query(
        value = "SELECT genre, COUNT(*) as jumlah FROM buku GROUP BY genre ORDER BY jumlah DESC",
        nativeQuery = true
    )
    List<Object[]> hitungBukuPerGenre();

   
    @Query(
        value = """
            SELECT genre,
                   MIN(harga)  AS harga_min,
                   MAX(harga)  AS harga_max,
                   AVG(harga)  AS harga_rata
            FROM buku
            GROUP BY genre
            """,
        nativeQuery = true
    )
    List<Object[]> statistikHargaPerGenre();

  
    @Modifying
    @Transactional
    @Query(
        value = "UPDATE buku SET stok = stok + :tambah WHERE id = :id",
        nativeQuery = true
    )
    int tambahStok(@Param("id") Long id, @Param("tambah") Integer tambah);

    
    @Query(
        value = "SELECT * FROM buku WHERE tersedia = true ORDER BY harga DESC LIMIT :limit",
        nativeQuery = true
    )
    List<Buku> findTopBukuTermahal(@Param("limit") int limit);
}
