# Proceso de instalación de las herramientas   
> Basada en la “Guía de configuración de desarrollo para JEE (v23)” de la UBU (Feb 2023).   

> **Objetivo:** dejar funcionando JDK 8u361 + Ant + Maven + Tomcat 10.0.27 + GlassFish 5.1.0 (Web) + Eclipse JEE 2022-12, con verificación por consola y con integración de servidores en Eclipse.   

 --- 
## 0) Datos de mi equipo   
-**Usuario Windows:**`Usuario`   
-**Sistema operativo:**`Windows(64-bit)`   
-**Ruta base para descargas y extracciones:** `C:\SistemasDistribuidos `    
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
-`...\Java\jdk1.8.0\_361\bin\javac.exe`   
Si NO existe, habría que reinstalar el JDK con el instalador oficial y elegir como ruta de instalación la carpeta: `…\Java\jdk1.8.0\_361` .   
### 2.2 Crear variables de entorno persistentes   
Abrir:   
-**Win + R** → `sysdm.cpl` → pestaña **Opciones avanzadas** → **Variables de entorno…**   

<img width="396" height="205" alt="captura-de-pantalla-2026-02-16-115001" src="https://github.com/user-attachments/assets/c55a8816-d878-4357-af4e-35ba1789a1ee" />

<img width="408" height="483" alt="captura-de-pantalla-2026-02-16-115140" src="https://github.com/user-attachments/assets/7357cf42-c82c-487e-9977-dc2b5ef3c695" />
  
<img width="615" height="580" alt="captura-de-pantalla-2026-02-16-115235" src="https://github.com/user-attachments/assets/6a267b26-26f1-4652-8ada-cb7f7ef6cff4" />

Crear en **Variables de sistema** :   
-`JAVA\_HOME` =` C:\SistemasDistribuidos\Java\jdk1.8.0\_361`    
Editar `Path` y **añadir**:   
-`%JAVA\_HOME%\bin`   

<img width="524" height="499" alt="captura-de-pantalla-2026-02-16-115614" src="https://github.com/user-attachments/assets/17645273-1850-4f02-a5f9-7bcf6a177d55" />

### 2.3 Verificación por consola   
Cerrar y abrir una consola nueva y ejecutar:   
```bat   
javac -version   
java -version   
echo%JAVA\_HOME%   
```   
<img width="517" height="168" alt="image_p" src="https://github.com/user-attachments/assets/eeda5107-e88f-4745-96d5-ac7ed1fe40a2" />

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

```bat
ant -version
echo %ANT\_HOME%   
```

<img width="465" height="101" alt="image_4" src="https://github.com/user-attachments/assets/960c096e-3899-4d1f-9d60-ee07942dd9f5" />

Nos salen las versiones y directorios correctos, así que está correctamente configurado.   
 --- 
### 3.2) Apache Maven 3.8.7   
**Variable de entorno:**   
- `M2\_HOME` = `C:\SistemasDistribuidos\Programas\apache-maven-3.8.7`   

Añadir al `Path`:   
- `%M2\_HOME%\bin`   
   
**Verificación:**   
```bat
mvn -version
echo %M2_HOME%
```

<img width="874" height="148" alt="image_5" src="https://github.com/user-attachments/assets/dd64548b-a69f-436d-8f67-a462888c8873" />

La versión y el directorio son correctos, así que está correctamente configurado.   
 --- 
### 3.3) Apache Tomcat 10.0.27   
Tomcat y GlassFish utilizan por defecto el puerto 8080.   
No deben ejecutarse simultáneamente.   
**Variable de entorno:**   
- `CATALINA\_HOME` = `C:\SistemasDistribuidos\Programas\apache-tomcat-10.0.27`   
   
Añadir al `Path`:   
- `%CATALINA\_HOME%\bin`   
   
**Arranque:**   
```bat
startup.bat
```
Comprobar en navegador:   
```
http://localhost:8080
```

<img width="1366" height="768" alt="image_n" src="https://github.com/user-attachments/assets/4f9b9ef8-f8d5-483d-ab2c-a944633564c0" />

Nos sale la página principal de TomCat, así que está configurado correctamente.   
**Parada:**   
```bat
shutdown.bat
```
 --- 
### 3.4) GlassFish 5.1.0 (Web Profile)   
**Variable de entorno:**   
- `GLASSFISH\_HOME` = `C:\SistemasDistribuidos\Programas\glassfish5\glassfish`   
   
Añadir al `Path`:   
- `%GLASSFISH\_HOME%\bin`   
   
**Arranque:**   
```bat
startserv
```
Comprobar en navegador:   
```
http://localhost:8080
```

<img width="1366" height="479" alt="image_g" src="https://github.com/user-attachments/assets/4257e525-57f5-4663-bcd4-72ad6779dd88" />

Como aparece esta pantalla, podemos decir que también está correctamente configurado.   
**Parada:**   
```bat
stopserv
```
 --- 
## 4) Eclipse IDE for Enterprise Java and Web Developers (2022-12)   
### 4.1 Arranque y workspace   
- Ejecutar `…\Programas\eclipse\eclipse.exe`    
- Seleccionar un **workspace** , por ejemplo:   
    -`C:\Users\Usuario\eclipse-workspace`

<img width="619" height="272" alt="captura-de-pantalla-2026-02-16-160627" src="https://github.com/user-attachments/assets/e8dbc586-8186-4b2d-8b5a-25e9b5baa6cd" />
   
### 4.2 Asegurar que Eclipse usa el JDK correcto   
En Eclipse:   
-`Window > Preferences > Java > Installed JREs`   

<img width="164" height="255" alt="captura-de-pantalla-2026-02-16-160743" src="https://github.com/user-attachments/assets/4111b112-d293-49c9-9548-cf29aa49146d" />

- Eclipse usa por defecto el jdk correcto, así que no hay que cambiar nada aquí:

<img width="1179" height="241" alt="image_t" src="https://github.com/user-attachments/assets/ec6a96c1-e3bc-4fb7-9a4f-10e99189ae78" />

 --- 
   
## 5) Integración de GlassFish en Eclipse con GlassFish Tools   
> Eclipse JEE integra Ant y Maven, pero para GlassFish hay que instalar herramientas adicionales.   

### 5.1 Instalar “Eclipse Glassfish Tools”   
En Eclipse:   
-`Help > Install New Software...`   
- En **Work with:**  hy que pegar:   
    -`http://download.oracle.com/otn\_software/oepe/12.2.1.8/oxygen/repository/dependencies/`
  
<img width="912" height="386" alt="image" src="https://github.com/user-attachments/assets/ae15ec2a-9200-4d9d-9256-d0dbb6968642" />

- Marcar **Eclipse Glassfish Tools**    
   
-`Next > Next > Accept > Finish`   
- Reiniciar Eclipse.   
   
### 5.2 Crear servidor GlassFish en la vista Servers   
En Eclipse:   
-`Window > Show View > Servers`   
- En la vista **Servers**:   
    -`No servers are available. Click this link to create a new server...`   
- Seleccionar:   
    -`GlassFish > GlassFish 5`   
- En la pantalla de rutas:   
    -**GlassFish location**: `...\Programas\glassfish5\glassfish`   
    -**Java location**: `...\Java\jdk1.8.0\_361`   
   
-`Next`  → `Finish`   
Verificación:   
- Botón derecho sobre el server → `Start`   
- Navegador: `http://localhost:8080`

<img width="1366" height="620" alt="glassfish" src="https://github.com/user-attachments/assets/0746a228-98f9-4a63-9fdf-72be31259e63" />
 
Como se ve en la captura, el servidor está ya operativo y funcionando correctamente.   
 --- 
## 6) Integración de Tomcat en Eclipse   
En Eclipse:   
- Vista **Servers** → crear nuevo server   
   
-`Apache > Tomcat v10.0 Server`   
- Ruta de instalación: `…\Programas\apache-tomcat-10.0.27`

<img width="509" height="681" alt="image_w" src="https://github.com/user-attachments/assets/fdb1ff50-30b4-4686-8424-21ee0a9d856c" />
   
-`Finish`   
Verificación:   
-`Start`   
- Navegador: `http://localhost:8080`
 
<img width="1366" height="767" alt="tomcat" src="https://github.com/user-attachments/assets/ad29789d-1e8a-4f29-9f32-a5b68c7f4a90" />
    
    Funciona correctamente, ya que, el acceso a `http://localhost:8080/` devuelve **HTTP 404**, **cosa esperada** porque no hay módulo publicado en `/`.    
 --- 
## 7) Notas y troubleshooting rápido   
-**Puerto 8080 ocupado**: si Tomcat está encendido, GlassFish fallará y viceversa.    
Para cambiar puertos, se puede hacer:   
- Tomcat: `...\apache-tomcat-10.0.27\conf\server.xml` (Connector port="8080")   
- GlassFish: suele cambiarse con `asadmin set server.http-service.http-listener.http-listener-1.port=XXXX` y reiniciar el dominio.   
