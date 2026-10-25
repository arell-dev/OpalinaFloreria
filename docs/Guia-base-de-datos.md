# Base de datos local con Docker Desktop

Esta guía explica cómo preparar Docker Desktop en Windows y ejecutar el MySQL que usa Opalina. No hace falta instalar MySQL directamente en Windows: Docker Compose crea el contenedor, la base de datos y el usuario de la aplicación.

## Requisitos

- Windows compatible con la versión actual de Docker Desktop.
- Java 21 para iniciar Opalina después de preparar MySQL.

Consulta los [requisitos oficiales de Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/) antes de instalarlo.

### Compatibilidad en los equipos del grupo

WSL no hace compatible una versión de Windows que Docker Desktop ya no admite. Cada integrante debe comprobar su edición y versión escribiendo `winver` en el menú **Inicio** y compararlas con los [requisitos vigentes de Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/). Como referencia, la documentación actual lista Windows 10 de 64 bits, versión 22H2 (compilación 19045), y Windows 11 de 64 bits, versión 23H2 (compilación 22631) o posterior para el motor WSL 2. Docker solo admite versiones de Windows que sigan dentro del periodo de soporte de Microsoft; los requisitos pueden cambiar.

Docker Desktop en Windows Home o Education solo puede ejecutar contenedores Linux. Eso es suficiente para Opalina, porque el servicio MySQL del archivo Compose usa un contenedor Linux. Cada equipo también necesita virtualización habilitada, un procesador de 64 bits compatible con SLAT (una función necesaria para crear entornos virtualizados), al menos 8 GB de RAM y WSL actualizado. Comprueba la edición y el hardware contra la tabla oficial antes de dar por hecho que funcionará en un equipo.

## Antes de instalar

### Virtualización de hardware

La virtualización es una función del procesador que permite ejecutar otro entorno de sistema operativo dentro de Windows, de forma aislada. Docker Desktop la necesita para proporcionar el entorno Linux donde se ejecuta el contenedor de MySQL. No reemplaza Windows ni modifica tus archivos: permite que ambos entornos funcionen en el mismo equipo.

Para comprobar si ya está activa:

1. Pulsa `Ctrl + Mayús + Esc` para abrir el Administrador de tareas.
2. Entra en **Rendimiento** y selecciona **CPU**.
3. Busca el dato **Virtualización**. Si dice **Activada**, puedes continuar.

Si aparece **Desactivada**, hay que activarla en el menú UEFI/BIOS del equipo. Para llegar a ese menú:

1. En Windows 11, abre **Configuración > Sistema > Recuperación > Inicio avanzado > Reiniciar ahora**.
2. En Windows 10, abre **Configuración > Actualización y seguridad > Recuperación > Inicio avanzado > Reiniciar ahora**.
3. Cuando aparezca el menú de recuperación, selecciona **Solucionar problemas > Opciones avanzadas > Configuración de firmware UEFI > Reiniciar**.

Ya en UEFI/BIOS, busca una opción de virtualización. Según el fabricante, puede llamarse `Intel Virtualization Technology`, `VT-x`, `AMD-V` o `SVM Mode`. Actívala, guarda los cambios y reinicia. La ubicación cambia según el equipo; sigue la guía de [Microsoft para habilitar la virtualización](https://support.microsoft.com/windows/experience/enable-virtualization-on-windows) o las instrucciones de su fabricante. Cambia solo la opción necesaria.

### WSL 2

WSL significa **Subsistema de Windows para Linux**. WSL 2 permite que Windows use el núcleo de Linux (la parte esencial de ese sistema operativo) dentro de un entorno ligero que administra Windows. Docker Desktop lo usa para ejecutar contenedores Linux, como el de MySQL de Opalina; no tienes que crear ni administrar una máquina virtual por tu cuenta. WSL 2 no es la base de datos: solo proporciona el entorno donde Docker puede ejecutar el contenedor MySQL.

WSL 2 y la virtualización están relacionados, pero no son lo mismo: WSL 2 es la función de Windows que proporciona el entorno Linux; la virtualización es la capacidad del equipo que permite que ese entorno funcione. WSL 2 necesita que Windows tenga habilitadas las características **Subsistema de Windows para Linux** y **Plataforma de máquina virtual**. El instalador de WSL habilita estas características; puede solicitar permisos de administrador y reiniciar Windows.

**¿Hace falta instalar Ubuntu?** No para ejecutar Opalina desde PowerShell con Docker Desktop. Docker Desktop puede ejecutar los contenedores y ofrecer el comando `docker` desde PowerShell sin que instales una distribución Linux. Ubuntu es opcional: sirve si también quieres abrir una terminal Linux o trabajar dentro de ella. Si instalas Ubuntu, Docker Desktop puede integrarse con esa terminal para que allí también puedas usar `docker`.

Docker Desktop pide WSL 2.1.5 o posterior para usar este motor. La [guía de Docker para WSL 2](https://docs.docker.com/desktop/features/wsl/) explica este requisito y la [guía de Microsoft](https://learn.microsoft.com/windows/wsl/install) detalla la instalación de WSL.

## 1. Preparar WSL e instalar Docker Desktop

Antes de instalar Docker Desktop, comprueba si WSL ya está instalado con `wsl --version`. Si el comando no se reconoce o WSL no está instalado, abre PowerShell como administrador. Para Opalina, puedes activar WSL sin instalar Ubuntu:

~~~powershell
wsl --install --no-distribution
~~~

Elige una de estas dos opciones; no hace falta ejecutar ambas. Si además quieres una terminal Linux, instala Ubuntu en vez de usar la opción anterior:

~~~powershell
wsl --install -d Ubuntu
~~~

`wsl --install` sin opciones instala Ubuntu de forma predeterminada. Si eliges instalar Ubuntu, Windows puede pedirte reiniciar; al abrir Ubuntu por primera vez, te pedirá crear un usuario y una contraseña para esa terminal. Son independientes de tu cuenta de Windows y de las credenciales de MySQL de Opalina.

Si `wsl --install` no está disponible en ese Windows, puedes habilitar las características desde la interfaz: abre **Inicio**, busca **Activar o desactivar las características de Windows**, marca **Subsistema de Windows para Linux** y **Plataforma de máquina virtual**, selecciona **Aceptar** y reinicia. Después, completa la instalación de WSL 2 siguiendo los [pasos manuales de Microsoft](https://learn.microsoft.com/windows/wsl/install-manual); algunos Windows antiguos necesitan instalar o actualizar el kernel de Linux por separado.

Después de instalar WSL, reinicia Windows si te lo pide. Luego abre PowerShell y comprueba o actualiza WSL:

~~~powershell
wsl --version
wsl --update
~~~

Si WSL ya estaba instalado, no repitas la instalación: ejecuta `wsl --update` y `wsl --version`. Docker Desktop requiere WSL 2.1.5 o posterior para este motor.

Luego instala Docker Desktop:

1. Descárgalo desde la [página oficial de instalación para Windows](https://docs.docker.com/desktop/setup/install/windows-install/).
2. Ejecuta el instalador y selecciona el motor basado en WSL 2 cuando aparezca esa opción. Es la opción recomendada para la mayoría de equipos.
3. Si el instalador solicita reiniciar Windows, hazlo.
4. Abre Docker Desktop y espera a que indique que el motor está iniciado.

Si Docker Desktop ya está instalado, comprueba que use el motor WSL 2 en **Settings > General > Use the WSL 2 based engine** y aplica los cambios si tuviste que activarlo.

Si instalaste Ubuntu y quieres usar Docker desde su terminal, habilita Ubuntu en Docker Desktop, en **Settings > Resources > WSL Integration**. Para trabajar desde PowerShell, esa integración con Ubuntu no hace falta.

### Cómo abrir WSL o Ubuntu

- Si instalaste Ubuntu, búscalo en **Inicio** y ábrelo como cualquier aplicación. También puedes abrir PowerShell y ejecutar `wsl -d Ubuntu`. La primera vez, Ubuntu te pedirá crear el usuario y la contraseña de esa distribución.
- Si usaste `wsl --install --no-distribution`, no aparecerá una aplicación Ubuntu que abrir. Es normal: WSL está habilitado para que Docker Desktop administre su propio entorno Linux.
- Para iniciar Opalina, no necesitas abrir Ubuntu. Abre **Docker Desktop** desde **Inicio**, espera a que el motor esté listo y ejecuta los comandos de la guía desde PowerShell, situado en la carpeta del proyecto.

## 2. Comprobar Docker

Abre PowerShell y ejecuta:

~~~powershell
docker --version
docker compose version
docker info
~~~

Los dos primeros comandos deben mostrar sus versiones. `docker info` debe mostrar información del servidor; si indica que no puede conectarse, abre Docker Desktop y espera a que el motor termine de iniciar.

## 3. Preparar la configuración de Opalina

Abre PowerShell en la carpeta raíz del proyecto, donde están `docker-compose.yml` y `.env.example`. Crea `.env` a partir del ejemplo:

~~~powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
~~~

El archivo `.env.example` incluye valores para desarrollo local:

| Variable | Configuración inicial | Uso |
| --- | --- | --- |
| `OPALINA_DB_USERNAME` | `opalina` | Usuario de la aplicación en MySQL. |
| `OPALINA_DB_PASSWORD` | `opalina_local` | Contraseña local de ese usuario. |
| `OPALINA_DB_ROOT_PASSWORD` | `opalina_root_local` | Contraseña de `root` dentro del contenedor. |
| `OPALINA_DB_PORT` | `3307` | Puerto de MySQL publicado en Windows. |
| `OPALINA_DB_URL` | Conecta con `opalina` en `localhost:3307`. | Conexión JDBC que usa Spring Boot. |
| `OPALINA_DEMO_DATA_ENABLED` | `true` | Habilita los datos de demostración. |

Estos valores son solo para desarrollo local. No uses estas contraseñas en un entorno compartido o publicado. Si cambias el puerto, actualiza también el puerto dentro de `OPALINA_DB_URL`.

## 4. Crear e iniciar MySQL

Desde la misma carpeta raíz del proyecto, ejecuta:

~~~powershell
docker compose up -d mysql
docker compose ps
~~~

La primera ejecución descarga la imagen `mysql:8.4` si aún no está disponible y crea el contenedor `opalina-mysql`. Compose inicializa la base `opalina` y su usuario con los valores de `.env`. El servicio debe aparecer como `healthy` antes de iniciar la aplicación. También puedes consultar el arranque en Docker Desktop o seguir los registros:

~~~powershell
docker compose logs -f mysql
~~~

El puerto `3307` de Windows se conecta con el `3306` del contenedor. Los datos se conservan en el volumen de Docker `opalina_mysql_data` aunque se detenga o elimine el contenedor.

## 5. Crear las tablas y arrancar Opalina

Con MySQL en estado `healthy`, inicia la aplicación en otra terminal:

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

Al arrancar, Spring Boot lee la conexión de `.env` y Flyway aplica las migraciones ubicadas en `src/main/resources/db/migration/` (actualmente V1 y V2). Después, Hibernate valida que las entidades correspondan con las tablas. Docker crea la base vacía; las tablas se crean cuando arranca la aplicación y Flyway ejecuta esas migraciones.

Abre [http://localhost:8080](http://localhost:8080) para entrar a Opalina.

## 6. Comprobar la base de datos

Puedes conectarte al MySQL del contenedor desde PowerShell:

~~~powershell
docker compose exec mysql mysql -u opalina -p opalina
~~~

Cuando lo solicite, escribe la contraseña de `OPALINA_DB_PASSWORD` (por defecto, `opalina_local`). En la consola de MySQL, consulta las tablas y las migraciones aplicadas:

~~~sql
SHOW TABLES;

SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
~~~

Para salir, escribe `exit;`.

## 7. Detener y volver a iniciar

Para detener MySQL y conservar los datos, ejecuta:

~~~powershell
docker compose down
~~~

Para iniciarlo de nuevo:

~~~powershell
docker compose up -d mysql
~~~

`docker compose down -v` también elimina el volumen `opalina_mysql_data` y borra la base local. Úsalo solo si quieres reiniciar la base desde cero y no necesitas conservar sus datos.

## Solución de problemas

| Problema | Qué revisar |
| --- | --- |
| Docker no puede conectarse al motor. | Abre Docker Desktop, espera a que termine de iniciar y confirma que usa el motor WSL 2. |
| El puerto `3307` ya está ocupado. | Cambia `OPALINA_DB_PORT` en `.env` y usa ese mismo puerto en `OPALINA_DB_URL`; después recrea el contenedor con `docker compose up -d mysql`. |
| La aplicación indica que faltan tablas. | Confirma que MySQL está `healthy` e inicia Spring Boot para que Flyway aplique las migraciones. |
| MySQL rechaza la contraseña después de editar `.env`. | MySQL conserva las credenciales de la primera inicialización en el volumen. Actualiza la configuración para que coincida con ellas o, si puedes borrar la base local, elimina el volumen con `docker compose down -v` y vuelve a iniciar el servicio. |

## Continuar

Con Docker y MySQL preparados, vuelve al [README principal](../README.md#ejecutar-en-local) para iniciar Opalina o continúa con la [guía de ejecución y pruebas](Guia-de-pruebas.md).
