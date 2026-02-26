# Nexus API

API REST desarrollada con **Spring Boot** para la gestión de la plataforma **Nexus**. Este sistema permite la administración de actores, empresas, contratos, ofertas y comentarios, siguiendo una arquitectura escalable basada en el diagrama de dominio del proyecto.


## 📋 Descripción

Nexus API es el backend encargado de gestionar la lógica de negocio y la persistencia de datos del ecosistema Nexus. El sistema implementa una jerarquía de entidades donde `Actor` sirve como clase base para los distintos roles del sistema (como Empresas), y gestiona relaciones complejas como la publicación de ofertas y la formalización de contratos publicitarios.

## 🛠️ Tecnologías Utilizadas

* **Java:** 17 (JDK 17)
* **Framework:** Spring Boot
* **Base de Datos:** PostgreSQL
* **ORM:** Spring Data JPA (Hibernate)
* **Documentación API:** SpringDoc OpenAPI (Swagger UI)
* **Gestor de Dependencias:** Maven
* **Validación:** Hibernate Validator (Jakarta Validation)
