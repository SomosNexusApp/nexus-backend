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

       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.roomId = ?1 ORDER BY m.fechaEnvio ASC")
       List<ChatMensaje> findByRoomId(String roomId);

       // Historial entre dos usuarios sobre un producto/room
       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.roomId = ?1 " +
                     "AND ((m.remitente.id = ?2 AND m.receptor.id = ?3) " +
                     "OR (m.remitente.id = ?3 AND m.receptor.id = ?2)) " +
                     "ORDER BY m.fechaEnvio ASC")
       List<ChatMensaje> findConversacion(String roomId, Integer usuario1Id, Integer usuario2Id);

       // Mensajes no leídos de un usuario
       @Query("SELECT m FROM ChatMensaje m WHERE m.receptor.id = ?1 AND m.leido = false ORDER BY m.fechaEnvio DESC")
       List<ChatMensaje> findNoLeidosByReceptor(Integer receptorId);

       // Contar mensajes no leídos (para el badge del icono de chat en Angular)
       @Query("SELECT COUNT(m) FROM ChatMensaje m WHERE m.receptor.id = ?1 AND m.leido = false")
       long countNoLeidosByReceptor(Integer receptorId);

       // Contar CONVERSACIONES (rooms) con mensajes no leídos
       @Query("SELECT COUNT(DISTINCT m.roomId) FROM ChatMensaje m WHERE m.receptor.id = ?1 AND m.leido = false")
       long countNoLeidosConversationsByReceptor(Integer receptorId);

       // Marcar como leídos todos los mensajes de una conversación para un usuario
       @Modifying
       @Transactional
       @Query("UPDATE ChatMensaje m SET m.leido = true WHERE m.producto.id = ?1 AND m.receptor.id = ?2")
       void marcarComoLeidosEnProducto(Integer productoId, Integer receptorId);

       @Modifying
       @Transactional
       @Query("UPDATE ChatMensaje m SET m.leido = true WHERE m.roomId = ?1 AND m.receptor.id = ?2")
       void marcarComoLeidosEnRoom(String roomId, Integer receptorId);

       @Modifying
       @Transactional
       @Query("UPDATE ChatMensaje m SET m.recibido = true WHERE m.roomId = ?1 AND m.receptor.id = ?2 AND m.recibido = false")
       void marcarComoRecibidosEnRoom(String roomId, Integer receptorId);

       // Último mensaje de cada conversación
       @EntityGraph(attributePaths = { "remitente", "receptor", "producto", "producto.vendedor", "producto.categoria" })
       @Query("SELECT m FROM ChatMensaje m WHERE m.roomId IN " +
                     "(SELECT DISTINCT m2.roomId FROM ChatMensaje m2 WHERE m2.remitente.id = ?1 OR m2.receptor.id = ?1) "
                     +
                     "AND m.fechaEnvio = (SELECT MAX(m3.fechaEnvio) FROM ChatMensaje m3 WHERE m3.roomId = m.roomId) "
                     +
                     "ORDER BY m.fechaEnvio DESC")
       List<ChatMensaje> findUltimosMensajesPorUsuario(Integer usuarioId);

       // Buscar ofertas aceptadas entre un comprador y un producto
       @Query("SELECT m FROM ChatMensaje m WHERE m.producto.id = ?1 " +
                     "AND (m.remitente.id = ?2 OR m.receptor.id = ?2) " +
                     "AND m.tipo = com.nexus.entity.TipoMensaje.OFERTA_PRECIO " +
                     "AND m.estadoPropuesta = 'ACEPTADA' " +
                     "ORDER BY m.fechaEnvio DESC")
       List<ChatMensaje> findAcceptedOffers(Integer productoId, Integer usuarioId);
}