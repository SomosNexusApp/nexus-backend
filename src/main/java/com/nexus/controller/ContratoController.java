package com.nexus.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nexus.entity.Contrato;
import com.nexus.service.ContratoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/contrato")
@Tag(name = "Contratos", description = "Gestión de contratos del sistema")
public class ContratoController {

	@Autowired
	private ContratoService contratoService;

	@GetMapping
	@Operation(summary = "Obtener todos los contratos")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
	})
	public ResponseEntity<List<Contrato>> findAll() {
		return ResponseEntity.ok(contratoService.findAll());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Buscar contrato por ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contrato encontrado"),
			@ApiResponse(responseCode = "400", description = "Contrato no encontrado")
	})
	public ResponseEntity<Contrato> findById(@PathVariable Integer id) {
		Optional<Contrato> oContrato = contratoService.findById(id);
		return oContrato.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
	}

	@PostMapping("/{idEmpresa}")
	@Operation(summary = "Crear contrato")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contrato creado"),
			@ApiResponse(responseCode = "400", description = "Empresa no encontrada"),
			@ApiResponse(responseCode = "500", description = "Error al crear")
	})
	public ResponseEntity<String> save(@RequestBody Contrato contrato, @PathVariable Integer idEmpresa) {
		Contrato c = contratoService.save(contrato, idEmpresa);
		if (c != null) {
			return ResponseEntity.status(HttpStatus.OK).body("Contrato creado correctamente");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empresa no encontrada");
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar contrato")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contrato actualizado"),
			@ApiResponse(responseCode = "400", description = "Contrato no encontrado")
	})
	public ResponseEntity<String> update(@PathVariable Integer id, @RequestBody Contrato contrato) {
		if (contratoService.update(id, contrato) != null) {
			return ResponseEntity.status(HttpStatus.OK).body("Contrato actualizado correctamente");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Contrato no encontrado");
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar contrato")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Contrato eliminado"),
			@ApiResponse(responseCode = "400", description = "Contrato no encontrado")
	})
	public ResponseEntity<String> delete(@PathVariable Integer id) {
		Optional<Contrato> oContrato = contratoService.findById(id);
		if (oContrato.isPresent()) {
			contratoService.delete(id);
			return ResponseEntity.status(HttpStatus.OK).body("Contrato eliminado correctamente");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Contrato no encontrado");
	}
}