package com.nexus.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nexus.entity.Usuario;
import com.nexus.service.UsuarioService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {
	@Autowired
	private UsuarioService usuarioService;
	
	// Obtener todos los usuarios
	@GetMapping
	@Operation(summary = "Obtener todos los usuarios")
	public List<Usuario> getAllUsuarios() {
		return usuarioService.findAll();
	}
	
	
	// Obtener usuario por id
	@GetMapping("/{id}")
	@Operation(summary = "Obtener todos los usuarios por id")
	public ResponseEntity<Usuario> getUsuarioById(@PathVariable Integer id) {
		Optional<Usuario> usuarioOptional = usuarioService.findById(id);
		
		if (usuarioOptional.isPresent()) {
			return ResponseEntity.ok(usuarioOptional.get());
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	
	// Crear usuario
	@PostMapping
	@Operation(summary = "Crear usuario")
	public ResponseEntity<Usuario> createUsuario(@RequestBody Usuario usuario) {
		Usuario nuevoUsuario = usuarioService.save(usuario);
		return ResponseEntity.ok(nuevoUsuario);
	}
	
	
	// Actualizar usuario
	@PutMapping("/{id}")
	@Operation(summary = "Actualizar usuario")
	public ResponseEntity<Usuario> updateUsuario(@PathVariable Integer id, @RequestBody Usuario usuarioDetalles) {
		Optional<Usuario> usuarioOptional = usuarioService.findById(id);
		
		if (usuarioOptional.isPresent()) {
			Usuario usuarioExistente = usuarioOptional.get();
			usuarioExistente.setUser(usuarioDetalles.getUser());
			usuarioExistente.setEmail(usuarioDetalles.getEmail());
			
			Usuario usuarioActualizado = usuarioService.save(usuarioExistente);
			return ResponseEntity.ok(usuarioActualizado);
		} else {
			return ResponseEntity.notFound().build();
		}
	}
	
	
	// Eliminar usuario
	@DeleteMapping("/{id}")
	@Operation(summary = "Eliminar usuario por id")
	public ResponseEntity<String> deleteUsuario(@PathVariable Integer id) {
		Optional<Usuario> usuarioOptional = usuarioService.findById(id);
		
		if (usuarioOptional.isPresent()) {
			usuarioService.delete(id);
			return ResponseEntity.ok("Usuario eliminado correctamente...");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha encontrado el usuario para eliminar");
		}
	}
}
