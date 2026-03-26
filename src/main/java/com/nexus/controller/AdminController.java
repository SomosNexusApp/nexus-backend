package com.nexus.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.entity.Admin;
import com.nexus.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/admin")
@Tag(name = "Administradores", description = "Gestión de administradores")
@Transactional(readOnly = true)
public class AdminController {
	@Autowired
	private AdminService adminService;
	
	@GetMapping
	@Operation(summary = "Obtener todos los administradores")
	public ResponseEntity<List<Admin>> findAll() {
		return ResponseEntity.ok(adminService.findAll());
	}
	
	@GetMapping("/{id}")
	@Operation(summary = "Buscar administrador por ID")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Encontrado"),
			@ApiResponse(responseCode = "400", description = "No encontrado")
	})
	public ResponseEntity<Admin> findById(@PathVariable Integer id) {
		Optional<Admin> oAdmin = adminService.findById(id);
		return oAdmin.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
	}
	
	@PostMapping
	@Operation(summary = "Crear administrador")
	@Transactional
	public ResponseEntity<String> save(@RequestBody Admin admin) {
		adminService.save(admin);
		return ResponseEntity.status(HttpStatus.OK).body("Administrador creado correctamente");
	}
	
	@PutMapping("/{id}")
	@Operation(summary = "Actualizar administrador")
	@Transactional
	public ResponseEntity<String> update(@PathVariable Integer id, @RequestBody Admin admin) {
		if (adminService.update(id, admin) != null) {
			return ResponseEntity.status(HttpStatus.OK).body("Administrador actualizado correctamente");
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Administrador no encontrado");
	}
	
	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar administrador")
	@Transactional
	public ResponseEntity<String> delete(@PathVariable Integer id) {
		Optional<Admin> oAdmin = adminService.findById(id);
		
		if (oAdmin.isPresent()) {
			adminService.delete(id);
			return ResponseEntity.status(HttpStatus.OK).body("Administrador eliminado correctamente");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Administrador no encontrado");
		}
	}
}
