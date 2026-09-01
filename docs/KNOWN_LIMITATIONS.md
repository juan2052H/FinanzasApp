# Limitaciones Conocidas

- `DataManager` sigue concentrando demasiadas responsabilidades; ya no debe crecer mas sin extraer servicios.
- No existe sincronizacion incremental continua con revision/updated_since; el cliente usa refresco por snapshot.
- El cliente aun tiene flujos de red que pueden ejecutarse desde acciones UI existentes; falta auditoria completa de EDT y `SwingWorker`.
- El selector de workspace, las invitaciones internas y el cambio de roles ya estan en Swing, pero falta suite E2E de dos clientes.
- Los emails de verificacion/reset tienen sink local `file/log/disabled`; falta proveedor SMTP/transaccional real.
- Ciudad, pais, zona horaria, formato monetario y preferencias de notificacion ya estan modelados en backend/cliente; falta aplicar el formato monetario en todos los renderizadores historicos.
- Tema claro/oscuro persistente existe con tokens centrales; falta barrer todos los componentes antiguos que todavia pintan colores fijos.
- Facturas/compras y preparacion DIAN no fueron implementadas.
- Simulador y calculadora avanzada no cumplen todo el alcance pedido.
- PDF/XLSX backend son funcionales pero minimos; falta PDFBox/OpenPDF y Apache POI.
- No hay pruebas E2E de dos usuarios/dos clientes ni suite Swing automatizada.
- Docker Compose no fue ejecutado localmente por falta de Docker.
- El backfill historico de ahorro no se ejecuta automaticamente; se requiere asistente con dry-run.
- La eliminacion de cuenta bloquea owners de workspaces compartidos; falta flujo de transferencia de propiedad desde UI/API.
