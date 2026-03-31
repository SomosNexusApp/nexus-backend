package com.nexus.repository;

import com.nexus.entity.EstadoProducto;
import com.nexus.entity.Producto;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductoSpecification {

    public static Specification<Producto> buscarConFiltros(
            List<String> searchTerms,
            String categoria,
            Double precioMin,
            Double precioMax,
            Integer vendedorId,
            List<Integer> excludedVendedorIds,
            Double minLat, Double maxLat, Double minLng, Double maxLng) {

        return (root, query, cb) -> {

            boolean isCount = Long.class.equals(query.getResultType())
                    || long.class.equals(query.getResultType());
            if (!isCount) {
                root.fetch("vendedor", JoinType.INNER);
                root.fetch("categoria", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> where = new ArrayList<>();

            // Si se busca por vendedor específico, no filtramos por DISPONIBLE
            // para que el usuario pueda ver sus propios productos VENDIDOS o RESERVADOS
            if (vendedorId != null) {
                Join<Object, Object> vendedorJoin = root.join("vendedor", JoinType.INNER);
                where.add(cb.equal(vendedorJoin.get("id"), vendedorId));
                // Opcional: No mostrar ELIMINADO a menos que se quiera explícitamente, pero el
                // frontend asume VENDIDO / DISPONIBLE / RESERVADO
                where.add(cb.notEqual(root.get("estado"), EstadoProducto.ELIMINADO));
            } else {
                where.add(cb.equal(root.get("estado"), EstadoProducto.DISPONIBLE));
            }

            // ── Sinónimos: OR entre todos los términos expandidos ─────────
            if (searchTerms != null && !searchTerms.isEmpty()) {
                List<Predicate> termOr = new ArrayList<>();
                for (String term : searchTerms) {
                    String p = "%" + term.toLowerCase() + "%";
                    // Solo titulo y descripcion — Producto NO tiene marca/modelo
                    termOr.add(cb.or(
                            cb.like(cb.lower(root.get("titulo")), p),
                            cb.like(cb.lower(root.get("descripcion")), p)));
                }
                where.add(cb.or(termOr.toArray(new Predicate[0])));
            }

            // ── Categoría ────────────────────────────────────────────────
            if (categoria != null && !categoria.isBlank()) {
                Join<Object, Object> catJoin = root.join("categoria", JoinType.LEFT);
                where.add(cb.or(
                        cb.equal(catJoin.get("nombre"), categoria),
                        cb.equal(catJoin.get("slug"), categoria)));
            }

            // ── Precio ───────────────────────────────────────────────────
            if (precioMin != null)
                where.add(cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
            if (precioMax != null)
                where.add(cb.lessThanOrEqualTo(root.get("precio"), precioMax));

            // ── Bloqueos ──────────────────────────────────────────────────
            if (excludedVendedorIds != null && !excludedVendedorIds.isEmpty()) {
                where.add(cb.not(root.get("vendedor").get("id").in(excludedVendedorIds)));
            }

            // ── Geografía ─────────────────────────────────────────────────
            if (minLat != null && maxLat != null) {
                where.add(cb.between(root.get("latitude"), minLat, maxLat));
            }
            if (minLng != null && maxLng != null) {
                where.add(cb.between(root.get("longitude"), minLng, maxLng));
            }

            return cb.and(where.toArray(new Predicate[0]));
        };
    }

    public static Specification<Producto> buscarComoAdmin(
            String q,
            Integer categoriaId,
            EstadoProducto estado,
            Integer vendedorId,
            Double precioMin,
            Double precioMax,
            LocalDateTime fechaDesde) {
        return (root, query, cb) -> {
            boolean isCount = Long.class.equals(query.getResultType())
                    || long.class.equals(query.getResultType());
            if (!isCount) {
                root.fetch("vendedor", JoinType.INNER);
                root.fetch("categoria", JoinType.LEFT);
            }

            List<Predicate> where = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), pattern),
                        cb.like(cb.lower(root.get("descripcion")), pattern)
                ));
            }

            if (categoriaId != null) {
                where.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (estado != null) {
                where.add(cb.equal(root.get("estado"), estado));
            }
            if (vendedorId != null) {
                where.add(cb.equal(root.get("vendedor").get("id"), vendedorId));
            }
            if (precioMin != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("precio"), precioMin));
            }
            if (precioMax != null) {
                where.add(cb.lessThanOrEqualTo(root.get("precio"), precioMax));
            }
            if (fechaDesde != null) {
                where.add(cb.greaterThanOrEqualTo(root.get("fechaPublicacion"), fechaDesde));
            }

            return cb.and(where.toArray(new Predicate[0]));
        };
    }
}