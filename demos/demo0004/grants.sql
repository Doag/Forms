rem Anmeldung von SYS als SYSDBA erforderlich
rem z.B. sqlplus sys/<PASSWD>@<CONNECT_STRING> as sysdba

grant select on v_$lock to &&username;

grant select on v_$session to &&username;

grant select on dba_objects to &&username;

grant select on v_$sqlarea to &&username;
