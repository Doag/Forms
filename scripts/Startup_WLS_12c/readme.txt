Zur Verwendung sind folgende Hinweise zu beachten:

(1)    Die Infrastruktur-Datenbank muss vorab gestartet werden, wenn der Admin Server verwendet werden soll. Der Start der DB ist nicht Teil des Scriptes.
(2)    Relevante Properties müssen vorab in der Datei domain.properties gesetzt werden
(3)    Das Script StartFMW_12212.py zusammen mit der Datei domain.properties in ein gemeinsames Verzeichnis kopieren.
(4)    Vor dem Ausführen des Scriptes muss die Umgebung gesetzt werde. Dazu bitte die Datei ..\<domain_home>\bin\setDomainEnv.cmd ausführen
(5)    Die zur Ausführung des Scriptes StartFMW_12212.py notwendige Python-Umgebung steht in der Installation von FMW 12c unter WLST zur Verfügung
        Aufruf des Scriptes aus dem gemeinsamen Verzeichnis mit ..\<Oracle_Home>\oracle_common\common\bin\wlst StartFMW_12212.py
(6)    Alle Komponenten der Fusion Middleware werden über den Node Manager im SSL-Modus gestartet und verwaltet.
(7)    Die Protokollierung erfolgt in den out-Dateien der jeweiligen Server.


