 

  CREATE TABLE "SC_ERRORS" 
   (	"V1" VARCHAR2(4000 BYTE), 
	"V2" VARCHAR2(4000 BYTE), 
	"V3" VARCHAR2(4000 BYTE), 
	"V4" VARCHAR2(4000 BYTE), 
	"V5" VARCHAR2(4000 BYTE), 
	"V6" VARCHAR2(4000 BYTE), 
	"V7" VARCHAR2(4000 BYTE), 
	"V8" VARCHAR2(4000 BYTE)
   )   ; 

INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','7','Apprentice','Frank Hoffmann','620','492','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','1','2','25','30',NULL,NULL,'1');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','8','Salesman','Jürgen Menge','246','92','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','2','4','50','30',NULL,NULL,'2');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','9','QS','Holger Lehmann','191','229','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','1','9','20','30',NULL,NULL,'3');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','1','5','10','30',NULL,NULL,'4');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','1','8','30','20',NULL,NULL,'5');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','4','6','700','10',NULL,NULL,'6');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','6','3','700','130',NULL,NULL,'7');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','3','7','700','360',NULL,NULL,'8');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','8','7','330','130',NULL,NULL,'9');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG_LINKS','8','3','330','130',NULL,NULL,'10');
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','10','QS','Friedhold Matz','160','467','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','1','Manager','Michael Ferrante','8','3','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','2','Marketing','Angelina Jolie','279','0','F',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','3','Senior Dev','Gerd Volberg','620','347','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','4','Production','Lisa Simpson','620','2','F',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','5','Team Leader','Maggie Simpson','30','349','M',NULL);
INSERT INTO sc_errors (V1,V2,V3,V4,V5,V6,V7,V8) 
VALUES('DIAG','6','Engineer','Tawfik','619','160','M',NULL);

commit;

-- Start of DDL Script for Procedure CD_DEMO_OWNER.DATA_MODELER_UPDT
-- Generated 18-Okt-2019 10:32:50 from CD_DEMO_OWNER@CDS

CREATE OR REPLACE 
PROCEDURE data_modeler_updt (p_param in varchar2)
 IS

    mtype varchar2(100);
    x varchar2(100);
    y varchar2(100);
    x1 varchar2(100);
    y1 varchar2(100);
    mID varchar2(100);
    mstr varchar2(1000);
begin

 for i in ( select regexp_substr(p_param,'[^,]+', 1, level) val , level l from dual
                   connect by regexp_substr(p_param, '[^,]+', 1, level) is not null )  loop
                   if i.l = 1 then
                       mtype:=i.val;
                   elsif i.l = 2 then
                       mID:=i.val;
                   elsif i.l = 3 then
                       x:=i.val;
                 elsif i.l = 4 then
                       y:=i.val;
                   end if;

               end loop;

                if mtype = 'A' then
                   update sc_errors set v5 = x,v6 = y where v2 = mID and V1='DIAG';

                elsif mtype = 'B' then
                    mstr:= substr(  replace(replace( REGEXP_REPLACE(p_param, '[^0-9A-Za-z]', ''),'x',','),'y',',') ,2) ;

                    for i in ( select regexp_substr(mstr,'[^,]+', 1, level) val , level l from dual
                   connect by regexp_substr(mstr, '[^,]+', 1, level) is not null )  loop
                               if i.l = 1 then
                                   mID:=i.val;
                               elsif i.l = 2 then
                                   x:=i.val;
                               elsif i.l = 3 then
                                   y:=i.val;
                             elsif i.l = 4 then
                                   x1:=i.val;
                               elsif i.l = 5 then
                                   y1:=i.val;
                               end if;

                   end loop;

                   update sc_errors set v4 = x,v5 = y ,v6 = x1,v7 = y1  where V8 = mID and V1='DIAG_LINKS';
                end if;
                commit;

end;
/



-- End of DDL Script for Procedure CD_DEMO_OWNER.DATA_MODELER_UPDT


