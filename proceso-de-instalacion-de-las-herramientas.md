---
# yaml-language-server: $schema=schemas\page.schema.json
Object type:
    - Page
Backlinks:
    - Sistemas Distribuidos
Creation date: "2026-02-16T10:34:08Z"
Created by:
    - Trendy Ivory
id: bafyreicxnz7glu7a2aw6ngklehgdulza4g67mopwfcgdlxzybz5nkliari
---
# Proceso de instalación de las herramientas   
> Basada en la “Guía de configuración de desarrollo para JEE (v23)” de la UBU (Feb 2023).   

> **Objetivo:** dejar funcionando JDK 8u361 + Ant + Maven + Tomcat 10.0.27 + GlassFish 5.1.0 (Web) + Eclipse JEE 2022-12, con verificación por consola y con integración de servidores en Eclipse.   

 --- 
## 0) Datos de mi equipo   
-\*\*Usuario Windows:\*\*\`Usuario\`   
-\*\*Sistema operativo:\*\*\`Windows \_\_\_\_ (64-bit)\`   
-\*\*Ruta base para descargas y extracciones:\*\*   
`C:\SistemasDistribuidos `    
 --- 
## 1) Estructura de directorios (actual)   
Dentro de la ruta base tengo:   
```
/SistemasDistribuidos
|
+---Java
|   \---jdk1.8.0_361
|       +---bin
|       +---include
|       +---jre
|       +---lib
|       \---...
|
\---Programas
    +---apache-ant-1.10.13
    |   +---bin
    |   +---lib
    |   \---...
    |
    +---apache-maven-3.8.7
    |   +---bin
    |   +---boot
    |   +---conf
    |   \---lib
    |
    +---apache-tomcat-10.0.27
    |   +---bin
    |   +---conf
    |   +---lib
    |   +---logs
    |   +---webapps
    |   \---...
    |
    +---glassfish5
    |   \---glassfish
    |       +---bin
    |       +---config
    |       +---domains
    |       +---lib
    |       \---...
    |
    \---eclipse
        |   eclipse.exe
        +---configuration
        +---features
        +---plugins
        \---...
```
 --- 
## 2) JDK 1.8.0\_361   
### 2.1 Comprobar que el JDK está completo   
Primero hay que verificar que exista:   
-\`...\Java\jdk1.8.0\_361\bin\javac.exe\`   
Si NO existe, habría que reinstalar el JDK con el instalador oficial y elegir como ruta de instalación la carpeta: `…\Java\jdk1.8.0\_361\` .   
### 2.2 Crear variables de entorno persistentes   
Abrir:   
-\*\*Win + R\*\* → \`sysdm.cpl\` → pestaña \*\*Opciones avanzadas\*\* → \*\*Variables de entorno…\*\*   
![Captura de pantalla 2026-02-16 115001](files\captura-de-pantalla-2026-02-16-115001.png)    
![Captura de pantalla 2026-02-16 115140](files\captura-de-pantalla-2026-02-16-115140.png)    
![Captura de pantalla 2026-02-16 115235](files\captura-de-pantalla-2026-02-16-115235.png)    
Crear e**n Variables de sistema** :   
-\`JAVA\_HOME\` =` C:\\SistemasDistribuidos\Java\jdk1.8.0\_361`    
Editar \`Path\` y \*\*añadir\*\*:   
-\`%JAVA\_HOME%\bin\`   
![Captura de pantalla 2026-02-16 115614](files\captura-de-pantalla-2026-02-16-115614.png)    
### 2.3 Verificación por consola   
Cerrar y abrir una consola nueva y ejecutar:   
\`\`\`bat   
javac -version   
java -version   
echo%JAVA\_HOME%   
\`\`\`   
![image](files\image.png)    
Con esto queda verificado que esta versión de java ha sido configurada correctamente.   
 --- 
## 3) Resto de Programas   
El procedimiento para estas herramientas es el mismo que se realizó para el JDK:   
  1. Crear la variable de entorno correspondiente.
   
  2. Añadir` %NOMBRE\_VARIABLE%\bin`  al `Path` .
   
  3. Abrir una consola nueva y verificar funcionamiento.   
 --- 
##  3.1) Apache ANT 1.10.13   
**Variable de entorno:** 

- `ANT\_HOME`  =
  `C:\SistemasDistribuidos\Programas\apache-ant-1.10.13` 

Añadir al `Path` :
- `%ANT\_HOME%\bin` 

**Verificación:** 

\`\`\`bat
ant -version
echo %ANT\_HOME%   
\`\`\`   
![image](files\image_2.png)    
Nos salen las versiones y directorios correctos, así que está correctamente configurado.   
 --- 
### 3.2) Apache Maven 3.8.7   
**Variable de entorno:**   
- `M2\_HOME` =   
    `C:\SistemasDistribuidos\Programas\apache-maven-3.8.7`   
   
Añadir al `Path`:   
- `%M2\_HOME%\bin`   
   
**Verificación:**   
```

mvn -version
echo %M2_HOME%

```
![image](files\image_k.png)    
LA versión y el directorio son correctos, así que está correctamente configurado.   
 --- 
### 3.3) Apache Tomcat 10.0.27   
Tomcat y GlassFish utilizan por defecto el puerto 8080.   
No deben ejecutarse simultáneamente.   
**Variable de entorno:**   
- `CATALINA\_HOME` =   
    `C:\SistemasDistribuidos\Programas\apache-tomcat-10.0.27`   
   
Añadir al `Path`:   
- `%CATALINA\_HOME%\bin`   
   
**Arranque:**   
```

startup.bat


```
Comprobar en navegador:   
```

http://localhost:8080


```
![image](files\image_x.png)    
Nos sale la página principal de TomCat, así que está configurado correctamente.   
**Parada:**   
```

shutdown.bat


```
 --- 
### 3.4) GlassFish 5.1.0 (Web Profile)   
**Variable de entorno:**   
- `GLASSFISH\_HOME` =   
    `C:\SistemasDistribuidos\Programas\glassfish5\glassfish`   
   
Añadir al `Path`:   
- `%GLASSFISH\_HOME%\bin`   
   
**Arranque:**   
```

startserv


```
Comprobar en navegador:   
```

http://localhost:8080


```
![image](files\image_t.png)    
Como aparece esta pantalla, podemos decir que también está correctamente configurado.   
**Parada:**   
```

stopserv

```
 --- 
## 4) Eclipse IDE for Enterprise Java and Web Developers (2022-12)   
### 4.1 Arranque y workspace   
- Ejecutar `…\Programas\eclipse\eclipse.exe`    
- Seleccionar un **workspace** , por ejemplo:   
    -\`C:\Users\Usuario\eclipse-workspace\`   
![Captura de pantalla 2026-02-16 160627](files\captura-de-pantalla-2026-02-16-160627.png)    
   
### 4.2 Asegurar que Eclipse usa el JDK correcto   
En Eclipse:   
-\`Window > Preferences > Java > Installed JREs\`   
![Captura de pantalla 2026-02-16 160743](files\captura-de-pantalla-2026-02-16-160743.png)    
- Eclipse usa por defecto el jdk correcto, así que no hay que cambiar nada aquí:

   
![image](files\image_7.png)    
 --- 
   
## 5) Integración de GlassFish en Eclipse con GlassFish Tools   
> Eclipse JEE integra Ant y Maven, pero para GlassFish hay que instalar herramientas adicionales.   

### 5.1 Instalar “Eclipse Glassfish Tools”   
En Eclipse:   
-\`Help > Install New Software...\`   
- En **Work with:**  hy que pegar:   
    -\`http://download.oracle.com/otn\_software/oepe/12.2.1.8/oxygen/repository/dependencies/\`   
![image](files\image_3.png)    
- Marcar **Eclipse Glassfish Tools**    
   
-\`Next > Next > Accept > Finish\`   
- Reiniciar Eclipse.   
   
### 5.2 Crear servidor GlassFish en la vista Servers   
En Eclipse:   
-\`Window > Show View > Servers\`   
- En la vista \*\*Servers\*\*:   
    -\`No servers are available. Click this link to create a new server...\`   
- Seleccionar:   
    -\`GlassFish > GlassFish 5\`   
- En la pantalla de rutas:   
    -\*\*GlassFish location\*\*: \`...\Programas\glassfish5\glassfish\`   
    -\*\*Java location\*\*: \`...\Java\jdk1.8.0\_361\`   
   
-\`Next\`  → `\`Finis`h\`   
Verificación:   
- Botón derecho sobre el server → \`Start\`   
- Navegador: `http://localhost:8080`    
![GlassFish](files\glassfish.png)    
   
Como se ve en la captura, el servidor está ya operativo y funcionando correctamente.   
 --- 
## 6) Integración de Tomcat en Eclipse   
En Eclipse:   
- Vista \*\*Servers\*\* → crear nuevo server   
   
-\`Apache > Tomcat v10.0 Server\`   
- Ruta de instalación: `\`…\Programas\apache-tomcat-10.0.2`7\`   
![image](files\image_p.png)    
   
-\`Finish\`   
Verificación:   
-\`Start\`   
- Navegador: \`http://localhost:8080\`   
![TomCat](files\tomcat.png)    
   
Funciona correctamente, ya que, el acceso a `http://localhost:8080/` devuelve **HTTP 404**, **cosa esperada** porque no hay módulo publicado en `/`.    
 --- 
## 7) Notas y troubleshooting rápido   
-\*\*Puerto 8080 ocupado\*\*: si Tomcat está encendido, GlassFish fallará y viceversa.    
Para cambiar puertos, se puede hacer:   
- Tomcat: \`...\apache-tomcat-10.0.27\conf\server.xml\` (Connector port="8080")   
- GlassFish: suele cambiarse con `\`asadmin set server.http-service.http-listener.http-listener-1.port=XXX`X\` y reiniciar el dominio.   
