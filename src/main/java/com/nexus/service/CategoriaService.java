package com.nexus.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.entity.Categoria;
import com.nexus.repository.CategoriaRepository;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Cacheable("categorias-raiz")
    public List<Categoria> getRaizActivas() {
        return categoriaRepository.findRaizActivas();
    }

    @Cacheable("categorias-todas")
    public List<Categoria> getTodas() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc();
    }

    @Cacheable(value = "categorias-hijas", key = "#pid")
    public List<Categoria> getHijas(Integer pid) {
        return categoriaRepository.findByParentIdAndActivaTrue(pid);
    }

    public Optional<Categoria> findBySlug(String s) {
        return categoriaRepository.findBySlug(s);
    }

    public Optional<Categoria> findById(Integer id) {
        return categoriaRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = {"categorias-raiz", "categorias-todas", "categorias-hijas"}, allEntries = true)
    public Categoria crear(Categoria categoria) {
        if (categoria.getSlug() != null
                && categoriaRepository.findBySlug(categoria.getSlug()).isPresent())
            throw new IllegalArgumentException("Ya existe una categoria con ese slug");
        return categoriaRepository.save(categoria);
    }

    @Transactional
    @CacheEvict(value = {"categorias-raiz", "categorias-todas", "categorias-hijas"}, allEntries = true)
    public Categoria actualizar(Integer id, Categoria datos) {
        Categoria c = categoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada"));
        if (datos.getNombre()      != null) c.setNombre(datos.getNombre());
        if (datos.getDescripcion() != null) c.setDescripcion(datos.getDescripcion());
        if (datos.getIcono()       != null) c.setIcono(datos.getIcono());
        if (datos.getColor()       != null) c.setColor(datos.getColor());
        if (datos.getOrden()       != null) c.setOrden(datos.getOrden());
        if (datos.getActiva()      != null) c.setActiva(datos.getActiva());
        return categoriaRepository.save(c);
    }

    @Transactional
    @CacheEvict(value = {"categorias-raiz", "categorias-todas", "categorias-hijas"}, allEntries = true)
    public void eliminar(Integer id) {
        categoriaRepository.deleteById(id);
    }
}
