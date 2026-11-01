"""Genera calidad_U/FinanzasApp_Formato_Calidad_Final.xlsx sobre la plantilla UNIMINUTO.

Uso: python scripts/generate-formato-calidad.py <plantilla.xlsx> <salida.xlsx> [BACKEND_RESULT=...] [BACKEND_DETAIL=...] [CLIENT_RESULT=...] [CLIENT_DETAIL=...]
Requiere: pip install openpyxl
"""
import copy
import sys
from datetime import datetime

import openpyxl

TEMPLATE = sys.argv[1]
OUTPUT = sys.argv[2]
HOY = datetime(2026, 11, 1)
EQUIPO = "Juan Jose Roman Bedoya"

wb = openpyxl.load_workbook(TEMPLATE)


def body_style_from(ws, row=6):
    return {c.column: (copy.copy(c.font), copy.copy(c.fill), copy.copy(c.alignment), copy.copy(c.border))
            for c in ws[row]}


def fill_table(ws, rows, first_col=1, skip_cols=()):
    """Escribe filas desde la fila 5, limpia la fila de ejemplo y conserva formulas en skip_cols."""
    style = body_style_from(ws)
    for r in range(5, 46):
        for c in range(first_col, ws.max_column + 1):
            if c in skip_cols:
                continue
            ws.cell(r, c).value = None
    for c in range(1, ws.max_column + 1):
        if c in style:
            f, fi, al, bo = style[c]
            cell = ws.cell(5, c)
            cell.font, cell.fill, cell.alignment, cell.border = copy.copy(f), copy.copy(fi), copy.copy(al), copy.copy(bo)
    if len(rows) > 41:
        raise SystemExit(f"{ws.title}: demasiadas filas ({len(rows)})")
    for i, row in enumerate(rows):
        r = 5 + i
        for j, value in enumerate(row):
            c = first_col + j
            if c in skip_cols:
                continue
            ws.cell(r, c).value = value
        lines = max(len(str(v)) // 45 + str(v).count("\n") + 1 for v in row if v is not None)
        ws.row_dimensions[r].height = max(30, min(15 * lines, 260))


def fill_card(ws, values, start_row=4):
    for i, value in enumerate(values):
        ws.cell(start_row + i, 2).value = value
        src = ws.cell(start_row + i, 2).font
        ws.cell(start_row + i, 2).font = openpyxl.styles.Font(name=src.name, size=src.sz, color="FF000000")
        ws.cell(start_row + i, 2).fill = openpyxl.styles.PatternFill(fill_type=None)


# ---------------------------------------------------------------- 1. Proyecto
fill_card(wb["1. Proyecto"], [
    "FinanzasApp - Gestion de finanzas personales y del hogar",
    "PRY-2026-FINANZAS",
    "Equipo FinanzasApp - Ingenieria de Software",
    "Juan Jose Roman Bedoya",
    "Luis Fernando Londono Lopez",
    "Aseguramiento de la Calidad del Software",
    "Ingenieria de Software - UNIMINUTO",
    "2026-2",
    "Incremental / Scrum",
    "2026-08-03",
    "2026-11-01",
    "Desarrollar una aplicacion de escritorio (Java Swing) con backend REST (Spring Boot + PostgreSQL) que permita a una persona "
    "o a un hogar registrar ingresos y gastos, controlar presupuestos, ahorrar hacia metas, dividir gastos compartidos y generar reportes.",
    "Registro/inicio de sesion local y por API; ingresos y gastos; categorias; dashboard con graficos; presupuestos y alertas; "
    "ahorro automatico con libro mayor; metas; workspaces del hogar con roles OWNER/ADMIN/MEMBER/VIEWER e invitaciones; gastos "
    "compartidos y liquidaciones; recurrencias; notificaciones; reportes CSV/PDF/Excel; perfil, tema y preferencias; facturas y "
    "preparacion DIAN; simulador; respaldo y restauracion local.",
    "Aplicacion movil nativa, pagos en linea, conexion con bancos, envio real de correo SMTP (se usa un buzon en archivos), "
    "certificacion tributaria DIAN, sincronizacion incremental en tiempo real y asistente completo de migracion local a backend.",
    "ISO/IEC 25010 - Modelo de calidad del producto software",
    "GitHub (repositorio y GitHub Actions), Maven Wrapper, Docker Compose, JUnit 5, Testcontainers, OpenAPI",
])

# ---------------------------------------------------------------- 2. Interesados
fill_table(wb["2. Interesados"], [
    ("INT-001", "Usuario individual", "Usuario final", "Registrar ingresos y gastos facilmente, ver su saldo real, proteger su informacion y exportarla.", "Alto", "Alto", "Pruebas de usabilidad y encuesta"),
    ("INT-002", "Responsable del hogar", "Usuario final (OWNER del hogar)", "Coordinar gastos familiares: miembros, invitaciones, division de gastos, deudas y liquidaciones.", "Alto", "Alto", "Reunion de validacion por sprint"),
    ("INT-003", "Miembro del hogar (MEMBER/ADMIN)", "Usuario colaborador", "Registrar gastos del hogar y conocer cuanto debe o le deben.", "Medio", "Alto", "Pruebas de aceptacion"),
    ("INT-004", "Integrante solo lectura (VIEWER)", "Usuario consultor", "Consultar movimientos y reportes sin riesgo de modificar datos.", "Bajo", "Medio", "Pruebas de aceptacion"),
    ("INT-005", "Docente de la asignatura", "Cliente academico / evaluador", "Producto instalable, documentado, con requisitos verificables y trazabilidad completa.", "Alto", "Alto", "Entregas en el aula virtual"),
    ("INT-006", "Equipo de testing cruzado", "Evaluador externo", "Poder montar el ambiente, ejecutar la aplicacion y probarla contra criterios de aceptacion.", "Medio", "Alto", "README, documento de arquitectura y este formato"),
    ("INT-007", "Analista de requisitos", "Equipo de desarrollo", "Alcance claro, requisitos y casos de uso trazables.", "Alto", "Alto", "Reunion semanal del equipo"),
    ("INT-008", "Analista de calidad", "Equipo de desarrollo", "Evidencia de pruebas, registro de defectos y no regresion.", "Alto", "Alto", "GitHub Issues y CI"),
    ("INT-009", "Arquitecto / desarrollador", "Equipo de desarrollo", "Separacion cliente/backend, persistencia segura y codigo mantenible.", "Alto", "Alto", "Revisiones de codigo en GitHub"),
    ("INT-010", "Administrador de despliegue", "Operacion", "Configurar Docker, variables de entorno, secretos, respaldos y monitoreo de salud.", "Medio", "Medio", "Documentacion docs/DEPLOYMENT.md"),
])

# ---------------------------------------------------------------- 3. Req Funcionales
RF = [
    ("RF-001", "Registrar cuenta", "El sistema debe permitir registrar una cuenta con nombre, correo, contrasena (minimo 8 caracteres), moneda y tipo de cuenta.", "Autenticacion", "INT-001", "Alta", "Media", "Implementado",
     "Dado un correo no registrado, cuando el usuario se registra con datos validos, entonces se crea la cuenta, un workspace 'Mi espacio' con 14 categorias por defecto y se abre la aplicacion. Con un correo ya registrado el sistema muestra 'Ya existe una cuenta con ese correo' (HTTP 409)."),
    ("RF-002", "Iniciar y cerrar sesion", "El sistema debe permitir iniciar y cerrar sesion en modo local o contra el backend.", "Autenticacion", "INT-001", "Alta", "Media", "Verificado",
     "Dado el usuario ana.duena@finanzasapp.test, cuando ingresa la contrasena Prueba2026*, entonces accede al dashboard (HTTP 200 con accessToken). Con una contrasena incorrecta se muestra 'Credenciales invalidas' (HTTP 401) y tras varios intentos fallidos se bloquea temporalmente (HTTP 429)."),
    ("RF-003", "Gestionar ingresos", "El sistema debe permitir registrar, editar, consultar y eliminar ingresos con categoria, descripcion, monto y fecha.", "Transacciones", "INT-001", "Alta", "Media", "Verificado",
     "Dado un usuario autenticado, cuando registra un ingreso de 1.000.000 en 'Salario', entonces aparece en la tabla de Ingresos, el total y el saldo del dashboard aumentan en 1.000.000. Un monto vacio, cero o negativo es rechazado con mensaje de validacion."),
    ("RF-004", "Gestionar gastos", "El sistema debe permitir registrar, editar, consultar y eliminar gastos con categoria, descripcion, monto y fecha.", "Transacciones", "INT-001", "Alta", "Media", "Verificado",
     "Dado un usuario MEMBER del hogar, cuando registra un gasto de 1.000 en 'Alimentacion', entonces el gasto aparece en la tabla y el total de gastos aumenta (HTTP 201). Un VIEWER que intenta lo mismo recibe 'sin permisos' (HTTP 403)."),
    ("RF-005", "Buscar transacciones", "El sistema debe permitir buscar transacciones por texto y filtrar por categoria.", "Transacciones", "INT-001", "Media", "Baja", "Implementado",
     "Dado el hogar de prueba, cuando el usuario escribe 'Arriendo' en el buscador de Gastos, entonces solo se listan las transacciones cuya descripcion contiene 'Arriendo' (2 registros)."),
    ("RF-006", "Crear categorias", "El sistema debe permitir crear categorias de ingreso y gasto con nombre, icono y color.", "Categorias", "INT-001", "Media", "Baja", "Implementado",
     "Dado un workspace activo, cuando el usuario crea la categoria de gasto 'Mascotas' con color #ff9900, entonces aparece en el selector de categorias. Si el nombre ya existe para ese tipo se rechaza (HTTP 409) y un color invalido se rechaza (HTTP 422)."),
    ("RF-007", "Archivar y restaurar categorias", "El sistema debe permitir archivar y restaurar categorias sin romper las transacciones historicas asociadas.", "Categorias", "INT-001", "Media", "Media", "Implementado",
     "Dada la categoria 'Entretenimiento' con transacciones, cuando se archiva, entonces deja de aparecer para nuevos registros pero las transacciones existentes la siguen mostrando; al restaurarla conserva el mismo identificador."),
    ("RF-008", "Dashboard con indicadores reales", "El dashboard debe mostrar saldo, ingresos, gastos y ahorro calculados a partir de los datos registrados.", "Dashboard", "INT-001", "Alta", "Media", "Verificado",
     "Dado el hogar 'Hogar Gomez' con los datos de prueba, cuando Ana abre Inicio, entonces los indicadores coinciden con /savings/summary: ingresos 10.200.000, gastos 4.785.000 y ahorro 2.040.000 (20% de los ingresos)."),
    ("RF-009", "Graficos de distribucion y flujo", "El sistema debe mostrar la distribucion de gastos por categoria (dona) y la comparativa de ingresos vs gastos de los ultimos meses (barras).", "Dashboard", "INT-001", "Media", "Media", "Implementado",
     "Dado un usuario con gastos en 4 categorias, cuando abre Inicio, entonces la dona muestra esas 4 categorias con sus porcentajes; si no hay gastos se muestra un estado vacio en lugar del grafico."),
    ("RF-010", "Presupuestos mensuales", "El sistema debe permitir crear presupuestos mensuales por categoria y mostrar gastado, disponible y porcentaje usado.", "Presupuesto", "INT-001", "Alta", "Media", "Verificado",
     "Dado el presupuesto de Alimentacion de 800.000 del mes actual con gastos por 650.000, cuando el usuario abre Presupuesto, entonces ve gastado 650.000, disponible 150.000 y 81% usado."),
    ("RF-011", "Alertas de presupuesto", "El sistema debe alertar cuando un presupuesto alcance los estados de atencion, critico o excedido.", "Presupuesto / Notificaciones", "INT-001", "Media", "Media", "Implementado",
     "Dado el presupuesto de Entretenimiento de 100.000 con gastos por 120.000, cuando el usuario abre Presupuesto o Notificaciones, entonces la tarjeta aparece como EXCEDIDO (120%) y existe una notificacion de presupuesto."),
    ("RF-012", "Ahorro automatico", "El sistema debe permitir configurar un porcentaje de ahorro automatico sobre cada ingreso y registrar depositos y retiros en un libro mayor.", "Ahorro", "INT-001", "Alta", "Alta", "Verificado",
     "Dado el hogar con ahorro automatico del 20%, cuando se registra un ingreso de 1.000.000, entonces el ahorro total aumenta en 200.000. Un retiro mayor al ahorro libre es rechazado (HTTP 409)."),
    ("RF-013", "Metas de ahorro", "El sistema debe permitir crear metas con monto objetivo y fecha limite, asignar ahorro y consultar el avance.", "Metas", "INT-001", "Alta", "Media", "Implementado",
     "Dada la meta 'Vacaciones en familia' de 3.000.000, cuando el usuario asigna 300.000 desde el ahorro libre, entonces el avance muestra 10% y faltan 2.700.000; no se puede asignar mas ahorro del disponible (HTTP 409)."),
    ("RF-014", "Workspaces", "El sistema debe permitir crear workspaces (personal, hogar o negocio) y seleccionar el workspace activo.", "Hogar / Workspaces", "INT-002", "Alta", "Media", "Verificado",
     "Dado el usuario Ana, cuando consulta sus workspaces, entonces ve 'Mi espacio' y 'Hogar Gomez' con rol OWNER; al cambiar el workspace activo en el encabezado los datos mostrados corresponden al workspace elegido."),
    ("RF-015", "Miembros, roles e invitaciones", "El sistema debe permitir invitar, aceptar, rechazar, remover y cambiar el rol de miembros segun los permisos del rol.", "Hogar / Workspaces", "INT-002", "Alta", "Alta", "Verificado",
     "Dado el hogar de prueba, cuando el OWNER invita a un correo nuevo, entonces la invitacion aparece en la bandeja del invitado y al aceptarla queda como MEMBER. Un VIEWER no puede invitar ni modificar datos (HTTP 403)."),
    ("RF-016", "Gastos compartidos", "El sistema debe permitir registrar gastos compartidos con division igual, porcentual o por monto.", "Hogar compartido", "INT-002", "Alta", "Alta", "Implementado",
     "Dado un gasto compartido de 300.000 pagado por Ana entre Ana y Carlos con division igual, cuando se guarda, entonces cada parte es 150.000. Si las partes por monto no suman el total el sistema lo rechaza (HTTP 422)."),
    ("RF-017", "Balances y liquidaciones", "El sistema debe calcular los balances por miembro y registrar liquidaciones conservando el historial.", "Hogar compartido", "INT-002", "Alta", "Alta", "Verificado",
     "Dado el gasto compartido de prueba, cuando se consultan los balances, entonces Carlos debe 150.000 y a Ana le deben 150.000; al registrar una liquidacion de 150.000 de Carlos a Ana ambos balances quedan en 0 y el gasto sigue en el historial."),
    ("RF-018", "Movimientos recurrentes", "El sistema debe permitir crear movimientos recurrentes (semanal, quincenal, mensual, anual o personalizado) y ejecutarlos.", "Recurrencias", "INT-001", "Media", "Media", "Implementado",
     "Dada la recurrencia 'Arriendo apartamento' mensual de 1.500.000, cuando se ejecuta, entonces se crea el gasto correspondiente y la proxima fecha avanza un mes."),
    ("RF-019", "Notificaciones", "El sistema debe mostrar notificaciones derivadas de presupuestos, metas proximas a vencer, recurrencias proximas y gasto inusual.", "Notificaciones", "INT-001", "Media", "Media", "Implementado",
     "Dado el presupuesto excedido de Entretenimiento, cuando Ana abre el panel de notificaciones, entonces ve 'Presupuesto excedido' sin leer y puede marcarla como leida."),
    ("RF-020", "Reportes por periodo", "El sistema debe permitir consultar reportes por periodos predefinidos (mes, trimestre, anio) o por un rango personalizado.", "Reportes", "INT-001", "Media", "Media", "Implementado",
     "Dado el hogar de prueba, cuando el usuario elige 'Rango personalizado' con el primer y ultimo dia del mes anterior y pulsa 'Aplicar', entonces el reporte muestra ingresos 5.700.000 y gastos 2.515.000. Un rango con fecha inicial mayor que la final es rechazado con mensaje."),
    ("RF-021", "Exportar reportes", "El sistema debe permitir exportar reportes a CSV, PDF y Excel.", "Reportes", "INT-001", "Media", "Media", "Implementado",
     "Dado un reporte del mes actual, cuando el usuario elige CSV, PDF o Excel y una carpeta destino, entonces se genera el archivo con los datos del periodo y se informa la ruta."),
    ("RF-022", "Perfil y preferencias", "El sistema debe permitir actualizar perfil, avatar (PNG/JPEG), tema claro/oscuro, moneda, idioma regional y zona horaria.", "Configuracion", "INT-001", "Media", "Media", "Implementado",
     "Dado un usuario autenticado, cuando cambia el tema a Oscuro y guarda, entonces la interfaz cambia de inmediato y conserva el tema al volver a iniciar sesion. Un avatar que no es una imagen PNG/JPEG real es rechazado (HTTP 422)."),
    ("RF-023", "Exportar y eliminar cuenta", "El sistema debe permitir exportar los datos de la cuenta en JSON y eliminar la cuenta con confirmacion de correo y contrasena.", "Configuracion", "INT-001", "Media", "Alta", "Implementado",
     "Dado un usuario sin workspaces compartidos, cuando confirma su correo y contrasena en 'Eliminar cuenta', entonces la cuenta se anonimiza, sus sesiones se revocan y vuelve a la pantalla de acceso. Un OWNER con un hogar compartido recibe un error (HTTP 409)."),
    ("RF-024", "Respaldo y restauracion local", "El sistema debe permitir crear y restaurar respaldos ZIP del estado local con validaciones de seguridad.", "Datos locales", "INT-001", "Media", "Media", "Implementado",
     "Dado el modo local, cuando el usuario crea un respaldo, entonces se genera un ZIP; al restaurarlo se pide confirmacion. Un ZIP con rutas '../' o demasiado grande es rechazado y los datos actuales no cambian."),
    ("RF-025", "Simulador financiero", "El sistema debe permitir simular escenarios (aumento de ahorro, reduccion de gastos) sin modificar los datos reales.", "Simulador", "INT-001", "Baja", "Media", "Implementado",
     "Dado un usuario con datos, cuando simula reducir 10% los gastos, entonces ve la proyeccion resultante y, al cerrar el simulador, las transacciones y saldos reales no cambian."),
    ("RF-026", "Facturas y adjuntos", "El sistema debe permitir administrar facturas (numero, comercio, NIT, subtotal, IVA, total) y sus adjuntos PDF o imagen en modo backend.", "Facturas", "INT-001", "Media", "Media", "Verificado",
     "Dado el hogar de prueba, cuando Ana abre Facturas, entonces ve la factura SPD-2026-000123 por 320.000 asociada al pago de servicios; puede adjuntar un PDF y descargarlo. Un total en cero es rechazado."),
    ("RF-027", "Preparacion tributaria DIAN", "El sistema debe resumir ingresos, compras y patrimonio del anio y compararlos con los topes en UVT para indicar si podria estar obligado a declarar renta.", "Facturas / Impuestos", "INT-001", "Baja", "Media", "Implementado",
     "Dado un valor UVT configurado para el anio, cuando el usuario abre el asistente DIAN, entonces ve cada tope (1.400 UVT ingresos/compras/consignaciones, 4.500 UVT patrimonio) y si lo supera; el sistema aclara que no es una asesoria tributaria certificada."),
]
RF_VERIFICADOS = {"RF-001", "RF-002", "RF-004", "RF-005", "RF-024", "RF-006", "RF-007", "RF-009", "RF-012", "RF-013", "RF-015", "RF-016",
                  "RF-017", "RF-018", "RF-019", "RF-020", "RF-021", "RF-023"}
RF = [r[:7] + ("Verificado" if r[0] in RF_VERIFICADOS else "Implementado",) + r[8:] for r in RF]
fill_table(wb["3. Req Funcionales"], [r + ("1.1",) for r in RF])

# ---------------------------------------------------------------- 4. Req No Funcionales
fill_table(wb["4. Req No Funcionales"], [
    ("RNF-001", "Seguridad", "Las contrasenas deben almacenarse con hash: BCrypt en el backend y PBKDF2 en el modo local.", "0 contrasenas en texto plano en la tabla users y en el estado local; hash BCrypt con costo 12.", "Consulta SQL a users.password_hash (prefijo $2a$/$2b$) y prueba unitaria de AuthService.", "Alta", "Implementado", "RF-001, RF-002"),
    ("RNF-002", "Seguridad", "Cada operacion financiera debe validar que el usuario pertenece al workspace y que su rol lo permite.", "100% de los endpoints /api/workspaces/{id}/** responden 403/404 a usuarios sin membresia o sin rol suficiente.", "Pruebas WorkspaceAccessServiceTest y prueba manual con el usuario VIEWER (CP-004).", "Alta", "Verificado", "RF-004, RF-015"),
    ("RNF-003", "Seguridad", "Las sesiones deben usar JWT de corta duracion y refresh tokens rotados, guardados como hash y revocables.", "Access token 15 min; refresh 14 dias; reutilizar un refresh token revocado responde 401.", "RefreshTokenServiceTest y revision de la tabla refresh_tokens (solo token_hash).", "Alta", "Verificado", "RF-002"),
    ("RNF-004", "Seguridad", "El inicio de sesion debe limitar los intentos fallidos para mitigar fuerza bruta.", "Tras 5 intentos fallidos consecutivos el correo queda bloqueado temporalmente (HTTP 429).", "Prueba manual con contrasena incorrecta repetida; LoginRateLimiter.", "Media", "Verificado", "RF-002"),
    ("RNF-005", "Seguridad", "Los secretos (JWT, credenciales de BD) deben llegar por variables de entorno y nunca estar en el codigo.", "0 secretos reales versionados; .env esta en .gitignore y solo existe .env.example.", "Revision de git status / busqueda de secretos en el repositorio.", "Alta", "Verificado", "-"),
    ("RNF-006", "Seguridad", "Los adjuntos y avatares deben validar tipo real, tamano y contenido; los ZIP de respaldo deben resistir path traversal y bombas ZIP.", "Archivo > 5 MB o con contenido no PNG/JPEG/PDF se rechaza (HTTP 413/422); ZIP con '../' se rechaza.", "AvatarStorageServiceTest, InvoiceServiceTest y DataManagerRegressionTest.", "Alta", "Implementado", "RF-022, RF-024, RF-026"),
    ("RNF-007", "Rendimiento", "Las consultas de la API deben responder rapido con los datos de prueba.", "Tiempo de respuesta <= 500 ms (p95) para login, listado de transacciones y resumen de ahorro en un equipo de desarrollo.", "Medicion con curl -w '%{time_total}' sobre 20 peticiones por endpoint.", "Media", "Implementado", "RF-003, RF-008"),
    ("RNF-008", "Rendimiento", "La interfaz no debe congelarse durante llamadas de red.", "Las llamadas HTTP se ejecutan fuera del hilo de eventos de Swing (EDT); la ventana sigue respondiendo durante la sincronizacion.", "Revision de codigo (SwingWorker/hilos) y prueba manual con backend lento.", "Media", "Implementado", "RF-014"),
    ("RNF-009", "Usabilidad", "Toda accion destructiva debe pedir confirmacion explicita.", "100% de las acciones Eliminar/Restaurar/Eliminar cuenta muestran un dialogo de confirmacion.", "Lista de chequeo manual sobre cada pantalla.", "Alta", "Implementado", "RF-003, RF-004, RF-023, RF-024"),
    ("RNF-010", "Usabilidad", "Los formularios deben conservar los datos validos y explicar el error cuando un campo falla.", "Al ingresar un monto invalido, el resto de campos permanece y se muestra un mensaje que indica el campo y como corregirlo.", "Prueba manual en el dialogo de transaccion.", "Media", "Implementado", "RF-003, RF-004"),
    ("RNF-011", "Usabilidad", "Las cantidades monetarias deben mostrarse con separadores de miles y simbolo de moneda segun la preferencia del usuario.", "Montos en COP se muestran como $ 1.500.000 en todas las tablas y tarjetas.", "Revision visual de Inicio, Ingresos, Gastos y Reportes.", "Media", "Implementado", "RF-022"),
    ("RNF-012", "Disponibilidad", "El modo local debe funcionar sin backend ni conexion a Internet.", "Con el backend apagado la aplicacion inicia en modo local y permite registrar transacciones.", "Prueba manual con FINANZAS_API_ENABLED=false y red desconectada.", "Alta", "Implementado", "RF-002"),
    ("RNF-013", "Disponibilidad", "El backend debe exponer un endpoint de salud que verifique la base de datos.", "GET /health/readiness responde {\"status\":\"UP\",\"database\":\"UP\"} con la BD disponible.", "curl http://localhost:8080/health/readiness y healthcheck de Docker Compose.", "Media", "Verificado", "-"),
    ("RNF-014", "Mantenibilidad", "Los importes deben manejarse con BigDecimal/NUMERIC(19,2) y redondeo controlado a 2 decimales.", "0 diferencias de centavos en las pruebas de ahorro y gastos compartidos.", "SavingsServiceTest y SharedExpenseSplitCalculatorTest.", "Alta", "Verificado", "RF-012, RF-016"),
    ("RNF-015", "Mantenibilidad", "El esquema de base de datos debe versionarse con migraciones Flyway y validarse contra las entidades JPA al arrancar.", "Base vacia -> el backend aplica V1..V9 y arranca sin errores de validacion (ddl-auto=validate).", "docker compose up sobre volumen limpio (CP-001).", "Alta", "Verificado", "-"),
    ("RNF-016", "Mantenibilidad", "El codigo debe contar con pruebas automatizadas que se ejecuten en integracion continua.", "Todas las pruebas de cliente y backend pasan en GitHub Actions en cada push.", "Workflow .github/workflows/ci.yml.", "Media", "Implementado", "-"),
    ("RNF-017", "Portabilidad", "La aplicacion debe instalarse en Windows sin requerir Java ni Maven preinstalados.", "El instalador FinanzasApp-Setup.exe incluye Java 25 y se instala sin permisos de administrador.", "Instalacion en un equipo Windows 10/11 limpio.", "Media", "Implementado", "-"),
    ("RNF-018", "Portabilidad", "El backend debe poder montarse con un solo comando en cualquier equipo con Docker.", "docker compose up -d deja postgres y backend 'healthy' y carga los usuarios de prueba.", "Prueba de montaje desde cero (CP-001).", "Alta", "Verificado", "-"),
    ("RNF-019", "Compatibilidad", "El contrato de la API debe mantenerse sincronizado con su especificacion OpenAPI.", "Las copias de openapi.yaml del backend y del cliente son identicas (cmp en CI).", "Paso de verificacion OpenAPI en el CI.", "Media", "Implementado", "-"),
    ("RNF-020", "Legal / Normativo", "El sistema debe proteger los datos personales (Ley 1581 de 2012) y permitir al titular exportarlos o eliminarlos.", "Exportacion JSON de la cuenta y eliminacion con anonimizacion disponibles desde Configuracion.", "Prueba manual de RF-023.", "Media", "Verificado", "RF-023"),
])

# ---------------------------------------------------------------- 5. Historias de usuario
HU_VERIFICADAS = {"HU-001", "HU-002", "HU-017", "HU-004", "HU-006", "HU-008", "HU-009", "HU-010", "HU-011", "HU-012", "HU-013",
                  "HU-014", "HU-016"}
HU = [
    ("HU-001", "Usuario", "crear una cuenta con mi correo y contrasena", "guardar mis finanzas de forma privada", "Dado un correo no registrado, cuando completo el registro con contrasena de 8+ caracteres, entonces entro a la aplicacion con mi espacio y categorias por defecto.\nDado un correo ya registrado, cuando intento registrarme, entonces veo 'Ya existe una cuenta con ese correo'.", 3, "Alta", "S1", "Implementado", "RF-001"),
    ("HU-002", "Usuario", "iniciar sesion y cerrarla", "acceder solo yo a mi informacion", "Dado ana.duena@finanzasapp.test / Prueba2026*, cuando inicio sesion, entonces veo el dashboard.\nDada una contrasena incorrecta, entonces veo 'Credenciales invalidas'.", 2, "Alta", "S1", "Verificado", "RF-002"),
    ("HU-003", "Usuario", "registrar un ingreso", "conocer mi saldo real", "Dado que estoy autenticado, cuando registro un ingreso de 1.000.000, entonces aparece en la tabla y el saldo e indicadores se actualizan.", 3, "Alta", "S1", "Verificado", "RF-003"),
    ("HU-004", "Usuario", "registrar un gasto con categoria y fecha", "saber en que gasto mi dinero", "Dado que estoy autenticado, cuando registro un gasto, entonces aparece en Gastos, afecta el total y se refleja en reportes.", 3, "Alta", "S1", "Verificado", "RF-004, RF-005"),
    ("HU-005", "Usuario", "crear un presupuesto mensual por categoria", "controlar cuanto gasto", "Dado un presupuesto de 800.000 en Alimentacion, cuando registro gastos por 650.000, entonces veo gastado 650.000, disponible 150.000 y 81%.\nCuando supero el 100% la tarjeta aparece como EXCEDIDO.", 5, "Alta", "S2", "Verificado", "RF-010, RF-011"),
    ("HU-006", "Usuario", "que una parte de cada ingreso se ahorre automaticamente", "ahorrar sin pensarlo", "Dado un ahorro automatico del 20%, cuando registro un ingreso de 1.000.000, entonces el ahorro aumenta 200.000.\nCuando intento retirar mas del ahorro libre, entonces el sistema lo impide.", 8, "Alta", "S2", "Verificado", "RF-012"),
    ("HU-007", "Usuario", "definir metas de ahorro y asignarles dinero", "saber cuanto me falta para cada objetivo", "Dada la meta de 3.000.000, cuando asigno 300.000, entonces el avance es 10% y faltan 2.700.000. Al llegar al 100% la meta se marca como completada.", 5, "Alta", "S2", "Implementado", "RF-013"),
    ("HU-008", "Usuario", "ver graficos de mis gastos e ingresos", "entender mis habitos", "Dado que tengo gastos, cuando abro Inicio, entonces la dona y las barras representan solo mis datos; si no tengo datos veo un estado vacio.", 5, "Media", "S2", "Implementado", "RF-008, RF-009"),
    ("HU-009", "Responsable del hogar", "crear un hogar e invitar miembros con un rol", "administrar las finanzas familiares juntos", "Dado que soy OWNER, cuando invito a un correo como MEMBER, entonces el invitado ve la invitacion y al aceptarla accede al hogar.\nDado que soy VIEWER, cuando intento invitar o registrar, entonces el sistema lo impide.", 5, "Alta", "S3", "Verificado", "RF-014, RF-015"),
    ("HU-010", "Miembro del hogar", "dividir un gasto entre los miembros", "saber cuanto debe cada uno", "Dado un gasto de 300.000 entre 2 personas en partes iguales, cuando lo guardo, entonces cada parte es 150.000 y los balances muestran quien debe a quien.", 8, "Alta", "S3", "Verificado", "RF-016, RF-017"),
    ("HU-011", "Miembro del hogar", "registrar una liquidacion de deuda", "dejar las cuentas en cero sin perder el historial", "Dado que Carlos debe 150.000 a Ana, cuando registro la liquidacion, entonces ambos balances quedan en 0 y el gasto sigue en el historial.", 3, "Media", "S3", "Implementado", "RF-017"),
    ("HU-012", "Usuario", "programar movimientos recurrentes", "no olvidar pagos fijos como el arriendo", "Dada una recurrencia mensual, cuando se ejecuta, entonces se crea la transaccion y la proxima fecha avanza un mes.", 3, "Media", "S3", "Implementado", "RF-018"),
    ("HU-013", "Usuario", "recibir notificaciones de presupuestos, metas y pagos proximos", "reaccionar a tiempo", "Dado un presupuesto excedido, cuando abro notificaciones, entonces veo la alerta y puedo marcarla como leida.", 3, "Media", "S4", "Implementado", "RF-019"),
    ("HU-014", "Usuario", "exportar un reporte del periodo", "compartirlo o archivarlo", "Dado un rango personalizado con el mes anterior, cuando exporto a CSV, PDF o Excel, entonces se crea el archivo con los datos de ese periodo.", 3, "Media", "S4", "Implementado", "RF-020, RF-021"),
    ("HU-015", "Usuario", "personalizar mi perfil, avatar, tema y moneda", "usar la aplicacion a mi gusto", "Dado que cambio el tema a oscuro, cuando vuelvo a iniciar sesion, entonces el tema se conserva.", 3, "Baja", "S4", "Implementado", "RF-022"),
    ("HU-016", "Usuario", "exportar o eliminar mi cuenta", "controlar mis datos personales", "Dado que confirmo correo y contrasena, cuando elimino la cuenta, entonces se anonimiza y vuelvo al login.", 3, "Media", "S4", "Implementado", "RF-023"),
    ("HU-017", "Usuario local", "respaldar y restaurar mis datos", "no perder informacion", "Dado el modo local, cuando creo un respaldo, entonces se genera un ZIP valido; la restauracion pide confirmacion y rechaza ZIP maliciosos.", 5, "Media", "S4", "Implementado", "RF-024"),
    ("HU-018", "Usuario", "simular un escenario financiero", "tomar decisiones sin afectar mis registros", "Dado que simulo reducir 10% los gastos, cuando cierro el simulador, entonces mis datos reales no cambian.", 3, "Baja", "S5", "Implementado", "RF-025"),
    ("HU-019", "Usuario", "registrar facturas con su soporte", "tener mis comprobantes organizados", "Dada una factura con total > 0, cuando la guardo y adjunto un PDF, entonces aparece en la lista y puedo descargar el soporte.", 5, "Media", "S5", "Verificado", "RF-026"),
    ("HU-020", "Usuario", "saber si podria estar obligado a declarar renta", "prepararme ante la DIAN", "Dado el valor UVT del anio, cuando abro el asistente DIAN, entonces veo mis totales frente a cada tope y una advertencia de que no es asesoria certificada.", 3, "Baja", "S5", "Implementado", "RF-027"),
]
fill_table(wb["5. Historias de Usuario"], [h[:8] + ("Verificado" if h[0] in HU_VERIFICADAS else "Implementado",) + h[9:] for h in HU])

# ---------------------------------------------------------------- 6. Casos de uso
fill_table(wb["6. Casos de Uso"], [
    ("CU-001", "Registrar cuenta", "Usuario", "Backend, base de datos", "El correo no esta registrado.",
     "1. El usuario abre la pantalla de acceso y elige 'Crear cuenta'.\n2. Ingresa nombre, correo, contrasena, moneda y tipo de cuenta.\n3. El sistema valida los datos.\n4. El sistema crea el usuario, el workspace 'Mi espacio' y 14 categorias por defecto.\n5. El sistema abre el onboarding y luego el dashboard.",
     "3a. Contrasena con menos de 8 caracteres: el sistema muestra el error y conserva los demas campos.", "Correo ya registrado: HTTP 409 'Ya existe una cuenta con ese correo'. Backend no disponible: mensaje de error de red.",
     "El usuario queda autenticado con su workspace y categorias.", "RF-001", "Alta"),
    ("CU-002", "Iniciar sesion", "Usuario", "Backend", "El usuario tiene una cuenta.",
     "1. El usuario ingresa correo y contrasena.\n2. El sistema verifica las credenciales.\n3. El sistema emite tokens de acceso y refresco.\n4. El sistema sincroniza los datos del workspace activo y muestra el dashboard.",
     "2a. Modo local: se valida contra el perfil local (PBKDF2).", "Credenciales invalidas: HTTP 401. Demasiados intentos: HTTP 429.",
     "Sesion activa; el token de acceso se renueva automaticamente al expirar.", "RF-002", "Alta"),
    ("CU-003", "Registrar y consultar una transaccion", "Usuario (MEMBER o superior)", "Backend, servicio de ahorro", "Sesion activa y existe una categoria del tipo correspondiente.",
     "1. El usuario abre Ingresos o Gastos y pulsa 'Nuevo'.\n2. Selecciona categoria, escribe descripcion, monto y fecha.\n3. El sistema valida y guarda.\n4. Si es ingreso, el sistema registra el ahorro automatico.\n5. La tabla, el total, el dashboard y los reportes se actualizan.",
     "1a. El usuario busca por texto o filtra por categoria.\n1b. El usuario edita o elimina una transaccion (con confirmacion); el ahorro se recalcula.", "Monto no numerico, cero o negativo: mensaje de validacion. Usuario VIEWER: HTTP 403.",
     "La transaccion queda guardada y los indicadores reflejan el cambio.", "RF-003, RF-004, RF-005, RF-012", "Alta"),
    ("CU-004", "Controlar un presupuesto", "Usuario", "Servicio de notificaciones", "Sesion activa; existe la categoria de gasto.",
     "1. El usuario abre Presupuesto y pulsa 'Nuevo presupuesto'.\n2. Elige categoria, mes y monto.\n3. El sistema calcula gastado, disponible y porcentaje con las transacciones del mes.\n4. El sistema muestra el estado (normal, atencion, critico, excedido).",
     "1a. El usuario edita o elimina un presupuesto existente.", "Presupuesto duplicado para la misma categoria y mes: error de validacion.",
     "El presupuesto queda registrado y genera alertas al superar umbrales.", "RF-010, RF-011", "Alta"),
    ("CU-005", "Crear una meta y asignar ahorro", "Usuario", "Libro mayor de ahorro", "Sesion activa; existe ahorro libre para asignar.",
     "1. El usuario abre Metas y pulsa 'Nueva meta'.\n2. Indica nombre, monto objetivo y fecha limite.\n3. Pulsa 'Agregar aporte' e ingresa el monto.\n4. El sistema registra la asignacion en el libro mayor.\n5. Se actualizan porcentaje, barra y monto faltante.",
     "3a. El usuario libera ahorro de una meta.", "Aporte mayor al ahorro libre: HTTP 409. Monto invalido: mensaje de validacion.",
     "La meta refleja el avance; al 100% queda como completada.", "RF-012, RF-013", "Alta"),
    ("CU-006", "Gestionar miembros del hogar", "OWNER / ADMIN", "Usuario invitado", "Workspace de tipo hogar y rol OWNER o ADMIN.",
     "1. El usuario abre Hogar > Miembros.\n2. Invita un correo con un rol.\n3. El invitado ve la invitacion en su bandeja y la acepta.\n4. El sistema lo agrega como miembro.\n5. El OWNER puede cambiar roles, remover miembros o transferir la propiedad.",
     "3a. El invitado rechaza la invitacion.\n3b. El OWNER cancela la invitacion pendiente.", "Rol sin permisos: HTTP 403. OWNER intenta salir sin transferir: HTTP 409. Invitacion expirada: HTTP 410.",
     "La membresia y los roles quedan actualizados y auditados.", "RF-014, RF-015", "Alta"),
    ("CU-007", "Registrar un gasto compartido", "MEMBER / ADMIN / OWNER", "Miembros del hogar", "Workspace hogar con 2 o mas miembros.",
     "1. El usuario abre Hogar y pulsa 'Nuevo gasto compartido'.\n2. Elige pagador, participantes y metodo de division (igual, porcentaje o monto).\n3. El sistema calcula las partes.\n4. El sistema guarda el gasto y actualiza los balances.",
     "2a. Division por porcentaje: los porcentajes deben sumar 100.", "Las partes no suman el total: HTTP 422. Participante que no es miembro: HTTP 403.",
     "El gasto aparece y los balances muestran quien debe a quien.", "RF-016, RF-017", "Alta"),
    ("CU-008", "Liquidar una deuda", "MEMBER / ADMIN / OWNER", "Miembro acreedor", "Existe un balance pendiente entre dos miembros.",
     "1. El usuario abre Hogar > Balances.\n2. Pulsa 'Liquidar' y elige deudor, acreedor y monto.\n3. El sistema registra la liquidacion.\n4. El sistema recalcula balances.",
     "2a. Liquidacion parcial: el balance se reduce por el monto pagado.", "Monto mayor que la deuda o deudor igual al acreedor: error 409/422.",
     "Los balances se actualizan y el historial se conserva.", "RF-017", "Media"),
    ("CU-009", "Generar y exportar un reporte", "Usuario", "Generadores CSV/PDF/XLSX", "Sesion activa.",
     "1. El usuario abre Reportes.\n2. Elige un periodo predefinido o un rango.\n3. Pulsa 'Aplicar' y revisa los indicadores.\n4. Elige CSV, PDF o Excel y la carpeta destino.\n5. El sistema genera el archivo e informa la ruta.",
     "2a. Rango personalizado.", "Fecha inicial mayor que la final: mensaje de error. Carpeta sin permisos: mensaje de error.",
     "Se crea el archivo con los datos del periodo.", "RF-020, RF-021", "Media"),
    ("CU-010", "Programar un movimiento recurrente", "Usuario", "Backend", "Sesion activa.",
     "1. El usuario abre la opcion de recurrencias.\n2. Indica tipo, categoria, monto, frecuencia y proxima fecha.\n3. El sistema guarda la recurrencia.\n4. Al ejecutarse, crea la transaccion y avanza la proxima fecha.",
     "2a. Frecuencia personalizada en dias.", "Monto invalido o frecuencia no soportada: HTTP 422.",
     "La recurrencia queda activa y genera notificaciones de proximidad.", "RF-018, RF-019", "Media"),
    ("CU-011", "Configurar perfil y preferencias", "Usuario", "Almacenamiento de avatares", "Sesion activa.",
     "1. El usuario abre Configuracion.\n2. Edita nombre, ciudad, pais, moneda, idioma, zona horaria y tema.\n3. Sube un avatar PNG/JPEG.\n4. El sistema guarda y aplica las preferencias.",
     "3a. El usuario elimina su avatar.", "Imagen corrupta o de otro formato: HTTP 422. Archivo > 5 MB: HTTP 413.",
     "Las preferencias se conservan entre sesiones.", "RF-022", "Media"),
    ("CU-012", "Exportar o eliminar la cuenta", "Usuario", "Backend", "Sesion activa en modo API.",
     "1. El usuario abre Configuracion > Cuenta.\n2a. Pulsa 'Exportar datos' y obtiene un JSON.\n2b. Pulsa 'Eliminar cuenta', confirma correo y contrasena.\n3. El sistema revoca sesiones, borra avatar y preferencias y anonimiza la cuenta.\n4. Vuelve a la pantalla de acceso.",
     "-", "Contrasena incorrecta: 401. OWNER de un hogar compartido: 409.",
     "La cuenta queda anonimizada y el correo liberado.", "RF-023", "Media"),
    ("CU-013", "Respaldar y restaurar datos locales", "Usuario local", "Sistema de archivos", "Modo local.",
     "1. El usuario abre Configuracion > Datos.\n2. Pulsa 'Crear respaldo' y elige destino.\n3. Para restaurar, pulsa 'Restaurar', confirma la advertencia y elige el ZIP.\n4. El sistema valida y restaura.",
     "-", "ZIP corrupto, con rutas '../' o demasiado grande: se rechaza y el estado actual se conserva.",
     "El estado queda restaurado o intacto con un mensaje claro.", "RF-024", "Media"),
    ("CU-014", "Simular un escenario", "Usuario", "-", "Existen datos del usuario.",
     "1. El usuario abre el simulador desde Inicio.\n2. Ajusta porcentajes de ahorro o reduccion de gastos.\n3. El sistema muestra la proyeccion.",
     "-", "Valores no numericos: mensaje de validacion.", "Los datos reales no se modifican.", "RF-025", "Baja"),
    ("CU-015", "Gestionar facturas y preparacion DIAN", "Usuario", "Almacenamiento de adjuntos", "Sesion activa en modo API.",
     "1. El usuario abre Facturas y pulsa 'Nueva factura'.\n2. Ingresa numero, comercio, NIT, fecha, subtotal, IVA y total; opcionalmente la asocia a una transaccion.\n3. Adjunta un PDF o imagen.\n4. El sistema guarda la factura.\n5. Desde 'Preparacion DIAN' el usuario revisa sus totales frente a los topes UVT.",
     "1a. Filtrar, editar, eliminar o descargar el adjunto.", "Total <= 0: error de validacion. Adjunto no permitido: HTTP 422.",
     "La factura queda registrada con su soporte.", "RF-026, RF-027", "Media"),
])

# ---------------------------------------------------------------- 7. Mockups
DOC = "calidad_U/DESCRIPCION_INTERFACES_Y_REQUISITOS.md"
UI = "src/main/java/com/finanzas/ui/"
MOCKUPS = [
    ("MK-001", "Acceso y registro", "Alta fidelidad (implementado en Swing)", "Inicio de sesion, registro de cuenta y recuperacion de contrasena.", f"{DOC} sec. 4.1; {UI}LoginFrame.java", "RF-001, RF-002", "HU-001, HU-002", "1.1", "Aprobado", "Se agrego recuperacion de contrasena con token por correo."),
    ("MK-002", "Onboarding", "Alta fidelidad (implementado en Swing)", "Configuracion inicial: moneda, tipo de cuenta y porcentaje de ahorro.", f"{DOC} sec. 4.2; {UI}dialogs/OnboardingDialog.java", "RF-001, RF-012", "HU-001", "1.0", "Aprobado", "-"),
    ("MK-003", "Ventana principal / Dashboard", "Wireframe + alta fidelidad", "Barra lateral, encabezado con workspace activo, tarjetas de saldo/ingresos/gastos/ahorro, dona y barras.", f"{DOC} sec. 4.3 y 10; {UI}DashboardPanel.java", "RF-008, RF-009", "HU-008", "1.2", "Aprobado", "Se agrego selector de workspace en el encabezado."),
    ("MK-004", "Ingresos y Gastos", "Wireframe + alta fidelidad", "Tabla con busqueda, total y acciones; dialogo modal de nueva transaccion.", f"{DOC} sec. 4.4 y 10; {UI}TransaccionesPanel.java; {UI}dialogs/TransactionDialog.java", "RF-003, RF-004, RF-005", "HU-003, HU-004", "1.1", "Aprobado", "-"),
    ("MK-005", "Presupuesto", "Wireframe + alta fidelidad", "Tarjetas de presupuesto con barra de progreso y estado, configuracion de ahorro.", f"{DOC} sec. 4.5 y 10; {UI}PresupuestoPanel.java", "RF-010, RF-011, RF-012", "HU-005, HU-006", "1.1", "Aprobado", "-"),
    ("MK-006", "Ahorro y Metas", "Wireframe + alta fidelidad", "Resumen de ahorro, metas con barra de avance, fecha limite y aportes.", f"{DOC} sec. 4.6 y 10; {UI}MetasPanel.java", "RF-012, RF-013", "HU-006, HU-007", "1.1", "Aprobado", "-"),
    ("MK-007", "Finanzas del Hogar", "Alta fidelidad (implementado en Swing)", "Miembros y roles, invitaciones, gastos compartidos, balances y liquidaciones.", f"{DOC} sec. 4.7; {UI}FinanzasHogarPanel.java", "RF-014..RF-017", "HU-009, HU-010, HU-011", "1.3", "Aprobado", "Etiquetas con correo para distinguir miembros con el mismo nombre. Corregidos miembros duplicados (DEF-006)."),
    ("MK-008", "Facturas / Comprobantes", "Alta fidelidad (implementado en Swing)", "Lista de facturas con filtros, formulario y adjuntos; asistente DIAN.", f"{DOC} sec. 4.8; {UI}FacturasPanel.java; {UI}dialogs/DianTaxDialog.java", "RF-026, RF-027", "HU-019, HU-020", "1.1", "Aprobado", "Carga al abrir la seccion y montos con separador de miles (DEF-007, DEF-008)."),
    ("MK-009", "Reportes", "Alta fidelidad (implementado en Swing)", "Selector de periodo/rango, indicadores y exportacion CSV/PDF/Excel.", f"{DOC} sec. 4.9; {UI}ReportesPanel.java", "RF-020, RF-021", "HU-014", "1.1", "Aprobado", "Filtros en una segunda fila para no montarse sobre el titulo; tarjeta renombrada a Margen de ahorro (DEF-008)."),
    ("MK-010", "Configuracion", "Alta fidelidad (implementado en Swing)", "Perfil, avatar, notificaciones, categorias, tema, moneda, seguridad/sesiones y datos.", f"{DOC} sec. 4.10; {UI}ConfiguracionPanel.java", "RF-006, RF-007, RF-022, RF-023, RF-024", "HU-015, HU-016, HU-017", "1.2", "Aprobado", "Se agrego listado y revocacion de sesiones."),
    ("MK-011", "Simulador y movimientos recurrentes", "Alta fidelidad (implementado en Swing)", "Dialogos del simulador financiero y de recurrencias.", f"{DOC} sec. 4.11; {UI}dialogs/SimulationDialog.java; {UI}dialogs/RecurringTransactionDialog.java", "RF-018, RF-025", "HU-012, HU-018", "1.0", "Aprobado", "-"),
]
CAPTURAS = {
    "MK-001": ["MK-001_acceso.png"],
    "MK-002": ["MK-002_onboarding.png"],
    "MK-003": ["pantalla_01_inicio.png"],
    "MK-004": ["pantalla_02_ingresos.png", "pantalla_03_gastos.png"],
    "MK-005": ["pantalla_04_presupuesto.png"],
    "MK-006": ["pantalla_05_metas.png"],
    "MK-007": ["pantalla_06_hogar.png"],
    "MK-008": ["pantalla_07_facturas.png"],
    "MK-009": ["pantalla_08_reportes.png"],
    "MK-010": ["pantalla_09_config.png"],
}
MOCKUPS = [m[:2] + ("Captura de la aplicacion implementada + wireframe textual" if m[0] in CAPTURAS else m[2],)
           + m[3:4]
           + (("; ".join("docs/capturas/" + f for f in CAPTURAS[m[0]]) + "; " + m[4]) if m[0] in CAPTURAS else m[4],)
           + m[5:] for m in MOCKUPS]
fill_table(wb["7. Mockups"], MOCKUPS)

# ---------------------------------------------------------------- 8. Arquitectura
BE = "Java 25 + Spring Boot 4.1"
fill_table(wb["8. Arquitectura"], [
    ("ARQ-001", "Cliente escritorio", "UI Swing (LoginFrame, MainFrame, paneles y dialogos)", "Presentar pantallas, capturar datos y mostrar validaciones.", "Presentacion", "Java 25 Swing", "ARQ-002", EQUIPO, "RF-001..RF-027"),
    ("ARQ-002", "Cliente escritorio", "DataManager (fachada)", "Orquestar el modo local o API, cachear datos y aplicar reglas locales.", "Logica de negocio (cliente)", "Java 25", "ARQ-003, ARQ-004", EQUIPO, "RF-003..RF-025"),
    ("ARQ-003", "Cliente escritorio", "PersistenceService", "Guardar estado local versionado, respaldos ZIP y filtro de deserializacion.", "Persistencia local", "Java 25 (serializacion + ZIP)", "-", EQUIPO, "RF-024"),
    ("ARQ-004", "Cliente escritorio", "FinanzasApiClient", "Consumir la API REST con JWT y renovacion automatica de tokens.", "Integracion", "java.net.http (Java 25)", "ARQ-005", EQUIPO, "RF-002, RF-014"),
    ("ARQ-005", "Backend", "Controladores REST (/api/**)", "Exponer endpoints, validar entradas y traducir errores a ProblemDetail.", "Presentacion (API)", BE + " Web MVC", "ARQ-006, ARQ-007", EQUIPO, "RF-001..RF-027"),
    ("ARQ-006", "Backend", "Seguridad (JWT, BCrypt, rate limit)", "Autenticar, emitir/rotar tokens y limitar intentos.", "Seguridad", BE + " Security, OAuth2 JOSE", "ARQ-008", EQUIPO, "RF-002"),
    ("ARQ-007", "Backend", "Servicios de aplicacion (Auth, Savings, SharedExpense, Report, Invoice, TaxPreparation...)", "Aplicar reglas de negocio y autorizacion por workspace/rol.", "Logica de negocio", BE, "ARQ-008, ARQ-010", EQUIPO, "RF-003..RF-027"),
    ("ARQ-008", "Backend", "Entidades JPA y repositorios", "Mapear tablas y consultar la base de datos.", "Acceso a datos", "Spring Data JPA / Hibernate 7", "ARQ-009", EQUIPO, "Todos"),
    ("ARQ-009", "Base de datos", "PostgreSQL + migraciones Flyway V1..V9", "Persistir usuarios, workspaces, transacciones, ahorro, hogar y facturas.", "Datos", "PostgreSQL 16, Flyway", "-", EQUIPO, "Todos"),
    ("ARQ-010", "Backend", "Almacenamiento de archivos y buzon de correo", "Guardar avatares, adjuntos y correos de verificacion/recuperacion.", "Infraestructura", "Sistema de archivos (volumen Docker)", "-", EQUIPO, "RF-022, RF-026"),
    ("ARQ-011", "Despliegue", "Docker Compose (postgres, backend, db-seed, tls-proxy opcional)", "Montar el ambiente completo con un comando.", "Infraestructura", "Docker, Caddy 2", "ARQ-005, ARQ-009", EQUIPO, "-"),
    ("ARQ-012", "Calidad", "CI GitHub Actions", "Compilar, probar, empaquetar, verificar OpenAPI y construir imagen.", "Infraestructura", "GitHub Actions, Maven Wrapper, JUnit 5, Testcontainers", "-", EQUIPO, "-"),
])

# ---------------------------------------------------------------- 9. Trazabilidad (I..K son formulas)
TRAZ = [
    ("RF-001", "Registrar cuenta", "HU-001", "CU-001", "MK-001, MK-002", "ARQ-001, ARQ-006, ARQ-007", "CP-002", ""),
    ("RF-002", "Iniciar y cerrar sesion", "HU-002", "CU-002", "MK-001", "ARQ-004, ARQ-006", "CP-003", ""),
    ("RF-003", "Gestionar ingresos", "HU-003", "CU-003", "MK-004", "ARQ-002, ARQ-007", "CP-005", ""),
    ("RF-004", "Gestionar gastos", "HU-004", "CU-003", "MK-004", "ARQ-002, ARQ-007", "CP-004", ""),
    ("RF-005", "Buscar transacciones", "HU-004", "CU-003", "MK-004", "ARQ-001, ARQ-002", "CP-014", ""),
    ("RF-006", "Crear categorias", "HU-015", "CU-011", "MK-010", "ARQ-007", "CP-015", ""),
    ("RF-007", "Archivar y restaurar categorias", "HU-015", "CU-011", "MK-010", "ARQ-007", "CP-015", ""),
    ("RF-008", "Dashboard con indicadores reales", "HU-008", "CU-003", "MK-003", "ARQ-001, ARQ-007", "CP-006", ""),
    ("RF-009", "Graficos de distribucion y flujo", "HU-008", "CU-003", "MK-003", "ARQ-001", "CP-016", ""),
    ("RF-010", "Presupuestos mensuales", "HU-005", "CU-004", "MK-005", "ARQ-007", "CP-007", ""),
    ("RF-011", "Alertas de presupuesto", "HU-005", "CU-004", "MK-005", "ARQ-007", "CP-017", ""),
    ("RF-012", "Ahorro automatico", "HU-006", "CU-005", "MK-005, MK-006", "ARQ-007", "CP-006, CP-011", ""),
    ("RF-013", "Metas de ahorro", "HU-007", "CU-005", "MK-006", "ARQ-007", "CP-018", ""),
    ("RF-014", "Workspaces", "HU-009", "CU-006", "MK-007", "ARQ-004, ARQ-007", "CP-008", ""),
    ("RF-015", "Miembros, roles e invitaciones", "HU-009", "CU-006", "MK-007", "ARQ-007", "CP-004, CP-012", ""),
    ("RF-016", "Gastos compartidos", "HU-010", "CU-007", "MK-007", "ARQ-007", "CP-012", ""),
    ("RF-017", "Balances y liquidaciones", "HU-010, HU-011", "CU-007, CU-008", "MK-007", "ARQ-007", "CP-009", ""),
    ("RF-018", "Movimientos recurrentes", "HU-012", "CU-010", "MK-011", "ARQ-007", "CP-012", ""),
    ("RF-019", "Notificaciones", "HU-013", "CU-010", "MK-003", "ARQ-007", "CP-017", ""),
    ("RF-020", "Reportes por periodo", "HU-014", "CU-009", "MK-009", "ARQ-007", "CP-012", ""),
    ("RF-021", "Exportar reportes", "HU-014", "CU-009", "MK-009", "ARQ-007", "CP-019", ""),
    ("RF-022", "Perfil y preferencias", "HU-015", "CU-011", "MK-010", "ARQ-007, ARQ-010", "CP-012", ""),
    ("RF-023", "Exportar y eliminar cuenta", "HU-016", "CU-012", "MK-010", "ARQ-007", "CP-012", ""),
    ("RF-024", "Respaldo y restauracion local", "HU-017", "CU-013", "MK-010", "ARQ-003", "CP-013", ""),
    ("RF-025", "Simulador financiero", "HU-018", "CU-014", "MK-011", "ARQ-002", "CP-013", ""),
    ("RF-026", "Facturas y adjuntos", "HU-019", "CU-015", "MK-008", "ARQ-007, ARQ-010", "CP-010", ""),
    ("RF-027", "Preparacion tributaria DIAN", "HU-020", "CU-015", "MK-008", "ARQ-007", "CP-012", ""),
]

# ---------------------------------------------------------------- 10. Plan de pruebas
fill_card(wb["10. Plan de Pruebas"], [
    "Verificar que FinanzasApp (cliente Swing + backend REST) cumple los requisitos funcionales y no funcionales y que otro equipo puede montar el ambiente y probarlo.",
    "Autenticacion, transacciones, presupuestos, ahorro y metas, hogar compartido, facturas, reportes, configuracion y montaje del ambiente con Docker.",
    "Pruebas de carga a gran escala, SMTP real, despliegue en nube y pruebas en macOS/Linux de escritorio.",
    "Unitaria, integracion (API + PostgreSQL), sistema y aceptacion (testing cruzado).",
    "Funcional, regresion, seguridad (permisos por rol, credenciales) y humo de montaje.",
    "Pruebas automatizadas JUnit 5 en cliente y backend ejecutadas en CI; pruebas de humo de API con curl sobre Docker Compose; pruebas manuales de aceptacion con los usuarios de prueba.",
    "Windows 11 + Docker Desktop 29 (Compose v5); PostgreSQL 16 (imagen postgres:16-alpine); backend Java 25 (eclipse-temurin:25); cliente Java 25.",
    "database/datos_prueba.sql: 3 usuarios (OWNER, MEMBER, VIEWER, clave Prueba2026*), hogar 'Hogar Gomez', 13 transacciones, 4 presupuestos, 2 metas, 1 recurrencia, 1 gasto compartido y 1 factura.",
    "JUnit 5, Mockito, Testcontainers 2, Maven Wrapper, Docker Compose, GitHub Actions y scripts/prueba-humo-api.ps1 (prueba de humo y aceptacion automatizada de la API).",
    "Ambiente montado con 'docker compose up -d' y backend en estado healthy; datos de prueba cargados.",
    "100% de casos criticos aprobados y cero defectos de severidad Critica o Mayor abiertos.",
    "El ambiente no arranca o mas del 30% de los casos fallan por el mismo bloqueo.",
    "Juan Jose Roman Bedoya: lider de pruebas, tester y desarrollador (proyecto individual).",
    "Diferencias de JDK (se requiere Java 25); Docker no disponible en el equipo del evaluador.",
    "Casos de prueba ejecutados (hoja 11), registro de defectos (hoja 12) y metricas (hoja 13).",
])

# ---------------------------------------------------------------- 11. Casos de prueba
CP = [
    ("CP-001", "Montaje del ambiente desde cero", "Humo", "Docker instalado; volumenes eliminados (docker compose down -v).", "Repositorio en la rama de entrega", "1. docker compose up -d --build\n2. docker compose ps\n3. curl http://localhost:8080/health/readiness", "postgres y backend 'healthy'; db-seed termina con codigo 0; readiness responde status UP y database UP.", "RF-002", "CU-002", "Aprobada", "postgres y backend healthy, db-seed Exited (0), Flyway aplica V1..V9.", "DEF-001, DEF-002"),
    ("CP-002", "Registro con correo duplicado", "Funcional", "Datos de prueba cargados.", "email ana.duena@finanzasapp.test, password Prueba2026*", "1. POST /api/auth/register con el correo existente.", "HTTP 409 'Ya existe una cuenta con ese correo'.", "RF-001", "CU-001", "No ejecutada", "", ""),
    ("CP-003", "Inicio de sesion valido e invalido", "Funcional", "Datos de prueba cargados.", "ana.duena@finanzasapp.test / Prueba2026* y / mala12345", "1. POST /api/auth/login con la clave correcta.\n2. Repetir con clave incorrecta.", "1: HTTP 200 con accessToken. 2: HTTP 401.", "RF-002", "CU-002", "Aprobada", "200 con accessToken; 401 con clave incorrecta. Los 3 usuarios de prueba inician sesion.", ""),
    ("CP-004", "VIEWER no puede registrar gastos y MEMBER si", "Seguridad", "Datos de prueba cargados.", "laura.lectora y carlos.miembro; gasto 1.000 en Alimentacion del Hogar Gomez", "1. Login como Laura (VIEWER) y POST /transactions.\n2. Login como Carlos (MEMBER) y POST /transactions.", "1: HTTP 403 FORBIDDEN. 2: HTTP 201 y el gasto queda creado.", "RF-004", "CU-003", "Aprobada", "Laura: 403 FORBIDDEN. Carlos: 201 Created.", ""),
    ("CP-005", "Listar transacciones del hogar", "Integracion", "Login como Ana.", "Workspace 20000000-...-000000000010", "1. GET /api/workspaces/{hogar}/transactions", "Lista con las 11 transacciones del hogar ordenadas por fecha.", "RF-003", "CU-003", "Aprobada", "Lista devuelta con las transacciones del hogar (Salario Ana del mes actual primero).", ""),
    ("CP-006", "Resumen de ahorro coherente con ingresos", "Integracion", "Login como Ana.", "Ahorro automatico 20%", "1. GET /api/workspaces/{hogar}/savings/summary", "ingresos 10.200.000; gastos 4.785.000; ahorroTotal 2.040.000; tasaAhorro 20.", "RF-008", "CU-005", "Aprobada", "ingresos 10200000.00, gastos 4785000.00, ahorroTotal 2040000.00, tasaAhorro 20.00.", ""),
    ("CP-007", "Consultar presupuestos del mes", "Integracion", "Login como Ana.", "Mes actual", "1. GET /api/workspaces/{hogar}/budgets", "4 presupuestos del mes actual (Alimentacion 800.000, Transporte 250.000, Entretenimiento 100.000, Vivienda 1.500.000).", "RF-010", "CU-004", "Aprobada", "Se listan los 4 presupuestos con periodMonth del mes actual.", ""),
    ("CP-008", "Workspaces del usuario", "Integracion", "Login como Ana.", "-", "1. GET /api/workspaces", "'Mi espacio' (PERSONAL) y 'Hogar Gomez' (HOUSEHOLD), ambos con rol OWNER.", "RF-014", "CU-006", "Aprobada", "Devuelve los 2 workspaces con rol OWNER.", ""),
    ("CP-009", "Balances del gasto compartido", "Integracion", "Login como Ana.", "Gasto compartido 300.000 dividido en partes iguales", "1. GET /api/workspaces/{hogar}/shared-expenses/balances", "Ana netBalance +150.000; Carlos -150.000; Laura 0.", "RF-017", "CU-007", "Aprobada", "Ana owed 150000; Carlos owes 150000; Laura 0.", ""),
    ("CP-010", "Listar facturas", "Integracion", "Login como Ana.", "-", "1. GET /api/workspaces/{hogar}/invoices", "HTTP 200 con la factura SPD-2026-000123.", "RF-026", "CU-015", "Aprobada", "HTTP 200.", ""),
    ("CP-011", "Suite automatizada del backend", "Regresion", "JDK 25 (o contenedor eclipse-temurin:25-jdk).", "-", "1. ./mvnw -f finanzas-backend/pom.xml test", "Todas las pruebas pasan (las de Testcontainers se omiten si no hay Docker).", "RF-012", "CU-005", "{BACKEND_RESULT}", "{BACKEND_DETAIL}", ""),
    ("CP-012", "Suite automatizada del cliente", "Regresion", "JDK 25.", "-", "1. ./mvnw -f pom.xml test", "Todas las pruebas pasan.", "RF-025", "CU-014", "{CLIENT_RESULT}", "{CLIENT_DETAIL}", ""),
    ("CP-013", "Respaldo local y simulador (manual)", "Aceptacion", "Cliente en modo local con FINANZAS_DEMO_MODE=true.", "samuel@finanzasapp.com / 123456", "1. Crear respaldo desde Configuracion.\n2. Abrir simulador y reducir gastos 10%.\n3. Cerrar y verificar datos.", "Se genera el ZIP; el simulador no altera los datos reales.", "RF-024", "CU-013", "No ejecutada", "", ""),
    ("CP-014", "Buscar transacciones por texto (manual)", "Aceptacion", "Login como Ana en modo API.", "Texto 'Arriendo'", "1. Abrir Gastos.\n2. Escribir 'Arriendo' en el buscador.", "Solo aparecen los 2 registros de arriendo.", "RF-005", "CU-003", "No ejecutada", "", ""),
    ("CP-015", "Crear y archivar categoria (manual)", "Aceptacion", "Login como Ana.", "Categoria 'Mascotas' #ff9900", "1. Configuracion > Categorias > Nueva.\n2. Archivarla y restaurarla.", "La categoria aparece, se archiva y se restaura con el mismo ID.", "RF-006", "CU-011", "No ejecutada", "", ""),
    ("CP-016", "Graficos del dashboard (manual)", "Aceptacion", "Login como Ana, hogar activo.", "-", "1. Abrir Inicio.", "La dona muestra Vivienda, Alimentacion, Entretenimiento, etc.; las barras muestran el mes anterior y el actual.", "RF-009", "CU-003", "No ejecutada", "", ""),
    ("CP-017", "Alerta de presupuesto excedido (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Entretenimiento 120.000 / 100.000", "1. Abrir Presupuesto.\n2. Abrir notificaciones.", "Entretenimiento aparece EXCEDIDO (120%) y hay una notificacion.", "RF-011", "CU-004", "No ejecutada", "", ""),
    ("CP-018", "Asignar ahorro a una meta (manual)", "Aceptacion", "Login como Ana.", "Meta 'Vacaciones en familia', 300.000", "1. Abrir Metas.\n2. Agregar aporte de 300.000.", "Avance 10%, faltan 2.700.000.", "RF-013", "CU-005", "No ejecutada", "", ""),
    ("CP-019", "Exportar reporte a Excel (manual)", "Aceptacion", "Login como Ana.", "Rango personalizado: primer y ultimo dia del mes anterior", "1. Abrir Reportes.\n2. Elegir 'Rango personalizado' con las fechas del mes anterior y Aplicar.\n3. Exportar reporte > Excel.", "Se crea el .xlsx con ingresos 5.700.000 y gastos 2.515.000.", "RF-021", "CU-009", "No ejecutada", "", ""),
    ("CP-020", "Archivar y restaurar categoria con historial (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Categoria Entretenimiento", "1. Configuracion > Categorias.\n2. Archivar 'Entretenimiento'.\n3. Abrir Gastos.\n4. Restaurarla.", "Los gastos 'Cine y comida' y 'Concierto' siguen mostrando la categoria; tras restaurar conserva su ID.", "RF-007", "CU-011", "No ejecutada", "", ""),
    ("CP-021", "Invitar miembro y aceptar invitacion (manual)", "Aceptacion", "Login como Ana; existe un segundo usuario registrado.", "Correo del nuevo usuario, rol MEMBER", "1. Hogar > Miembros > Invitar.\n2. Iniciar sesion como el invitado y aceptar.\n3. Como Laura (VIEWER) intentar invitar.", "El invitado aparece como MEMBER; Laura recibe error de permisos.", "RF-015", "CU-006", "No ejecutada", "", ""),
    ("CP-022", "Gasto compartido con division por monto invalida (manual)", "Funcional", "Login como Carlos, hogar activo.", "Total 100.000; partes 60.000 + 30.000", "1. Hogar > Nuevo gasto compartido.\n2. Division por monto con partes que no suman el total.", "El sistema rechaza el gasto indicando que las partes no suman el total.", "RF-016", "CU-007", "No ejecutada", "", ""),
    ("CP-023", "Ejecutar recurrencia de arriendo (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Recurrencia 'Arriendo apartamento'", "1. Abrir recurrencias.\n2. Ejecutar 'Arriendo apartamento'.", "Se crea un gasto de 1.500.000 y la proxima fecha avanza un mes.", "RF-018", "CU-010", "No ejecutada", "", ""),
    ("CP-024", "Marcar notificacion como leida (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Notificacion 'Presupuesto excedido'", "1. Abrir notificaciones.\n2. Marcar como leida.", "La notificacion deja de contarse como no leida.", "RF-019", "CU-010", "No ejecutada", "", ""),
    ("CP-025", "Reporte del mes anterior (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Rango personalizado: primer y ultimo dia del mes anterior", "1. Abrir Reportes.\n2. Elegir 'Rango personalizado' con las fechas del mes anterior y Aplicar.", "Ingresos 5.700.000 y gastos 2.515.000.", "RF-020", "CU-009", "No ejecutada", "", ""),
    ("CP-026", "Cambiar tema y conservarlo (manual)", "Aceptacion", "Login como Carlos.", "Tema Oscuro", "1. Configuracion > Apariencia > Oscuro > Guardar.\n2. Cerrar sesion e iniciar de nuevo.", "La interfaz se muestra en tema oscuro tras volver a iniciar sesion.", "RF-022", "CU-011", "No ejecutada", "", ""),
    ("CP-027", "Exportar y eliminar cuenta (manual)", "Aceptacion", "Usuario nuevo sin hogares compartidos.", "Correo y contrasena del usuario", "1. Configuracion > Exportar datos.\n2. Eliminar cuenta confirmando correo y contrasena.\n3. Intentar iniciar sesion de nuevo.", "Se descarga un JSON; la cuenta se elimina y el login posterior responde 'Credenciales invalidas'.", "RF-023", "CU-012", "No ejecutada", "", ""),
    ("CP-028", "Asistente de preparacion DIAN (manual)", "Aceptacion", "Login como Ana, hogar activo.", "Anio actual, valor UVT del anio", "1. Facturas > Preparacion DIAN.\n2. Ingresar el valor UVT.", "Se muestran los topes en UVT, los totales del usuario y la advertencia de que no es asesoria certificada.", "RF-027", "CU-015", "No ejecutada", "", ""),
]
CP += [
    ("CP-029", "Bloqueo tras 5 intentos fallidos", "Seguridad", "Ambiente montado.", "Correo cualquiera, clave incorrecta", "1. POST /api/auth/login 5 veces con clave incorrecta.\n2. Repetir una sexta vez.", "El sexto intento responde HTTP 429 (bloqueo de 5 minutos).", "RF-002", "CU-002", "Aprobada", "", ""),
    ("CP-031", "Hogar muestra cada miembro una vez con su deuda real", "Aceptacion", "Login como Ana en modo API, hogar activo.", "Gasto compartido de 300.000 dividido en partes iguales", "1. Abrir Finanzas del Hogar.\n2. Revisar 'Miembros del hogar' y 'Resumen de deudas'.", "3 miembros (Ana, Carlos, Laura) sin duplicados; Ana: le deben 150.000; Carlos: debe 150.000; Laura: 0.", "RF-017", "CU-007", "Aprobada", "", ""),
    ("CP-032", "Facturas se cargan al abrir la seccion", "Aceptacion", "Login como Ana en modo API, hogar activo.", "Factura SPD-2026-000123", "1. Abrir Facturas.", "La tabla muestra SPD-2026-000123 con subtotal $268.907,56, IVA $51.092,44 y total $320.000.", "RF-026", "CU-015", "Aprobada", "", ""),
    ("CP-030", "Retiro mayor al ahorro libre", "Funcional", "Login como Ana.", "Retiro de 99.000.000", "1. POST /api/workspaces/{hogar}/savings/withdrawals", "HTTP 409; el saldo de ahorro no cambia.", "RF-012", "CU-005", "Aprobada", "", ""),
]
# Resultados de scripts/prueba-humo-api.ps1 sobre datos recien sembrados (25/25 aprobados)
HUMO = {
    "CP-001": "status UP, database UP; db-seed Exited (0).",
    "CP-002": "Registro nuevo HTTP 201 con 'Mi espacio' y 14 categorias; correo duplicado HTTP 409.",
    "CP-003": "Los 3 usuarios de prueba inician sesion (200); clave incorrecta 401.",
    "CP-004": "VIEWER 403 FORBIDDEN; MEMBER 201 Created.",
    "CP-005": "11 transacciones del hogar.",
    "CP-006": "ingresos 10.200.000, gastos 4.785.000, ahorro 2.040.000, tasa 20%.",
    "CP-007": "4 presupuestos del mes actual.",
    "CP-008": "'Mi espacio' y 'Hogar Gomez' con rol OWNER.",
    "CP-009": "Carlos debia 150.000; tras liquidar los balances quedan en 0 y el gasto sigue en el historial.",
    "CP-010": "Aparece SPD-2026-000123; factura con total 0 rechazada (422).",
    "CP-015": "Categoria creada; duplicado 409; color invalido 422 (verificado por API).",
    "CP-017": "Notificacion BUDGET_THRESHOLD sin leer (verificado por API; la tarjeta EXCEDIDO se revisa en la UI).",
    "CP-018": "Meta con 300.000 de 3.000.000 (10%); asignar mas que el ahorro libre: 409 (verificado por API).",
    "CP-019": "CSV 716 B, PDF 1.965 B y XLSX 3.392 B generados para el mes anterior (verificado por API).",
    "CP-020": "Archivada sin perder sus transacciones; restaurada con el mismo ID (verificado por API).",
    "CP-021": "VIEWER no puede invitar (403); el invitado ve la invitacion, la acepta y queda MEMBER.",
    "CP-022": "Rechazado con 422 (antes respondia 400, ver DEF-004).",
    "CP-023": "Gasto de arriendo creado; proxima fecha 2026-12-01 -> 2027-01-01.",
    "CP-024": "Notificacion marcada como leida; ya no aparece entre las no leidas.",
    "CP-025": "Ingresos 5.700.000 y gastos 2.515.000; rango invertido rechazado (400).",
    "CP-026": "Tema DARK persiste en una nueva sesion (verificado por API).",
    "CP-027": "Export 200; clave incorrecta 401; cuenta eliminada y login posterior 401; OWNER del hogar 409 (antes 500, ver DEF-003).",
    "CP-028": "Con UVT de prueba 50.000 el tope de ingresos es 70.000.000 (verificado por API).",
    "CP-029": "Sexto intento: 429.",
    "CP-030": "Rechazado con 409.",
    "CP-013": "Respaldo y restauracion verificados con pruebas automatizadas (DataManagerRegressionTest): restaura el estado previo y rechaza ZIP sin estado, con rutas '../' o con imagenes > 5 MB sin tocar los datos actuales. Simulador: whatIfSimulationDoesNotMutateRealDataAndProjectsGoalDate.",
    "CP-014": "Buscar 'Arriendo' en Gastos deja 2 filas (01/11 y 02/10, $1.500.000 c/u) (docs/capturas/CP-014_busqueda_gastos.png).",
    "CP-016": "Dona con Vivienda 63%, Alimentacion 22%, Servicios 7%, Entretenimiento 4%, Transporte 4%; barras de octubre y noviembre (docs/capturas/pantalla_01_inicio.png).",
    "CP-031": "3 miembros unicos; Ana le deben 150.000, Carlos debe 150.000, Laura 0 (docs/capturas/pantalla_06_hogar.png). Antes aparecian duplicados, ver DEF-006.",
    "CP-032": "SPD-2026-000123 visible con montos $268.907,56 / $51.092,44 / $320.000 (docs/capturas/pantalla_07_facturas.png). Antes la tabla quedaba vacia, ver DEF-007.",
}
DEF_CP = {"CP-022": "DEF-004", "CP-027": "DEF-003", "CP-011": "DEF-005", "CP-031": "DEF-006", "CP-032": "DEF-007, DEF-008"}
CP = [c[:9] + (("Aprobada", HUMO[c[0]]) if c[0] in HUMO else c[9:11]) + (DEF_CP.get(c[0], c[11]),) for c in CP]
CP.sort(key=lambda c: c[0])
cp_rows = [(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], EQUIPO, c[9], HOY if c[9] != "No ejecutada" else None, c[10], c[11]) for c in CP]
fill_table(wb["11. Casos de Prueba"], cp_rows)
cps_by_rf = {}
for c in CP:
    cps_by_rf.setdefault(c[7], []).append(c[0])
TRAZ = [t[:6] + (", ".join(cps_by_rf.get(t[0], [])) or "Pendiente",) + t[7:] for t in TRAZ]
DEFECTOS_RF = {"RF-001": "DEF-002", "RF-002": "DEF-001", "RF-012": "DEF-005", "RF-016": "DEF-004", "RF-017": "DEF-006",
               "RF-020": "DEF-008", "RF-023": "DEF-003", "RF-026": "DEF-007, DEF-008"}
TRAZ = [t[:7] + (DEFECTOS_RF.get(t[0], ""),) for t in TRAZ]
fill_table(wb["9. Trazabilidad"], TRAZ, skip_cols=(9, 10, 11))

# ---------------------------------------------------------------- 12. Defectos (O es formula)
fill_table(wb["12. Defectos"], [
    ("DEF-001", "Flyway no ejecuta las migraciones al arrancar el backend", "Con Spring Boot 4.1 la dependencia flyway-core ya no activa la autoconfiguracion; el backend arranca sin crear tablas y falla con 'missing table [account_tokens]'.",
     "1. docker compose down -v\n2. docker compose up -d\n3. docker compose logs backend: 'Schema validation: missing table'.", "Critica", "Alta", "Backend / Base de datos", "CP-001", "RF-002", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "Se reemplazo flyway-core por spring-boot-starter-flyway en finanzas-backend/pom.xml."),
    ("DEF-002", "Tipo de columna users.moneda no coincide con la entidad", "La migracion V1 crea moneda como CHAR(3) y la entidad JPA espera VARCHAR(3); Hibernate (ddl-auto=validate) impide el arranque.",
     "1. Con DEF-001 corregido, levantar el backend sobre una base vacia.\n2. Log: 'wrong column type encountered in column [moneda]'.", "Critica", "Alta", "Backend / Base de datos", "CP-001", "RF-001", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "Nueva migracion V9__users_moneda_varchar.sql (ALTER COLUMN moneda TYPE VARCHAR(3))."),
    ("DEF-003", "Eliminar cuenta responde HTTP 500", "Al eliminar cualquier cuenta el servicio borra el workspace personal (la FK elimina la membresia en cascada) y luego intenta borrar esa misma membresia: Hibernate falla con 'expected row count 1 but was 0'. Ademas el 500 no quedaba registrado en el log.",
     "1. Registrar un usuario nuevo.\n2. DELETE /api/users/me con correo y clave correctos.\n3. Respuesta HTTP 500 'Error interno no esperado'.", "Mayor", "Alta", "Backend / Cuenta", "CP-027", "RF-023", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "UserAccountService omite las membresias de workspaces ya borrados; prueba de regresion en UserAccountServiceTest; ApiExceptionHandler registra los errores 500 con su correlationId."),
    ("DEF-004", "Division de gasto compartido invalida responde 400 en lugar de 422", "Cuando las partes no suman el total la API respondia 400, contrario a la especificacion (RF-016, CU-007) y al resto de errores de validacion (422).",
     "1. POST /shared-expenses con CUSTOM_AMOUNT: total 100.000 y partes 60.000 + 30.000.\n2. Respuesta HTTP 400.", "Menor", "Media", "Hogar compartido", "CP-022", "RF-016", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "SharedExpenseSplitCalculator responde 422 (UNPROCESSABLE_ENTITY)."),
    ("DEF-005", "La prueba de migraciones con PostgreSQL se omitia en silencio", "Testcontainers 1.21.x usa la API 1.32 de Docker y Docker 29 exige 1.40 o superior; la prueba quedaba como omitida y no verificaba las migraciones reales.",
     "1. ./mvnw -f finanzas-backend/pom.xml test con Docker 29.\n2. FlywayPostgresMigrationTest: 'disabledWithoutDocker is true and Docker is not available'.", "Moderada", "Media", "Calidad / CI", "CP-011", "RF-012", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "Migracion a Testcontainers 2 (version gestionada por Spring Boot 4.1); la prueba ahora corre y valida V1..V9."),
    ("DEF-006", "Miembros duplicados y deudas en cero en Finanzas del Hogar", "En modo API cada miembro aparecia dos veces: una con 'Nombre <correo>' y saldo 0, y otra solo con el nombre y la deuda real. Los miembros usaban etiqueta unica pero pagador y participantes del gasto usaban el nombre visible.",
     "1. Login como Ana (API) y seleccionar Hogar Gomez.\n2. Abrir Finanzas del Hogar.\n3. Ana y Carlos aparecen dos veces con saldos distintos.", "Mayor", "Alta", "Cliente / Hogar", "CP-031", "RF-017", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "DataManager.syncBackendHousehold resuelve pagador y participantes por userId con la misma etiqueta de miembrosHogar."),
    ("DEF-007", "La seccion Facturas aparece vacia al abrirla", "FacturasPanel solo cargaba datos cuando DataManager notificaba un cambio; al abrir la seccion la tabla quedaba vacia aunque existieran facturas.",
     "1. Login como Ana (API).\n2. Abrir Facturas.\n3. La tabla esta vacia aunque la API devuelve SPD-2026-000123.", "Moderada", "Alta", "Cliente / Facturas", "CP-032", "RF-026", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "FacturasPanel recarga las facturas cada vez que la seccion se muestra (HierarchyListener)."),
    ("DEF-008", "Detalles de presentacion en Facturas y Reportes", "Facturas mostraba montos sin separador de miles ($268907.56) y el anio como '2,026'; en Reportes los filtros se montaban sobre el titulo a 1280 px y la tarjeta 'Tasa de ahorro' mostraba el margen balance/ingresos (49,56%) con el mismo nombre que la tasa del libro de ahorro (20%).",
     "1. Abrir Facturas y observar montos y anio.\n2. Abrir Reportes a 1280x800.", "Menor", "Media", "Cliente / UI", "CP-032", "RF-026", EQUIPO, HOY, EQUIPO, "Cerrado", HOY, None,
     "Formato monetario con la configuracion regional, editor de anio sin separador, filtros en una segunda fila y tarjeta renombrada a 'Margen de ahorro (Balance / ingresos)'."),
], skip_cols=(15,))

# ---------------------------------------------------------------- 14-16: limpiar filas de ejemplo
fill_table(wb["14. Control de Cambios"], [
    ("CC-001", HOY, "Equipo de testing cruzado (INT-006)", "Agregar script SQL de creacion con datos de prueba y servicio de carga automatica en Docker Compose.",
     "Permitir que otro equipo monte el ambiente y pruebe con usuarios conocidos.", "README, docker-compose.yml, database/", "Documentacion y despliegue; sin cambio funcional", 1, "Bajo", "Implementado", EQUIPO, HOY),
    ("CC-002", HOY, "Docente (INT-005)", "Incluir RF-027 Preparacion tributaria DIAN en el alcance documentado.", "La funcionalidad ya estaba implementada (V8, TaxPreparationService).", "RF-027, HU-020, CU-015", "Nuevo requisito documentado", 0, "Bajo", "Aprobado", EQUIPO, HOY),
])
fill_table(wb["15. Riesgos"], [
    ("RSG-001", "El equipo evaluador no tiene Java 25 instalado", "Tecnico", "Alta", "Alto", None, None, "Backend en Docker (no requiere Java local); instalador Windows con Java 25 incluido.", "Ejecutar las pruebas dentro del contenedor eclipse-temurin:25-jdk.", EQUIPO, "En progreso"),
    ("RSG-002", "Docker no disponible en el equipo evaluador", "Tecnico", "Media", "Alto", None, None, "Documentar instalacion manual de PostgreSQL 16 con crear_bd.sql.", "Usar el modo local del cliente con FINANZAS_DEMO_MODE=true.", EQUIPO, "En progreso"),
    ("RSG-003", "Diferencias entre esquema SQL y entidades JPA", "Tecnico", "Baja", "Alto", None, None, "Prueba de montaje desde volumen limpio antes de cada entrega (CP-001).", "Agregar una migracion correctiva Flyway.", EQUIPO, "Terminada"),
], skip_cols=(6, 7))
fill_table(wb["16. Cronograma"], [
    ("ACT-001", "Requisitos", "Levantamiento de requisitos, interesados e historias de usuario", "Hojas 1 a 5 de este formato", EQUIPO, datetime(2026, 8, 3), datetime(2026, 8, 21), None, 1, "Terminada", "-"),
    ("ACT-002", "Analisis", "Casos de uso y mockups", "Hojas 6 y 7; docs/USE_CASES.md", EQUIPO, datetime(2026, 8, 24), datetime(2026, 9, 4), None, 1, "Terminada", "-"),
    ("ACT-003", "Diseno", "Arquitectura cliente/backend y modelo de datos", "docs/ARQUITECTURA_Y_AMBIENTE.md", EQUIPO, datetime(2026, 9, 7), datetime(2026, 9, 18), None, 1, "Terminada", "-"),
    ("ACT-004", "Construccion", "Implementacion de cliente Swing y backend REST", "Codigo fuente en GitHub", EQUIPO, datetime(2026, 9, 21), datetime(2026, 10, 30), None, 1, "Terminada", "-"),
    ("ACT-005", "Pruebas", "Pruebas automatizadas, humo de API y registro de defectos", "Hojas 11 y 12", EQUIPO, datetime(2026, 10, 19), datetime(2026, 11, 1), None, 0.8, "En progreso", "Pendientes las pruebas manuales de aceptacion."),
    ("ACT-006", "Despliegue", "Empaquetado, Docker Compose, scripts SQL y README", "README.md, database/, dist/", EQUIPO, datetime(2026, 10, 26), datetime(2026, 11, 1), None, 1, "Terminada", "-"),
], skip_cols=(8,))

# Resultado de las suites automatizadas (se reemplaza desde la linea de comandos)
results = dict(arg.split("=", 1) for arg in sys.argv[3:])
ws = wb["11. Casos de Prueba"]
for row in ws.iter_rows(min_row=5, max_row=45):
    for cell in row:
        if isinstance(cell.value, str) and cell.value.startswith("{") and cell.value.endswith("}"):
            cell.value = results.get(cell.value[1:-1], "No ejecutada")
    if row[10].value == "No ejecutada":
        row[11].value = None

wb.active = 0
wb.save(OUTPUT)
print("OK", OUTPUT)
