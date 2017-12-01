In attachement you can find two things:
- a .jar file with
    - a look and feel implementation
    - a bunch of PJC's to use in an application
- a demo form (12.2.1.3)
- a .zip file with an Eclipse project with the source code

The .jar file is signed!

Here are some instructions:
- the jar file needs to be added in the formsweb.cfg configuration
     archive=frmall.jar,modernize.jar
- the entry point for the applet needs to be (normaly it is fixed to oracle.forms.engine.Main)
     code=ilias.oracle.forms.engine.FormsMain

Check the base...htm file to verify that the CODE="oracle.forms.engine.Main" parameter is taken from the formsweb.cfg; 
I had to alter it to CODE="%code%".

The form has a dependency on the DUAL table. It needs a connection to a/the database.

