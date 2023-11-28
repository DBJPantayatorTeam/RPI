# RPI
Servidor per utilitzar Pantayator.

## Video:
  [![Video promocional](https://img.youtube.com/vi/-N1VcjdA8EM/0.jpg)](https://youtu.be/-N1VcjdA8EM)
  
# Inici Ràpid: Servidor amb Raspberry Pi

## Requisits Previs

1. **Maquinari:**
   - Raspberry Pi connectada a la xarxa.
   - Pantalla connectada a la Raspberry Pi.

2. **Programari:**
   - [Especificacions del Panell](https://github.com/hzeller/rpi-rgb-led-matrix).

## Passos per Iniciar el Servidor

### 1. Clonar el Projecte
```bash
git clone https://github.com/EL_TEU_USUARI/EL_TEU_PROJECTE.git
cd EL_TEU_PROJECTE/server_java
```
### 2. Netegar i Compilar
```bash
mvn clean
mvn compile
```
### 3. Iniciar el Servidor
#### A Windows:
Executa la següent ordre en PowerShell:
```bash
.\run.ps1 com.project.Main
```
#### A Linux o MacOs:
Executa la següent ordre en PowerShell:
```bash
./run.sh com.project.Main
```
El teu servidor hauria de funcionar ara.

### Notes adicionals
Per obtenir més informació sobre el panell, consulta les [Especificacions de la pantalla](https://github.com/hzeller/rpi-rgb-led-matrix)
Assegura't de tenir els permisos necessaris per executar els scripts.
