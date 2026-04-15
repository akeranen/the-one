@echo off
setlocal enabledelayedexpansion

REM ===================================================================
REM  Run 3 protocols x 5 seeds for comparative analysis.
REM  Parses MessageStatsReport into a single comparison_results.csv.
REM ===================================================================

set JAVA_OPTS=-Xmx512M
set CP=target;lib/ECLA.jar;lib/DTNConsoleConnection.jar
set SEEDS=1 2 3 4 5

echo [*] Compiling...
call compile.bat
if errorlevel 1 ( echo [!] Compilation failed. & exit /b 1 )

if not exist "tmp_configs" mkdir tmp_configs
if not exist "reports"     mkdir reports

REM Write CSV header
set "CSV=reports\comparison_results.csv"
echo protocol,seed,created,delivered,delivery_ratio,overhead_ratio,latency_avg,hopcount_avg,relayed> "!CSV!"

REM ── protocols and their scenario files ────────────────────────────
set "PROTO[0]=PoDC"
set "SCENE[0]=scenarios\base_podc.txt"
set "PROTO[1]=Epidemic"
set "SCENE[1]=scenarios\base_epidemic.txt"
set "PROTO[2]=Prophet"
set "SCENE[2]=scenarios\base_prophet.txt"

for /L %%P in (0,1,2) do (
    for %%S in (%SEEDS%) do (
        set "PNAME=!PROTO[%%P]!"
        set "SFILE=!SCENE[%%P]!"
        set "LABEL=!PNAME!_seed%%S"
        echo [*] Running !LABEL! ...

        REM Generate temp overlay with unique name and seed
        set "TMP=tmp_configs\!LABEL!.txt"
        echo Scenario.name = !LABEL!> "!TMP!"
        echo MovementModel.rngSeed = %%S>> "!TMP!"

        REM Run simulation (batch mode, 1 run)
        java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt "!SFILE!" "!TMP!"

        REM Parse the MessageStatsReport output
        set "RPT=reports\!LABEL!_MessageStatsReport.txt"
        if exist "!RPT!" (
            call :PARSE_REPORT "!RPT!" "!PNAME!" "%%S"
        ) else (
            echo [!] Report not found: !RPT!
        )
    )
)

echo.
echo [*] All runs complete. Results in !CSV!
echo [*] Now run: python build_charts.py
goto :EOF

REM ── subroutine: parse one MessageStatsReport ──────────────────────
:PARSE_REPORT
set "RPT_FILE=%~1"
set "PNAME=%~2"
set "SEED=%~3"

set "V_CREATED=0"
set "V_DELIVERED=0"
set "V_DPROB=0"
set "V_OVERHEAD=NaN"
set "V_LATENCY=0"
set "V_HOPS=0"
set "V_RELAYED=0"

for /F "tokens=1,2 delims=: " %%a in ('type "%RPT_FILE%"') do (
    if "%%a"=="created"        set "V_CREATED=%%b"
    if "%%a"=="delivered"      set "V_DELIVERED=%%b"
    if "%%a"=="delivery_prob"  set "V_DPROB=%%b"
    if "%%a"=="overhead_ratio" set "V_OVERHEAD=%%b"
    if "%%a"=="latency_avg"    set "V_LATENCY=%%b"
    if "%%a"=="hopcount_avg"   set "V_HOPS=%%b"
    if "%%a"=="relayed"        set "V_RELAYED=%%b"
)

echo %PNAME%,%SEED%,%V_CREATED%,%V_DELIVERED%,%V_DPROB%,%V_OVERHEAD%,%V_LATENCY%,%V_HOPS%,%V_RELAYED%>> "!CSV!"
goto :EOF
