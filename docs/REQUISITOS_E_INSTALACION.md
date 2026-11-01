# FinanzasApp - Requisitos del equipo, instalacion y cuentas de prueba

**Proyecto:** FinanzasApp - Gestion de finanzas personales y del hogar
**Autor:** Juan Jose Roman Bedoya - Aseguramiento de la Calidad del Software, UNIMINUTO (2026-2)
**Repositorio:** https://github.com/juan2052H/FinanzasApp

## Acceso rapido - usuario y contrasena

| Usuario (correo) | Contrasena | Rol | Donde se usa |
|---|---|---|---|
| **`ana.duena@finanzasapp.test`** | **`Prueba2026*`** | Duena del hogar (OWNER) | Acceso directo "FinanzasApp (servidor)" |
| **`carlos.miembro@finanzasapp.test`** | **`Prueba2026*`** | Miembro (MEMBER) | Acceso directo "FinanzasApp (servidor)" |
| **`laura.lectora@finanzasapp.test`** | **`Prueba2026*`** | Solo lectura (VIEWER) | Acceso directo "FinanzasApp (servidor)" |
| **`samuel@finanzasapp.com`** | **`123456`** | Demo sin servidor | Acceso directo "FinanzasApp" (modo local, seccion 4.3) |

La contrasena **`Prueba2026*`** termina en asterisco: el asterisco es parte de la contrasena.
Para que las tres primeras cuentas funcionen, el servidor debe estar encendido (seccion 4.1).

FinanzasApp tiene dos partes:

- **Aplicacion de escritorio** (Windows): la que usa el usuario final. Se instala con `FinanzasApp-Setup.exe`.
- **Servidor (backend + base de datos)**: guarda los datos compartidos del hogar y los usuarios de prueba. Se levanta con Docker con un solo comando.

---

## 1. Requisitos del equipo

### 1.1 Hardware

| Recurso | Minimo | Recomendado | Por que |
|---|---|---|---|
| Procesador | 64 bits, 2 nucleos, **virtualizacion activada** en la BIOS | 4 nucleos | Docker Desktop necesita virtualizacion (WSL 2) |
| Memoria RAM | 8 GB | 16 GB | Docker Desktop ~2 GB, servidor ~470 MB, base de datos ~70 MB, aplicacion ~300 MB |
| Disco libre | 10 GB | 15 GB | Docker Desktop, imagenes (~1,6 GB), compilacion (~2 GB) y aplicacion (~55 MB) |
| Pantalla | 1280 x 720 | 1920 x 1080 | La ventana principal mide 1280 x 800 (minimo 960 x 640) |
| Internet | Solo la primera vez | - | Descarga ~1,5 GB (imagenes de Docker y dependencias Maven) |

### 1.2 Software

| Software | Version | Obligatorio | Para que |
|---|---|---|---|
| Windows | 10 (22H2) u 11, 64 bits | Si | Sistema operativo de la aplicacion de escritorio |
| Docker Desktop | 4.x con WSL 2 (probado con Docker 29.4 / Compose v5.1) | Si, para el modo servidor | Levantar PostgreSQL 16 y el backend |
| Git | Cualquiera reciente | Opcional | Clonar el repositorio (tambien sirve el ZIP) |
| Java | **No hace falta** | No | El instalador incluye Java 25. Solo se necesita JDK 25 para compilar desde el codigo |
| Navegador web | Cualquiera | Opcional | Comprobar `http://localhost:8080/health/readiness` |

### 1.3 Red y permisos

- Puertos libres: **5432** (PostgreSQL) y **8080** (servidor).
- La aplicacion se instala **sin permisos de administrador** (en `%LOCALAPPDATA%\Programs\FinanzasApp`).
- Docker Desktop si requiere administrador para instalarse (una sola vez).
- El instalador no esta firmado: si Windows SmartScreen avisa, elegir **Mas informacion > Ejecutar de todas formas**.

---

## 2. Cuentas de prueba

Contrasena de todas las cuentas: **`Prueba2026*`**

| Usuario (correo) | Contrasena | Nombre | Rol en el hogar "Hogar Gomez" | Que permite probar |
|---|---|---|---|---|
| `ana.duena@finanzasapp.test` | `Prueba2026*` | Ana Gomez | **OWNER** (propietaria) | Todo: miembros, invitaciones, presupuestos, metas, facturas, reportes, DIAN |
| `carlos.miembro@finanzasapp.test` | `Prueba2026*` | Carlos Ruiz | **MEMBER** | Registrar ingresos, gastos y gastos compartidos |
| `laura.lectora@finanzasapp.test` | `Prueba2026*` | Laura Mejia | **VIEWER** (solo lectura) | Consultar; cualquier creacion o edicion es rechazada |

**Datos que ya trae el hogar "Hogar Gomez"** (fechas relativas al dia en que se levanta el servidor):

| Dato | Detalle |
|---|---|
| Ingresos | Salario Ana $4.500.000 (mes anterior y mes actual), proyecto Carlos $1.200.000 |
| Gastos | Arriendo $1.500.000 (x2), mercado, servicios, transporte, entretenimiento |
| Saldo del hogar | **$5.415.000** (ingresos $10.200.000 - gastos $4.785.000) |
| Ahorro automatico | 20 % de cada ingreso = $2.040.000 |
| Presupuestos del mes | Alimentacion $800.000, Transporte $250.000, Vivienda $1.500.000, **Entretenimiento $100.000 (excedido a proposito)** |
| Metas | Fondo de emergencia ($10.000.000) y Vacaciones en familia ($3.000.000) |
| Gasto compartido | Mercado $300.000 pagado por Ana, dividido con Carlos: **Carlos le debe $150.000 a Ana** |
| Otros | 1 movimiento recurrente (arriendo mensual), 1 factura (SPD-2026-000123, $320.000), 1 notificacion |

**Cuenta del modo local (sin servidor):** `samuel@finanzasapp.com` / `123456` (ver seccion 4.3).

---

## 3. El ejecutable si ve esos datos?

**Si, abriendo el acceso directo "FinanzasApp (servidor)" con el servidor encendido.** Verificado el 2026-11-01 con el instalador final: al iniciar sesion como Ana, el ejecutable abre directamente "Hogar Gomez - OWNER" con saldo $5.415.000, la grafica de gastos, presupuestos, metas, hogar y facturas.

El instalador crea **dos** accesos directos y cada uno trabaja con datos distintos:

| Acceso directo | Donde estan los datos | Ve las cuentas de prueba? |
|---|---|---|
| **FinanzasApp (servidor)** | En el servidor (PostgreSQL en Docker) | **Si** - ana, carlos y laura con todos sus datos |
| **FinanzasApp** | En el propio PC (`%USERPROFILE%\.finanzasapp`) | **No** - empieza vacio; se puede crear una cuenta o usar la demo local |

Si el servidor no esta encendido, "FinanzasApp (servidor)" no puede iniciar sesion: primero se ejecuta el paso 4.1.

---

## 4. Como ejecutar el proyecto

### 4.1 Paso 1 - Encender el servidor (una vez por sesion)

1. Abrir **Docker Desktop** y esperar a que diga *Engine running*.
2. Abrir PowerShell en la carpeta del proyecto (repositorio clonado o `FinanzasApp-codigo-fuente.zip` descomprimido):

```powershell
docker compose up -d --build
```

3. Esperar 2-5 minutos la primera vez (las siguientes son segundos) y comprobar:

```powershell
docker compose ps -a
```

Debe mostrar `finanzas-postgres` y `finanzas-backend` como **(healthy)** y `db-seed` como **Exited (0)** (este ultimo carga las cuentas de prueba y termina; es normal que quede detenido).

4. Opcional: abrir <http://localhost:8080/health/readiness> en el navegador. Debe mostrar `{"status":"UP","database":"UP"}`.

### 4.2 Paso 2 - Instalar y abrir la aplicacion

1. Ejecutar **`FinanzasApp-Setup.exe`** (35,7 MB). Al terminar muestra un mensaje de confirmacion y crea los accesos directos en el Escritorio y en el menu Inicio.
2. Abrir **"FinanzasApp (servidor)"**. La pantalla de acceso debe decir *Conectado al backend: http://localhost:8080*.
3. Escribir `ana.duena@finanzasapp.test` y `Prueba2026*` y pulsar **Enter** o **INICIAR SESION**.
4. La primera vez en cada PC aparece la ventana **"Primeros pasos"**: dejar los campos opcionales vacios y pulsar **Empezar**.
5. Se abre el panel **Inicio** del hogar "Hogar Gomez". El selector de arriba a la derecha permite cambiar entre "Hogar Gomez" y "Mi espacio" (personal).

### 4.3 Solo la aplicacion, sin servidor (modo local)

No necesita Docker. Abrir el acceso directo **"FinanzasApp"** y crear una cuenta con **Registrate**, o activar la cuenta de demostracion local:

```powershell
[Environment]::SetEnvironmentVariable("FINANZAS_DEMO_MODE", "true", "User")
```

Cerrar sesion de Windows (o reiniciar el Explorador), abrir "FinanzasApp" y entrar con `samuel@finanzasapp.com` / `123456`. La demo solo se crea si el modo local nunca se habia usado en ese PC; si ya se uso, cree una cuenta con **Registrate**. En este modo no existen los usuarios de prueba del servidor ni las funciones de hogar compartido entre varias personas.

### 4.4 Desde el codigo fuente (alternativa al instalador)

Requiere **JDK 25** (por ejemplo Eclipse Temurin 25). Con el servidor del paso 4.1 encendido:

```powershell
$env:FINANZAS_API_ENABLED = "true"
.\mvnw.cmd -f pom.xml compile exec:java
```

### 4.5 Sin Docker (PostgreSQL instalado en el PC)

Instalar PostgreSQL 16 y JDK 25, crear la base `finanzas` y ejecutar `database\crear_bd.sql` (tablas + cuentas de prueba). Los comandos exactos estan en el `README.md`, seccion 4.

---

## 5. Verificacion rapida (5 minutos)

| # | Accion | Resultado esperado |
|---|---|---|
| 1 | Entrar como Ana | Inicio de "Hogar Gomez" con saldo $5.415.000 |
| 2 | Gastos > escribir `Arriendo` en Buscar | 2 filas de $1.500.000 |
| 3 | Presupuesto | Entretenimiento en rojo: $120.000 de $100.000 |
| 4 | Finanzas del Hogar | Ana: le deben $150.000; Carlos: debe $150.000 |
| 5 | Facturas | Factura SPD-2026-000123 por $320.000 |
| 6 | Cerrar sesion y entrar como Laura; intentar crear un gasto | El sistema lo rechaza por permisos |

El recorrido completo (13 pasos) esta en `GUIA_EVALUADOR.md` y los 32 casos de prueba en el Excel.

**Prueba automatica opcional** (25 casos contra el servidor):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prueba-humo-api.ps1
```

Debe terminar con `Aprobadas: 25  Fallidas: 0`. Modifica datos: para volver al estado inicial ver la seccion 6.

---

## 6. Detener, reiniciar y restablecer los datos

| Que quiere hacer | Comando |
|---|---|
| Apagar el servidor conservando los datos | `docker compose down` |
| Volver a encenderlo | `docker compose up -d` |
| **Borrar todo y recargar las cuentas de prueba** | `docker compose down -v` y luego `docker compose up -d` |

## 7. Problemas frecuentes

| Sintoma | Solucion |
|---|---|
| `docker` no se reconoce o *cannot connect to the Docker daemon* | Abrir Docker Desktop y esperar a *Engine running* |
| `port is already allocated` | Otro programa usa el puerto 5432 u 8080: cerrarlo, o crear un archivo `.env` (copia de `.env.example`) y cambiar `POSTGRES_PORT` / `BACKEND_PORT` |
| La pantalla de acceso no dice "Conectado al backend" | Se abrio "FinanzasApp" (local); usar "FinanzasApp (servidor)" |
| "Credenciales invalidas" con las cuentas de prueba | El servidor no esta encendido o los datos no se cargaron: revisar `docker compose ps -a` (paso 4.1) |
| "Demasiados intentos" | 5 intentos fallidos bloquean ese correo 5 minutos |
| La aplicacion abre sin datos | Elegir "Hogar Gomez" en el selector de arriba a la derecha |
| SmartScreen bloquea el instalador | Mas informacion > Ejecutar de todas formas |
| Correos de verificacion o recuperacion | No se envian por internet; quedan en el servidor: `docker compose exec backend ls /app/storage/mail` |

## 8. Desinstalar

Menu Inicio > FinanzasApp > **Desinstalar FinanzasApp**, o Configuracion de Windows > Aplicaciones > Aplicaciones instaladas > FinanzasApp. Los datos del modo local (`%USERPROFILE%\.finanzasapp`) se conservan. Para borrar el servidor y su base de datos: `docker compose down -v`.

## 9. Documentos relacionados

| Documento | Contenido |
|---|---|
| `GUIA_EVALUADOR.md` | Recorrido de verificacion de 13 pasos ligado a requisitos y casos de prueba |
| `ARQUITECTURA_Y_AMBIENTE.md` | Componentes y capas, tecnologias y versiones, variables de entorno, base de datos |
| `FinanzasApp_Formato_Calidad_Final.xlsx` | Requisitos, historias, casos de uso, mockups, trazabilidad, 32 casos de prueba y defectos |
| `crear_bd.sql` | Script de la base de datos PostgreSQL 16 con las cuentas de prueba |
| `KNOWN_LIMITATIONS.md` (en el repositorio) | Limitaciones conocidas |
