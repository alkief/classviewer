package com.alkief.classviewer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

// Visualizes the internals of a given class file
public class ClassFileView extends JPanel {
	JScrollPane scrollPane;

	JTextArea rowHeader;
	JTextArea area;

	ClassExecutionCounter cec = null;

	public ClassFileView () {
		setLayout(new BorderLayout());

		area = new JTextArea();
		area.setTabSize(2);
		area.setBackground(new Color(200, 200, 200));
		area.setEditable(false);

		rowHeader = new JTextArea();
		rowHeader.setColumns(5);
		rowHeader.setBackground(new Color(175, 175, 175));

		scrollPane = new JScrollPane(area);
		scrollPane.setRowHeaderView(rowHeader);

		add(scrollPane);
	}

	public void displayClassFile () {
		// Remove previous text from area and rowheader
		area.setText("");
		rowHeader.setText("");

		// Get class from counter for reflection access
		Class c = cec.getStoredClass();

		// Display package name
		area.append(c.getPackage().getName() + "\n\n");
		rowHeader.append("\n\n");

		area.append(Modifier.toString(c.getModifiers()) + " " + c.getName() + " {\n\n");
		rowHeader.append("\n\n");

		appendFields(c);

		area.append("\n");
		rowHeader.append("\n");

		appendConstructors(c);

		area.append("\n");
		rowHeader.append("\n");

		appendMethods(c);
	}

	public void appendFields (Class c) {
		area.append("\t// Fields\n");
		rowHeader.append("\n");

		for (Field f : c.getDeclaredFields()) {
			String modifiers = Modifier.toString(f.getModifiers());
			area.append("\t" + modifiers + " " + f.getName() + ";\n");

			int fieldCount = cec.getFieldCount(f.getName());
			rowHeader.append(fieldCount + "\n");
		}
	}

	public void appendMethods (Class c) {
		area.append("\t// Methods\n");
		rowHeader.append("\n");

		for (Method m : c.getDeclaredMethods()) {
			String modifiers = Modifier.toString(m.getModifiers());
			String methodName = m.getReturnType().getCanonicalName();
			area.append("\t" + modifiers + " " + methodName + " " + m.getName() + "(");

			// Append param types
			for (int i = 0; i < m.getParameterCount(); i++) {
				Class paramType = m.getParameterTypes()[i];
				if (i != m.getParameterCount() - 1) {
					area.append(paramType.getCanonicalName() + ", ");
				} else {
					area.append(paramType.getCanonicalName());
				}
			}

			area.append(") {}\n");

			int methodCount = cec.getMethodCount(m.getName());
			rowHeader.append(methodCount + "\n");
		}
	}

	public void appendConstructors (Class c) {
		area.append("\t// Constructors\n");
		rowHeader.append("\n");

		for (Constructor ctr : c.getDeclaredConstructors()) {
			String modifiers = Modifier.toString(ctr.getModifiers());
			String ctrName = ctr.getName().substring(ctr.getName().lastIndexOf(".") + 1, ctr.getName().length());
			area.append("\t" + modifiers + " " + ctrName + " (");

			// Append parameter types
			for (int i = 0; i < ctr.getParameterCount(); i++) {
				Class paramType = ctr.getParameterTypes()[i];
				if (i != ctr.getParameterCount() - 1) {
					area.append(paramType.getCanonicalName() + ", ");
				} else {
					area.append(paramType.getCanonicalName());
				}
			}

			area.append(") {}\n");

			rowHeader.append("\n");
		}
	}

	public void setClassExecutionCounter (ClassExecutionCounter cec) {
		this.cec = cec;
	}

	public void reset () {
		area.setText("");
		rowHeader.setText("");
	}
}