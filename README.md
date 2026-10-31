# Luka

Registro personal de dinero para Colombia. Anota lo que entra y lo que sale. No mueve plata, no pide claves de Nequi, Nu ni Bancolombia, y no se conecta a esas apps. Lo que lee es la notificación que tú autorizas, y el registro se queda en el teléfono.

## APK

El archivo no va en el repositorio. Al publicar una etiqueta `v*`, la acción de GitHub construye `luka.apk` y lo deja en esa release para descargarlo directo.

En tu máquina, después de las pruebas, el instalable queda en:

`app/build/outputs/apk/debug/app-debug.apk`

Ese APK sirve para instalar a mano. No es el firmado de Play. Cada computadora lo firma con su clave de depuración, así que para actualizar encima a veces hay que desinstalar antes.

## Qué no se sube

Quedan fuera la ruta del SDK (`local.properties`), las carpetas de compilación, los APK, los keystore (`*.jks`, `*.keystore`) y cualquier `.env`. La clave de la base se crea en el teléfono. No está en el código.

Este proyecto tiene que subirse desde la carpeta `luka`. La carpeta personal tiene otro Git vacío: no hagas `git add` ahí, porque metería llaves y credenciales del sistema.
