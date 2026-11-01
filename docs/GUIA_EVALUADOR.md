# FinanzasApp - Guia para el equipo evaluador

Esta guia permite montar el ambiente y verificar los requisitos principales en unos 20 minutos.
El detalle completo (27 requisitos funcionales, 20 no funcionales, 32 casos de prueba) esta en
[`calidad_U/FinanzasApp_Formato_Calidad_Final.xlsx`](../calidad_U/FinanzasApp_Formato_Calidad_Final.xlsx).

## 1. Montar el ambiente (5-10 min)

Necesita **Docker Desktop** encendido y los puertos **5432** y **8080** libres.

```bash
git clone https://github.com/juan2052H/FinanzasApp.git
cd FinanzasApp
docker compose up -d --build
docker compose ps -a
```

Listo cuando `finanzas-postgres` y `finanzas-backend` dicen **(healthy)** y el servicio `db-seed` dice **Exited (0)**.
Comprobacion: abrir <http://localhost:8080/health/readiness> en el navegador debe mostrar `{"status":"UP","database":"UP"}`.

**Atajo opcional:** `powershell -ExecutionPolicy Bypass -File scripts\prueba-humo-api.ps1` ejecuta 25 casos contra la API y debe terminar con `Aprobadas: 25  Fallidas: 0`. Modifica datos: para repetir las pruebas manuales despues, reinicie con `docker compose down -v` y `docker compose up -d`.

## 2. Instalar la aplicacion de escritorio (2 min)

1. Ejecute `FinanzasApp-Setup.exe` (incluye Java; no requiere permisos de administrador). Si Windows SmartScreen avisa, elija **Mas informacion > Ejecutar de todas formas** (el instalador no esta firmado).
2. Abra **"FinanzasApp (servidor)"** desde el Escritorio o el menu Inicio. La pantalla de acceso debe decir *Conectado al backend: http://localhost:8080*.

Sin instalador (con JDK 25): `.\mvnw.cmd -f pom.xml compile exec:java` con `FINANZAS_API_ENABLED=true` (ver README).

## 3. Usuarios de prueba

Contrasena de todos: **`Prueba2026*`**

| Usuario | Rol en "Hogar Gomez" |
|---|---|
| `ana.duena@finanzasapp.test` | OWNER (propietaria) |
| `carlos.miembro@finanzasapp.test` | MEMBER (registra datos) |
| `laura.lectora@finanzasapp.test` | VIEWER (solo lectura) |

**Importante:** despues de iniciar sesion, seleccione **"Hogar Gomez"** en el selector de workspace del encabezado (arriba a la derecha). Los datos de prueba estan en ese hogar, no en "Mi espacio".

Los datos se cargan con fechas relativas: hay movimientos del **mes actual** y del **mes anterior** a la fecha en que se levanto el ambiente.

## 4. Recorrido de verificacion (10 min)

Cada fila indica que hacer, que debe verse y que requisito/caso de prueba cubre. Si algo no coincide, registrelo como defecto (seccion 5).

| # | Usuario | Accion | Resultado esperado | RF / CP |
|---|---|---|---|---|
| 1 | Ana | Iniciar sesion con clave incorrecta `mala12345` | Mensaje de credenciales invalidas | RF-002 / CP-003 |
| 2 | Ana | Iniciar sesion bien y elegir "Hogar Gomez" | Dashboard con ingresos, gastos, saldo, dona por categoria y barras de 6 meses | RF-008, RF-009 / CP-016 |
| 3 | Ana | **Gastos** > escribir `Arriendo` en Buscar | Solo 2 filas de "Arriendo apartamento" ($1.500.000 c/u) | RF-005 / CP-014 |
| 4 | Ana | **Presupuesto** | 4 presupuestos; Alimentacion $650.000 de $800.000; Entretenimiento excedido ($120.000 de $100.000) | RF-010, RF-011 / CP-007, CP-017 |
| 5 | Ana | **Ahorro y Metas** > "Vacaciones en familia" > **+ Agregar aporte** de 300.000 | Avance 10%, faltan $2.700.000 | RF-013 / CP-018 |
| 6 | Ana | **Finanzas del Hogar** | 3 miembros (Ana OWNER, Carlos MEMBER, Laura VIEWER); Ana: le deben $150.000; Carlos: debe $150.000 | RF-015, RF-017 / CP-031 |
| 7 | Ana | **Facturas** | Factura SPD-2026-000123, total $320.000 | RF-026 / CP-032 |
| 8 | Ana | **Reportes** > Periodo "Rango personalizado" > Desde/Hasta = primer y ultimo dia del mes anterior > Aplicar > **Exportar reporte** (Excel) | Ingresos $5.700.000 y gastos $2.515.000; se genera el archivo | RF-020, RF-021 / CP-019, CP-025 |
| 9 | Ana | **Reportes** > **Preparacion DIAN** | Topes en UVT y aviso de que no es asesoria certificada | RF-027 / CP-028 |
| 10 | Laura | Elegir "Hogar Gomez" e intentar **Nuevo Gasto** | El sistema rechaza la operacion por permisos (VIEWER) | RF-004, RNF-002 / CP-004 |
| 11 | Carlos | **Nuevo Gasto** de $1.000 en Alimentacion en "Hogar Gomez" | El gasto aparece y el total de gastos aumenta | RF-004 / CP-004 |
| 12 | Carlos | **Configuracion** > Tema, moneda y region > Oscuro > Guardar; cerrar sesion y volver a entrar | Se conserva el tema oscuro | RF-022 / CP-026 |
| 13 | Nuevo | En la pantalla de acceso, **Registrate** con un correo nuevo | Se crea la cuenta con "Mi espacio" y 14 categorias | RF-001 / CP-002 |

Modo local (sin servidor): abra el acceso directo **"FinanzasApp"**. Funciona sin backend; con la variable `FINANZAS_DEMO_MODE=true` crea el usuario `samuel@finanzasapp.com` / `123456`.

## 5. Como reportar un defecto

Use la hoja **"12. Defectos"** del Excel (o un issue en GitHub) con:

- **Titulo** y **pasos para reproducir** numerados (usuario, pantalla, datos).
- **Resultado esperado** vs **resultado obtenido**.
- **Severidad:** Critica (no se puede usar), Mayor (falla una funcion), Moderada, Menor, Cosmetica.
- Para errores del servidor: el `correlationId` que aparece en la respuesta y `docker compose logs backend`.

## 6. Limitaciones conocidas

Antes de reportar, revise [`docs/KNOWN_LIMITATIONS.md`](KNOWN_LIMITATIONS.md): por ejemplo, los correos no se envian por SMTP (quedan en `/app/storage/mail`) y la sincronizacion entre equipos es por intervalos de 20 s.

## 7. Si algo no arranca

| Sintoma | Solucion |
|---|---|
| `port is already allocated` | Otro servicio usa 5432/8080: deténgalo o cambie `POSTGRES_PORT`/`BACKEND_PORT` en `.env` |
| `db-seed` no termina en `Exited (0)` | `docker compose logs db-seed backend` y reportar el error |
| La app abre sin datos | Seleccione "Hogar Gomez" en el encabezado |
| La pantalla de acceso no dice "Conectado al backend" | Abrio "FinanzasApp" (modo local); use "FinanzasApp (servidor)" |
| Login responde "demasiados intentos" | 5 intentos fallidos bloquean ese correo 5 minutos |

Arquitectura, versiones, variables de entorno y script SQL: [`docs/ARQUITECTURA_Y_AMBIENTE.md`](ARQUITECTURA_Y_AMBIENTE.md).
