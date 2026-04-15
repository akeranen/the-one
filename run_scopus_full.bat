@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

set JAVA_OPTS=-Xmx1G
set CP=target;lib/ECLA.jar;lib/DTNConsoleConnection.jar
set CSV=reports\full_results.csv

if not exist tmp_configs mkdir tmp_configs
if not exist reports mkdir reports

echo protocol,scenario,nodes,seed,created,delivered,delivery_prob,overhead_ratio,latency_avg,hopcount_avg,relayed> %CSV%

:: ── Total run count ─────────────────────────────────────────────────
:: Part 1: 7 protocols x 3 sizes x 5 seeds = 105
:: Part 2: 5 sybil
:: Part 3: 5 n126 clean
:: Part 4: 2 thresholds x 5 seeds = 10
:: Total = 125
set TOTAL=125
set /a DONE=0

:: Store start time (seconds since midnight)
call :time_to_secs START_SECS

echo.
echo ============================================================
echo   PoDC Scopus Experiments: !TOTAL! total simulation runs
echo ============================================================
echo.

:: ══════════════════════════════════════════════════════════════════════
:: PART 1: Base comparison — 7 protocols x 5 seeds x 3 node counts
:: ══════════════════════════════════════════════════════════════════════

for %%S in (1 2 3 4 5) do (
    call :run_base_seed %%S
)

:: ══════════════════════════════════════════════════════════════════════
:: PART 2: Sybil scenario (PoDC only, default nodes, 5 seeds)
:: ══════════════════════════════════════════════════════════════════════

for %%S in (1 2 3 4 5) do (
    set LABEL=PoDC_sybil_s%%S
    set TMPF=tmp_configs\!LABEL!.txt

    echo Scenario.name = !LABEL!> "!TMPF!"
    echo MovementModel.rngSeed = %%S>> "!TMPF!"

    java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt scenarios\sybil_20pct.txt "!TMPF!"

    set RPT=reports\!LABEL!_MessageStatsReport.txt
    if exist "!RPT!" call :parse_report "PoDC" "sybil20" "126" "%%S" "!RPT!"
    set /a DONE+=1
    call :show_progress "!LABEL!"
)

:: ══════════════════════════════════════════════════════════════════════
:: PART 3: PoDC N=126 clean (for fair Sybil comparison)
:: ══════════════════════════════════════════════════════════════════════

for %%S in (1 2 3 4 5) do (
    set LABEL=PoDC_n126_clean_s%%S
    set TMPF=tmp_configs\!LABEL!.txt

    echo Scenario.name = !LABEL!> "!TMPF!"
    echo MovementModel.rngSeed = %%S>> "!TMPF!"

    java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt scenarios\base_podc.txt "!TMPF!"

    set RPT=reports\!LABEL!_MessageStatsReport.txt
    if exist "!RPT!" call :parse_report "PoDC" "base" "126" "%%S" "!RPT!"
    set /a DONE+=1
    call :show_progress "!LABEL!"
)

:: ══════════════════════════════════════════════════════════════════════
:: PART 4: dropThreshold sensitivity (PoDC, N=100, thresholds 0.3/0.7)
:: ══════════════════════════════════════════════════════════════════════

for %%D in (0.3 0.7) do (
    for %%S in (1 2 3 4 5) do (
        set LABEL=PoDC_drop%%D_s%%S
        set TMPF=tmp_configs\!LABEL!.txt

        echo Scenario.name = !LABEL!> "!TMPF!"
        echo MovementModel.rngSeed = %%S>> "!TMPF!"
        echo Group1.nrofHosts = 33>> "!TMPF!"
        echo Group2.nrofHosts = 33>> "!TMPF!"
        echo Group3.nrofHosts = 34>> "!TMPF!"
        echo Events1.hosts = 0,106>> "!TMPF!"
        echo Group.PoDCRouter.dropThreshold = %%D>> "!TMPF!"

        java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt scenarios\base_podc.txt "!TMPF!"

        set RPT=reports\!LABEL!_MessageStatsReport.txt
        if exist "!RPT!" call :parse_report "PoDC" "drop%%D" "100" "%%S" "!RPT!"
        set /a DONE+=1
        call :show_progress "!LABEL!"
    )
)

echo.
echo ============================================================
echo   ALL !TOTAL! EXPERIMENTS COMPLETE
echo   Results in: %CSV%
echo ============================================================
goto :eof

:: ══════════════════════════════════════════════════════════════════════
:: Subroutine: run all protocols and node counts for one seed
:: ══════════════════════════════════════════════════════════════════════
:run_base_seed
set SEED=%1
for %%P in (PoDC Epidemic Prophet SprayAndWait DirectDelivery FirstContact MaxProp) do (
    for %%N in (50 100 150) do (
        set LABEL=%%P_n%%N_s!SEED!
        set TMPF=tmp_configs\!LABEL!.txt

        echo Scenario.name = !LABEL!> "!TMPF!"
        echo MovementModel.rngSeed = !SEED!>> "!TMPF!"

        set /a G1=%%N / 3
        set /a G2=%%N / 3
        set /a G3=%%N - !G1! - !G2!
        echo Group1.nrofHosts = !G1!>> "!TMPF!"
        echo Group2.nrofHosts = !G2!>> "!TMPF!"
        echo Group3.nrofHosts = !G3!>> "!TMPF!"

        set /a TOTAL_H=!G1! + !G2! + !G3! + 6
        echo Events1.hosts = 0,!TOTAL_H!>> "!TMPF!"

        if "%%P"=="PoDC"           set SCEN=scenarios\base_podc.txt
        if "%%P"=="Epidemic"       set SCEN=scenarios\base_epidemic.txt
        if "%%P"=="Prophet"        set SCEN=scenarios\base_prophet.txt
        if "%%P"=="SprayAndWait"   set SCEN=scenarios\base_sprayandwait.txt
        if "%%P"=="DirectDelivery" set SCEN=scenarios\base_directdelivery.txt
        if "%%P"=="FirstContact"   set SCEN=scenarios\base_firstcontact.txt
        if "%%P"=="MaxProp"        set SCEN=scenarios\base_maxprop.txt

        java %JAVA_OPTS% -cp %CP% core.DTNSim -b 1 default_settings.txt !SCEN! "!TMPF!"

        set RPT=reports\!LABEL!_MessageStatsReport.txt
        if exist "!RPT!" (
            call :parse_report "%%P" "base" "%%N" "!SEED!" "!RPT!"
        ) else (
            echo WARNING: !RPT! not found
        )
        set /a DONE+=1
        call :show_progress "!LABEL!"
    )
)
goto :eof

:: ══════════════════════════════════════════════════════════════════════
:: Subroutine: show progress bar with ETA
:: ══════════════════════════════════════════════════════════════════════
:show_progress
set _run_name=%~1

call :time_to_secs NOW_SECS

set /a ELAPSED=!NOW_SECS! - !START_SECS!
if !ELAPSED! lss 0 set /a ELAPSED+=86400

set /a PCT=!DONE! * 100 / !TOTAL!

:: Build progress bar (30 chars wide)
set /a FILLED=!DONE! * 30 / !TOTAL!
set /a EMPTY=30 - !FILLED!
set "BAR="
for /l %%i in (1,1,!FILLED!) do set "BAR=!BAR!#"
for /l %%i in (1,1,!EMPTY!) do set "BAR=!BAR!-"

:: Calculate ETA
if !DONE! gtr 0 (
    set /a AVG_PER_RUN=!ELAPSED! / !DONE!
    set /a REMAINING=!TOTAL! - !DONE!
    set /a ETA_SECS=!AVG_PER_RUN! * !REMAINING!
) else (
    set ETA_SECS=0
)

:: Format elapsed time
set /a E_MIN=!ELAPSED! / 60
set /a E_SEC=!ELAPSED! %% 60
if !E_SEC! lss 10 (set "E_FMT=!E_MIN!:0!E_SEC!") else (set "E_FMT=!E_MIN!:!E_SEC!")

:: Format ETA
set /a R_MIN=!ETA_SECS! / 60
set /a R_SEC=!ETA_SECS! %% 60
if !R_SEC! lss 10 (set "R_FMT=!R_MIN!:0!R_SEC!") else (set "R_FMT=!R_MIN!:!R_SEC!")

echo   [!BAR!] !DONE!/!TOTAL! (!PCT!%%)  elapsed !E_FMT!  ETA ~!R_FMT!  ^| !_run_name!
goto :eof

:: ══════════════════════════════════════════════════════════════════════
:: Subroutine: convert current %TIME% to seconds since midnight
:: ══════════════════════════════════════════════════════════════════════
:time_to_secs
set "_T=%TIME: =0%"
set /a "_H=1%_T:~0,2% - 100"
set /a "_M=1%_T:~3,2% - 100"
set /a "_S=1%_T:~6,2% - 100"
set /a "%1=!_H! * 3600 + !_M! * 60 + !_S!"
goto :eof

:: ══════════════════════════════════════════════════════════════════════
:: Subroutine: parse MessageStatsReport and append to CSV
:: ══════════════════════════════════════════════════════════════════════
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
