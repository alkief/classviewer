package com.alkief.classviewer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.ArrayList;
import java.net.URL;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

// Exposes application control buttons and visualizes a list of files contained in a selected
// directory
public class FileListView extends JPanel {
	ArrayList<File> selectedFiles;
	File rootFile = null;

	JPanel buttonGroup = new JPanel();
	JButton chooseFileBtn = new JButton("Choose directory");
	JButton runBtn = new JButton("Run");

	JFileChooser chooser = new JFileChooser();

	DefaultListModel<String> listModel = new DefaultListModel<String>();
	JList classList = new JList();

	public FileListView () {
		setLayout(new BorderLayout());

		// Configure button / button display
		buttonGroup.setLayout(new FlowLayout());
		buttonGroup.add(chooseFileBtn);
		buttonGroup.add(runBtn);
		add(buttonGroup, BorderLayout.NORTH);

		// Configure JList display for selected files
		classList.setVisibleRowCount(-1);
		classList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		classList.setLayoutOrientation(JList.VERTICAL);
		classList.setModel(listModel);
		add(classList, BorderLayout.CENTER);

		// Configure file chooser
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}

	// Use file chooser to store selected file or all files in selected directory+subdirectories
	public void chooseFile () {
		selectedFiles = new ArrayList<File>(); // Clear previous file selections

		int chooserVal = chooser.showOpenDialog(this);

		if (chooserVal == JFileChooser.APPROVE_OPTION) {
			rootFile = chooser.getSelectedFile(); // Set the root selected file

			if (chooser.getSelectedFile().isFile()) {
				selectedFiles.add(chooser.getSelectedFile()); // Store only a single file
			} else if (chooser.getSelectedFile().isDirectory()) {
				getFilesInDirectory(chooser.getSelectedFile()); // Recursively store files
			}
		}

		displayFiles();
	}

	// Recursively iterate through the given directory and all subdirectories to store '.class'
	// files in selectedFiles list
	public void getFilesInDirectory (File directory) {
		for (File f : directory.listFiles()) {
			if (f.isDirectory()) {
				getFilesInDirectory(f);
			} else {
				int i = f.getName().lastIndexOf(".");
				if (i > 0 && f.getName().substring(i+1).equals("class")) {
					selectedFiles.add(f);
				}
			}
		}
	}

	// Populate the class list with filename Strings
	public void displayFiles () {
		listModel.clear();

		for (File f : selectedFiles) {
			File parent = f.getParentFile();
			String className = f.getName().substring(0, f.getName().lastIndexOf("."));

	  	while (!parent.getAbsolutePath().equals(getRootFile().getAbsolutePath())) {
				className = parent.getName() + "." + className;
				parent = parent.getParentFile(); // Move parent up a layer
	  	}

			listModel.addElement(className);
		}

		repaint();
	}

	// Return the file object corresponding to the name selected in the class list
	public File getSelectedFile () {
		File file = null;

		for (File f : selectedFiles) {
			if (f.getName().equals(classList.getSelectedValue())) {
				file = f;
				break;
			}
		}

		return file;
	}

	public ArrayList<File> getChosenFiles () {
		return selectedFiles;
	}

	public JList getList () {
		return classList;
	}

	public JButton getRunBtn () {
		return runBtn;
	}

	public JButton getChooseFileBtn () {
		return chooseFileBtn;
	}

	public File getRootFile () {
		return rootFile;
	}


}