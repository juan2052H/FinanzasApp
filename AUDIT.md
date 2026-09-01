# AUDIT.md - FinanzasApp 2.0

Fecha de auditoria: 2026-08-18

## Resumen

FinanzasApp conserva un cliente Java 11 Swing con persistencia local serializada y ahora incluye un modulo `finanzas-backend` con Spring Boot, PostgreSQL/Flyway, JPA, JWT, autorizacion por workspace, invitaciones, hogar compartido, recurrencias, notificaciones reales, reportes REST/CSV y auditoria por workspace. La UI Swing puede operar en modo local o, con `FINANZAS_API_ENABLED=true`, iniciar sesion contra el backend, listar workspaces y sincronizar categorias, transacciones, presupuestos del mes actual, metas, recurrencias, notificaciones y hogar compartido del workspace activo sin persistir tokens.

Se creo un respaldo local previo del estado encontrado: `data/app-state.bin.audit-backup-20260818-150324`.

## Arquitectura Encontrada

| Area | Implementacion actual | Observaciones |
| --- | --- | --- |
| Entrada | `com.finanzas.Main` | Lanza `LoginFrame`. |
| UI | `com.finanzas.ui.*` | Swing, pantallas por panel y `CardLayout`. |
| Estado | `DataManager` singleton | Centraliza datos, sesion, listeners, persistencia local y puente opcional al backend. |
| Persistencia | `PersistenceService` | Serializa `AppState` en `app-state.bin`. |
| Auth | `AuthService` | Antes usaba SHA-256 simple; ahora PBKDF2 compatible con legacy. |
| Exportacion | `ExportService` | CSV, XML tipo Excel y PDF simple generado a mano. |
| Modelos | `Usuario`, `Transaccion`, `Presupuesto`, `MetaAhorro`, `GastoHogar`, `FinancialCategory`, `RecurringTransaction` | Los importes usan `BigDecimal` como valor canonico y conservan `double` legacy solo para compatibilidad de serializacion. |
| Backend | `finanzas-backend` | Spring Boot 4.1.0, JPA, Flyway, Security, JWT, Google PKCE, endpoints REST para miembros/invitaciones, categorias, transacciones, presupuestos, metas, gastos compartidos, recurrencias, notificaciones, reportes y audit logs. |

## Hallazgos

| Problema | Archivo/clase afectada | Causa | Severidad | Solucion aplicada o propuesta | Riesgos de modificarlo |
| --- | --- | --- | --- | --- | --- |
| Usuario demo inicial y datos Samuel en modo normal | `DataManager`, `Usuario`, `LoginFrame` | `initData()` creaba una cuenta demo; `Usuario()` tenia datos por defecto; login precargaba correo y `123456`. | Critica | Aplicado: demo solo se inicializa con `FINANZAS_DEMO_MODE=true`; `Usuario()` queda vacio; login queda vacio. | Estados binarios antiguos pueden seguir conteniendo usuarios reales/demo ya creados; no se borran datos existentes. |
| Sesion persistida | `PersistenceService.AppState`, `DataManager` | `currentUser` se guardaba dentro de `app-state.bin`. | Critica | Aplicado: nuevos guardados no persisten `currentUser`; al cargar/restaurar se exige login. | Usuarios que esperaban reapertura directa ahora deben iniciar sesion. |
| Hogar global mezclaba datos entre usuarios | `DataManager`, `HouseholdInsights`, `FinanzasHogarPanel` | `gastosHogar` y `miembrosHogar` eran colecciones globales. | Critica | Aplicado: cada `UserProfile` tiene sus propias colecciones de hogar; datos legacy se migran solo a un propietario confiable o quedan no expuestos. | Los datos legacy globales sin propietario requieren revision manual en una migracion formal. |
| Graficos con datos inventados | `DataManager`, `DashboardPanel`, `ReportesPanel` | Series y porcentajes hardcodeados para seis meses/categorias. | Alta | Aplicado: ingresos/gastos por mes y categorias se calculan desde transacciones reales; estados vacios cuando no hay datos. | En backend debe normalizarse por `category_id`; en Swing ya existen categorias editables por perfil. |
| Tendencias falsas | `DashboardPanel`, `ReportesPanel` | Textos como `+12% vs mes anterior` y periodos fijos `Abril 2024`. | Alta | Aplicado: se eliminan textos fijos; variaciones se calculan si existe mes anterior; reportes tienen periodos y rango personalizado. | Dashboard sigue orientado a mes actual y ultimos 6 meses por diseno. |
| Avatar fragil | `PersistenceService`, `DataManager`, `AvatarView`, `ConfiguracionPanel` | Se copiaban archivos con rutas relativas y sin validar imagen real. | Alta | Aplicado: almacenamiento en directorio estable configurable, validacion PNG/JPG/JPEG, limite 5 MB, crop cuadrado y miniatura PNG. | La app debe migrar referencias antiguas relativas si existen avatares previos. |
| Errores silenciados | `DataManager`, `PersistenceService` | `catch` vacios o `false` sin detalle. | Alta | Aplicado parcialmente: errores de persistencia/avatar se registran y exponen mediante `getLastErrorMessage()`. | Falta logging estructurado completo e IDs de error. |
| SHA-256 simple para contrasenas | `AuthService` | Hash sin salt ni coste. | Critica | Aplicado: PBKDF2WithHmacSHA256 con salt; login legacy SHA-256 rehashea al autenticar. | Usuarios legacy con hash SHA-256 solo se migran al iniciar sesion correctamente. |
| Cambio de correo rompe login/perfil | `ConfiguracionPanel`, `DataManager` | UI mutaba `Usuario.email` pero el mapa seguia indexado por correo antiguo. | Critica | Aplicado: `DataManager.updateProfile(...)` valida email unico, mueve la clave y conserva datos. | Falta verificacion de nuevo correo en una arquitectura real. |
| Presupuesto pide gastado manual | `PresupuestoPanel`, `DataManager`, `Presupuesto` | El usuario capturaba `montoGastado` y el modelo dividia por cero si presupuesto era 0. | Alta | Aplicado: formulario ya no pide gastado; se calcula desde gastos reales del mes y categoria; division por cero protegida. | Falta historial de periodos presupuestales y categorias normalizadas. |
| Botones falsos / funciones simuladas | `Sidebar`, `DashboardPanel`, `LoginFrame` | Acciones mostraban mensajes sin flujo real. | Alta | Aplicado: sidebar abre alta de ingreso; dashboard usa formularios reales/navegacion; `Ctrl+K` agregado; Google queda deshabilitado en Swing si no esta configurado; backend ya implementa Google Authorization Code + PKCE. | Falta conectar la UI desktop al backend OAuth. |
| Fechas ignoradas al crear transaccion desde dashboard | `DashboardPanel` | Usaba `LocalDate.now()` aunque el formulario mostraba campo de fecha. | Alta | Aplicado: dialogo reutilizable parsea `dd/MM/yyyy` y usa la fecha introducida. | Falta calendario visual. |
| Parseo de montos inconsistente | `DashboardPanel`, `TransaccionesPanel`, `PresupuestoPanel`, importadores CSV | Se eliminaban puntos/comas de forma distinta por pantalla. | Media | Aplicado: `Money.parseFlexible(...)` y dialogs comunes interpretan montos tipo `1.250.000,50`; importadores usan `BigDecimal`. | Faltan controles visuales de moneda/calendario mas ricos. |
| `double` para dinero | Modelos y servicios | Riesgo de precision financiera. | Alta | Aplicado: campos `BigDecimal` canonicos en transacciones, presupuestos, metas, hogar y recurrencias; `readObject` migra legacy. | Las APIs publicas antiguas con `double` se conservan para no romper UI/serializacion. |
| `app-state.bin` como almacenamiento principal | `PersistenceService` | Persistencia binaria no auditable ni multiusuario segura. | Alta | Parcial: backend PostgreSQL/Flyway creado; Swing sigue usando persistencia local hasta integrar API. | Migracion debe preservar datos existentes sin fugas. |
| Sistema de hogar incompleto en backend | `finanzas-backend` | La migracion tenia tablas para invitaciones/splits/liquidaciones, pero no habia servicios REST que las usaran. | Alta | Aplicado: entidades, repositorios, servicios y endpoints para invitaciones, roles de miembros, gastos compartidos, split igual/porcentaje/monto, balances y liquidaciones. | Falta conectar la UI desktop de hogar y agregar pruebas de integracion con PostgreSQL/Testcontainers. |
| Auditoria de acciones sensibles ausente | `finanzas-backend` | La tabla `audit_logs` existia, pero no habia entidad, servicio ni eventos escritos. | Alta | Aplicado: audit logs con metadata JSON para workspaces, membresias, categorias, transacciones, presupuestos, metas, gastos compartidos y liquidaciones; consulta protegida para OWNER/ADMIN. | Falta ampliar cobertura cuando se implementen nuevos modulos REST. |
| Recurrencias y notificaciones faltaban en REST | `finanzas-backend` | El esquema tenia `recurring_transactions` y `notifications`, pero no existia API funcional. | Alta | Aplicado: CRUD operativo de recurrencias, ejecucion que crea una transaccion real y avanza fecha; notificaciones recalculadas desde presupuestos, metas y recurrencias. | Falta scheduler automatico y pruebas de integracion con base real. |
| Reportes REST incompletos | `finanzas-backend` | Solo existia un resumen analytics global sin rango ni exportacion. | Alta | Aplicado: `/reports/summary` con rango, transacciones, categorias, presupuestos, metas y proximos movimientos; `/reports/export.csv` con auditoria. | Falta PDF/XLSX servidor y pruebas de integracion con datos reales. |
| Cliente desktop desconectado del backend | `DataManager`, `FinanzasApiClient`, UI Swing | La UI solo usaba colecciones locales, aun cuando ya existia backend REST. | Alta | Aplicado: modo API opcional con login/registro REST, Google Sign-In desktop PKCE, logout con revocacion, badge de workspace, sincronizacion y CRUD remoto de categorias, transacciones, presupuestos del mes actual, metas, recurrencias, notificaciones, hogar compartido, invitaciones por email y exportacion CSV de reportes. | Edicion avanzada de roles y PDF/XLSX servidor todavia no estan sincronizados desde Swing. |
| Sin tests | Proyecto completo | No habia suite automatizada. | Alta | Aplicado: se agregan pruebas JUnit para regresiones criticas de datos/sesion/avatar/presupuesto/hogar/reportes/busqueda/notificaciones/categorias/ahorro/rate limiting y parser JSON monetario. | Falta ampliar pruebas de integracion end-to-end con PostgreSQL/Testcontainers y cliente HTTP real. |

## Plan Tecnico Basado En El Codigo Real

### Fase 0 - Auditoria

Estado: aplicada en este documento.

Acciones:
- Inventariar arquitectura actual Swing + singleton.
- Respaldar `data/app-state.bin`.
- Identificar datos demo, fugas de hogar, graficos falsos, auth debil y botones simulados.

### Fase 1 - Estabilidad

Estado: aplicada en el cliente Swing/local.

Acciones aplicadas:
- Login/registro sin datos precargados.
- Demo mode solo por `FINANZAS_DEMO_MODE=true`.
- Sesion no persistida.
- Hogar aislado por perfil.
- Avatar validado, recortado y persistido en ruta estable.
- Graficos y porcentajes desde transacciones reales.
- Estados vacios para dashboard/reportes.
- Presupuesto calcula gastado desde transacciones.
- Cambio de correo seguro dentro del mapa de perfiles.
- `Ctrl+K` con acciones reales.
- PBKDF2 para nuevas contrasenas.
- Rate limiting local para intentos fallidos de login.
- Reportes con periodos, rango personalizado, graficas y exportacion filtrada.
- Busqueda global de transacciones, presupuestos, metas, miembros y categorias.
- Categorias personalizables por perfil con archivado.
- Ahorro recomendado configurable: conservador, equilibrado, agresivo, personalizado, monto fijo o desactivado.
- Salud financiera explicable en dashboard.
- Simulador "que pasa si" conectado a servicio de dominio sin mutar datos reales.
- Transacciones recurrentes, proximos movimientos y detector de posibles suscripciones.
- Notificaciones reales basadas en presupuestos, metas, gasto inusual y recurrencias, respetando preferencias.
- Hogar con splits iguales, porcentuales, montos personalizados y liquidaciones.
- Pruebas de regresion agregadas.

Pendiente dentro de Fase 1:
- Recuperacion de contrasena real o retirar completamente ese flujo hasta backend.
- Google OAuth completo con Authorization Code + PKCE.
- Mas pruebas automatizadas de UI, export/import y migracion legacy completa.
- Conectar `LegacyStateMigrationService` a un comando/flujo visible de mantenimiento si se requiere migracion manual.

### Fase 2 - Persistencia y Arquitectura

Estado: iniciada.

Aplicado:
- Modulo independiente `finanzas-backend`.
- Entidades JPA con UUID para users, workspaces, members, categories, transactions, budgets y savings_goals.
- Repositorios JPA y servicios de autorizacion por workspace.
- PostgreSQL + Flyway con migracion inicial y `V2__persistent_refresh_tokens.sql`.
- OpenAPI copiado al modulo backend.
- Cliente Swing con `FinanzasApiClient`, login/registro/logout REST opcional por `FINANZAS_API_ENABLED=true`.
- Google Sign-In desktop con navegador del sistema, callback temporal en `127.0.0.1`, PKCE S256 y entrega del code al backend `/api/auth/google`.
- Sincronizacion desktop de categorias, transacciones, presupuestos del mes actual, metas, recurrencias y notificaciones del workspace activo, con ids remotos para editar/borrar sin heuristicas.
- Sincronizacion desktop de miembros, gastos compartidos y liquidaciones del workspace; en modo backend no se crean miembros locales inventados.
- Exportacion CSV de reportes desde el backend cuando el cliente esta en modo API; PDF/Excel permanecen locales.
- Endpoints REST para invitaciones, miembros, roles, gastos compartidos, splits, balances y liquidaciones.
- CRUD REST completo de categorias, transacciones, presupuestos y metas, incluyendo `PUT` y validaciones de tipo categoria/transaccion/presupuesto.
- Recurrencias REST con intervalo personalizado versionado en Flyway (`V3__recurring_custom_interval.sql`) y dashboard desktop conectado a crear, desactivar y ejecutar recurrencias remotas.
- Audit logs por workspace para acciones sensibles y endpoint protegido para consultar ultimos eventos.
- Endpoints REST para recurrencias y notificaciones basadas en datos reales.
- Endpoints REST de reportes por rango y exportacion CSV auditada.
- Pruebas unitarias backend para split exacto de gastos y guardas de acceso por workspace.

Pendiente:
- Agregar edicion avanzada de roles y vista de invitaciones pendientes.
- Completar exportaciones PDF/XLSX del lado servidor y scheduler de recurrencias.
- Consolidar `LegacyStateMigrationService` con repositorios reales y un comando de migracion versionado.

### Fase 3 - Seguridad y Auth

Estado: iniciada.

Aplicado en backend:
- Spring Security stateless.
- BCrypt para password hashing.
- Access token JWT con expiracion.
- Refresh tokens persistidos como hash en PostgreSQL, revocables y rotados al renovar sesion.
- Rate limiting por email.
- Google Authorization Code + PKCE con validacion de `id_token`.
- Validacion de membresia de workspace en rutas financieras implementadas.

Pendiente:
- Integrar desktop con el flujo OAuth real.
- Endurecer auditoria y cierre de otras sesiones.

### Fase 4 - UI Moderna

Propuesta:
- Mantener Swing mientras se estabiliza dominio.
- Migrar gradualmente pantallas a JavaFX cuando exista API estable.
- Crear sistema de componentes, estados vacios, loading, tema claro/oscuro y command palette completo.

### Fase 5 - Funciones Fintech

Propuesta:
- Salud financiera explicable.
- Simulador "que pasa si".
- Recurrencias, detector de suscripciones, notificaciones e insights por reglas.
- Reportes profesionales con rangos y exportaciones robustas.

## Criterio De Seguridad Multiusuario

A partir de esta fase, ningun metodo de `DataManager` debe exponer datos financieros si `currentUser` es nulo, y los datos de hogar se leen desde el perfil autenticado. En `finanzas-backend`, las rutas financieras implementadas ya verifican membresia del `workspaceId` antes de leer o modificar datos.
