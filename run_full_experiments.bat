@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

:: ── Compile ───────────────────────────────────────────────────────────
call compile.bat
if errorlevel 1 (
    echo COMPILE FAILED
    exit /b 1
)

:: ── Prepare directories ──────────────────────────────────────────────
if not exist tmp_configs mkdir tmp_configs
if not exist reports mkdir reports

set CSV=reports\full_results.csv
echo protocol,scenario,nodes,seed,created,delivered,delivery_prob,overhead_ratio,latency_avg,hopcount_avg,relayed> %CSV%

set JAVA_OPTS=-Xmx1G
set CP=target;lib/ECLA.jar;lib/DTNConsoleConnection.jar

:: ══════════════════════════════════════════════════════════════════════
:: PART 1: Base comparison — 3 protocols x 5 seeds x 3 node counts
:: Flattened via subroutine to avoid CMD nested-for parsing bugs
:: ══════════════════════════════════════════════════════════════════════
echo.
echo ===== PART 1: Base comparison (3 protocols x 5 seeds x 3 node counts) =====

for %%S in (1 2 3 4 5) do (
    call :run_base_seed %%S
)

:: ══════════════════════════════════════════════════════════════════════
:: PART 2: Sybil scenario (PoDC only, default nodes, 5 seeds)
:: ══════════════════════════════════════════════════════════════════════
echo.
echo ===== PART 2: Sybil 20%% scenario =====

for %%S in (1 2 3 4 5) do (
    set LABEL=PoDC_sybil_s%%S
    set TMPF=tmp_configs\!LABEL!.txt

    echo Scenario.name = !LABEL!> "!TMPF!"
    echo MovementModel.rngSeed = %%S>> "!TMPF!"

    echo Running !LABEL! ...
    java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt scenarios\sybil_20pct.txt "!TMPF!"

    set RPT=reports\!LABEL!_MessageStatsReport.txt
    if exist "!RPT!" (
        call :parse_report "PoDC" "sybil20" "126" "%%S" "!RPT!"
    )
)

echo.
echo ===== ALL EXPERIMENTS COMPLETE =====
echo Results in: %CSV%
goto :eof

:: ── Subroutine: run all protocols and node counts for one seed ──────
:run_base_seed
set _S=%1
for %%P in (PoDC Epidemic Prophet) do (
    for %%N in (50 100 150) do (
        set LABEL=%%P_n%%N_s!_S!
        set TMPF=tmp_configs\!LABEL!.txt

        echo Scenario.name = !LABEL!> "!TMPF!"
        echo MovementModel.rngSeed = !_S!>> "!TMPF!"

        set /a G1=%%N / 3
        set /a G2=%%N / 3
        set /a G3=%%N - !G1! - !G2!
        echo Group1.nrofHosts = !G1!>> "!TMPF!"
        echo Group2.nrofHosts = !G2!>> "!TMPF!"
        echo Group3.nrofHosts = !G3!>> "!TMPF!"

        set /a TOTAL=!G1! + !G2! + !G3! + 6
        echo Events1.hosts = 0,!TOTAL!>> "!TMPF!"

        if "%%P"=="PoDC" set SCEN=scenarios\base_podc.txt
        if "%%P"=="Epidemic" set SCEN=scenarios\base_epidemic.txt
        if "%%P"=="Prophet" set SCEN=scenarios\base_prophet.txt

        echo Running !LABEL! ...
        java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt !SCEN! "!TMPF!"

        set RPT=reports\!LABEL!_MessageStatsReport.txt
        if exist "!RPT!" (
            call :parse_report "%%P" "base" "%%N" "!_S!" "!RPT!"
        ) else (
            echo WARNING: !RPT! not found
        )
    )
)
goto :eof

:: ── Subroutine: parse MessageStatsReport and append to CSV ──────────
:parse_report
set _proto=%~1
set _scen=%~2
set _nodes=%~3
set _seed=%~4
set _file=%~5

set _created=
set _delivered=
set _dprob=
set _overhead=
set _latency=
set _hops=
set _relayed=

for /f "tokens=1,2 delims=: " %%A in ('type "%_file%"') do (
    if "%%A"=="created" set _created=%%B
    if "%%A"=="delivered" set _delivered=%%B
    if "%%A"=="delivery_prob" set _dprob=%%B
    if "%%A"=="overhead_ratio" set _overhead=%%B
    if "%%A"=="latency_avg" set _latency=%%B
    if "%%A"=="hopcount_avg" set _hops=%%B
    if "%%A"=="relayed" set _relayed=%%B
)

echo %_proto%,%_scen%,%_nodes%,%_seed%,%_created%,%_delivered%,%_dprob%,%_overhead%,%_latency%,%_hops%,%_relayed%>> %CSV%
goto :eof
