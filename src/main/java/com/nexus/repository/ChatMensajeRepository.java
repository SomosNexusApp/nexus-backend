package com.nexus.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.EntityGraph;

import com.nexus.entity.ChatMensaje;

@Repository
public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Integer> {

       // Historial de chat de un producto ordenado cronológicamente (ASC = tipo
       // WhatsApp)
       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.producto.id = ?1 ORDER BY m.fechaEnvio ASC")
       List<ChatMensaje> findByProductoId(Integer productoId);

       // Historial entre dos usuarios sobre un producto (para filtrar solo los dos
       // participantes)
       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.producto.id = ?1 " +
                     "AND ((m.remitente.id = ?2 AND m.receptor.id = ?3) " +
                     "OR (m.remitente.id = ?3 AND m.receptor.id = ?2)) " +
                     "ORDER BY m.fechaEnvio ASC")
       List<ChatMensaje> findConversacion(Integer productoId, Integer usuario1Id, Integer usuario2Id);

       // Mensajes no leídos de un usuario
       @Query("SELECT m FROM ChatMensaje m WHERE m.receptor.id = ?1 AND m.leido = false ORDER BY m.fechaEnvio DESC")
       List<ChatMensaje> findNoLeidosByReceptor(Integer receptorId);

       // Contar mensajes no leídos (para el badge del icono de chat en Angular)
       @Query("SELECT COUNT(m) FROM ChatMensaje m WHERE m.receptor.id = ?1 AND m.leido = false")
       long countNoLeidosByReceptor(Integer receptorId);

       // Marcar como leídos todos los mensajes de una conversación para un usuario
       @Modifying
       @Transactional
       @Query("UPDATE ChatMensaje m SET m.leido = true WHERE m.producto.id = ?1 AND m.receptor.id = ?2")
       void marcarComoLeidosEnProducto(Integer productoId, Integer receptorId);

       // Último mensaje de cada conversación (para la lista de chats del usuario, como
       // en Instagram)
       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.producto.id IN " +
                     "(SELECT DISTINCT m2.producto.id FROM ChatMensaje m2 WHERE m2.remitente.id = ?1 OR m2.receptor.id = ?1) "
                     +
                     "AND m.fechaEnvio = (SELECT MAX(m3.fechaEnvio) FROM ChatMensaje m3 WHERE m3.producto.id = m.producto.id) "
                     +
                     "ORDER BY m.fechaEnvio DESC")
       List<ChatMensaje> findUltimosMensajesPorUsuario(Integer usuarioId);
}