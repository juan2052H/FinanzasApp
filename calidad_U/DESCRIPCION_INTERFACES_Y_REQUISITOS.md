# FinanzasApp - Descripcion de interfaces, requisitos y casos de uso

**Carpeta de entrega:** `calidad_U`  
**Fecha de elaboracion:** 3 de septiembre de 2026  
**Producto:** FinanzasApp  
**Tipo de aplicacion:** Aplicacion de escritorio Java Swing para finanzas personales, familiares y de pequenos negocios.

## 1. Descripcion general del proyecto

FinanzasApp permite registrar, organizar y analizar los movimientos financieros de una persona o de un hogar. La aplicacion se puede utilizar en dos modalidades:

- **Modo local:** funciona sin servidor y guarda el estado en el equipo del usuario, en `~/.finanzasapp` o en la ruta definida por `FINANZAS_DATA_DIR`.
- **Modo API:** el cliente de escritorio se conecta a un backend Spring Boot y PostgreSQL. En este modo el backend es la fuente principal de verdad y permite sincronizar datos, administrar workspaces, invitar miembros y trabajar desde varios equipos.

El objetivo es ofrecer una herramienta clara para responder preguntas como:

- Cuanto dinero tengo actualmente.
- Cuanto ingrese y cuanto gaste durante un periodo.
- En que categorias se concentra el gasto.
- Que parte del presupuesto ya utilice y cuanto queda disponible.
- Cuanto llevo ahorrado y cuanto me falta para cada meta.
- Quien pago un gasto compartido y quien debe dinero.
- Que movimientos recurrentes se aproximan.
- Como cambia mi situacion financiera si ahorro mas o reduzco gastos.
- Como exportar, respaldar y recuperar la informacion.

## 2. Alcance del producto

### Incluido

1. Registro e inicio de sesion local o mediante backend.
2. Registro, edicion, busqueda y eliminacion de ingresos y gastos.
3. Categorias de ingreso, gasto y hogar, con icono, color, filtros y archivado.
4. Dashboard con saldo, ingresos, gastos, ahorro, graficos y alertas.
5. Presupuestos mensuales por categoria.
6. Metas de ahorro, aportes y seguimiento de progreso.
7. Ahorro recomendado o automatico con porcentaje, modo o monto fijo.
8. Gastos compartidos, miembros, invitaciones, divisiones y liquidaciones.
9. Transacciones recurrentes y proximos movimientos.
10. Notificaciones sobre presupuestos, metas, gasto inusual y recurrencias.
11. Reportes por periodo y exportacion a CSV, PDF y Excel.
12. Gestion de perfil, avatar, moneda, region, zona horaria, tema y seguridad.
13. Facturas o comprobantes en el modo backend, incluyendo adjuntos PDF o imagen.
14. Simulador financiero de solo lectura.
15. Respaldo y restauracion del estado local.

### Fuera de alcance o parcial

- La migracion completa de datos locales al backend requiere un flujo de mantenimiento mas visible.
- El scheduler automatico de recurrencias puede requerir configuracion adicional del backend.
- La integracion de un proveedor SMTP real para correos depende del despliegue.
- La preparacion DIAN es una ayuda de organizacion y no constituye certificacion tributaria.
- Las pruebas visuales end-to-end con dos usuarios y PostgreSQL requieren un entorno con Docker.

## 3. Descripcion visual y de interaccion

### 3.1 Lenguaje visual

La interfaz es una aplicacion desktop de estilo administrativo y orientado a lectura rapida:

- Ventana principal de aproximadamente 1280 x 800 pixeles, con minimo de 960 x 640.
- Barra superior o encabezado de aplicacion.
- Barra lateral izquierda para la navegacion principal.
- Area central con paneles intercambiables mediante `CardLayout`.
- Fondo general gris azulado claro en tema claro y fondo oscuro azulado en tema oscuro.
- Tarjetas blancas o de alto contraste para agrupar informacion.
- Tipografia Segoe UI, titulos compactos, subtitulos explicativos y valores numericos destacados.
- Azul para acciones principales, verde para ingresos o ahorro, rojo para gastos y advertencias, amarillo/naranja para atencion.
- Bordes suaves, separadores discretos, tablas sin cuadricula pesada y barras de progreso.

La interfaz debe mantener una jerarquia estable: primero el titulo y el contexto, luego los indicadores principales, despues el detalle y finalmente las acciones.

### 3.2 Componentes comunes

- **Encabezado de pagina:** titulo del modulo, subtitulo y acciones principales a la derecha.
- **Boton primario:** crea, guarda, aplica o confirma una accion importante.
- **Boton secundario:** filtra, actualiza, edita o cancela.
- **Campo de texto:** captura nombres, descripciones, correos, fechas o valores monetarios.
- **Selector desplegable:** selecciona categoria, periodo, moneda, tipo, rol o modo.
- **Tabla:** muestra registros con columnas legibles, altura de fila uniforme y menu de acciones.
- **Tarjeta resumen:** muestra una cifra acompanada de su etiqueta y, cuando aplica, una variacion.
- **Barra de progreso:** representa porcentaje utilizado o avance hacia una meta.
- **Dialogo modal:** concentra formularios cortos y evita perder el contexto de la pantalla.
- **Estado vacio:** explica que aun no hay datos y sugiere la primera accion.
- **Mensaje de validacion:** informa el dato incorrecto sin borrar los demas valores del formulario.
- **Selector de archivos:** se utiliza para exportaciones, importaciones, respaldos y adjuntos.

### 3.3 Navegacion principal

La barra lateral conduce a estas secciones:

1. **Inicio:** resumen financiero.
2. **Ingresos:** registro y consulta de entradas de dinero.
3. **Gastos:** registro y consulta de salidas de dinero.
4. **Presupuesto:** limites mensuales y ahorro recomendado.
5. **Metas:** objetivos de ahorro y aportes.
6. **Hogar:** miembros, gastos compartidos e invitaciones.
7. **Facturas:** comprobantes y adjuntos, principalmente con backend.
8. **Reportes:** analisis por periodo y exportacion.
9. **Configuracion:** perfil, notificaciones, categorias, tema, moneda, seguridad y datos.

La seccion activa debe permanecer identificada visualmente. Al cambiar de modulo, el contenido cambia sin abrir una ventana nueva y conserva el contexto de la sesion.

## 4. Interfaces detalladas

### 4.1 Pantalla de acceso y registro

**Proposito:** autenticar a un usuario existente o crear una cuenta nueva.

**Composicion:**

- Ventana centrada de aproximadamente 460 x 640 pixeles.
- Marca `FinanzasApp` en la parte superior.
- Subtitulo: gestion de finanzas personales y del hogar.
- Vista de inicio de sesion y vista de registro intercambiables.

**Inicio de sesion:**

- Campo `Correo electronico`.
- Campo `Contrasena`.
- Boton `Iniciar sesion`.
- Opcion `Continuar con Google` cuando el backend y el identificador OAuth estan configurados.
- Enlace para registrarse.
- Enlace para solicitar recuperacion de contrasena cuando el backend esta activo.
- Enlace para verificar correo cuando el backend esta activo.
- Mensajes claros para credenciales invalidas, backend no disponible o configuracion incompleta.

**Registro:**

- Nombre completo.
- Correo electronico.
- Contrasena y confirmacion.
- Moneda inicial: COP, USD o EUR.
- Tipo de cuenta: Personal u Hogar.
- Casilla de aceptacion de terminos y condiciones.
- Boton `Crear cuenta`.
- Enlace para regresar al inicio de sesion.

**Validaciones:** correo con formato valido, campos obligatorios completos, contrasenas coincidentes y restricciones de seguridad de la contrasena. Un correo duplicado debe mostrar un error entendible.

### 4.2 Configuracion inicial o onboarding

Se muestra al primer acceso cuando el perfil aun no esta configurado. Es un dialogo modal titulado `Primeros pasos`.

Campos y controles:

- Que se quiere administrar: Personal, Hogar o Negocio.
- Moneda: COP, USD, EUR o MXN.
- Ingreso mensual aproximado, opcional.
- Objetivo principal: ahorrar, reducir gastos, salir de deudas, organizar el hogar o controlar el negocio.
- Categoria para un primer presupuesto, opcional.
- Monto del presupuesto, opcional.
- Boton `Empezar`.

El ingreso aproximado sirve como referencia inicial y no se registra como una transaccion ficticia. Los montos incorrectos producen una alerta y permiten corregir el formulario.

### 4.3 Dashboard o pantalla Inicio

**Proposito:** ofrecer una lectura inmediata de la salud financiera.

**Encabezado:** titulo `Inicio`, subtitulo `Resumen de tu situacion financiera` y fecha actual.

**Tarjetas superiores:**

- `Saldo actual` y variacion.
- `Ingresos del mes` y variacion.
- `Gastos del mes` y variacion.
- `Ahorro del mes` y tasa de ahorro.

**Bloque de analisis:**

- Grafico de dona para la distribucion de gastos por categoria.
- Leyenda con categoria, color y porcentaje.
- Grafico de barras de ingresos contra gastos de los ultimos seis meses.
- Tarjeta de metas de ahorro con hasta dos metas visibles, porcentaje y monto faltante.

**Bloques complementarios:**

- Salud financiera explicable, con indicadores y razones.
- Notificaciones relevantes.
- Proximos movimientos recurrentes.
- Acciones rapidas: nuevo ingreso, nuevo gasto, nueva meta, nuevo presupuesto, gasto compartido, recurrencia, reporte, busqueda y simulador.

Cuando no hay datos se muestran estados vacios, por ejemplo: registrar el primer ingreso o gasto, crear una meta o configurar un presupuesto. Los graficos nunca deben inventar datos.

### 4.4 Pantallas de Ingresos y Gastos

Ambas pantallas comparten estructura y se diferencian por el tipo de movimiento y el color de sus acciones.

**Elementos visibles:**

- Titulo `Ingresos` o `Gastos`.
- Subtitulo descriptivo.
- Boton `Nuevo ingreso` en verde o `Nuevo gasto` en rojo.
- Campo de busqueda.
- Total acumulado del tipo seleccionado.
- Tabla con `Tipo`, `Categoria`, `Descripcion`, `Monto`, `Fecha` y `Acciones`.
- Menu contextual de cada fila con `Editar` y `Eliminar`.

**Dialogo de transaccion:**

- Categoria filtrada segun el tipo.
- Descripcion obligatoria.
- Monto mayor que cero.
- Fecha en formato `dd/MM/yyyy`.
- Boton de guardar o guardar cambios.

El parser monetario acepta formatos locales como `1.250.000,50`. Al editar se precargan los datos existentes. Eliminar requiere confirmacion. En modo API la operacion se valida contra el workspace y los cambios se sincronizan.

### 4.5 Pantalla Presupuesto

**Proposito:** controlar limites de gasto por categoria y mes.

**Resumen superior:** presupuesto total, total gastado, disponible y ahorro recomendado.

**Tarjeta por categoria:**

- Nombre y color de la categoria.
- Estado: `OK`, `ATENCION`, `CRITICO` o `EXCEDIDO`.
- Porcentaje utilizado.
- Barra de progreso.
- Gastado, presupuesto y disponible.
- Acciones `Editar` y `Borrar`.

**Acciones:**

- `Nuevo presupuesto` abre un formulario con categoria, monto y periodo.
- `Configurar ahorro` permite activar o desactivar la recomendacion.
- Modos de ahorro: conservador, equilibrado, agresivo, personalizado o monto fijo.

El gasto real se calcula a partir de transacciones, no de valores decorativos. Los umbrales deben generar notificaciones o estados visibles sin ocultar el valor numerico.

### 4.6 Pantalla Ahorro y Metas

**Proposito:** convertir objetivos financieros en avances medibles.

**Resumen:** ahorrado total, objetivo total y cantidad de metas activas.

**Tarjeta de meta:**

- Icono y nombre.
- Porcentaje de avance o estado `Completada`.
- Barra de progreso coloreada.
- Monto ahorrado frente al objetivo.
- Fecha limite y dias restantes, o aviso de vencimiento.
- Monto faltante.
- `Agregar aporte`, `Editar` y eliminar con confirmacion.

**Formulario de meta:** nombre, icono, color, monto objetivo, monto inicial y fecha limite. Un aporte no puede superar las reglas de saldo disponible configuradas. La meta no crea dinero; solamente asigna o representa ahorro existente.

### 4.7 Pantalla Finanzas del Hogar

**Proposito:** administrar gastos compartidos de una familia, pareja, equipo o pequeno negocio.

**Parte superior:**

- Tarjeta de miembros del hogar.
- Lista de balances: quien debe y quien tiene saldo a favor.
- Invitaciones recibidas o enviadas.
- Botones `Invitar` o `Agregar`, `Actualizar`, `Transferir` y `Abandonar` segun permisos.

**Parte inferior:**

- Tabla de gastos compartidos.
- Pagador, participantes, descripcion, monto, fecha y forma de division.
- Acceso a la liquidacion de deudas.

**Nuevo gasto compartido:**

- Categoria y descripcion.
- Monto total.
- Fecha.
- Pagador.
- Participantes.
- Metodo de division: partes iguales, porcentajes o montos personalizados.

La suma de las partes debe coincidir con el total. La interfaz muestra nombres y correos de forma que dos miembros con nombres similares se puedan distinguir. Las operaciones del backend identifican miembros mediante UUID, no por el texto visible.

**Invitaciones y roles:** OWNER y ADMIN pueden administrar el workspace; MEMBER puede operar segun sus permisos; VIEWER consulta sin mutaciones financieras. Transferir la propiedad o abandonar el workspace requiere reglas seguras y confirmacion.

### 4.8 Pantalla Facturas / Comprobantes

Esta pantalla requiere una sesion financiera backend activa.

**Tabla:** numero de factura, proveedor, NIT/RUT, fecha, subtotal, IVA, total y notas.

**Filtros:** anio inicial, anio final y boton `Filtrar`.

**Acciones:** nueva factura, editar, eliminar, adjuntar PDF o imagen y ver/descargar adjunto.

**Formulario:** numero de factura, proveedor, identificacion fiscal, fecha, subtotal, IVA, total, notas e ID de transaccion opcional.

La pantalla debe indicar si esta cargando, cuantas facturas encontro o si no existe conexion backend. Los adjuntos se validan y se almacenan de forma autenticada.

### 4.9 Pantalla Reportes

**Proposito:** analizar informacion por un periodo y compartirla.

**Controles superiores:** periodo predefinido, fecha desde, fecha hasta, `Aplicar`, `Exportar reporte` y `Preparacion DIAN`.

**Periodos:** mes actual, mes anterior, ultimos meses, ano actual, todo el historial y rango personalizado.

**Contenido:**

- Cuatro indicadores estadisticos.
- Graficos comparativos.
- Distribucion por categoria.
- Resumen de transacciones, presupuestos, metas y proximos movimientos.

**Exportaciones:** CSV, PDF y Excel. CSV puede utilizar el backend en modo API; PDF y Excel se generan segun la modalidad disponible. Un rango invalido debe mostrar un mensaje y no reemplazar silenciosamente el reporte correcto.

### 4.10 Pantalla Configuracion

La configuracion se divide en secciones seleccionables:

- **Perfil de usuario:** avatar, nombre, apellido, correo, ciudad y pais; boton `Guardar perfil`.
- **Notificaciones:** preferencias y avisos del sistema.
- **Categorias:** crear categoria con tipo, nombre, icono y color; buscar; filtrar por tipo y estado; editar, archivar y restaurar.
- **Tema, moneda y region:** tema claro u oscuro, moneda, locale, zona horaria y formato monetario.
- **Seguridad:** cambio de contrasena, verificacion de correo y sesiones activas; en backend se pueden revocar sesiones.
- **Exportar datos:** exportacion de cuenta, exportacion financiera, importacion CSV y respaldo/restauracion ZIP.
- **Acerca de:** version, descripcion y enlaces de documentacion.

La eliminacion de cuenta requiere confirmar correo y contrasena, revoca sesiones y anonimiza la cuenta. No debe permitirse que un propietario abandone un workspace compartido sin transferir antes la propiedad.

### 4.11 Busqueda global, acciones rapidas y simulador

La combinacion `Ctrl+K` abre una paleta de acciones. Incluye crear ingresos, gastos, metas, presupuestos y gastos compartidos; abrir facturas y reportes; buscar globalmente; abrir el simulador y preparar informacion DIAN.

La busqueda global muestra resultados agrupados por tipo, titulo y detalle. Al seleccionar un resultado, la aplicacion debe dirigir al modulo correspondiente.

El simulador permite elegir:

- Ahorrar un monto adicional cada mes.
- Reducir gastos en un porcentaje.

Tambien solicita meses a proyectar, ahorro actual y meta objetivo. El resultado presenta la proyeccion sin modificar transacciones, presupuestos ni metas reales.

## 5. Interesados y responsabilidades

| Interesado | Interes en el sistema | Necesidades y expectativas | Nivel de influencia |
|---|---|---|---|
| Usuario individual | Organizar sus finanzas | Registro sencillo, privacidad, indicadores claros y exportacion | Alto |
| Responsable del hogar | Coordinar gastos familiares | Miembros, invitaciones, divisiones, deudas y liquidaciones | Alto |
| OWNER del workspace | Controlar el espacio compartido | Gestion de miembros, roles, propiedad y auditoria | Alto |
| ADMIN | Administrar la operacion diaria | Categorias, registros, invitaciones y reportes | Medio-alto |
| MEMBER | Registrar y consultar informacion permitida | Formularios claros y balances actualizados | Medio |
| VIEWER | Consultar sin modificar | Lectura de reportes y movimientos | Bajo-medio |
| Analista de requisitos | Validar alcance y trazabilidad | Requisitos, casos de uso y criterios verificables | Alto |
| Analista de calidad | Comprobar comportamiento | Evidencia, pruebas, errores y no regresion | Alto |
| Arquitecto | Mantener coherencia tecnica | Separacion cliente/backend, persistencia y seguridad | Alto |
| Administrador de despliegue | Operar backend y base de datos | Configuracion, Docker, secretos, monitoreo y respaldo | Medio-alto |

## 6. Requisitos funcionales

| ID | Requisito |
|---|---|
| RF-001 | El sistema debe permitir registrar una cuenta con nombre, correo, contrasena, moneda y tipo de cuenta. |
| RF-002 | El sistema debe permitir iniciar y cerrar sesion, localmente o mediante backend. |
| RF-003 | El sistema debe permitir registrar, editar, consultar y eliminar ingresos. |
| RF-004 | El sistema debe permitir registrar, editar, consultar y eliminar gastos. |
| RF-005 | El sistema debe permitir buscar transacciones por texto y categoria. |
| RF-006 | El sistema debe permitir crear categorias de ingreso, gasto y hogar con nombre, icono y color. |
| RF-007 | El sistema debe permitir archivar y restaurar categorias sin romper referencias historicas. |
| RF-008 | El dashboard debe mostrar saldo, ingresos, gastos y ahorro calculados desde datos reales. |
| RF-009 | El sistema debe generar distribucion de gastos y comparativa de flujo de caja. |
| RF-010 | El sistema debe permitir crear presupuestos mensuales y mostrar gasto, disponible y porcentaje usado. |
| RF-011 | El sistema debe alertar cuando un presupuesto alcance estados de atencion, critico o excedido. |
| RF-012 | El sistema debe permitir configurar ahorro recomendado o automatico. |
| RF-013 | El sistema debe permitir crear metas, hacer aportes y consultar avance y fecha limite. |
| RF-014 | El sistema debe permitir crear workspaces y seleccionar el workspace activo. |
| RF-015 | El sistema debe permitir invitar, aceptar, rechazar, remover o cambiar miembros segun permisos. |
| RF-016 | El sistema debe permitir registrar gastos compartidos con division igual, porcentual o por monto. |
| RF-017 | El sistema debe calcular balances y registrar liquidaciones sin perder el historial. |
| RF-018 | El sistema debe permitir crear y ejecutar movimientos recurrentes. |
| RF-019 | El sistema debe mostrar notificaciones derivadas de presupuestos, metas, recurrencias y gasto inusual. |
| RF-020 | El sistema debe permitir consultar reportes por periodos predefinidos o rango personalizado. |
| RF-021 | El sistema debe permitir exportar reportes a CSV, PDF y Excel segun la modalidad disponible. |
| RF-022 | El sistema debe permitir actualizar perfil, avatar, tema, moneda, locale y zona horaria. |
| RF-023 | El sistema debe permitir exportar datos de cuenta y eliminar la cuenta con confirmacion. |
| RF-024 | El sistema debe permitir crear y restaurar respaldos locales con validaciones de seguridad. |
| RF-025 | El sistema debe permitir simular escenarios financieros sin modificar datos reales. |
| RF-026 | El sistema debe permitir administrar facturas y adjuntos cuando existe sesion backend. |

## 7. Requisitos no funcionales

### Usabilidad

- La navegacion debe ser consistente y visible en todo momento.
- Cada accion destructiva debe pedir confirmacion.
- Los formularios deben conservar los datos validos cuando un campo falla.
- Los mensajes deben explicar que ocurrio y como corregirlo.
- Los estados vacios deben orientar al usuario hacia una accion util.
- Las cantidades monetarias deben mostrarse con separadores y moneda coherentes.

### Rendimiento

- Las pantallas locales deben responder sin depender de una red.
- Las operaciones de red deben ejecutarse fuera del hilo de interfaz cuando puedan tardar.
- Las tablas deben cargar solo los registros necesarios y actualizarse tras una mutacion.
- El dashboard debe recalcularse cuando cambian transacciones, presupuestos o metas.

### Seguridad

- Las contrasenas backend deben protegerse con BCrypt y las locales con PBKDF2.
- Los tokens de refresco y recuperacion deben almacenarse como valores protegidos, con rotacion y expiracion.
- Debe validarse la pertenencia al workspace en cada operacion financiera.
- Los roles deben impedir que un VIEWER modifique datos.
- Los avatares y adjuntos deben validar formato, tamano y contenido real.
- Los respaldos ZIP deben protegerse contra traversal, exceso de entradas y exceso de bytes.
- Los secretos deben llegar por variables de entorno y no por codigo fuente.
- Los logs no deben exponer contrasenas, tokens ni datos financieros innecesarios.

### Confiabilidad e integridad

- Los importes deben procesarse con `BigDecimal` y redondeo financiero controlado.
- Los movimientos de ahorro deben conservar un libro mayor consistente.
- Una repeticion de una operacion idempotente no debe duplicar datos.
- Un error en una llamada al backend debe mostrar un estado recuperable.
- El sistema debe mantener los datos historicos aunque una categoria sea archivada.

### Compatibilidad y mantenibilidad

- El cliente debe funcionar con Java 11 o superior.
- El backend requiere Java 21, Spring Boot y PostgreSQL.
- La aplicacion debe poder ejecutarse en Windows mediante el wrapper Maven y el paquete portable.
- El contrato de la API debe mantenerse sincronizado con OpenAPI.
- Las reglas de dominio deben ser comprobables mediante pruebas unitarias e integracion.

## 8. Historias de usuario

| ID | Historia | Puntos sugeridos | Criterio de aceptacion |
|---|---|---:|---|
| HU-001 | Como usuario quiero crear una cuenta para guardar mis finanzas. | 3 | Con datos validos se crea la cuenta y se muestra el acceso al sistema. |
| HU-002 | Como usuario quiero registrar un ingreso para conocer mi saldo real. | 3 | El ingreso aparece en la tabla y actualiza saldo e indicadores. |
| HU-003 | Como usuario quiero registrar un gasto con categoria y fecha. | 3 | El gasto aparece, afecta el total y se refleja en reportes. |
| HU-004 | Como usuario quiero crear un presupuesto para controlar una categoria. | 5 | Se muestra el limite, el gasto real, el disponible y el porcentaje. |
| HU-005 | Como usuario quiero definir una meta de ahorro y hacer aportes. | 5 | El avance se recalcula y la meta se marca como completada al llegar al objetivo. |
| HU-006 | Como usuario quiero visualizar graficos para entender mis habitos. | 5 | Los graficos representan solamente los datos registrados y muestran estado vacio si no existen. |
| HU-007 | Como responsable del hogar quiero dividir gastos entre miembros. | 8 | Las partes suman el total y cada balance se calcula por miembro. |
| HU-008 | Como OWNER quiero invitar miembros y controlar sus roles. | 5 | Solo roles autorizados pueden invitar, cambiar o remover miembros. |
| HU-009 | Como usuario quiero exportar un reporte para compartirlo. | 3 | El archivo se crea en el formato elegido y contiene el periodo seleccionado. |
| HU-010 | Como usuario quiero respaldar mis datos para no perder informacion. | 5 | Se genera un ZIP valido y la restauracion solicita confirmacion. |
| HU-011 | Como usuario quiero simular un escenario sin modificar mis registros. | 3 | El resultado cambia segun los parametros y los datos reales permanecen iguales. |
| HU-012 | Como usuario backend quiero administrar facturas y adjuntos. | 5 | Puedo crear, filtrar, editar, eliminar, adjuntar y descargar soportes autorizados. |

## 9. Casos de uso principales

### CU-01 Registrar y consultar una transaccion

- **Actor principal:** Usuario autenticado.
- **Precondiciones:** existe una categoria compatible y el perfil o workspace esta activo.
- **Flujo:** abrir Ingresos o Gastos, pulsar el boton de nuevo registro, completar categoria, descripcion, monto y fecha, validar y guardar.
- **Alternativas:** monto no numerico, monto cero, descripcion vacia, fecha invalida o categoria inexistente.
- **Postcondicion:** la tabla, el total, el dashboard y los reportes reflejan el movimiento.

### CU-02 Controlar un presupuesto

- **Actor principal:** Usuario o miembro autorizado.
- **Flujo:** abrir Presupuesto, crear o editar una categoria y monto, revisar la tarjeta y consultar el progreso.
- **Reglas:** el gasto usado se obtiene de las transacciones del periodo; los umbrales producen estados visibles.
- **Resultado:** el usuario conoce cuanto puede gastar y recibe alertas oportunas.

### CU-03 Crear una meta y realizar un aporte

- **Actor principal:** Usuario.
- **Flujo:** abrir Metas, crear objetivo, indicar monto y fecha, abrir `Agregar aporte`, ingresar cantidad y confirmar.
- **Alternativas:** aporte invalido, saldo insuficiente o meta ya completada.
- **Resultado:** se actualizan porcentaje, barra, monto faltante y fecha de cumplimiento.

### CU-04 Gestionar un gasto compartido

- **Actor principal:** MEMBER, ADMIN u OWNER autorizado.
- **Flujo:** seleccionar Hogar, crear gasto, elegir pagador y participantes, escoger metodo de division y guardar.
- **Alternativas:** suma de partes diferente al total, participante no autorizado o monto invalido.
- **Resultado:** aparece el gasto y se actualizan balances y deudas.

### CU-05 Generar un reporte

- **Actor principal:** Usuario autenticado.
- **Flujo:** abrir Reportes, elegir periodo o rango, pulsar `Aplicar`, revisar indicadores y elegir CSV, PDF o Excel.
- **Alternativas:** rango mal formado, fechas invertidas o formato no disponible en la modalidad actual.
- **Resultado:** se genera un archivo con datos del periodo elegido y se informa la ruta de salida.

### CU-06 Restaurar un respaldo

- **Actor principal:** Usuario local.
- **Flujo:** abrir Configuracion > Exportar datos, seleccionar `Restaurar`, confirmar la advertencia, elegir ZIP y validar su contenido.
- **Alternativas:** archivo corrupto, path traversal, entrada demasiado grande o formato no permitido.
- **Resultado:** se restaura el estado valido o se conserva el estado actual con un mensaje de error.

## 10. Mockups textuales de referencia

### Ventana principal

```text
+--------------------------------------------------------------------------+
| FinanzasApp                 Workspace activo                 Usuario     |
+------------------+-------------------------------------------------------+
| Inicio            | Inicio                         [fecha]               |
| Ingresos          | Resumen de tu situacion financiera                     |
| Gastos            | +----------+ +----------+ +----------+ +----------+ |
| Presupuesto       | | Saldo    | | Ingresos | | Gastos   | | Ahorro   | |
| Metas             | +----------+ +----------+ +----------+ +----------+ |
| Hogar             | +------------------+ +------------------+ +--------+ |
| Facturas          | | Dona: categorias | | Barras: 6 meses | | Metas  | |
| Reportes          | +------------------+ +------------------+ +--------+ |
| Configuracion     | | Salud financiera / notificaciones / proximos         |
|                  | | [Nuevo ingreso] [Nuevo gasto] [Nueva meta]          |
+------------------+-------------------------------------------------------+
```

### Tabla de transacciones

```text
Ingresos                                             [Nuevo ingreso]
Registro de todos tus ingresos
Buscar: [____________________]       Total ingresos: $___________
+--------+----------+-------------+-----------+------------+---------+
| Tipo   | Categoria| Descripcion | Monto     | Fecha      | Acciones|
+--------+----------+-------------+-----------+------------+---------+
|Ingreso | Trabajo  | Salario     | +$...     | dd/mm/yyyy |   ...   |
+--------+----------+-------------+-----------+------------+---------+
```

### Formulario modal

```text
Nuevo ingreso
Categoria:    [Seleccionar................]
Descripcion:  [___________________________]
Monto:        [___________________________]
Fecha:         [dd/mm/yyyy________________]
                              [Guardar]
```

### Presupuesto y metas

```text
Presupuesto mensual                    [Configurar ahorro] [+ Nuevo presupuesto]
[Presupuesto total] [Total gastado] [Disponible] [Ahorro recomendado]

Comida                         ATENCION  78%
[======================------]  Gastado $780.000 / Presupuesto $1.000.000
                                Disponible $220.000       [Editar] [Borrar]

Ahorro y Metas                                      [+ Nueva Meta]
[Ahorrado total] [Objetivo total] [Metas activas]
Vacaciones                         45%
[================----------------] $900.000 / $2.000.000
Fecha limite: dd/mm/yyyy           Falta: $1.100.000
[+ Agregar aporte] [Editar] [X]
```

## 11. Criterios generales de aceptacion

1. La aplicacion inicia en la pantalla de acceso y no muestra informacion de otro usuario.
2. Un usuario puede completar el registro, el onboarding y llegar al dashboard sin datos ficticios no solicitados.
3. Cada alta, edicion o eliminacion valida los campos y actualiza las vistas relacionadas.
4. Los calculos monetarios conservan precision y muestran formatos legibles para la configuracion regional.
5. Las pantallas tienen estados vacios, estados de carga y mensajes de error comprensibles.
6. El acceso a workspaces, miembros, facturas y reportes backend respeta sesion y rol.
7. El modo local sigue funcionando cuando el backend no esta disponible.
8. El modo API informa los errores de red y permite reintentar o actualizar.
9. Los reportes y respaldos se generan solo despues de validar el destino y el contenido.
10. Los cambios de tema y preferencias se conservan y se aplican al volver a iniciar.
11. Las acciones destructivas requieren confirmacion explicita.
12. Las pruebas unitarias y de integracion disponibles deben pasar; las pruebas que requieren Docker se ejecutan en un entorno con Docker.

## 12. Trazabilidad resumida

| Area solicitada | Seccion de este documento | Evidencia tecnica relacionada |
|---|---|---|
| Datos generales, objetivo, alcance y equipo | 1, 2 y 5 | `README.md`, `docs/ARCHITECTURE.md` |
| Interesados y nivel de influencia | 5 | Roles OWNER, ADMIN, MEMBER y VIEWER |
| Requisitos funcionales | 6 | Pantallas Swing, servicios locales y API REST |
| Requisitos no funcionales | 7 | Seguridad, BigDecimal, persistencia, sincronizacion y UX |
| Historias de usuario | 8 | Flujos de acceso, finanzas, hogar, reportes y respaldo |
| Casos de uso | 9 | `docs/USE_CASES.md` y servicios de dominio |
| Interfaces y mockups | 3, 4 y 10 | `src/main/java/com/finanzas/ui` |

## 13. Fuentes consultadas del proyecto

- `README.md`
- `docs/ARCHITECTURE.md`
- `docs/USE_CASES.md`
- `docs/SECURITY.md`
- `docs/IMPLEMENTATION_PROGRESS.md`
- `AUDIT.md`
- `src/main/java/com/finanzas/ui/MainFrame.java`
- `src/main/java/com/finanzas/ui/LoginFrame.java`
- `src/main/java/com/finanzas/ui/DashboardPanel.java`
- `src/main/java/com/finanzas/ui/TransaccionesPanel.java`
- `src/main/java/com/finanzas/ui/PresupuestoPanel.java`
- `src/main/java/com/finanzas/ui/MetasPanel.java`
- `src/main/java/com/finanzas/ui/FinanzasHogarPanel.java`
- `src/main/java/com/finanzas/ui/FacturasPanel.java`
- `src/main/java/com/finanzas/ui/ReportesPanel.java`
- `src/main/java/com/finanzas/ui/ConfiguracionPanel.java`
