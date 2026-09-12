@echo off
REM
REM API-CHEK 启动脚本（Windows）
REM
REM 用法:
REM   bin\api-check.cmd --help
REM   bin\api-check.cmd run -c apis.yaml
REM
REM 说明: 本脚本只负责「切 UTF-8 代码页 + 找到 jar + 用正确的编码启动 JVM」，不含任何业务逻辑。
setlocal

REM 切到 UTF-8 代码页。报告含中文与 emoji，Windows 默认 GBK(936) 下会显示成 "?"。
chcp 65001 >nul

REM 脚本位于 <root>\bin\ 下，%~dp0 自带结尾反斜杠。
for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"
set "JAR=%ROOT_DIR%\target\api-check.jar"

if not exist "%JAR%" (
  echo 未找到 "%JAR%" 1>&2
  echo 请先在项目根目录执行: mvnw.cmd package 1>&2
  exit /b 2
)

REM 优先使用 JAVA_HOME 下的 java：PATH 上的 java 可能是低版本，会报 UnsupportedClassVersionError。
if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

"%JAVA%" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%JAR%" %*
exit /b %ERRORLEVEL%
