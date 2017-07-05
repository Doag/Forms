package forms;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import oracle.forms.handler.IHandler;
import oracle.forms.ui.DrawnPanel;
import oracle.forms.ui.VBean;
import oracle.forms.properties.ID;
import oracle.forms.ui.VButton;
import java.awt.event.MouseEvent;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.Graphics;

/**
    This is just sample code, its free to use.
    It is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

    The code relies on the internal structure of forms when rendering stacked canvases
    It is tested against Forms 10.1.2.0.2, but may stop working with any patch or future version of forms
    
    Sample code for a Javabean to implement an expandabled/collapseable canvas.
    For usage-notes see the the package PK_ACCORDION, which is the counterpart for this code 
    on the forms-side
*/
public class AccordionButton extends VButton implements Runnable
{
  /** Ids for Forms-events */
  public static final ID  INIT_ACCORDION    =ID.registerProperty("INIT_ACCORDION");
  public static final ID  SCALE_ACCORDION   =ID.registerProperty("SCALE_ACCORDION");
  public static final ID  ACTIVATE          =ID.registerProperty("ACTIVATE");

  /** default-images */
  private final ImageIcon IMG_COLLAPSED=new ImageIcon(this.getClass().getResource("collapsed.png"));
  private final ImageIcon IMG_EXPANDED=new ImageIcon(this.getClass().getResource("expanded.png"));  
  /** constants */ 
  private static final int MODE_SHRINK=0;
  private static final int MODE_EXPAND=1;
  /** class variables */
  private static boolean  c_processing=false;

  /** currently used images */
  private ImageIcon     m_collapsed=null;
  private ImageIcon     m_expanded=null;
  /** codebase */
  private URL           m_codeBase=null;  

  /** other vars */
  private int             m_mode=-1;
  private String          m_buttonName=null;
  private Dimension       m_dimension=null;
  private DrawnPanel      m_masterCanvas=null;
  private String          m_previousItemName=null;
  private String          m_nextItemName=null;
  private boolean         m_active=false;
  private AccordionButton m_previousItem=null;
  private AccordionButton m_nextItem=null;
  private AccordionButton m_toMakeActive=null;
  private int             m_minHeight=-1;
  private int             m_maxHeight=-1;

  /**
   * Initialize the codebase, used for image-loading
   * 
   * @param handler
   */
  public void init(IHandler handler)
  {
    // Remember Codebase
    m_codeBase = handler.getCodeBase();
    super.init(handler);
  }

  /**
   * 
   * @return Searches for an AccordionButton with the given name, 
   *         starting the search at the given Container
   * @param name Name of the accordion button to search
   * @param c    Container to start at
   */
  private AccordionButton rekuFindChild(Container c, String name) 
  {
    AccordionButton result=null;
    for (int i=0;i<c.getComponentCount();i++) 
    {
      Component comp=c.getComponent(i);
      if (comp instanceof AccordionButton) 
      {
        if (name.equals(((AccordionButton)comp).getButtonName())) 
        {
          result=(AccordionButton)comp;
          break;
        }
      }
    }
    if (result==null) 
    {
      for (int i=0;i<c.getComponentCount();i++) 
      {
        if (c.getComponent(i) instanceof Container) 
        {
          result=rekuFindChild((Container)c.getComponent(i), name);
          if (result!=null) 
          {
            break;
          }
        }
      }
    }
    return result;
  }

  /**
   * finds the "Neighbor"-Accordion-Button by takeing the names given by 
   * the initialization property 
   */
  private void findNeighbors() 
  {
    if (!".".equals(m_previousItemName)) 
    {
      m_previousItem=rekuFindChild(m_masterCanvas, m_previousItemName);
    }
    if (!".".equals(m_nextItemName)) 
    {
      m_nextItem=rekuFindChild(m_masterCanvas, m_nextItemName);
    }
  }

  /**
   * Initializationof the AccordionButton. The data is given as a string, the
   * values are concatenated by |.
   * @param data
   */
  private void init(String data) 
  {
    StringTokenizer st=new StringTokenizer(data, "|");
    if (st.hasMoreTokens()) 
    {
      m_buttonName=st.nextToken();
      //System.out.println("My name is " + m_buttonName);
    }
    String active="N";
    if (st.hasMoreTokens()) 
    {
      active=st.nextToken();
    }
    m_active="J".equals(active);
    if (st.hasMoreTokens()) 
    {
      m_previousItemName=st.nextToken();
    }
    if (st.hasMoreTokens()) 
    {
      m_nextItemName=st.nextToken();
    }
    if (st.hasMoreTokens()) 
    {
      String image=st.nextToken();
      m_expanded=loadImage(image);
    }
    if (st.hasMoreTokens()) 
    {
      String image=st.nextToken();
      m_collapsed=loadImage(image);
    }


    int dp=0;
    Container c=this;
    while (c.getParent()!=null && dp<2) 
    {
      c=c.getParent();
      if (c instanceof DrawnPanel) 
      {
        dp++;
      }
    }
    if (c instanceof DrawnPanel) 
    {
      m_masterCanvas=(DrawnPanel)c;
    }
    m_dimension=this.getParent().getParent().getParent().getSize();
  }

  /**
   * Delegates the current maximum height to the next AccordionButon in the group
   * @param maxHeight
   */
  protected void scaleNext(int maxHeight) 
  {
    // init Neighbours
    findNeighbors();
  
    m_minHeight=this.getX()+this.getHeight();
    m_maxHeight=maxHeight;
    if (m_active) 
    {
      m_maxHeight=(int)m_dimension.getHeight()-m_minHeight;
    }
    if (m_nextItem!=null) 
    {
      m_nextItem.scaleNext(m_maxHeight);
    } else if (m_previousItem!=null) 
    {
      m_previousItem.scalePrev(m_maxHeight);
    }
  }

  /**
   * Delegates the current maximum height to the previous AccordionButon in the group
   * @param maxHeight
   */
  protected void scalePrev(int maxHeight) 
  {
    m_maxHeight=maxHeight;
    if (m_previousItem!=null) 
    {
      m_previousItem.scalePrev(m_maxHeight);
    }
  }


  /**
   * Searches the previous buttons if one of them is the active one,
   * if found, it will start the logic to deactivate (shrink) it
   * 
   * @return true if one of the previous buttons was the active one
   * @param toMakeActive the button which should be activated
   */
  protected boolean closePrevActive(AccordionButton toMakeActive) 
  {
    if (m_active) 
    {
      m_mode=MODE_SHRINK;
      m_toMakeActive=toMakeActive;
      m_active=false;
      Thread t=new Thread(this);
      t.start();
      return true;
    }
    if (m_previousItem!=null) 
    {
      return m_previousItem.closePrevActive(toMakeActive);
    }
    return false;
  }

  /**
   * Searches the following buttons if one of them is the active one,
   * if found, it will start the logic to deactivate (shrink) it
   * 
   * @return true if one of the following buttons was the active one
   * @param toMakeActive the button which should be activated
   */
  protected boolean closeNextActive(AccordionButton toMakeActive) 
  {
    if (m_active) 
    {
      m_mode=MODE_SHRINK;
      m_toMakeActive=toMakeActive;
      m_active=false;
      Thread t=new Thread(this);
      t.start();
      return true;
    }
    if (m_nextItem!=null) 
    {
      return m_nextItem.closeNextActive(toMakeActive);
    }
    return false;
  }


  /**
   * closes the active Accordion, either a previos or a following one
   */
  private void closeActive() 
  {
    // Try to close to prev-direction
    if (!closePrevActive(this)) 
    {
      // Not found, close next active
      closeNextActive(this);
    }
  }

  /**
   * Standard Method, overwritten to make the bean-specific properties from forms
   * @return true
   * @param value
   * @param id    
   */
  public boolean setProperty(ID id, Object value)
  {
    if (id==INIT_ACCORDION) 
    {
      //System.out.println(INIT_ACCORDION);    
      init((String)value);
      return true;
    } else if (id==SCALE_ACCORDION) 
    {
      //System.out.println(SCALE_ACCORDION);    
      // Scale
      scaleNext(-1);
      // Adjust Initial Layout
      if (m_nextItem!=null) 
      {
        m_nextItem.adjustPosition(this.getParent().getParent().getParent().getY()+this.getParent().getParent().getParent().getHeight());
      }
      return true;
    } else if (id==ACTIVATE) 
    {
      processActionEvent(null);
      return true;
    } else 
    {
      return super.setProperty(id, value);
    }
  }

  /**
   * Run-Method of the Runnable-interface, does the animation of
   * slowly shrinking or growing the canvas
   */
  public void run() 
  {
    boolean done=false;
    int height=this.getParent().getParent().getHeight();
    //System.out.println("Start mit Höhe" + height);
    int offset=(m_mode==MODE_SHRINK ? -10 : 10);
    while (!done)
    {
      try 
      {
        height=height+offset;
        if (m_mode==MODE_SHRINK && height<=m_minHeight) 
        {
          height=m_minHeight;
          done=true;
        } else if (m_mode==MODE_EXPAND && height>=m_maxHeight+m_minHeight) 
        {
          height=m_maxHeight+m_minHeight;
          done=true;
        }
        this.getParent().getParent().setSize((int)m_dimension.getWidth(), height);
        this.getParent().getParent().getParent().setSize((int)m_dimension.getWidth(), height);
        if (m_nextItem!=null) 
        {
          m_nextItem.adjustPosition(this.getParent().getParent().getParent().getY()+this.getParent().getParent().getParent().getHeight());
        }
        Thread.sleep(10);
      } catch (Exception e) 
      {
      }
    }
    if (m_mode==MODE_SHRINK && m_toMakeActive!=null) 
    {
      // Now make the other one active
      m_toMakeActive.makeActive();
    }
    c_processing=false;
    ActionEvent e=new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null);
    super.processActionEvent(e);
  }

  /**
   * Intercepty the standard WHEN-BUTTON-PRESSED and activates the
   * Accordion instead. The WHEN-BUTTON-PRESSED-event is deferred until
   * the current accordion has become active.
   * @param p0
   */
  protected void processActionEvent(ActionEvent p0)
  {
    // Nothing happens whn already active
    if (!m_active && !c_processing) 
    {
      c_processing=true;
      // close the active item
      // This will in turn start the thread to open this accordion
      closeActive();
    }
  }

  /**
   * Name assigned to the button
   * @return Name of the button
   */
  public String getButtonName() 
  {
    return m_buttonName;
  }

  /**
   * Set flags and start expanding thread
   */
  public void makeActive() 
  {
    m_active=true;
    m_mode=MODE_EXPAND;
    m_toMakeActive=null;
    Thread t=new Thread(this);
    t.start();
  }
  
  /**
   * adjust the position of the stacked canvas and delegate to the following Accordion.
   * @param yPosition
   */
  protected void adjustPosition(int yPosition) 
  {
    this.getParent().getParent().getParent().setLocation(this.getParent().getParent().getParent().getX(), yPosition);
    if (m_nextItem!=null) 
    {
      m_nextItem.adjustPosition(this.getParent().getParent().getParent().getY()+this.getParent().getParent().getParent().getHeight());
    }
    
  }

  /**
   * Paint the button
   * @param g
   */
  public void paint(Graphics g)
  {
    g.setColor(getBackground());
    g.fillRect(getX()+1, getY()+1, getWidth()-2, getHeight()-2);
    getBorderPainter().paint(getPaintContext(),g, getX(), getY(), getWidth(), getHeight());
    int textpos=4;
    if (m_collapsed!=null || m_expanded!=null) 
    {
      if (m_active && m_expanded!=null) 
      {
        int yPos=((m_expanded.getIconHeight()<getHeight()) ? ((getHeight()-m_expanded.getIconHeight())/2) : 2);
        int height=((m_expanded.getIconHeight()<getHeight()) ? m_expanded.getIconHeight() : getHeight()-4);
        g.drawImage(m_expanded.getImage(), 4, yPos, m_expanded.getIconWidth(), height, null);
      } else if (!m_active && m_collapsed!=null) 
      {
        int yPos=((m_collapsed.getIconHeight()<getHeight()) ? ((getHeight()-m_collapsed.getIconHeight())/2) : 2);
        int height=((m_collapsed.getIconHeight()<getHeight()) ? m_collapsed.getIconHeight() : getHeight()-4);
        g.drawImage(m_collapsed.getImage(), 4, yPos, m_collapsed.getIconWidth(), height, m_collapsed.getImageObserver());
      }
      textpos=textpos+Math.max(m_collapsed.getIconWidth(), m_expanded.getIconWidth());
    }
    g.setFont(getFont());
    g.setColor(getForeground());
    g.drawString(getLabel(), textpos, (getHeight()/2)+(g.getFontMetrics().getHeight()-g.getFontMetrics().getDescent())/2);
  }

  /**
   * Load the image given by name. Code is taken from oracle-demo RolloverButton
   * @return loaded Icon or null
   * @param imageName
   */
  private ImageIcon loadImage(String imageName)
  {
    // LoadImage, taken from oracle-demo RolloverButton
    URL imageURL = null;
    ImageIcon result=null;
    boolean loadSuccess=false;
    if ("DEFAULT_EXPANDED".equals(imageName)) 
    {
      result=IMG_EXPANDED;
    } else if ("DEFAULT_COLLAPSED".equals(imageName)) 
    {
      result=IMG_COLLAPSED;
    } else if (!".".equals(imageName)) 
    {
      //JAR
      imageURL = getClass().getResource("/"+imageName);
      if (imageURL != null)
      {
        try
        {
          result = new ImageIcon(Toolkit.getDefaultToolkit().getImage(imageURL));
          loadSuccess = true;
        }
        catch (Exception ilex)
        {
        }
        //DOCBASE
        if (!loadSuccess)
        {
          try
          {
            if (imageName.toLowerCase().startsWith("http://")||imageName.toLowerCase().startsWith("https://"))
            {
              imageURL = new URL(imageName);
            }
            else
            {
              imageURL = new URL(m_codeBase.getProtocol() + "://" + m_codeBase.getHost() + ":" + m_codeBase.getPort() + imageName);
            }
            try
            {
              result= new ImageIcon(createImage((java.awt.image.ImageProducer) imageURL.getContent()));
              loadSuccess = true;
            }
            catch (Exception ilex)
            {
            }
          }
          catch (java.net.MalformedURLException urlex)
          {
          }
        }
        //CODEBASE
        if (!loadSuccess)
        {
          try
          {
            imageURL = new URL(m_codeBase, imageName);
            try
            {
              result= new ImageIcon(createImage((java.awt.image.ImageProducer) imageURL.getContent()));
              loadSuccess = true;
            }
            catch (Exception ilex)
            {
            }
          }
          catch (java.net.MalformedURLException urlex)
          {
          }
        }
      }
    }
    return result;
  }

}