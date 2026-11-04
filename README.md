# FinanzasApp

Aplicacion de escritorio (Java 25 Swing) para finanzas personales y del hogar, con backend REST (Spring Boot 4.1 + PostgreSQL 16).

- **Modo local**: el cliente funciona sin servidor y guarda los datos en `~/.finanzasapp`.
- **Modo API**: el cliente se conecta al backend; permite hogares compartidos con varios usuarios, roles e invitaciones.

Repositorio: https://github.com/juan2052H/FinanzasApp

> **Instalador de Windows:** [ejecutable/FinanzasApp-Setup.exe](ejecutable/FinanzasApp-Setup.exe) (35,7 MB; en GitHub use el boton *Download raw file*). Instrucciones rapidas, incluido Docker: [ejecutable/LEEME_PRIMERO.txt](ejecutable/LEEME_PRIMERO.txt).
>
> **Equipo evaluador:** empiece por [docs/REQUISITOS_E_INSTALACION.md](docs/REQUISITOS_E_INSTALACION.md) (requisitos del PC, ejecucion y cuentas de prueba; tambien en [Word](docs/REQUISITOS_E_INSTALACION.docx)) y luego [docs/GUIA_EVALUADOR.md](docs/GUIA_EVALUADOR.md) (recorrido de verificacion).

| Documento | Contenido |
|---|---|
| [docs/ARQUITECTURA_Y_AMBIENTE.md](docs/ARQUITECTURA_Y_AMBIENTE.md) | Diagrama de componentes y capas, tecnologias y versiones, dependencias, puertos, variables de entorno y base de datos |
| [calidad_U/FinanzasApp_Formato_Calidad_Final.xlsx](calidad_U/FinanzasApp_Formato_Calidad_Final.xlsx) | Formato de calidad: proyecto, interesados, requisitos, historias, casos de uso, mockups, trazabilidad, casos de prueba y defectos |
| [database/crear_bd.sql](database/crear_bd.sql) | Script SQL de la base de datos (tablas + datos de prueba) |
| [docs/API.md](docs/API.md) | Endpoints del backend |
| [docs/capturas/](docs/capturas/) | Capturas reales de cada pantalla con los datos de prueba (se regeneran con `scripts/capturas/capturar.sh`) |
| [scripts/prueba-humo-api.ps1](scripts/prueba-humo-api.ps1) | Prueba de humo/aceptacion automatizada de la API (25 casos) |

---

## 1. Requisitos previos

| Para... | Necesita |
|---|---|
| Backend + base de datos (recomendado) | **Docker Desktop** (Windows/macOS) o Docker Engine + Compose v2 (Linux). No necesita Java. |
| Cliente de escritorio con instalador | Windows 10/11. El instalador incluye Java 25. |
| Cliente desde el codigo fuente | **JDK 25** (por ejemplo Eclipse Temurin 25). Maven no hace falta: se usa `mvnw`. |
| Montaje sin Docker | PostgreSQL 16 + JDK 25. |

Puertos que deben estar libres: **5432** (PostgreSQL) y **8080** (backend).

## 2. Usuarios de prueba

Se cargan automaticamente con Docker Compose (o con `database/crear_bd.sql`). Contrasena de todos: **`Prueba2026*`**

| Correo | Rol en el hogar "Hogar Gomez" | Que permite probar |
|---|---|---|
| `ana.duena@finanzasapp.test` | OWNER | Todo: miembros, invitaciones, presupuestos, metas, facturas, reportes |
| `carlos.miembro@finanzasapp.test` | MEMBER | Registrar transacciones y gastos compartidos |
| `laura.lectora@finanzasapp.test` | VIEWER | Solo lectura (crear o editar responde "sin permisos") |

El hogar trae datos del mes anterior y del actual: ingresos, gastos, ahorro automatico del 20%, 4 presupuestos (Entretenimiento excedido a proposito), 2 metas, 1 recurrencia, 1 gasto compartido y 1 factura. Despues de iniciar sesion, seleccione **"Hogar Gomez"** en el selector de workspace del encabezado.

**Modo local** (sin backend): ejecute el cliente con `FINANZAS_DEMO_MODE=true` y use `samuel@finanzasapp.com` / `123456`.

## 3. Instalacion y ejecucion

### Paso 1. Obtener el codigo

```bash
git clone https://github.com/juan2052H/FinanzasApp.git
cd FinanzasApp
```

(O descomprima el `.zip` entregado y abra una terminal en la carpeta.)

### Paso 2. Levantar backend y base de datos (Docker)

```bash
# Opcional: personalizar variables (si no existe .env se usan valores de desarrollo)
cp .env.example .env            # PowerShell: Copy-Item .env.example .env

docker compose up -d --build
```

La primera vez tarda unos minutos (descarga imagenes y compila el backend). Compose levanta:

1. `postgres` (PostgreSQL 16, puerto 5432),
2. `backend` (Spring Boot, puerto 8080), que crea las tablas con las migraciones Flyway,
3. `db-seed`, que carga los usuarios y datos de prueba y termina.

Verifique:

```bash
docker compose ps -a
# finanzas-postgres        ... Up (healthy)
# finanzas-backend         ... Up (healthy)
# finanzasapp-db-seed-1    ... Exited (0)

curl http://localhost:8080/health/readiness
# {"status":"UP","database":"UP"}
```

En PowerShell use `curl.exe` o `Invoke-RestMethod http://localhost:8080/health/readiness`.

### Paso 3. Ejecutar el cliente de escritorio en modo API

**Opcion A - Instalador de Windows (sin Java instalado)**

1. Ejecute `FinanzasApp-Setup.exe` (35,7 MB; en la entrega esta en la carpeta **`ejecutable`**, o se genera en `dist\` con `.\scripts\build-windows-standalone.ps1 -JdkHome <JDK 25>`). Instala en `%LOCALAPPDATA%\Programs\FinanzasApp`, sin permisos de administrador y con Java 25 incluido.
2. Abra **"FinanzasApp (servidor)"** desde el Escritorio o el menu Inicio: entra directo en modo API contra `http://localhost:8080`. No hace falta configurar variables de entorno.

| Acceso directo | Modo |
|---|---|
| FinanzasApp | Local, sin servidor (datos en `%USERPROFILE%\.finanzasapp`) |
| FinanzasApp (servidor) | API; usa `FINANZAS_API_BASE_URL` si esta definida, si no `http://localhost:8080` |
| Desinstalar FinanzasApp (menu Inicio) | Quita la aplicacion y sus accesos; conserva los datos locales |

Tambien aparece en **Configuracion > Aplicaciones > Aplicaciones instaladas**, desde donde se puede desinstalar.

**Opcion B - Desde el codigo fuente (requiere JDK 25)**

Windows (PowerShell):

```powershell
$env:FINANZAS_API_ENABLED="true"
$env:FINANZAS_API_BASE_URL="http://localhost:8080"
.\mvnw.cmd -f pom.xml compile exec:java
```

Linux/macOS:

```bash
FINANZAS_API_ENABLED=true FINANZAS_API_BASE_URL=http://localhost:8080 ./mvnw -f pom.xml compile exec:java
```

Inicie sesion con `ana.duena@finanzasapp.test` / `Prueba2026*`.

### Paso 4 (opcional). Solo modo local, sin backend

```powershell
$env:FINANZAS_DEMO_MODE="true"
.\mvnw.cmd -f pom.xml compile exec:java
```

Usuario: `samuel@finanzasapp.com` / `123456`. Los datos quedan en `%USERPROFILE%\.finanzasapp`.

## 4. Montaje sin Docker (PostgreSQL instalado localmente)

1. Cree la base y el usuario en PostgreSQL 16:

```sql
CREATE USER finanzas WITH PASSWORD 'finanzas_dev_password';
CREATE DATABASE finanzas OWNER finanzas;
```

2. Ejecute el script (crea tablas y datos de prueba):

```bash
psql -h localhost -p 5432 -U finanzas -d finanzas -v ON_ERROR_STOP=1 -f database/crear_bd.sql
```

3. Arranque el backend con JDK 25 indicando que el esquema ya existe:

```powershell
$env:FINANZAS_DB_URL="jdbc:postgresql://localhost:5432/finanzas"
$env:POSTGRES_USER="finanzas"
$env:POSTGRES_PASSWORD="finanzas_dev_password"
$env:FINANZAS_JWT_SECRET="cambie-este-secreto-de-desarrollo-de-al-menos-32-caracteres"
$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE="true"
$env:SPRING_FLYWAY_BASELINE_VERSION="9"
.\mvnw.cmd -f finanzas-backend\pom.xml spring-boot:run
```

Alternativa: cree solo la base vacia, arranque el backend **sin** las dos variables `SPRING_FLYWAY_*` (Flyway crea las tablas) y luego ejecute `database/datos_prueba.sql`.

4. Siga el Paso 3 para el cliente.

## 5. Pruebas automatizadas

```bash
./mvnw -f finanzas-backend/pom.xml test     # backend: 65 pruebas (la de migraciones usa Testcontainers y requiere Docker)
./mvnw -f pom.xml test                      # cliente: 29 pruebas
```

**Prueba de humo / aceptacion de la API** (25 casos de la hoja "11. Casos de Prueba"; modifica datos, ejecutela sobre datos recien sembrados):

```powershell
docker compose down -v; docker compose up -d --build
powershell -ExecutionPolicy Bypass -File scripts\prueba-humo-api.ps1
# ... tabla CP / Caso / Resultado / Detalle ...
# Aprobadas: 25  Fallidas: 0
```

Sin JDK 25 local, se pueden ejecutar en un contenedor:

```bash
docker run --rm -v "$PWD":/src -w /src eclipse-temurin:25-jdk sh mvnw -B -f finanzas-backend/pom.xml test
```

Los casos de prueba manuales y de aceptacion (con datos y resultado esperado) estan en la hoja **"11. Casos de Prueba"** del Excel; cada requisito funcional tiene al menos uno (hoja "9. Trazabilidad").

## 6. Detener y reiniciar

```bash
docker compose down        # detiene, conserva los datos
docker compose down -v     # detiene y BORRA la base de datos (vuelve a sembrarse en el siguiente "up")
```

## 7. Problemas frecuentes

| Sintoma | Solucion |
|---|---|
| `port is already allocated` en 5432 u 8080 | Cambie `POSTGRES_PORT` o `BACKEND_PORT` en `.env` (y `FINANZAS_API_BASE_URL` en el cliente). |
| `Java 25 is required` | Instale JDK 25 o use el instalador / Docker. |
| El cliente no muestra los datos del hogar | Seleccione "Hogar Gomez" en el selector de workspace del encabezado. |
| El cliente entra en modo local | Verifique `FINANZAS_API_ENABLED=true` en la misma terminal o en las variables de usuario. |
| Login responde 429 | 5 intentos fallidos seguidos bloquean ese correo 5 minutos; espere. |
| `./mvnw: Permission denied` (Linux/macOS) | `chmod +x mvnw` |
| Correos de verificacion o recuperacion | No se envian por SMTP; quedan como archivos en el volumen `finanzas-backend-storage` (`/app/storage/mail`). Ver con `docker compose exec backend ls /app/storage/mail`. |

## 8. Funcionalidades

Registro e inicio de sesion (local y backend), ingresos y gastos con busqueda, categorias con archivado, dashboard con graficos, presupuestos con alertas, ahorro automatico con libro mayor, metas, workspaces del hogar con roles OWNER/ADMIN/MEMBER/VIEWER e invitaciones, gastos compartidos y liquidaciones, movimientos recurrentes, notificaciones, reportes CSV/PDF/Excel, perfil/avatar/tema/moneda, exportacion y eliminacion de cuenta, facturas con adjuntos, preparacion DIAN (topes UVT), simulador y respaldo/restauracion local.

Limitaciones conocidas: [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## 9. Documentacion adicional

- [docs/ARQUITECTURA_Y_AMBIENTE.md](docs/ARQUITECTURA_Y_AMBIENTE.md) - arquitectura y ambiente
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - decisiones de arquitectura (ADR en `docs/adr/`)
- [docs/API.md](docs/API.md), [docs/SECURITY.md](docs/SECURITY.md), [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)
- [docs/USE_CASES.md](docs/USE_CASES.md), [docs/TRACEABILITY_MATRIX.md](docs/TRACEABILITY_MATRIX.md)
- [calidad_U/DESCRIPCION_INTERFACES_Y_REQUISITOS.md](calidad_U/DESCRIPCION_INTERFACES_Y_REQUISITOS.md) - interfaces y mockups textuales
