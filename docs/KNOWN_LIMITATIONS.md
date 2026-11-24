# Limitaciones Conocidas

Revisado el 2026-11-24 tras el Informe QA (prueba de humo 25/25, pruebas backend 65/65 y cliente 29/29).
Ninguna de estas limitaciones impide montar el ambiente ni ejecutar los casos de prueba del Excel.

## Funcionales

- **Correo electronico:** los correos de verificacion y recuperacion de contrasena no se envian por SMTP; quedan como archivos en `/app/storage/mail` (modo `file`). Para leerlos: `docker compose exec backend ls /app/storage/mail`.
- **Sincronizacion entre equipos:** el cliente refresca el workspace activo por intervalos (`FINANZAS_SYNC_POLL_SECONDS`, 20 s por defecto), no en tiempo real.
- **Recurrencias:** se ejecutan automaticamente una vez al dia (01:00, `finanzas.recurring.cron`) y tambien manualmente con el boton **Ejecutar ahora** de Inicio > Proximos movimientos (se muestran las de los proximos 30 dias) desde la aplicacion. En varias instancias del backend no hay bloqueo distribuido.
- **Migracion de datos locales al backend:** no hay asistente para pasar los datos del modo local a una cuenta del servidor.
- **Preparacion DIAN:** compara totales con los topes en UVT; es una ayuda de organizacion, no asesoria tributaria certificada. El valor UVT del anio lo configura el usuario.
- **Reportes PDF/XLSX:** se generan correctamente pero con formato basico (sin graficos).

## Interfaz

- El tema oscuro usa FlatLaf y tokens de color propios (corregido tras el Informe QA); los dialogos se ajustan a su contenido (DEF-017). Si se cambia el tema con la aplicacion abierta, la barra de titulo de Windows conserva el color anterior hasta reiniciar la aplicacion.
- Los iconos de la barra lateral usan emojis; se ven correctamente en Windows y pueden aparecer como cuadros en Linux sin fuente de emojis.

## Calidad y arquitectura

- No hay pruebas automatizadas de interfaz Swing ni E2E con dos clientes simultaneos; las pantallas se verificaron con capturas generadas desde la aplicacion real (`docs/capturas/`, `scripts/capturas/`).
- `DataManager` concentra demasiadas responsabilidades; el siguiente paso es extraer servicios de cliente (ver `docs/ARCHITECTURE.md`).
- El instalador `FinanzasApp-Setup.exe` (IExpress) no tiene firma digital: Windows SmartScreen puede pedir "Mas informacion > Ejecutar de todas formas". La aplicacion instalada si usa el icono propio; el archivo del instalador muestra el icono generico.
