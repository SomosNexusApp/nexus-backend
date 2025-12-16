package com.nexus.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Empresa;
import com.nexus.service.EmpresaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/empresa")
@Tag(name = "Empresas", description = "Gestión de empresas del sistema")
public class EmpresaController {

	@Autowired
	private EmpresaService empresaService;

	@GetMapping
	@Operation(summary = "Obtener todas las empresas")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
	})
	public ResponseEntity<List<Empresa>> findAll() {
		return ResponseEntity.ok(empresaService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar empresa por ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Empresa encontrada"),
			@ApiResponse(responseCode = "400", description = "Empresa no encontrada")
	})
	public ResponseEntity<Empresa> findById(@PathVariable Integer id) {
		Optional<Empresa> oEmpresa = empresaService.findById(id);
		return oEmpresa.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
	}

	@PostMapping
	@Operation(summary = "Crear empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Empresa creada"),
			@ApiResponse(responseCode = "500", description = "Error al crear")
	})
	public ResponseEntity<String> save(@RequestBody Empresa empresa) {
		empresaService.save(empresa);
		return ResponseEntity.status(HttpStatus.OK).body("Empresa creada correctamente");
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Empresa actualizada"),
			@ApiResponse(responseCode = "400", description = "Empresa no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error al actualizar")
	})
	public ResponseEntity<String> update(@PathVariable Integer id, @RequestBody Empresa empresa) {
		if (empresaService.update(id, empresa) == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empresa no encontrada");
		}
		return ResponseEntity.status(HttpStatus.OK).body("Empresa actualizada correctamente");
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar empresa")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Empresa eliminada"),
			@ApiResponse(responseCode = "400", description = "Empresa no encontrada")
	})
	public ResponseEntity<String> delete(@PathVariable Integer id) {
		Optional<Empresa> oEmpresa = empresaService.findById(id);
		if (oEmpresa.isPresent()) {
			empresaService.delete(id);
			return ResponseEntity.status(HttpStatus.OK).body("Empresa eliminada correctamente");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empresa no encontrada");
	}
}