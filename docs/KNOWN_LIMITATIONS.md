# Limitaciones Conocidas

- `DataManager` sigue concentrando demasiadas responsabilidades; ya no debe crecer mas sin extraer servicios.
- La seleccion de workspace sigue usando el primer workspace disponible; falta selector persistente por usuario.
- No existe sincronizacion incremental continua con revision/updated_since; el cliente usa refresco por snapshot.
- El cliente aun tiene flujos de red que pueden ejecutarse desde acciones UI existentes; falta auditoria completa de EDT y `SwingWorker`.
- Invitaciones recibidas, aceptar/rechazar, cancelar invitaciones y cambio de rol no estan completos en Swing.
- Ciudad, pais, zona horaria, formato monetario y preferencias de notificacion no estan modelados de extremo a extremo en backend/UI.
- Tema claro/oscuro persistente queda pendiente.
- Facturas/compras y preparacion DIAN no fueron implementadas.
- Simulador y calculadora avanzada no cumplen todo el alcance pedido.
- PDF/XLSX backend son funcionales pero minimos; falta PDFBox/OpenPDF y Apache POI.
- No hay pruebas E2E de dos usuarios/dos clientes ni suite Swing automatizada.
- Docker Compose no fue ejecutado localmente por falta de Docker.
- El backfill historico de ahorro no se ejecuta automaticamente; se requiere asistente con dry-run.
- No existe endpoint de recuperacion de contrasena, verificacion de correo, exportacion completa de usuario ni eliminacion de cuenta.
