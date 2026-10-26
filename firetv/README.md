# Las dos Fire TV

Scripts de debloat, adaptados de [ivantrj/firetv-glass-launcher](https://github.com/ivantrj/firetv-glass-launcher)
para usar `adb` directo en vez de su cliente propio, que no se lleva bien con Fire OS 6.

| | Principal | LG |
|---|---|---|
| IP | `192.168.100.29` | `192.168.100.15` |
| Modelo | AFTKRT (karat) | AFTMM (mantis) |
| Fire OS | 8.1.8.5 | 6.7.1.1 |
| Android | 11 / API 30 | 7.1.2 / API 25 |
| ABI | `armeabi-v7a` | `armeabi-v7a` |
| Config | `cosy.properties` | `cosy.lg.properties` |

Las dos son de 32 bits: un APK `arm64-v8a` **no** instala en ninguna.

## Uso

```sh
TV=192.168.100.29 ./firetv/debloat.sh     # deshabilitar, repetir tras updates de Fire OS
TV=192.168.100.29 ./firetv/restore.sh     # deshacer todo
```

Con `adb` fuera del PATH, anteponer `ADB=/ruta/al/adb`.

De los 30 paquetes de la lista, en Fire OS 6 solo existen 21; los otros nueve dan
`Unknown package` y está bien.

## Cómo toma Home cada una

**Principal (Fire OS 8):** el launcher de Amazon está protegido y no se puede deshabilitar, así
que el redirect lo hace `HomeButtonService`, el servicio de accesibilidad de esta app. Se habilita
por adb:

```sh
adb shell settings put secure enabled_accessibility_services \
  com.cosyhome.tv/com.cosyhome.tv.HomeButtonService
adb shell settings put secure accessibility_enabled 1
```

**LG (Fire OS 6):** ahí los servicios de accesibilidad **no se pueden habilitar** — escribir ese
setting no bindea nada y el intent de ajustes de accesibilidad crashea. El redirect lo hace
**Launcher Manager (`com.wolf.minilm`)**, que ya estaba instalado. No desinstalarlo o se pierde el
botón Home. `pauseLauncherManager()` en `HomeActivity` existe por eso.

Un `install -r` encima no lo molesta, pero **desinstalar Cosy Home sí deja a Launcher Manager
apuntando a nada**, y después de reinstalarla el botón Home vuelve al launcher de Amazon. Se
arregla reiniciándole el servicio, sin tocar la TV:

```sh
adb shell am force-stop com.wolf.minilm
adb shell am startservice -n com.wolf.minilm/.service.LogcatService
```

Hace falta cada vez que se cambie la firma de la app, porque eso obliga a desinstalar.

## Lo que no está en este repo

- **La keystore.** Hoy se firma con la debug key de `~/.android/debug.keystore`. Si se pierde, no
  se puede actualizar la app en las TVs sin desinstalarla primero.
- **`~/.android/adbkey`**, la clave con la que las TVs autorizaron el ADB.
- **El APK de Flow** (`ar.com.flow.androidtv`), que se instala solo en la principal: pide API 27 y
  la LG tiene 25.
- **Las cámaras de IP Cam Viewer**, cargadas dentro de la app en la TV.
