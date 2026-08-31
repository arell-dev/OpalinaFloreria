# Opalina Florería Piura

Frontend estático de Opalina Florería Piura, desarrollado con HTML, Bootstrap
5.3.8 y una capa pequeña de estilos propios. El proyecto representa una tienda
floral con catálogo, detalle de producto, carrito, pedido, cuenta de usuario y
secciones informativas.

La implementación prioriza una base Bootstrap reutilizable y responsive. Los
estilos propios se limitan a la identidad visual de Opalina, variables de tema
y ajustes puntuales que Bootstrap no expresa directamente.

## Estado actual

El alcance documentado en este README corresponde al FrontEnd. La lógica de
autenticación, persistencia de usuarios, inventario, pagos y pedidos queda
reservada para el backend futuro.

Las páginas HTML existentes son funcionales como prototipo navegable. Algunos
formularios todavía son visuales y no envían información a una API. El carrito
incluye una interacción local para modificar cantidades y recalcular subtotal
y total.

## Estructura del proyecto

```text
OpalinaFloreria/
├── frontend/
│   ├── index.html
│   ├── pages/
│   │   ├── catalogo.html
│   │   ├── producto.html
│   │   ├── carrito.html
│   │   ├── pedido.html
│   │   ├── confirmacion.html
│   │   ├── contacto.html
│   │   ├── nosotros.html
│   │   ├── login.html
│   │   ├── registro.html
│   │   ├── perfil.html
│   │   ├── historial-pedidos.html
│   │   └── detalle-pedido.html
│   ├── components/
│   │   ├── navbar.html
│   │   ├── footer.html
│   │   ├── cuenta.html
│   │   └── legal.html
│   ├── assets/
│   │   ├── imagenes/
│   │   └── svg/
│   ├── css/
│   │   ├── colores.css
│   │   └── componentes.css
│   └── js/
│       └── cuenta.js
├── backend/
├── LICENSE
└── README.md
```

`frontend/index.html` es la referencia visual y estructural principal. Las
páginas dentro de `frontend/pages/` reutilizan el mismo orden de carga, navbar,
footer, sistema de rutas y convenciones de Bootstrap.

## Tecnologías y dependencias

- HTML5 semántico.
- Bootstrap 5.3.8 mediante CDN.
- CSS propio dividido en tokens y overrides.
- JavaScript nativo únicamente para interacciones puntuales del frontend.
- Google Fonts para `Cormorant Garamond` y `Nunito Sans`.
- SVG e imágenes locales para identidad visual e ilustraciones.

No se utiliza un framework adicional, preprocesador, sistema de plantillas ni
dependencia de compilación.

## Orden de carga

Cada documento carga sus estilos en este orden:

1. Fuentes externas.
2. Bootstrap 5.3.8.
3. `css/colores.css`.
4. `css/componentes.css`.

Este orden permite que Bootstrap defina la base y que los archivos propios
ajusten únicamente sus variables y componentes. El bundle JavaScript de
Bootstrap se incluye una sola vez al final de cada página para habilitar el
menú responsive del navbar.

## Organización del CSS

### `css/colores.css`

Contiene exclusivamente tokens de diseño: colores de la marca, tipografías,
radios, sombras y espaciado base. Las variables permiten mantener la misma
identidad en botones, tarjetas, enlaces, navbar y footer sin repetir valores.

### `css/componentes.css`

Contiene los overrides de Bootstrap y las variantes visuales reutilizables del
proyecto. Sus bloques principales son:

- Variables CSS de Bootstrap conectadas con los tokens de Opalina.
- Tipografía, botones, tarjetas y enlaces globales.
- Navbar compartido y sus estados responsive.
- Etiquetas `ceja` e `insignia`.
- Hero compartido e imagen principal del inicio.
- Variantes de cuenta y sidebar de perfil.
- Footer y barra legal.
- Ajustes responsive específicos para inicio, carrito, perfil y elementos
  legales.

No se copian las hojas de estilo de `PaginasSinBootstrap`. La distribución,
las columnas, los espaciados, las proporciones y los estados se resuelven con
Bootstrap; el CSS propio solo conserva el aspecto de la marca y ajustes
pequeños de presentación.

## Componentes Bootstrap utilizados

### Navbar y Collapse

El navbar aparece en todas las interfaces desarrolladas. Utiliza
`navbar`, `navbar-expand-md`, `navbar-brand`, `navbar-nav`, `nav-link` y
`navbar-toggler`. El componente `collapse` permite convertir la navegación en
un menú desplegable hasta el breakpoint `md`.

Se eligió `collapse` porque el menú es corto y debe permanecer integrado en el
encabezado. No se utiliza `offcanvas`, ya que no existe una navegación extensa
que justifique un panel lateral.

### Container y sistema Grid

`container`, `row`, `col-*`, `row-cols-*` y `g-*` estructuran todas las páginas.
El grid permite cambiar la distribución sin crear media queries duplicadas:

- El hero usa columnas que se apilan en móvil y se dividen desde `lg`.
- El catálogo usa una columna en pantallas pequeñas, dos desde `md` y tres
  desde `lg`.
- Pedido y perfil separan formularios, selección de productos y sidebar.
- El footer distribuye sus bloques en escritorio y los apila en móvil.

### Card

Las tarjetas agrupan productos, formularios, resúmenes, bloques informativos y
secciones de cuenta. `card`, `card-body`, `card-title`, `card-text` y `h-100`
mantienen una estructura uniforme y permiten que las tarjetas del catálogo
tengan igual altura.

Se utiliza `card` en lugar de crear contenedores visuales independientes para
cada sección, porque proporciona una superficie, borde y estructura coherentes
con poco CSS adicional.

### Buttons

Las acciones emplean `btn-primary`, `btn-outline-primary`,
`btn-outline-secondary`, `btn-sm`, `btn-lg`, `w-100` y `rounded-pill`.
Esto mantiene una jerarquía clara entre acciones principales, secundarias y
acciones compactas de las tarjetas o del carrito.

### Forms e Input group

Los formularios utilizan `form-label`, `form-control`, `form-select`,
`form-check-input`, `form-text` e `input-group`.

- Contacto usa inputs, select y textarea.
- Login y registro usan campos de cuenta y grupos para mostrar u ocultar la
  contraseña.
- Pedido usa datos de entrega, fecha, hora y radios de selección.
- Perfil usa un grid de datos personales.
- Producto usa un input numérico para la cantidad.
- Carrito usa grupos compactos para incrementar y disminuir cantidades.

Se utilizan los controles nativos de Bootstrap porque ya incluyen estados de
foco, tamaños, espaciado y comportamiento accesible consistentes.

### List group

`list-group`, `list-group-flush`, `list-group-item` y
`list-group-item-action` organizan enlaces de cuenta, productos seleccionables,
información de disponibilidad y bloques de contacto. Es una solución adecuada
para contenido lineal y repetitivo sin convertirlo en una tabla o en un menú
complejo.

### Badge

`badge`, `rounded-pill` y `bg-success` muestran el contador estático del
carrito. En pedido también se utiliza una insignia para identificar la
selección disponible. El contador se posiciona con las utilidades
`position-absolute` y `translate-middle`.

### Alert

`alert`, `alert-success` y `alert-danger` comunican confirmaciones y mensajes
de error en confirmación, login y registro. Se utiliza este componente porque
los mensajes son breves, contextuales y no requieren modal ni interacción
adicional.

### Ratio e imágenes responsive

`ratio`, `ratio-4x3`, `ratio-1x1`, `img-fluid`, `object-fit-cover`, `w-100` y
`h-100` mantienen proporciones consistentes en heroes, tarjetas, producto y
miniaturas del carrito. Esto evita que las imágenes deformen las tarjetas o
produzcan saltos de altura entre productos.

## Utilidades Bootstrap y responsive

Las utilidades de Bootstrap resuelven la mayor parte del comportamiento
responsive:

- `d-flex`, `d-grid`, `flex-column`, `flex-wrap` y `gap-*` controlan la
  distribución y el apilamiento.
- `align-items-*`, `justify-content-*` y `text-*` alinean contenido sin CSS
  específico por página.
- `p-*`, `m-*`, `py-*`, `px-*` y sus variantes responsive controlan el ritmo
  vertical y horizontal.
- `order-*` permite priorizar la imagen o el formulario en móvil en las
  páginas de cuenta.
- `min-vh-100`, `flex-grow-1` y `d-flex flex-column` mantienen el footer del
  carrito al final del viewport incluso cuando el contenido es corto.
- `visually-hidden` conserva información para lectores de pantalla, como el
  texto descriptivo del contador del carrito.

Los breakpoints principales son los de Bootstrap: `sm` desde 576 px, `md`
desde 768 px y `lg` desde 992 px. No se agregan cortes personalizados cuando
una utilidad de Bootstrap resuelve el mismo comportamiento.

## Interfaces disponibles

- `index.html`: presentación, beneficios y productos destacados.
- `pages/catalogo.html`: filtros visuales y grilla de doce productos.
- `pages/producto.html`: detalle del Ramo Opalina Morado.
- `pages/carrito.html`: productos seleccionados, cantidades y resumen.
- `pages/pedido.html`: formulario de entrega y selección de arreglo.
- `pages/confirmacion.html`: confirmación y resumen de solicitud.
- `pages/contacto.html`: información de contacto y formulario.
- `pages/nosotros.html`: historia, valores y propuesta de Opalina.
- `pages/login.html`: acceso visual de usuarios.
- `pages/registro.html`: creación visual de cuentas.
- `pages/perfil.html`: datos de cuenta y perfil.

`historial-pedidos.html` y `detalle-pedido.html` están reservados para una
iteración posterior y actualmente no forman parte del flujo implementado.

## Navegación y componentes compartidos

Los fragmentos de `frontend/components/` funcionan como fuentes de referencia
para copiar el markup compartido en cada documento. Debido a que el proyecto
es HTML estático, el navbar y el footer se incrustan en línea; no se utilizan
`fetch`, iframes ni un motor de plantillas.

Las rutas principales ya conectan Inicio, Catálogo, Nosotros, Contacto,
Carrito, Login, Registro, Producto, Pedido y Confirmación. Los enlaces legales
permanecen pendientes hasta que existan páginas legales definitivas.

Desde una página en `frontend/pages/` se utilizan rutas relativas como:

```text
../index.html
../css/componentes.css
../assets/imagenes/...
```

Desde `frontend/index.html` las rutas comienzan directamente en `pages/`,
`css/` o `assets/`.

## JavaScript del FrontEnd

El JavaScript se mantiene dentro de `frontend/` porque controla comportamiento
del navegador y de la interfaz:

- `frontend/js/cuenta.js` alterna la visibilidad de las contraseñas en login y
  registro.
- `carrito.html` contiene una interacción local para sumar, restar y eliminar
  productos, además de recalcular subtotal y total.
- Bootstrap proporciona el comportamiento del navbar responsive mediante su
  bundle oficial.

No se colocan scripts de interfaz dentro de `backend/`. En el futuro, el
backend podrá exponer APIs y persistencia, mientras el frontend consumirá esos
servicios mediante una capa de integración separada.

## Ejecución y comprobación

El frontend puede abrirse directamente desde `frontend/index.html`. También es
recomendable usar cualquier servidor estático local para que las rutas relativas
se comporten igual que en un despliegue web.

Antes de integrar una nueva página se debe comprobar:

1. Que el CDN de Bootstrap aparezca antes de `colores.css` y `componentes.css`.
2. Que el bundle JavaScript de Bootstrap se cargue una sola vez.
3. Que el navbar y footer sigan el markup de referencia.
4. Que las rutas de imágenes, CSS y páginas sean relativas a la ubicación del
   documento.
5. Que la página funcione en 320, 576, 768, 992 y 1440 px sin desbordamiento
   horizontal.
6. Que las tarjetas, formularios, botones e imágenes conserven su jerarquía y
   proporción.

## Referencias oficiales de Bootstrap

- [Introducción](https://getbootstrap.com/docs/5.3/getting-started/introduction/)
- [Navbar](https://getbootstrap.com/docs/5.3/components/navbar/)
- [Grid](https://getbootstrap.com/docs/5.3/layout/grid/)
- [Botones](https://getbootstrap.com/docs/5.3/components/buttons/)
- [Cards](https://getbootstrap.com/docs/5.3/components/card/)
- [Badges](https://getbootstrap.com/docs/5.3/components/badge/)
- [Formularios](https://getbootstrap.com/docs/5.3/forms/overview/)
- [Checks y radios](https://getbootstrap.com/docs/5.3/forms/checks-radios/)
- [List group](https://getbootstrap.com/docs/5.3/components/list-group/)
- [Alerts](https://getbootstrap.com/docs/5.3/components/alerts/)
- [Ratio](https://getbootstrap.com/docs/5.3/helpers/ratio/)
- [Utilidades responsive](https://getbootstrap.com/docs/5.3/layout/breakpoints/)

## Trabajo pendiente

- Implementar el backend y conectar los formularios con servicios reales.
- Persistir carrito, usuarios, pedidos e inventario.
- Completar historial y detalle de pedidos.
- Crear las páginas definitivas de términos y privacidad.
- Sustituir los datos de ejemplo por información dinámica.
