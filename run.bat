@echo off
:: Use javaw (not java) — javaw is the windowless GUI launcher.
:: It never opens a console window, even if double-clicked.
if exist VpsManager.jar (
    start "" javaw -jar VpsManager.jar
) else (
    start "" javaw -cp lib\flatlaf-3.5.4.jar;bin com.vpsmanager.App
)
