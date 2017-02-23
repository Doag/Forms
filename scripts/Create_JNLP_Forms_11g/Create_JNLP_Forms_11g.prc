CREATE OR REPLACE PROCEDURE OSE.TRAS11_START_JNLP (
   user_in         IN VARCHAR2 DEFAULT NULL,
   form            IN VARCHAR2 DEFAULT 'Startform',
   server          IN VARCHAR2 DEFAULT 'Server',
   port            IN NUMBER DEFAULT 8888,
   client_dpi      IN NUMBER DEFAULT NULL,
   weblogic        IN VARCHAR2 DEFAULT 'weblogic',
   download_file   IN VARCHAR2 DEFAULT 'menue4.zip',
   tns_db          IN VARCHAR2 DEFAULT 'db_name')
IS
   client_dpi_parameter   NUMBER;
BEGIN

--Beispiel client_dpi aus DB
IF user_in is not null and client_dpi is null
then 
  BEGIN 
    select nvl(client_dpi_field,100)
    into client_dpi_parameter
    from user_table
    where username = upper(user_in);
  exception
  when no_data_found then client_dpi_parameter := 100;
  END;
end if;
--Wenn externer Parameter nicht gesetzt ist dann DB/Standardwert nehmen
if client_dpi is not null
then
  client_dpi_parameter := client_dpi;
end if;
owa_util.mime_header( 'application/x-java-jnlp-file', FALSE );
owa_util.http_header_close; 
  htp.p('
<jnlp spec="1.0+" 
#href="C:\Users\milchma.HD001\Desktop/start.jnlp" 
codebase="http://'||server||':'||port||'/forms/java/">
<information>
<title>TRAS11</title>
<vendor>sensis GmbH</vendor>
<description>Ihr freundliches EDV-System lädt für Sie</description>
<description kind="one-line">TRAS11</description>
<description kind="tooltip">TRAS11</description>
<description kind="short">TRAS11</description>
</information>
<security>
<all-permissions/>
</security>
<resources>
<j2se version="1.7+" initial-heap-size="128m" max-heap-size="512m"/>
<jar href="frmallborder2.jar" size="" main="true" />
<jar href="icons.jar" size="" main="true" />
<jar href="keypressed.jar" size="" main="true" />
<jar href="sensisLaF.jar" size="" main="true" />
<jar href="jacob_sich.jar" size="" main="true" />
<jar href="jcalendar-1.3.2.jar" size="" main="true" />
<jar href="looks-2.0.1.jar" size="" main="true" />
<jar href="ftpbean.jar" size="" main="true" />
<jar href="filedrop.jar" size="" main="true" />
<jar href="XLSUTIL.jar" size="" main="true" />
<jar href="poi-3.5-FINAL.jar" size="" main="true" />
<jar href="QTJava.jar" size="" main="true" />
<jar href="ojdbc6.jar" size="" main="true" />
<jar href="JMapViewerOSM.jar" size="" main="true" />
<jar href="frmwebutil_webstart.jar" size="" main="true" />
<jar href="jna.jar" size="" main="true" />
<jar href="jna-lib.jar" size="" main="true" />
<jar href="win32-x86.jar" size="" main="true" />
<jar href="commons-net-1.4.1.jar" size="" main="true" />
<jar href="commons-logging-1.1.jar" size="" main="true" />
<jar href="DirectPrint.jar" size="" main="true" />
<jar href="fontbox.jar" size="" main="true" />
<jar href="pdfbox.jar" size="" main="true" />
<jar href="icepdf-core.jar" size="" main="true" />
<jar href="icepdf-viewer.jar" size="" main="true" />
<jar href="icepdf.jar" size="" main="true" />
<jar href="iText.jar" size="" main="true" />
<jar href="PDFUTIL.jar" size="" main="true" />
<jar href="pdf-renderer.jar" size="" main="true" />
</resources>
<applet-desc main-class="oracle.forms.engine.Main" name="Forms" width="1200" height="760">
<param name="pageTitle" value="TRAS11" />
<param name="envFile" value="tras11.env" />
<param name="imageBase" value="codebase" />
<param name="ClientDPI" value="'||client_dpi_parameter||'" />
<param name="webUtilArchive" value="frmwebutil.jar , jacob_sich.jar" />
<param name="WebUtilLogging" value="on" />
<param name="WebUtilLoggingDetail" value="normal" />
<param name="WebUtilErrorMode" value="Alert" />
<param name="WebUtilDispatchMonitorInterval" value="5" />
<param name="WebUtilTrustInternal" value="true" />
<param name="WebUtilMaxTransferSize" value="16384" />
<param name="WorkingDirectory" value="c:\tras11\menue4" />
<param name="jpi_mimetype" value="application/x-java-applet" />
<PARAM NAME = "mapFonts" VALUE = "yes" />
<param name="prestartMin" value="5"/>    
<param name="prestartRuntimes" value="true"/>
<param name="baseHTMLjinitiator" value="webutiljpi.htm" />
<param name="baseHTMLjpi" value="webutiljpi.htm" />
<param name="baseHTML" value="webutilbase.htm" />
<param name="separateFrame" value="false" />
<param name="lookAndFeel" value="oracle" />
<param name="splashScreen" value="dinosaurier.gif" />
<param name="colorScheme" value="swan" />
<param name="background" value="LogoU1.jpg" />
<param name="logo" value="false" />
<param name="serverURL" value="/frmservlet?ifcfs=http://'||server||':'||port||'/forms/frmservlet?config=tras11&amp;ifsessid=formsapp&amp;userid=dummy/dummy@'||tns_db||'" />
<param name="serverApp" value="default" />
<param name="serverArgs" value="module='||form||' download_file='||download_file||' weblogic='||weblogic||' term='||chr(39)||'C:\Oracle\Middleware\Oracle_FRHome1\forms\tras11.res'||chr(39)||'" />
</applet-desc>
</jnlp>
');
END;
/