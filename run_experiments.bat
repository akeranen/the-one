@echo off
setlocal enabledelayedexpansion

REM ===================================================================
REM  PoDC experiment runner
REM
REM  Compiles, then executes batch simulations across scenarios,
REM  node counts, weight combinations, and repeat seeds.
REM  Results are appended to reports\podc_results.csv automatically.
REM ===================================================================

set RUNS=10
set JAVA_OPTS=-Xmx512M
set CP=target;lib/ECLA.jar;lib/DTNConsoleConnection.jar

REM ── 0. compile ─────────────────────────────────────────────────────
echo [*] Compiling...
call compile.bat
if errorlevel 1 (
    echo [!] Compilation failed.
    exit /b 1
)

REM ── Create temp dir for generated configs ──────────────────────────
if not exist "tmp_configs" mkdir tmp_configs
if not exist "reports"     mkdir reports

REM ── 1. Define scenarios ────────────────────────────────────────────
set SCENARIOS=base_podc base_epidemic base_prophet energy_limited sybil_20pct disconnect
REM ── 2. Node counts ────────────────────────────────────────────────
set NODE_COUNTS=50 100 150
REM ── 3. Alpha-Beta-Gamma combos (only meaningful for PoDC/Sybil) ──
set ABG[0]=0.6,0.3,0.1
set ABG[1]=0.8,0.1,0.1
set ABG[2]=0.4,0.4,0.2
set ABG[3]=0.5,0.3,0.2
set ABG[4]=0.7,0.2,0.1
set ABG_COUNT=5

echo [*] Starting experiment grid: %RUNS% repeats per configuration
echo.

for %%S in (%SCENARIOS%) do (
    for %%N in (%NODE_COUNTS%) do (
        REM Only vary ABG for PoDC-family scenarios
        set "DO_ABG=0"
        if "%%S"=="base_podc"       set "DO_ABG=1"
        if "%%S"=="energy_limited"  set "DO_ABG=1"
        if "%%S"=="sybil_20pct"     set "DO_ABG=1"
        if "%%S"=="disconnect"      set "DO_ABG=1"

        if "!DO_ABG!"=="1" (
            for /L %%A in (0,1,4) do (
                call :RUN_COMBO %%S %%N %%A
            )
        ) else (
            call :RUN_COMBO %%S %%N -1
        )
    )
)

echo.
echo [*] All experiments complete. Results in reports\podc_results.csv
goto :EOF

REM ── subroutine: run one scenario+nodes+abg combo ──────────────────
:RUN_COMBO
set "SC=%~1"
set "NODES=%~2"
set "ABG_IDX=%~3"

REM Build scenario label
if "%ABG_IDX%"=="-1" (
    set "LABEL=%SC%_n%NODES%"
) else (
    set "LABEL=%SC%_n%NODES%_abg%ABG_IDX%"
)

echo [*] %LABEL% (%RUNS% runs)

REM Write temp overlay config
set "TMP=tmp_configs\%LABEL%.txt"
echo Scenario.name = %LABEL%> "!TMP!"
echo Group.nrofHosts = %NODES%>> "!TMP!"

if not "%ABG_IDX%"=="-1" (
    for /F "tokens=1,2,3 delims=," %%a in ("!ABG[%ABG_IDX%]!") do (
        echo Group.PoDCRouter.alpha = %%a>> "!TMP!"
        echo Group.PoDCRouter.beta = %%b>> "!TMP!"
        echo Group.PoDCRouter.gamma = %%c>> "!TMP!"
    )
)

REM Run the batch  ( -b RUNS  default_settings.txt  scenario_overlay  tmp_overlay )
java %JAVA_OPTS% -cp %CP% core.DTNSim -b %RUNS% default_settings.txt scenarios\%SC%.txt "!TMP!"

goto :EOF
