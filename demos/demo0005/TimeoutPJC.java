/*
*
* @author Frank Hoffmann
* @date 10.10.2016
* @version 2.3, frmmain.jar 11.1.2.2 or 12.2.1.x
* @file TimeoutPJC.java
*
****************************************************************************************
*
* Forms Pluggable Java Component that intercepts mouse operations and key 
* events to reset a timer. This PJC is supposed to be used in Forms applications 
* that need to perform some security related activities after a pre-defined
* period of user inacticity. When starting the timer, a separate thread is 
* spawned that frequently checks for the difference between the current client 
* time and the last time of user interaction. If the time difference exceeds a 
* pre-defined tolerance value, a custom event is raised to Forms for the developer
* to handle user inactivity. 
* 
* To handle the case where to many user action recordings hava a performance 
* impact to the application (e.g. refresh problems) it is possible to switch 
* off specific events from being captured. 
*
* Forms usage:
* Standardtimer 1 Second (one time)
* =================================
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'ENABLE_DEBUGGING','false');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'TIMER_NAME','ST:' || upper(NAME_IN('SYSTEM.CURRENT_FORM')));
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'ENABLE_STANDARDTIMER','true');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'RECORDING_EVENTS','KEY');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'START_TIMER','1000'); 
*
* Intervaltimer 4 Min with 60 min Inactivity
* ==========================================
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'ENABLE_INTERVALTIMER','true');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'TIMER_NAME','IT:'|| upper(NAME_IN('SYSTEM.CURRENT_FORM')) );
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'RECORDING_EVENTS','ALL');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'ENABLE_DEBUGGING','true');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'TIMER_SLEEP_TIME','60000');
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'START_TIMER','3600000'); 
*
* When-New-Item_instance to simulate A Key Move on the Screen
* ===========================================================
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'FORMS_WHEN_NEW_ITEM_INSTANCE','');
*
* Stop Timer Signal
* =================
* set_custom_property('WEBUTIL.WEBUTIL_TIMER_FUNCTIONS',1,'END_TIMER','');
*
* Event  MAX_ACTIVITY_EXCEEDED:TIMER_TYP:TIMER_NAME Timer End Signal (Interval-, Repeating-, Standardtimer)
* Event  INACTIVITY_EXCEEDED:TIMER_TYP:TIMER_NAME   No activity during an Pause-Interval (Intervaltimer)
* Event  ACTIVITY_EXCEEDED::TIMER_TYP:TIMER_NAME    User Activity during an Pause-Interval (Intervaltimer)
*
* Requires -classpath frmmain.jar version 11.1.2.2. or 12.2.1.x
*  
* example steps to create own timeout.jar:
*
* 1. Copy frmmain.jar and TimeoutPJC.java in a jdk bin directory
* 2. Open a cmd.exe or unix session in this directory
* 3. execute: javac TimeoutPJC.java -classpath frmmain.jar
* 4. replace the generated classes in the existing timeout.jar to keep directory structure
* 5. sign timeout.jar to avoid warning messages and to use it in new JRE versions
* 6. replace timeout.jar and restart the Formsserver
* 
*/

package oracle.forms.demos;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;


import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import oracle.forms.engine.Main;
import oracle.forms.handler.IHandler;
import oracle.forms.properties.ID;
import oracle.forms.ui.CustomEvent;
import oracle.forms.ui.FormMouseGrabProvider;
import oracle.forms.ui.VBean;

/**
* TimeoutPJC: class to stop and start timers with "activity signals" send to Forms
*
* @author Frank Hoffmann, Frank Nimphius
* @date 10.10.2016
*/
public class TimeoutPJC extends VBean
{
  // version shown in debug modus

  private String tj_version = "V2.3: ";

  //**********************
  // FORMS STATIC IDs
  //**********************

  protected static final ID pEnableDebugging     = ID.registerProperty("ENABLE_DEBUGGING");
  protected static final ID pEnableIntervaltimer = ID.registerProperty("ENABLE_INTERVALTIMER");
  protected static final ID pEnableStandardtimer = ID.registerProperty("ENABLE_STANDARDTIMER");
  protected static final ID pSleepTime           = ID.registerProperty("TIMER_SLEEP_TIME");
  protected static final ID pStartTimer          = ID.registerProperty("START_TIMER");
  protected static final ID pNewFormSignal       = ID.registerProperty("NEW_FORM_SIGNAL");
  protected static final ID pEndTimer            = ID.registerProperty("END_TIMER");
  protected static final ID pTimerName           = ID.registerProperty("TIMER_NAME"); 
  protected static final ID pFormsNewItem        = ID.registerProperty("FORMS_WHEN_NEW_ITEM_INSTANCE"); 
  protected static final ID pRecordingEvents     = ID.registerProperty("RECORDING_EVENTS");

  public    static       String VisibleInstance  ="ST";  

  public ID ceMaxInactivityExceeded;                 
  public ID ceInActivityExceeded;                    
  public ID ceActivityExceeded;                        

  private                   IHandler mHandler     = null;   // Forms Handler
  private Main              formsMain             = null;   // Forms main class

  private boolean           debug_enabled         = false;  // Activates Debug messages [true|false]
  private boolean           i_timer_enabled       = false;  // Activates Intervaltimer  [true|false]
  private boolean           stop_timer_signal     = false;
  private boolean           timerStopped          = true;
  private boolean           timerExists           = false;

  private long              max_inactivity        = 100;    // ms
  private long              timer_break           = 100;    // ms
  private long              sleep_time            = 100;    // ms
  private long              timer_counter         = 0;
  private long              last_user_action;
  private long              last_check_time; 
  private Thread            t                     = null;
  private String            this_instance_name    = this.getDefaultName();
  private String            this_timer_name       = "ST"; // Default ST=Standardtimer or Customized Name
  private String            last_action           = "?";

  private String            ActiveInstance;
  private String            LastActiveInstance;

  private SimpleDateFormat  sdf                   = new SimpleDateFormat("dd-MMM-yyyy, hh:mm:ss a");

  private boolean           record_on_mouse_move    = true;
  private boolean           record_on_mouse_click   = true;
  private boolean           record_on_mouse_enter   = true;
  private boolean           record_on_mouse_exit    = true;
  private boolean           record_on_all_keys      = true;
  private boolean           record_on_all_events    = true;
  private boolean           record_on_focus_events  = true;

  
  // default constructor
  public TimeoutPJC()
  {
  }


  /**
  * Initialisierung der Timerumgebungsvariablen
  */       
  public final void init(IHandler handler)
  {
    mHandler = handler;
    super.init(handler);

    // getting the Forms Main class    
    formsMain  =  (Main) mHandler.getApplet();

    //setKeyListener();
    // getting the Forms Main class
    try{
        Method method = handler.getClass().getMethod("getApplet", new Class[0]);
        Object applet = method.invoke(handler, new Object[0]);
       if (applet instanceof Main) 
        {
           formsMain = (Main)applet;
        }    
        }catch(Exception ex) {;}    


    //******************************************************************
    //             ADDING MOUSE TO FORMS
    //******************************************************************
    // Forms main returns desktop focus gain and lost
    _addFocusListeners(formsMain.getDesktop().getDesktopComponent());

    // the glass mouse grab provider is the proxy component for all 
    _addMouseListeners(formsMain.getCursorGrabProvider());
 
    // New to remember Current Instance
    LastActiveInstance=VisibleInstance;
    ActiveInstance=this_instance_name;
  }


/**
 * _addFocusListeners : listener to raise focus signals
 *
 * @param c component 
 */

  public void _addFocusListeners (Component c)
  {
  
   c.addFocusListener
   (
    new FocusListener()
    {
      public void focusLost(FocusEvent fe)
      {
        if(record_on_focus_events || record_on_all_events){
          last_action = "Focus Lost recognized";
          log_activity();
        }
      }

      public void focusGained(FocusEvent fe)
      {
        if(record_on_focus_events || record_on_all_events){
           last_action = "Focus Gained recognized";
           log_activity();
         }
        }
      }
    );
  }


/**
 * _addMouseListeners : Listener for mouse grabbing
 *
 * @param FormMouseGrabProvider 
 *
 */

  public void _addMouseListeners(FormMouseGrabProvider c)
  {
  
    c.addMouseMotionGrab
    (
     new MouseMotionListener()
     {
      public void mouseMoved(MouseEvent me)
      {
       if(record_on_mouse_move || record_on_all_events)
       {
          last_action = "MouseMoved recognized";
          log_activity();
       }
      }

      public void mouseDragged(MouseEvent me)
      {
      // drag and drop is not supported in Forms. Therefore the
      // drag event is useless to capture
      last_action = "Mouse dragged";
      log_activity();
      }
     }
    ); // addMouseMotionGrab  

   c.addMouseGrab
   (
    new MouseListener()
    {
     public void mouseExited(MouseEvent me)
     {
      if(record_on_mouse_exit || record_on_all_events)
      {
       last_action = "MouseExited recognized";
       log_activity();
      }
     }
     
     public void mouseEntered(MouseEvent me)
     {
      if(record_on_mouse_enter || record_on_all_events)
       {
        last_action = "MouseEntered recognized";
        log_activity();
       }
     }

     public void mousePressed(MouseEvent me)
     {
      if(record_on_mouse_click || record_on_all_events)
      {
       last_action = "MousePressed recognized";
       log_activity();
      }
     }

     public void mouseReleased(MouseEvent me)
     {
      if(record_on_mouse_click || record_on_all_events)
      {
       last_action = "MouseReleased recognized";
       log_activity();
      }
     }

     public void mouseClicked(MouseEvent me)
     {
      if(record_on_mouse_click || record_on_all_events)
      {
       last_action = "MouseClicked recognized";
       log_activity();
      }
     }
    }
   ); // addMouseGrab

  } // _addMouseListeners

  

/**
 * setProperty : timer parameter setup
 *
 * @param ID  Parameter ID (EnableDebugging,SleepTime usw.) 
 * @param ARGS values
 * @return boolean
 *
 */
  public boolean setProperty(ID _ID, Object _args)
  {
   /*=========================
    * Enable Debugging
    * ========================*/
    if(_ID==pEnableDebugging)
    {
      if (_args!=null && ((String)_args).length()!=0)
      {
        if ("TRUE".equalsIgnoreCase((String)_args))
        {
          debug_enabled = true;
        }
        else
        {
          debug_enabled = false;
        }
      }
      return true;
    }
    
   /* =========================
    * Timer Sleep-Time
    * ========================*/
    else if (_ID == pSleepTime)
    {
      if (_args != null)
        {
          try{
              // FH 13.08.2015 Changed sleep time to ms
              sleep_time = new Integer((String)_args).intValue();
              write_message("Setting sleep intervall to "+ sleep_time +" ms");
          }
          catch (NumberFormatException nfe)
          {
            write_message("Error in setting sleep interval - invalid syntax for sleep "+ 
                          "time. Value must be provided in seconds and of type "+
                          "integer");
          }
        }
        return true;
    }

   /*===========================
    * START TIMER 
    *==========================*/

    else if (_ID == pStartTimer)
      {
        if (_args != null)
        {
          int mx_inactivity = 0;
          try{
              mx_inactivity = new Integer((String)_args).intValue();
              mx_inactivity = mx_inactivity;
              startTimer(mx_inactivity);
          }
          catch (NumberFormatException nfe)
          {
            write_message("Error in starting timer - invalid syntax for max "+ 
                          "inactivity. Value must be in ms and of type "+
                          "integer");
          }
        }
        return true;
      }

    /*===========================
     * RECORD USER ACTIONS
     *==========================*/

      if (_ID==pRecordingEvents)
      {
        // check if arguments are passed
        if (((String)_args).length()>0)
        {
          write_message("Setting events: "+(String)_args);
          // tokenize argument String allowing "|" and " " as delimiters
          StringTokenizer st = new StringTokenizer((String)_args,"| ");

          if (st.countTokens() >0)
          {
            // set all event flags to false
            record_on_mouse_move    = false;
            record_on_mouse_click   = false;
            record_on_mouse_enter   = false;
            record_on_mouse_exit    = false;
            record_on_all_keys      = false;
            record_on_all_events    = false;
            record_on_focus_events  = false;

            while(st.hasMoreTokens())
            {
              String s = st.nextToken();

              if ("ENTER".equalsIgnoreCase(s))
              {
                write_message("Enabling mouse enter events ...");
                record_on_mouse_enter   = true;
              }
              else if("EXIT".equalsIgnoreCase(s))
              {
                write_message("Enabling mouse exit events ...");
                record_on_mouse_exit    = true;
              }
              else if ("MOVE".equalsIgnoreCase(s))
              { 
                write_message("Enabling mouse move events ...");
                record_on_mouse_move    = true;
              }
              else if("KEY".equalsIgnoreCase(s))
              {
                write_message("Enabling key events ...");
                record_on_all_keys      = true;
              }
              else if("CLICK".equalsIgnoreCase(s))
              {
                write_message("Enabling mouse click events ...");
                record_on_mouse_click   = true;
              }
              else if ("ALL".equalsIgnoreCase(s))
              {
                write_message("Enabling all events ...");
                record_on_all_events = true;
              }
              else if ("FOCUS".equalsIgnoreCase(s))
              {
                write_message("Enabling focus events ...");
                record_on_focus_events = true;
              }
              else
              {
                write_message("No valid argument when calling RECORDING_EVENTS. "+
                              "Statement ignored and all events activated for recording");

                record_on_all_events = true;
              }
            }
            return true;
          }
          else
          {
            write_message("No valid argument in call to RECORDING_EVENTS. "+
                          "Statement ignored");
            return true;
          }
        }
        else
        {
          write_message("No argument in call to RECORDING_EVENTS. Statement ignored");
          return true;
        }
      }


    /*===========================
     * Set Customized TIMER NAME 
     *==========================*/

 
      else if (_ID ==pTimerName)
      {
        if (_args!=null) 
        {
          this_timer_name = (String)_args;
          VisibleInstance = ActiveInstance;
        }
        write_message("Timername Setting");
        return true;
      }


    /*===============================
     * Set Customized NEW FORM SIGNAL 
     *===============================*/

 
      else if (_ID ==pNewFormSignal)
      {
        VisibleInstance = LastActiveInstance;
        write_message("NewFormSignal fired");
        return true;
      }


    /*===========================
     * Forms New Item event 
     *==========================*/

 
      else if (_ID ==pFormsNewItem)
      {
        write_message("Forms When New Item Event fired");
        last_action = "Forms W-N-I-E Trigger fired";
        log_activity();
        return true;
      }


    /*===========================
     * END TIMER 
     *==========================*/


      else if (_ID ==pEndTimer)
      {
        write_message("Stop signal send");
        stopTimer();
        return true;
      }

     /*===========================
     * Enable Intervaltimer 
     *==========================*/
 
      else if(_ID==pEnableIntervaltimer)
      {
        if (_args!= null)
      {
        // enable Intervaltimer
        write_message("Enable Intervaltimer ...");
        i_timer_enabled=true;
 
     }
     return true;
     }

     /*===========================
     * Enable Standardtimer
     *==========================*/
 
      else if(_ID==pEnableStandardtimer)
      {
        if (_args!= null)
      {
        // enable Intervaltimer
        write_message("Enable Standardtimer ...");
        i_timer_enabled=false;
 
     }
     return true;
     }

   
   /*===========================
     * Enable Debugging 
     *==========================*/
 
      else if(_ID==pEnableDebugging)
      {
        if (_args!= null)
      {
        // enable debugging
        debug_enabled=true;
        write_message("Enabled debug messages ...");
      }
      else
      {
        // disable debugging
        debug_enabled=false;
        write_message("Disabled debug messages ...");
      }
      return true;
    }
    
    return super.setProperty(_ID, _args);
  } // setProperty


/**
 * startTimer : activate timer on base of setProperty
 *
 * @param max_inactivity = time in ms 
 *
 */
  private void startTimer(int _max_inactivity)
  {
   // get start time
   if (!timerExists)
   {
    last_user_action   = Calendar.getInstance().getTime().getTime();
    last_check_time    = Calendar.getInstance().getTime().getTime();
    max_inactivity     = _max_inactivity;
    write_message("Max allowed inactivity "+max_inactivity+" ms.");  

    // debug
    write_message("Start timer = "+sdf.format(new Date(last_check_time)));
      
    // timer is allowed to run
    timerStopped  = false;
      
    // create timer thread
    t = new Thread()
    {
     public void run() 
     {
      // only one timer allowed per session
      timerExists = true;
      stop_timer_signal = false;
      timer_counter = 0;

      // For debug purposes
      write_message("New Timer is active and running...");

      while (!timerStopped && !isInterrupted())
      {
       // compare last user action with current time and raise event 
       // when max allowed time is exceeded
       long current_time = Calendar.getInstance().getTime().getTime();

       if (!stop_timer_signal)
       {
        try
        {
         t.sleep(timer_break); //msec
         timer_counter = timer_counter + timer_break;
         if (VisibleInstance==LastActiveInstance)
         {
          stop_timer_signal = true;
          write_message("Removing TimerThread");
         } 
        }
        catch (InterruptedException ie)
        {
         // ignore
        }
        
        // 
        // Customevent "ActivityExceeded" fires if a Useraction occured during the sleeptime
        //
              
        if (
           (current_time - last_user_action) < (max_inactivity) 
           && last_action != "?" 
           && ActiveInstance==VisibleInstance 
           && (!stop_timer_signal) 
           && (i_timer_enabled)
           && (timer_counter>=sleep_time)
           )
        {
         try
         {
          // raise custom Forms event
          ceActivityExceeded = ID.registerProperty("ACTIVITY_EXCEEDED"+":"+this_timer_name);
          CustomEvent ce = new CustomEvent(mHandler,ceActivityExceeded);
          dispatchCustomEvent(ce);
          last_user_action   = Calendar.getInstance().getTime().getTime();
          write_message("Activity: "+sdf.format(new Date(current_time))+" "+last_action);
          timer_counter = 0;
          last_action = "?";
         }
         catch (Exception e)
         {
          write_message("Error occured when raising custom event = e.getMessage()");
         }
        }
        else   
        // 
        // Customevent "No_Activity_Exceeded" fires if no Useraction occured during the sleeptime
        //
        {
        if 
          ( 
          ((current_time - last_user_action) < (max_inactivity)) 
          && (ActiveInstance==VisibleInstance) 
          && (!stop_timer_signal) 
          && (i_timer_enabled)                   
          && (timer_counter>=sleep_time)
          )
          try
            {
             // raise custom Forms event
             ceInActivityExceeded = ID.registerProperty("INACTIVITY_EXCEEDED"+":"+this_timer_name);
             CustomEvent ce = new CustomEvent(mHandler,ceInActivityExceeded);
             dispatchCustomEvent(ce);
             last_check_time   = Calendar.getInstance().getTime().getTime();
             write_message("NoActivity "+sdf.format(new Date(last_check_time)));
             timer_counter = 0;
            }
            catch (Exception e)
            {
             write_message("Error occured when raising custom event ="+e.getMessage());
            }
        
            // 
            // Customevent "Activity_Exceeded" fires if Timer is no longer needed
            //
         if 
           (
             ((current_time - last_user_action) > (max_inactivity)) 
             && (ActiveInstance==VisibleInstance)
           )
            try
            {
             // raise custom Forms event
             ceMaxInactivityExceeded = ID.registerProperty("MAX_ACTIVITY_EXCEEDED"+":"+this_timer_name);
             CustomEvent ce = new CustomEvent(mHandler,ceMaxInactivityExceeded);
             dispatchCustomEvent(ce);
             write_message("Timer finished.."); 
             timerStopped = true; 
             stop_timer_signal=true;
            }
            catch (Exception e)
            {
             write_message("Error occured when raising custom event ="+e.getMessage());
            }
           }
       }
       else
       {
        write_message("Timer marked by Stopsignal");
        timerStopped = true;
       }
     }

     // interrupt thread when Forms calls out to stop the thread. In this
     // case the timerStopped flag is set but the isInterrupted method does
     // not return a true value
          
     if (timerStopped && !isInterrupted())
     {
      last_user_action = Calendar.getInstance().getTime().getTime();
      write_message("End Timer = "+ sdf.format(new Date(last_user_action)));
      t.interrupt();
      t = null;
      System.gc();
     }
     timerExists = false;
    }                       // if (!stop_timer_signal)
   };                       // run() 

   t.start();               // Start Thread t
   }                        // if (!timerExists) 

  }                         // startTimer

/**
 * stopTimer : stop timer within 100ms
 *
 */
  
  private void stopTimer()
  {
    stop_timer_signal = true;
  }

/**
 * log_activity : set last_user_action timestamp
 *
 */
  private void log_activity()
  {
    if (i_timer_enabled)
    {   
    last_user_action = Calendar.getInstance().getTime().getTime();
    }
  }

/**
 * write_message : send debug message if "debug_enabled" is set
 *
 */
  private void write_message(String st)
  {
    if (debug_enabled)
    {
      System.out.println("DEBUG: Active:"+ActiveInstance+" Visible:"+VisibleInstance+" LastActive:"+LastActiveInstance+" "+tj_version+this_timer_name+":"+st);
    }

  }

/**
* destroy
*
* Beendingung der Java-Session
*
*/
  public void destroy()
  {
    super.destroy();
  }

} // class TimeoutPJC