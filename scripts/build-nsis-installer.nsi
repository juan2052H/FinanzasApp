; FinanzasApp Windows Installer NSIS
; Script NSIS para generar un instalador profesional

!include "MUI2.nsh"

; Define el nombre de la aplicación y el instalador
!define APPNAME "FinanzasApp"
!define APPVERSION "1.0.0"
!define APPEXE "FinanzasApp.exe"
!define COMPANYNAME "FinanzasApp"
!define APPPATH "..\..\dist\FinanzasApp"

; Información del instalador
Name "${APPNAME} ${APPVERSION}"
OutFile "..\..\dist\FinanzasApp-${APPVERSION}-Installer.exe"
InstallDir "$PROGRAMFILES\${APPNAME}"

; Requiere permisos de administrador
RequestExecutionLevel admin

; Configuración de interfaz
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Spanish"

; Sección de instalación
Section "Install"
  ; Crear directorio de instalación
  SetOutPath "$INSTDIR"
  
  ; Copiar archivos
  File /r "${APPPATH}\*.*"
  
  ; Crear acceso directo en el Menú Inicio
  CreateDirectory "$SMPROGRAMS\${APPNAME}"
  CreateShortcut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" "$INSTDIR\${APPEXE}"
  CreateShortcut "$SMPROGRAMS\${APPNAME}\Desinstalar ${APPNAME}.lnk" "$INSTDIR\uninstall.exe"
  
  ; Crear acceso directo en el Escritorio
  CreateShortcut "$DESKTOP\${APPNAME}.lnk" "$INSTDIR\${APPEXE}"
  
  ; Registrar en Agregar/Quitar Programas
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME} ${APPVERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayVersion" "${APPVERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "${COMPANYNAME}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\${APPEXE}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$INSTDIR\uninstall.exe"
  
  ; Crear desinstalador
  WriteUninstaller "$INSTDIR\uninstall.exe"
SectionEnd

; Sección de desinstalación
Section "Uninstall"
  ; Eliminar archivos
  RMDir /r "$INSTDIR"
  
  ; Eliminar accesos directos
  RMDir /r "$SMPROGRAMS\${APPNAME}"
  Delete "$DESKTOP\${APPNAME}.lnk"
  
  ; Eliminar entrada del registro
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
SectionEnd
