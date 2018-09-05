-- Start of DDL Script for Package Body CD_DEMO_OWNER.PDW_TEMP_BLOB_EXPORT
-- Generated 03-Sep-2018 21:44:08 from CD_DEMO_OWNER@CDS

CREATE OR REPLACE 
PACKAGE pdw_temp_blob_export
  IS
  /*
   * DOK:====================================================================
   * pdw_temp_blob_export
   *
   * Generates an CSV File from an SQL Statement 
   * SQL-INJECTION protection
   *
   * references a temp table and a download procedure
   * temp_blob_export(tbe_col, tbe_type, tbe_sessionid) ON COMMIT PRESERVE ROWS
   * (empties table at the end of the session)
   * ===================================================================
   * Example for Forms logic
   * 
   * Button logic:
   * declare vch_return_value varchar2(2000);
   * begin
   *    :last_query := get_block_property('ALL_OBJECTS',last_query);
   *    vch_return_value := pdw_temp_blob_export.f_make_blob(:last_query,'ALL_OBJ');
   *   if vch_return_value != '0'
   *   then
   *      message(vch_return_value);
   *    else
   *      synchronize;
   *      p_get_blob;
   *      pdw_temp_blob_export.p_drop_blob;
   *   end if;
   * end;
   *
   * PROCEDURE p_get_blob(p_type in varchar2) IS
   * boolean_success boolean;
   * vch_file_bez  varchar2(500);
   * vch_dir_and_file varchar2(500);
   *
   * BEGIN
   * vch_file_bez :=  'Exceltest_' || to_char(sysdate,'YYYYDDMM_HH24MISS')||  '.' ||'csv';
   * vch_dir_and_file := webutil_clientinfo.get_system_property('user.home')|| '\' || vch_file_bez;
   * -- message('Dir=' || vch_dir_and_file || ' ID=' || p_id);
   * -- show document
   * if vch_dir_and_file is not null
   * then
   * -- #3 Download BLOB
   *  if not webutil_file.FILE_EXISTS(vch_dir_and_file)
   *      then
   *         boolean_success := webutil_file_transfer.DB_To_Client_with_progress
   *                                         (clientFile      =>  vch_dir_and_file
   *                                         ,tableName       => 'TEMP_BLOB_EXPORT'
   *                                         ,columnName      => 'TBE_COL'
   *                                         ,whereClause     => 'TBE_TYPE=''' || p_type || '''' 
   * 	                                     ,progressTitle   => 'Document will be loaded'
   *		                             ,progressSubTitle=> 'Please wait'
   *		                             );
   *  end if;  
   *	  
   *  -- Open File on the client
   *  if webutil_file.FILE_EXISTS(vch_dir_and_file) or boolean_success
   *	 then
   *	    --WEBUTIL_CLIENT_FUNCTIONS.HOST('cmd /c rundll32.exe url.dll,FileProtocolHandler "'||vch_dir_and_file||'"',NO_SCREEN);
   *	    --Client_Host( 'cmd /c start "" "' || vch_dir_and_file || '"');
   *	    webutil_host.NonBlocking( 'rundll32 url.dll,FileProtocolHandler ' || vch_dir_and_file);
   *	      else
   *		message('Error in p_get_blob : Document ' || vch_dir_and_file || ' could not be loaded!');
   *		end if;
   *  end if;
   *
   * exception
   *	when others
   *	then
   *	  message('Error in p_get_blob : '||sqlerrm);
   * END;
   * Autor    : F.Hoffmann, Cologne Data
   * Datum    : 6.04.2017
   *
   * Änd-Nr. Datum       Name            Beschreibung
   * 1       21.04.2017  HOFFMANNF       QS: VCH_SESSIONID -> NUM_SESSIONID
  */

   FUNCTION  f_make_blob (p_sql IN VARCHAR2,p_type in varchar2 default 'Standardabfrage')
   /*
    * f_make_blob
    * Create CSV in a BLOB Column
    * Parameter:
    * p_sql  SQL Statement
    * p_type (LOG, FRM, REP etc.)
    * Returnvalue 0=success äor Error Message (SQLERRM)
    */
   return varchar2;
   PROCEDURE p_drop_blob;
   /*
    * p_drop_blob
    * Empty BLOB table by the session ID
    */
END;
/


CREATE OR REPLACE 
PACKAGE BODY pdw_temp_blob_export
IS
   -- Voreinstellungen Exportparameter
   vch_sep                VARCHAR2 (1)   := ';';                  -- Excel Separator
   vch_quot               VARCHAR2 (1)   := '"';                  -- Mark field for special characters
   vch_lang               VARCHAR2 (3)   := 'DE';                 -- DE or  ENG
   num_excel_max_lines    NUMBER(6)      := 128000;               -- Maximum Export
   num_sessionid          number(30)     := userenv('SESSIONID'); -- ID of a session

   -- Dynamische Cursorvariablen
   int_thecursor   INTEGER      DEFAULT DBMS_SQL.open_cursor;
   vch_nl          VARCHAR2(2)  DEFAULT CHR(10); -- Zeilenende (CR)

   PROCEDURE p_write_blob (p_write_blob in blob,p_blob_type in varchar2)    is
   PRAGMA AUTONOMOUS_TRANSACTION;
   begin
    -- Clean temp table
    delete from temp_blob_export where tbe_sessionid = num_sessionid;
    -- Fill temp table
    insert into temp_blob_export(tbe_col,tbe_type,tbe_sessionid) values (p_write_blob,p_blob_type,num_sessionid);
    commit;
   end;

   PROCEDURE p_drop_blob is
   PRAGMA AUTONOMOUS_TRANSACTION;
   begin
    -- clear table
    delete from temp_blob_export where tbe_sessionid = num_sessionid;
    commit;
   end;

   FUNCTION f_get_col_name (rec IN DBMS_SQL.desc_rec)
      RETURN VARCHAR2
   IS
   BEGIN
      RETURN (rec.col_name);
   END;

   FUNCTION f_quote (p_str IN VARCHAR2, p_enclosure IN VARCHAR2)
      RETURN VARCHAR2
   IS
   BEGIN
      -- Use quotes to allow special characters
      RETURN p_enclosure || REPLACE (p_str, p_enclosure, p_enclosure || p_enclosure) || p_enclosure;
   END;

   FUNCTION f_make_blob (p_sql IN VARCHAR2,p_type IN VARCHAR2 default 'Standardabfrage') RETURN VARCHAR2
   IS
      vch_query           VARCHAR2 (32767);   -- Query with select
      vch_check_sql       VARCHAR2 (32767);   -- lowercase
      vch_zeile           VARCHAR2 (32767);   -- row
      vch_columnvalue     VARCHAR2 (4000);    -- values
      vch_columnname      VARCHAR2 (30);      -- colummn name
      num_colcnt          NUMBER   DEFAULT 0; -- number of rows
      num_cnt             NUMBER   DEFAULT 0; -- pointer to the row
      vch_header          VARCHAR2 (32767);   -- Header
      vch_datefmt         VARCHAR2 (255);     -- NLS DATE Format
      vch_numberfmt       VARCHAR2 (255);     -- NLS NUM Format
      tab_l_desctbl       DBMS_SQL.desc_tab;  -- local table
      vch_warning         VARCHAR2 (200);     -- warning
      blob_l_blob         BLOB;               -- blob column

   BEGIN

      -- Generate CSV File
      IF p_sql IS NOT NULL
      THEN

         vch_query := p_sql;

         -- Simple SQL Injection Check
         vch_check_sql := lower(vch_query);

         if (instr(vch_check_sql,'insert ',1,1) +
             instr(vch_check_sql,'update ',1,1) +
             instr(vch_check_sql,'delete ',1,1) +
             instr(vch_check_sql,'alter ' ,1,1) +
             instr(vch_check_sql,'drop '  ,1,1) +
             instr(vch_check_sql,'create ',1,1) +
             instr(vch_check_sql,'grant ' ,1,1)
            ) > 0
          then
            return('Query contains statements that are not allowed');
         end if;

         SELECT VALUE
           INTO vch_datefmt
           FROM nls_session_parameters
          WHERE parameter = 'NLS_DATE_FORMAT';

         SELECT VALUE
           INTO vch_numberfmt
           FROM nls_session_parameters
          WHERE parameter = 'NLS_NUMERIC_CHARACTERS';

         --  NLS_LANG Setting

         -- Language settings
         IF vch_lang IN ('ENG', 'US', 'GB','EN')
         THEN
            EXECUTE IMMEDIATE 'alter session set nls_NUMERIC_CHARACTERS=''.,'' ';

            EXECUTE IMMEDIATE 'alter session set nls_date_format=''mm/dd/yyyy'' '; --hh24:mi:ss
         ELSE
            -- Default Deutsch (DE)
            EXECUTE IMMEDIATE 'alter session set nls_NUMERIC_CHARACTERS='',.'' ';

            EXECUTE IMMEDIATE 'alter session set nls_date_format=''dd.mm.yyyy'' '; --hh24:mi:ss
         END IF;

         -- we capsulate the query to get the header
         vch_query := 'select * from (' || vch_query || ')';
         DBMS_SQL.parse (int_thecursor, vch_query, DBMS_SQL.native);
         DBMS_SQL.describe_columns (int_thecursor, num_colcnt, tab_l_desctbl);

         -- we bind all rows (Numer, Date, Varchar in Spalten a 4000 Zeichen)
         FOR i IN 1 .. num_colcnt
         LOOP
            DBMS_SQL.define_column (int_thecursor, i, vch_columnvalue, 4000);
            vch_zeile := vch_zeile || vch_columnvalue || vch_sep;
         END LOOP;

         -- We Calculate hits
         num_cnt := DBMS_SQL.EXECUTE (int_thecursor);

         -- We Generate the CSV File into a blob
         LOOP
            EXIT WHEN (DBMS_SQL.fetch_rows (int_thecursor) <= 0) OR (num_cnt = num_excel_max_lines);

            vch_zeile := '';
            vch_header := '';

            FOR i IN 1 .. num_colcnt
            LOOP
               DBMS_SQL.column_value (int_thecursor, i, vch_columnvalue);

               -- Generate header
               IF num_cnt < 1
               THEN
                  vch_columnname := f_get_col_name (tab_l_desctbl (i));
                  vch_header := vch_header || f_quote (vch_columnname, vch_quot) || vch_sep;
               END IF;

               vch_zeile := vch_zeile ||  vch_quot  || vch_columnvalue || vch_quot   || vch_sep;
            END LOOP;

            -- Header schreiben
            IF num_cnt < 1
            THEN
                  DBMS_LOB.createtemporary (blob_l_blob, TRUE);
                  DBMS_LOB.append (blob_l_blob, UTL_RAW.cast_to_raw (vch_header || CHR (10) ));
            END IF;

            -- Excel Zeilen schreiben
            DBMS_LOB.append (blob_l_blob, UTL_RAW.cast_to_raw (vch_zeile || CHR (10)));

            num_cnt := num_cnt + 1;
         END LOOP;
        --end if;

         -- Reset NLS settings
         EXECUTE IMMEDIATE 'alter session set nls_date_format=''' || vch_datefmt || '''';
         EXECUTE IMMEDIATE 'alter session set nls_numeric_characters=''' || vch_numberfmt || '''';


         -- Warning messages
         IF num_cnt >= num_excel_max_lines
         THEN
           vch_warning := 'Warning!!! The query had mor rows than the permitted '
                       || num_excel_max_lines || '  lines of code.';
         ELSIF num_cnt = 0
         THEN
            vch_warning := 'Sorry, the query did not retrieve any data';
         ELSE
            vch_warning := null;
         end if;
      -- End of dynamic SQL Export
      END IF;

   if vch_warning is not null
    then
      return(vch_warning);
    else
      p_write_blob(blob_l_blob,p_type);
      return ('0');
   end if;

   EXCEPTION
      -- In Error Case the format declarations will also set back
      WHEN OTHERS
      THEN
         EXECUTE IMMEDIATE 'alter session set nls_date_format=''' || vch_datefmt || '''';
         EXECUTE IMMEDIATE 'alter session set nls_NUMERIC_CHARACTERS=''' || vch_numberfmt || '''';
         return(SQLERRM);
   END;
end;
/


-- End of DDL Script for Package Body CD_DEMO_OWNER.PDW_TEMP_BLOB_EXPORT

