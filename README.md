# Classviewer

The goal of this project was to explore some of the functionality provided by [Java Debug Interface](https://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/).
I've always been interested in the process IDEs use to allow for things such as
breakpoints, stepping through code, etc., and so I decided to do some exploration into the internals
of the language to see what I could find.

This application allows the user to load a list of classes by selecting a directory that contains
the desired classes. The subdirectory structure of this root directory must mirror that of the
source code (ie. subdirectories represent packages, and classes are stored in the correct
locations). A list of discovered class files is displayed on the left, and the user is able to select and run
a given class provided it contains the main method. Upon selecting a class from the list, the
application will visualize all constructors, fields and methods declared within the class, as
well as the number of times each field has been accessed or each method has been run if the user
launches the application.

*NOTE:* It is entirely possible for class files to be stored in any number of desired locations after
compilation, however, tools such as Maven (which this project was made using) maintain directory
structure when compiling source code. The assumption of preserving subdirectory structure allows
the process of identifying the fully qualified name (ie. java.lang.String) of a given class to be
simplified when attempting to load the selected classes via ClassLoader.

## Implementation

The application uses the [Reflect API](https://docs.oracle.com/javase/7/docs/api/java/lang/reflect/package-summary.html)
in addition to the Java Debug Interface API to provide all of its functionality. When running
a target class, the application launches a separate JVM by using the [VirtualMachineManager](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/VirtualMachineManager.html)
interface instance provided by the [Bootstrap](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/Bootstrap.html)
class to create a [LaunchingConnector](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/connect/LaunchingConnector.html).
This connector is then configured to launch using a given classpath argument, and the desired
fully qualified class name. A [ClassPrepareRequest](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/request/ClassPrepareRequest.html)
is registered for all classes found within the selected root project folder after successfully
launching a class, and a thread is started to monitor the [EventQueue](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/event/EventQueue.html)
of the launched virtual machine so that [BreakpointRequests](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/request/BreakpointRequest.html)
(for methods) and [AccessWatchpointRequests](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/request/AccessWatchpointRequest.html)
(for fields) can be registered for all loaded classes. This same thread will then encounter
[BreakpointEvents](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/event/BreakpointEvent.html)
and [AccessWatchpointEvents](https://docs.oracle.com/javase/9/docs/api/com/sun/jdi/event/AccessWatchpointEvent.html)
corresponding to the requests that were registered, and these events are
used to increment a counter corresponding to each method and field in each loaded class. From there,
it is a simple matter of representing this information visually and updating the display as these
events continue to occur.

## To run

This project was created using Java 9.0.1 and Maven, so be sure have both of those installed.

`cd <directory you want to store the project>`


`git clone <this repo>`


`cd <project root (where pom.xml is)>`


`mvn compile exec:java`

It is worth noting that usage of the JDI limits optimizations that can be made were an application launched
normally. In addition to this, my code does not make accomodations for multithreading. As such,
you will find that large applications, or those that are even moderately computationally intensive
will cause many problems if you try to run them via this application.

## Screenshots

A freshly loaded project folder (this project)

![img1](https://i.imgur.com/YFzKcww.png)

Selecting a class

![img2](https://i.imgur.com/lV2YCzN.png)

Here I run a small (far from finished) side project meant to be a simplified clone of one of my
favorite game series, Fire Emblem.

You can see the number of times the Tile class' paintComponent method has been run is expectedly
quite high:

![img3](https://i.imgur.com/G2uAO6E.png)

And again after resizing the window, which calls the same method many times:

![img4](https://i.imgur.com/2siro9O.png)