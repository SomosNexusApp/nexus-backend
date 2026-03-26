package com.nexus.service;

import com.nexus.entity.Categoria;
import com.nexus.repository.CategoriaRepository;
import com.nexus.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;

@Service
@Transactional
public class AdminCategoriasService {

    private final CategoriaRepository categoriaRepo;
    private final ProductoRepository productoRepo;
    public AdminCategoriasService(
            CategoriaRepository categoriaRepo,
            ProductoRepository productoRepo) {
        this.categoriaRepo = categoriaRepo;
        this.productoRepo = productoRepo;
    }

    // ── Árbol raíz ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Categoria> getArbolRaiz() {
        return categoriaRepo.findByParentIsNullOrderByOrdenAsc();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public Categoria crear(CategoriaRequest req) {
        Categoria c = new Categoria();
        c.setNombre(req.nombre());
        c.setSlug(req.slug() != null ? req.slug() : slugify(req.nombre()));
        c.setIcono(req.icono());
        c.setColor(req.color());
        c.setActiva(req.activa() != null ? req.activa() : true);
        if (req.padreId() != null) {
            categoriaRepo.findById(req.padreId()).ifPresent(c::setParent);
        }
        return categoriaRepo.save(c);
    }

    public Categoria editar(Integer id, CategoriaRequest req) {
        Categoria c = findOrThrow(id);
        if (req.nombre() != null)  c.setNombre(req.nombre());
        if (req.slug()   != null)  c.setSlug(req.slug());
        if (req.icono()  != null)  c.setIcono(req.icono());
        if (req.color()  != null)  c.setColor(req.color());
        if (req.activa() != null)  c.setActiva(req.activa());
        if (req.padreId() != null) {
            categoriaRepo.findById(req.padreId()).ifPresent(c::setParent);
        }
        return categoriaRepo.save(c);
    }

    public void eliminar(Integer id) {
        Categoria c = findOrThrow(id);
        long prods = productoRepo.countByCategoriaId(id);
        if (prods > 0) throw new IllegalStateException("Hay " + prods + " productos en esta categoría.");
        if (!c.getHijos().isEmpty()) throw new IllegalStateException("La categoría tiene subcategorías.");
        categoriaRepo.delete(c);
    }

    // ── Toggle activa (en cascada a hijos) ──────────────────────────────────

    public Categoria toggleActiva(Integer id) {
        Categoria c = findOrThrow(id);
        boolean nuevo = !Boolean.TRUE.equals(c.getActiva());
        c.setActiva(nuevo);
        if (!nuevo) {
            desactivarHijosRecursivo(c);
        }
        return categoriaRepo.save(c);
    }

    private void desactivarHijosRecursivo(Categoria c) {
        for (Categoria hijo : c.getHijos()) {
            hijo.setActiva(false);
            desactivarHijosRecursivo(hijo);
            categoriaRepo.save(hijo);
        }
    }

    // ── Reordenar ────────────────────────────────────────────────────────────

    public void reordenar(List<ReordenarItem> items) {
        for (ReordenarItem item : items) {
            categoriaRepo.findById(item.id()).ifPresent(c -> {
                c.setOrden(item.nuevoOrden());
                if (item.padreId() != null) {
                    categoriaRepo.findById(item.padreId()).ifPresent(c::setParent);
                }
                categoriaRepo.save(c);
            });
        }
    }

    // ── Check slug ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean checkSlug(String slug) {
        return categoriaRepo.existsBySlug(slug);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Categoria findOrThrow(Integer id) {
        return categoriaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + id));
    }

    private String slugify(String nombre) {
        return Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    // ── DTOs record ──────────────────────────────────────────────────────────
    public record CategoriaRequest(
            String nombre, String slug, String icono, String color,
            Boolean activa, Integer padreId) {}

    public record ReordenarItem(Integer id, Integer nuevoOrden, Integer padreId) {}
}
