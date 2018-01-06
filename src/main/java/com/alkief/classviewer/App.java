package com.alkief.classviewer;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.MalformedURLException;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;


public class App extends JFrame {
	// Window components
	ClassFileView classView;
	FileListView fileView;
	JSplitPane splitPane;

	// JVM components
	VirtualMachineManager vmm = null;
	LaunchingConnector lc = null;
	VirtualMachine vm = null;

	URLClassLoader classLoader;
	ArrayList<ClassExecutionCounter> classes = new ArrayList<ClassExecutionCounter>();

	public App () {
		initGUI();
		vmm = Bootstrap.virtualMachineManager();
	}

	public void initGUI () {
		classView = new ClassFileView();
		fileView = new FileListView();
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, fileView, classView);

		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		fileView.getList().addListSelectionListener(new ClassSelectionListener());
		fileView.getRunBtn().addActionListener(new RunButtonListener());
		fileView.getChooseFileBtn().addActionListener(new ChooseFileListener());

		add(splitPane);
		setVisible(true);

		splitPane.setDividerLocation(getWidth() / 3);
	}

  public void launchClass () {
  	lc = vmm.defaultConnector(); // Reset launching connector to default

  	// Construct connector argument set
  	Map<String, Connector.Argument> env = lc.defaultArguments();

    String className;
  	// Get the class name selected for launch
    try {
  	 className = fileView.getList().getSelectedValue().toString();
    } catch (NullPointerException e) {
      JOptionPane.showMessageDialog(null, "Unable to get class name from selected value", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

  	// Set the main arg (ie. command-line: java *mainArg*) of the launching connector
  	// Includes all arguments to the java command as well as the file to execute
  	Connector.Argument mainArg = env.get("main");
  	mainArg.setValue("-cp \"" + fileView.getRootFile().getAbsolutePath() + "\"" + className);

    try {
		  vm = lc.launch(env);
  	} catch (Exception e) {
  		JOptionPane.showMessageDialog(null, "Error when launching application VM", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
  	}

  	EventRequestManager erm = vm.eventRequestManager();

  	// Register prepare requests for all classes loaded from selected folder
  	for (ClassExecutionCounter c : classes) {
  		ClassPrepareRequest cpr = erm.createClassPrepareRequest();
  		cpr.addClassFilter(c.getStoredClass().getName());
  		cpr.enable();
  	}

  	// Start thread for monitoring VM event queue
  	Thread eqThread = new Thread(new EventQueueThread());
  	eqThread.start();

  	// Find main thread on VM
  	ThreadReference mainThread = null;
  	for (ThreadReference t : vm.allThreads()) {
  		if ("main".equals(t.name())) {
  			mainThread = t;
  			break;
  		}
  	}

  	// Resume operation of launched application
  	mainThread.resume();
  	vm.resume();
  }

  // Continually attempt to extract event sets from the stored virtual machine instance
  // Based on the event type, we call additional functions to handle the event encountered
  public void monitorEventQueue () {
		EventQueue eq = vm.eventQueue();

  	while (true) {
  		try {
  			EventSet eventSet = eq.remove();

  			for (Event event : eventSet) {

  				if (event instanceof ClassPrepareEvent) {

  					ClassPrepareEvent cpe = (ClassPrepareEvent) event;
  					ReferenceType rt =  cpe.referenceType();
  					registerBreakPoints(rt);

  				} else if (event instanceof BreakpointEvent) { // Registered method is run

            BreakpointEvent be = (BreakpointEvent) event;
            Method m = be.location().method();
  					increaseMethodCount(m); // Increment count of corresponding method

  				} else if (event instanceof AccessWatchpointEvent) { // Registered field is accessed

            AccessWatchpointEvent awe = (AccessWatchpointEvent) event;
            Field f = awe.field();
            // Increment count of corresponding field
            increaseFieldCount(f, awe.location().sourceName()); // Need to access sourceName via event

          }
  			}

        classView.displayClassFile(); // Redraw the class view with updated counters

  			eventSet.resume(); // Resume any threads suspended due to this event set
  		}
      catch (InterruptedException e) {}
      catch (AbsentInformationException e) {}
      catch (VMDisconnectedException e) {
        break; // Allow thread to exit after VM disconect
      }
  	} // End while loop
  }

  // Use the source name of the corresponding method to locate counter and increment
  // the count of the given method name
  public void increaseMethodCount (Method m) {
    try {
      String sourceName = m.location().sourceName();
      for (ClassExecutionCounter cec : classes) {
        if (cec.getSourceName().equals(sourceName))
          cec.incrementMethodCount(m.name());
      }
    }
    catch (AbsentInformationException e) {}
  }

  // Use the source name of the corresponding field to locate counter and increment
  // the count of the given field name
  public void increaseFieldCount (Field f, String sourceName) {
    for (ClassExecutionCounter cec : classes) {
      if (cec.getSourceName().equals(sourceName))
        cec.incrementFieldCount(f.name());
    }
  }

  // Register breakpoints for the given reference type
  // Assume only Class reference types are passed
  public void registerBreakPoints (ReferenceType rt) {
    // Set the source name of the counter which holds the corresponding reference type class
    // This is used to locate the correct counter for incrementing field / method counts
    try {
      for (ClassExecutionCounter cec : classes) {
        if (rt.name().equals(cec.getStoredClass().getName())) {
          cec.setSourceName(rt.sourceName());
        }
      }
    }
    catch (AbsentInformationException e) {}

  	EventRequestManager erm = vm.eventRequestManager();

    // Register events for field accesses
  	for (Field field : rt.fields()) {
  		AccessWatchpointRequest awr = erm.createAccessWatchpointRequest(field);
      awr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
  		awr.enable();
  	}

    // Register events for method executions
  	for (Method method : rt.methods()) {
  		BreakpointRequest  bpr = erm.createBreakpointRequest(method.location());
      bpr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
  		bpr.enable();
  	}
  }

  // Rebuilds the stored URLClassLoader when root directory is changed
  public void buildClassLoader () {
		URL root = null;

    try {
			root = fileView.getRootFile().toURI().toURL();
		} catch (MalformedURLException e) {
      JOptionPane.showMessageDialog(null, "Unable to form URL from selected folder",
                                    "URL Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }

		URL[] urlArray = { root };

  	classLoader = new URLClassLoader(urlArray);
  }

  public void loadAllClasses () {
  	classes.clear(); // Remove previous counters from list

  	for (File f : fileView.getChosenFiles()) {
  		Class c = null;
  		File parent = f.getParentFile();

  		// Remove file extension (.class)
  		String className = f.getName().substring(0, f.getName().lastIndexOf("."));

      // Prepend package names to build fully qualified class name
	  	while (!parent.getAbsolutePath().equals(fileView.getRootFile().getAbsolutePath())) {
				className = parent.getName() + "." + className;
				parent = parent.getParentFile(); // Move parent up a layer
	  	}

	  	try {
	  		c = classLoader.loadClass(className); // Load the selected class
	  	} catch (ClassNotFoundException e) {
        continue; // Skip to next class if not found by URLClassLoader
      }

      // Store the loaded class as a counter object in list
	  	if (c != null)
	  		classes.add(new ClassExecutionCounter(c));
  	}
  }

  public static void main(String[] args) {
  	App app = new App();
  }

  // Listen for EventSets from the EventQueue of VM stored in App
  private class EventQueueThread implements Runnable {
  	public void run () {
  		monitorEventQueue();
  	}
  }

  // Listen for a class file to be selected from left pane list
  private class ClassSelectionListener implements ListSelectionListener {
  	public void valueChanged (ListSelectionEvent event) {
      try {
        // Display class contents in classView pane
        for (ClassExecutionCounter cec : classes) {
          // If the class file of a given CEC matches the class selected from list
          if (cec.getStoredClass().getName().equals(fileView.getList().getSelectedValue().toString())) {
            classView.setClassExecutionCounter(cec); // Set the counter to be displayed by classView
            classView.displayClassFile(); // Display
          }
        }
      } catch (NullPointerException e) {
        // JList is in transitive state from changing project folders
      }
  	}
  }

  // Listen for run button to be pressed
  private class RunButtonListener implements ActionListener {
    // Attempt to launch the selected class file
  	public void actionPerformed (ActionEvent e) {
  		if (fileView.getRootFile() != null)
  			launchClass();
      else
        JOptionPane.showMessageDialog(null, "Unable to launch class", "Warning", JOptionPane.WARNING_MESSAGE);
  	}
  }

  // Listen for choose file button to be pressed
	private class ChooseFileListener implements ActionListener {
		public void actionPerformed (ActionEvent e) {
			fileView.chooseFile();
      classView.reset();
			buildClassLoader();
			loadAllClasses();
		}
	}
}