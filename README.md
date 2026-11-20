# 🌱 Huerto Hogar — Backend

API REST para **Huerto Hogar**, desarrollada con **Java 21, Spring Boot 4 y MySQL**. Gestiona autenticación JWT, permisos por rol, productos, categorías, usuarios y compras registradas en boletas.

**Proyecto académico:** Desarrollo Fullstack II (DSY1104) · Duoc UC.

> **Alcance:** el backend registra boletas y descuenta stock cuando corresponde, pero **no procesa pagos reales**. Una boleta con estado `PENDIENTE` no equivale a un pago confirmado.

## Funcionalidades

- Registro de cuentas e inicio de sesión con contraseñas protegidas mediante **BCrypt**.
- Autenticación sin estado mediante JWT y autorización con Spring Security.
- Roles `CLIENTE`, `VENDEDOR` y `ADMIN` con rutas y operaciones diferenciadas.
- CRUD de productos, con validaciones de entrada y persistencia mediante JPA.
- CRUD de categorías y relación con los productos.
- Creación de boletas, detalles de compra y descuento de stock.
- Historial de compras por usuario y consulta administrativa de órdenes.
- Consulta de usuarios y cambio de rol exclusivo para administradores.
- Documentación interactiva de la API mediante Swagger/OpenAPI.
- Pruebas automatizadas de lógica y seguridad con JUnit, Mockito y Spring Security Test.

## Tecnologías principales

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.2 |
| API | Spring Web MVC / REST |
| Seguridad | Spring Security, OAuth2 Resource Server, JWT, BCrypt |
| Persistencia | Spring Data JPA / Hibernate, MySQL |
| Construcción | Maven |
| Documentación | OpenAPI / Swagger UI |
| Pruebas | JUnit 5, MockMvc, Mockito, Spring Security Test |

## Requisitos previos

- JDK **21** instalado y configurado.
- Maven o soporte Maven de IntelliJ IDEA.
- Servidor **MySQL** operativo.
- Una base de datos de Huerto Hogar creada y accesible desde la aplicación.

## Configuración de MySQL

Revisa los valores reales en:

```text
src/main/resources/application.properties
```

O en `application.yml`, si tu proyecto utiliza ese formato.

La aplicación necesita, como mínimo, la URL JDBC, el usuario y la contraseña correctos para tu instalación de MySQL. El formato habitual es:

```properties
# EJEMPLO: adapta estos valores a tu configuración.
spring.datasource.url=jdbc:mysql://localhost:3306/NOMBRE_BD
spring.datasource.username=USUARIO_MYSQL
spring.datasource.password=CONTRASENA_MYSQL
```

**Importante:** `NOMBRE_BD`, `USUARIO_MYSQL` y `CONTRASENA_MYSQL` son marcadores, no los valores reales de tu proyecto. Utiliza el **nombre exacto de la base de datos configurada** en tu entorno. No cambies otras propiedades de JPA ni del esquema sin revisar la configuración existente.

Mantén las credenciales y la clave de firma JWT fuera del repositorio público. Puedes usar variables de entorno o archivos de configuración locales excluidos de Git, según cómo esté implementada tu configuración.

## Instalación y ejecución

Desde la raíz del proyecto backend, con MySQL iniciado:

```bash
mvn spring-boot:run
```

También puedes ejecutar la clase principal **`HuertoBackendApplication`** desde IntelliJ IDEA.

La configuración local utilizada en el desarrollo de Huerto Hogar atiende en:

```text
http://localhost:8081
```

Comprueba el puerto efectivo en la salida de Spring Boot o en la configuración del proyecto.

### Swagger / OpenAPI

Con el backend encendido, abre:

```text
http://localhost:8081/swagger-ui/index.html
```

Para probar endpoints protegidos: ejecuta `POST /api/v1/auth/login`, copia el token retornado y utiliza **Authorize** en Swagger. No compartas capturas con el token visible.

## Endpoints principales

**Prefijo común:** `/api/v1`

| Método | Endpoint | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/auth/registro` | Público | Registro de usuario |
| POST | `/auth/login` | Público | Inicio de sesión; retorna JWT |
| GET | `/productos` | Público | Listar productos |
| GET | `/productos/{id}` | Público | Consultar producto |
| POST | `/productos` | ADMIN | Crear producto |
| PUT | `/productos/{id}` | ADMIN | Actualizar producto |
| DELETE | `/productos/{id}` | ADMIN | Eliminar producto |
| GET | `/categorias` | Público | Listar categorías |
| POST | `/categorias` | ADMIN | Crear categoría |
| PUT | `/categorias/{id}` | ADMIN | Modificar categoría |
| DELETE | `/categorias/{id}` | ADMIN | Eliminar categoría |
| POST | `/boletas` | CLIENTE / ADMIN | Crear boleta y sus detalles |
| GET | `/boletas/mis-compras` | CLIENTE / ADMIN | Consultar compras propias |
| GET | `/boletas` | VENDEDOR / ADMIN | Listar órdenes |
| GET | `/boletas/{id}` | CLIENTE / VENDEDOR / ADMIN | Consultar detalle; para CLIENTE, sujeto a comprobación de propiedad |
| GET | `/admin/usuarios` | ADMIN | Consultar usuarios |
| PUT | `/admin/usuarios/{id}/rol` | ADMIN | Cambiar rol de usuario |

Los cuerpos JSON, validaciones y respuestas exactas se pueden inspeccionar en Swagger UI. No se recomienda asumir que todos los errores del dominio usan el mismo código: el resultado depende también de la existencia de la entidad y de las validaciones del servicio.

### Ejemplo de acceso (sin credenciales reales)

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "correo": "usuario@ejemplo.cl",
  "password": "CONTRASENA_DE_PRUEBA"
}
```

Para llamadas autenticadas se envía:

```http
Authorization: Bearer <JWT_OBTENIDO_EN_LOGIN>
```

### Ejemplo de estructura de una boleta

```json
{
  "detalles": [
    {
      "productoId": 1,
      "cantidad": 1
    }
  ]
}
```

Este ejemplo **no debe ejecutarse como parte de las pruebas de permisos**, porque `POST /boletas` puede modificar el stock. Para comprobar permisos de consulta, utiliza endpoints `GET`.

## Seguridad y roles

- **CLIENTE:** consulta productos y categorías, crea compras y accede a sus propias boletas.
- **VENDEDOR:** consulta las órdenes registradas, sin gestionar usuarios ni productos.
- **ADMIN:** administra productos, categorías y roles; consulta usuarios, boletas y panel administrativo.

La configuración principal de permisos se encuentra en:

```text
src/main/java/cl/huertohogar/huerto_backend/config/SecurityConfig.java
```

Además de verificar los JWT, esta implementación consulta el rol actual almacenado en MySQL. Si el rol difiere del que aparece en el token, se rechaza el acceso y el usuario debe iniciar sesión nuevamente. **Esto no constituye revocación permanente:** si el rol vuelve a coincidir mientras el token sigue vigente, el token anterior podría volver a ser aceptado.

Los permisos deben comprobarse siempre en el backend, incluso si el frontend oculta botones o rutas.

## Organización del código

```text
src/
├── main/
│   ├── java/cl/huertohogar/huerto_backend/
│   │   ├── config/        # Configuración de seguridad y OpenAPI
│   │   ├── controller/    # Endpoints REST
│   │   ├── dto/           # Objetos de entrada y respuesta
│   │   ├── entity/        # Entidades JPA y roles
│   │   ├── repository/    # Repositorios de persistencia
│   │   ├── service/       # Reglas de negocio
│   │   └── HuertoBackendApplication.java
│   └── resources/        # Configuración de la aplicación
└── test/java/            # Pruebas automatizadas
```

Es una vista resumida de la organización general del proyecto.

## Pruebas automatizadas

Desde IntelliJ, abre el panel **Maven → Lifecycle → test**. También puedes ejecutar:

```bash
mvn test
```

Las pruebas implementadas incluyen verificaciones de permisos con MockMvc, solicitudes sin autenticación y validación de cambios de rol frente al JWT con dependencias simuladas.

**Última comprobación local documentada (20-11-2026):** 52 pruebas del backend aprobadas al ejecutar Maven. Esta cifra corresponde a la versión verificada en esa fecha y podría cambiar si se agregan nuevas pruebas.

> Algunas pruebas usan usuarios, repositorios o decodificadores JWT simulados. Esto permite comprobar la configuración y las reglas de acceso sin escribir en MySQL, pero **no reemplaza las pruebas de integración con la base de datos ni la verificación criptográfica de tokens reales**.

## Conexión con el frontend

El frontend React utiliza Axios y apunta habitualmente a:

```text
http://localhost:8081/api/v1
```

Durante desarrollo, el frontend suele ejecutarse en `http://localhost:5173`. Verifica que la configuración CORS de Spring Security permita ese origen.

## Solución de problemas frecuentes

- **No conecta con MySQL:** revisa que el servicio esté iniciado y que la URL JDBC, el usuario, la contraseña y el esquema sean correctos.
- **`401 Unauthorized`:** falta un JWT válido, está vencido o ya no corresponde a la sesión esperada.
- **`403 Forbidden`:** la identidad autenticada no tiene el rol autorizado para el endpoint.
- **`404 Not Found`:** comprueba la URL, el método HTTP y si el recurso solicitado existe; un `404` no demuestra por sí mismo un error de permisos.
- **Swagger no envía el JWT:** vuelve a utilizar **Authorize** y verifica que el encabezado `Authorization` esté presente en la solicitud.
- **Fallo en Maven Test:** revisa los errores específicos de la consola y la versión de Java (21).

## Estado del proyecto

Backend académico en preparación de entrega. Antes de publicar el repositorio, revisa la documentación, los archivos de configuración, las credenciales locales y la ejecución completa de `mvn test`.
