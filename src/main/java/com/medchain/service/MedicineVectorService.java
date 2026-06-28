package com.medchain.service;

import com.medchain.entity.Medicine;
import com.medchain.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Medicine search service.
 * Vector/semantic search is not available (pgvector removed).
 * Falls back to database keyword search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineVectorService {

    private final MedicineRepository medicineRepository;

    /**
     * No-op: indexing is handled by the DB. Previously used pgvector.
     */
    public void indexMedicine(Medicine medicine) {
        log.debug("Medicine available in DB (vector indexing disabled): {}", medicine.getName());
    }

    /**
     * Fallback to DB-based search by name/description instead of semantic vector search.
     */
    public List<UUID> semanticSearch(String query, int topK) {
        try {
            return medicineRepository.findAll().stream()
                    .filter(m -> m.getName() != null && m.getName().toLowerCase().contains(query.toLowerCase())
                            || m.getDescription() != null && m.getDescription().toLowerCase().contains(query.toLowerCase()))
                    .limit(topK)
                    .map(Medicine::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to perform medicine search", e);
            return List.of();
        }
    }
}
