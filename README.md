# vg-ms-authService

## Pruebas, cobertura y SonarQube Cloud

Requiere JDK 17. Maven se descarga con el wrapper incluido.

Desde la carpeta de este microservicio:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

En Linux/macOS: `bash ./mvnw --batch-mode --no-transfer-progress clean verify`.

Resultados:

- `target/surefire-reports/`: resultados de JUnit.
- `target/site/jacoco/index.html`: cobertura navegable por clase y línea.
- `target/site/jacoco/jacoco.xml`: cobertura que importa SonarQube.

JaCoCo mide todo el código compilado sin exclusiones de cobertura añadidas.
No se impone un porcentaje mínimo local; el Quality Gate se configura en SonarQube.
El objetivo `verify` genera los informes; ejecutar solamente `test` no genera el informe HTML/XML.

### Qué se prueba

- `AuthServiceTest`: `@ParameterizedTest` con `@MethodSource` para módulos de
  SUPERADMIN, PARROCO, SECRETARIO y un rol desconocido, con y sin permisos personalizados.
  También comprueba rol inexistente y propagación de errores.
- `PermissionServiceTest`: `@CsvSource` y `@ValueSource` para autorizar o rechazar
  asignación, eliminación y sincronización de permisos, y usuarios inexistentes.
- `KeycloakAuthSyncAdapterTest`: creación de usuarios y respuesta de error con mocks.
- `AuthApplicationTests`: carga del contexto Spring con perfil `test`, Keycloak y
  decodificador JWT simulados; Eureka desactivado y R2DBC apuntando a localhost sin inicialización SQL.
  No requiere una base de datos o un proveedor de identidad real. No comprueba la integración real con ellos.

SonarQube analiza calidad y consume la cobertura; las pruebas parametrizadas las ejecuta JUnit.

### Lo que debes hacer primero en SonarQube Cloud

1. Inicia sesión con GitHub, crea o selecciona tu organización e importa el repositorio
   de **vg-ms-usersmicroservice**. Esta carpeta tiene su propio repositorio Git.
2. Copia **Project Key** y **Organization Key** exactamente como aparecen en SonarQube.
3. Selecciona análisis mediante **GitHub Actions / Maven** y desactiva **Automatic Analysis**
   si aparece habilitado: el análisis lo hará Maven en CI.
4. Genera un token con permiso de análisis para el proyecto desde la sección de seguridad
   de tu cuenta. Guarda el valor directamente en GitHub, nunca en este repositorio o el chat.
5. En el repositorio de GitHub, abre **Settings > Secrets and variables > Actions**.

| Tipo | Nombre | Valor |
| --- | --- | --- |
| Secret | `SONAR_TOKEN` | Token de SonarQube Cloud |
| Variable | `SONAR_PROJECT_KEY` | Project Key |
| Variable | `SONAR_ORGANIZATION` | Organization Key |
| Variable | `SONAR_ENABLED` | `true` cuando hayas completado la configuración |
| Variable opcional | `SONAR_REGION` | Dejar sin crear para región EU (`sonarcloud.io`); usar `us` para región US |

No necesitas crear un token personal de GitHub, ni proporcionar credenciales de PostgreSQL,
Keycloak o Eureka para estas pruebas.

### GitHub Actions preparado para que lo actives tú

El archivo `.github/workflows/ci.yml` ejecuta compilación, pruebas y JaCoCo en push,
pull request y ejecución manual. Guarda los informes como artefacto `users-test-reports`.
Debe estar en la raíz del repositorio del microservicio, junto al `pom.xml`;
si lo trasladas a un monorepo, debes adaptar las rutas de trabajo y de artefactos.

El análisis Sonar se ejecuta cuando `SONAR_ENABLED=true`, en push o ejecución manual
sobre la rama predeterminada del repositorio. Los PR y otras ramas ejecutan pruebas y cobertura.
Esto evita depender del soporte de análisis de ramas/PR de tu plan de SonarQube Cloud.
Comprueba que la rama principal del proyecto Sonar coincida con la predeterminada de GitHub.
El paso de Sonar espera el Quality Gate y falla si no lo supera. Sin `SONAR_ENABLED=true`,
un CI verde solo acredita compilación y pruebas, no una aprobación de SonarQube.

Para activarlo, sube tú los cambios al repositorio y revisa la pestaña **Actions**.
En la rama predeterminada también puedes usar **Run workflow**. La ejecución remota
y el resultado del Quality Gate requieren tus datos de Sonar; no quedan acreditados por la prueba local.

### Análisis local opcional

Con `SONAR_TOKEN` ya definido de forma privada en el entorno de tu terminal:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify sonar:sonar "-Dsonar.projectKey=TU_PROJECT_KEY" "-Dsonar.organization=TU_ORGANIZATION_KEY" "-Dsonar.qualitygate.wait=true"
```

### Migración manual y credenciales existentes

`KeycloakMigrationTest` es una utilidad de exportación de usuarios reales, no una prueba
unitaria. Se omite por defecto porque consulta la base y exporta hashes de contraseñas.
Para ejecutarla manualmente, solo con una base autorizada y configurada:

```powershell
.\mvnw.cmd "-Dtest=KeycloakMigrationTest" "-Dkeycloak.migration.enabled=true" test
```

`keycloak-users.json` y los archivos `.env` están excluidos de Git.
La aplicación no carga `.env` automáticamente.

Antes de publicar, reemplaza las credenciales existentes en
`src/main/resources/application.yml` y `KeycloakClientConfig.java` por variables de entorno.
Si siguen vigentes, rótalas en PostgreSQL/Keycloak; borrarlas del archivo no las elimina del historial Git.
Las pruebas usan configuración local ficticia y mocks, sin necesitar esas credenciales.

Referencias: [JaCoCo para Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html),
[SonarScanner para Maven](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/scanners/sonarscanner-for-maven),
[setup-java](https://github.com/actions/setup-java).
