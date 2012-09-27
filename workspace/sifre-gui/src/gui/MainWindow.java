package gui;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;

// TODO 
// # Add a console argument so a file for decryption can be provided at startup.
// # Change look and feel from Java to some kind of native
// # Keyboard shortcuts for the menu bar.
// . An "about" menu in the menu bar with an item that opens an "about" box.
// . A status bar.
// # Add very fast shortcuts 
// . Maybe make it start at system startup so that JVM starts then instead at
//   program startup thereby making the program take a long time to start.

// . Add a nice search feature to the text editor. Something like Chrome's.
// # See if there are Vim-like JTextArea equivalents.
// . Start using the idea for having a unit in the code that stores all the 
//   text strings for all the elements of the GUI so that the program can be
//   set to use any language its been translated to.

// . Hide the letters of the password in the entry dialog.
// . Have a byte counter/display in the password entry dialog.  
// . Have a help button in the password entry dialog which explains the 
//   encryption algorithm, password -> key translation, handling of the cases
//	 when a password isn't exactly 128, 192 or 256 bits.
// . Solve the problem with not being able to use a 32-byte password in Win7.
// . Check out the AES strengths that are available. 
// . Check out the advantages of using salts.
// . Switch from ECB to some other type. Set up proper IV handling.

// . Put it onto GitHub or something. 
// . Discuss starting a nicely designed, simple web site for Sifre with D.D.
// . See if this program or its variants could be sold (AES not free).
// . Think about variants of the program:
//     . A very simple all-around file encrypter/decrypter.
//     . A simple program that encrypts certain directories and has them 
//       decrypted only when they are needed (or the program that uses them is
//       turned on - e.g. Messenger history).

// . Refactor into a MVC or MVP pattern.

public class MainWindow {

	private final static String ENCRYPTION_ALGORITHM = "AES";

	private final static String WINDOW_TITLE = "Sifre 0.3";
	private final static String LBL_STATUS_BASIC = "Status: ";
	private final static String LBL_STATUS_EMPTY = "Content not loaded";
	private final static String LBL_STATUS_LOADED = "Content loaded";
	private final static String LBL_STATUS_LOAD_ERROR = 
			"Error occurred while reading file";
	private final static String LBL_STATUS_FILE_TOO_BIG = "File too big";
	private final static String LBL_STATUS_DECRYPTION_ERROR = 
			"Error in the decryption process";
	private final static String LBL_STATUS_ENCRYPTION_ERROR = 
			"Error in the encryption process";
	private final static String LBL_STATUS_ENCRYPTED = 
			"Encrypted the content and written it to a file";
	private final static String LBL_STATUS_SAVE_ERROR = 
			"Error occurred while trying to write the content to a file.";
	private final static String LBL_CURR_FILE_BASIC = "Current file:";
	
	private JFrame frmSifre;
	private JLabel lblStatus; 
	private JLabel lblCurrentFile; 
	private JTextArea contentHolder; 
	
	private JMenuItem mntmOpen;
	private JMenuItem mntmSave; 
	private JMenuItem mntmSaveAs; 
	private JMenuItem mntmQuit; 
	
	private ActionListener menuItemOpenListener;
	private ActionListener menuItemSaveListener;
	private ActionListener menuItemSaveAsListener;
	private ActionListener menuItemQuitListener;
	
	private JFileChooser chooser;
	private File inputFile;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		File argumentFileForChecking = null;
		final int nrArgs = args.length;
		boolean openFile = true;
		 
		switch (nrArgs) {
			case 0: 
				openFile = false;
				break;
			case 1: 
				break;
			default: // Wrong number of parameters.
				System.out.println("Wrong number of arguments. Only one is " 
						+ "expected (path to the file that should be opened).");
				openFile = false;
				break;
		}
		if (openFile) {
			argumentFileForChecking = new File(args[0]);
			if (!argumentFileForChecking.isFile()) {
				System.out.println("The specified argument isn't a path to a" 
						+ " file on the disk.");
				openFile = false;
			}
		}
		final File argumentFile = openFile ? argumentFileForChecking : null;
			
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow(argumentFile);
					window.frmSifre.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow(File argumentFile) {
		chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		initialize();
		if (argumentFile != null) {
			loadContentFromFile(argumentFile);
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		prepareListeners();
		
		frmSifre = new JFrame();
		frmSifre.setTitle(WINDOW_TITLE);
		frmSifre.setBounds(100, 100, 720, 700);
		frmSifre.setLocationRelativeTo(null);
		frmSifre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		contentHolder = new JTextArea();
		contentHolder.setFont(new Font("Droid Sans Mono", Font.PLAIN, 13));
		JScrollPane scroller = new JScrollPane(contentHolder);
		
		lblStatus = new JLabel(LBL_STATUS_BASIC + LBL_STATUS_EMPTY);
		lblStatus.setFont(new Font("Droid Sans Mono", Font.PLAIN, 12));
		
		lblCurrentFile = new JLabel(LBL_CURR_FILE_BASIC);
		lblCurrentFile.setFont(new Font("Droid Sans Mono", Font.PLAIN, 12));
		GroupLayout groupLayout = new GroupLayout(frmSifre.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(scroller/*contentHolder*/, GroupLayout.DEFAULT_SIZE, 680, Short.MAX_VALUE)
						.addComponent(lblCurrentFile)
						.addComponent(lblStatus))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(8)
					.addComponent(lblCurrentFile)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(lblStatus)
					.addGap(12)
					.addComponent(scroller/*contentHolder*/, GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
					.addContainerGap())
		);
		frmSifre.getContentPane().setLayout(groupLayout);
		
		JMenuBar menuBar = new JMenuBar();
		frmSifre.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		mnFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(mnFile);
		
		mntmOpen = new JMenuItem("Open");
		mntmOpen.setMnemonic(KeyEvent.VK_O);
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 
				ActionEvent.CTRL_MASK));
		mntmOpen.addActionListener(menuItemOpenListener);
		mnFile.add(mntmOpen);
		
		mntmSave = new JMenuItem("Save");
		mntmSave.setMnemonic(KeyEvent.VK_S);
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
				ActionEvent.CTRL_MASK));
		mntmSave.addActionListener(menuItemSaveListener);
		mnFile.add(mntmSave);
		
		mntmSaveAs = new JMenuItem("Save as");
		mntmSaveAs.setMnemonic(KeyEvent.VK_A);
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
				ActionEvent.CTRL_MASK));
		mntmSaveAs.addActionListener(menuItemSaveAsListener);
		mnFile.add(mntmSaveAs);
		
		mntmQuit = new JMenuItem("Quit");
		mntmQuit.setMnemonic(KeyEvent.VK_C);
		mntmQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.CTRL_MASK));
		mntmQuit.addActionListener(menuItemQuitListener);
		mnFile.add(mntmQuit);
	}
	
	private void prepareListeners() {
		menuItemOpenListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadContentFromFile(null);
			}
		};
		menuItemSaveListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveContentToFile(inputFile);
			}
		};
		menuItemSaveAsListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveContentToFile(null);
			}
		};
		menuItemQuitListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Check if something should be saved maybe. 
				System.exit(0); // Ugly?
			}
		};
	}
	
	private void loadContentFromFile(File argumentFile) {
		inputFile = null; 
		if (argumentFile != null) {
			inputFile = argumentFile;
			lblCurrentFile.setText(LBL_CURR_FILE_BASIC + inputFile.getName());
		} else {
			// Show open file dialog.
			int fcReturnValue = chooser.showOpenDialog(mntmOpen);
			if (fcReturnValue == JFileChooser.APPROVE_OPTION) {
				inputFile = chooser.getSelectedFile();
				lblCurrentFile.setText(LBL_CURR_FILE_BASIC 
						+ inputFile.getName());
			} else {
				return;
			}
		}
		// Show password dialog. 
		String password = (String) JOptionPane.showInputDialog(mntmOpen, 
				"Enter the encryption password:", 
				"Password entry", 
				JOptionPane.PLAIN_MESSAGE, 
				null, 
				null, 
				"");
		byte[] key;
		try {
			key = password.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			lblStatus.setText(LBL_STATUS_BASIC + "Error processing password");
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		}
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, 
				ENCRYPTION_ALGORITHM);
		
		// Read the selected file.
		byte[] encrypted = null; 
		try {
			InputStream is = new FileInputStream(inputFile);
			long fileLengthL = inputFile.length(); // In bytes.
			if (fileLengthL > 0 && fileLengthL < Integer.MAX_VALUE)
				encrypted = new byte[(int)fileLengthL];
			else
				throw new Exception("File too big.");
			int nrBytesRead = 0;
			try {
				nrBytesRead = is.read(encrypted);
				if (nrBytesRead != fileLengthL) 
					throw new Exception("Too few or too many bytes read.");
			} catch (IOException e) {
				e.printStackTrace();
				lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_LOAD_ERROR);
				JOptionPane.showMessageDialog(lblStatus, e.getMessage());
				return;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_LOAD_ERROR);
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		} catch (Exception e) {
			e.printStackTrace();
			lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_FILE_TOO_BIG);
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		}
		
		// Decrypt the read contents.
		Cipher cipher = null; 
		byte[] decrypted = null;
		try {
			cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
			decrypted = cipher.doFinal(encrypted);
		} catch (Exception e) {
			lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_DECRYPTION_ERROR);
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		} 
		
		// Put the decrypted contents into the text pane.
		String decryptedContents = new String(decrypted);
		contentHolder.setText(decryptedContents);
		contentHolder.setCaretPosition(0);
		
		lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_LOADED);
	}

	private void saveContentToFile(File outputFile) {
		File chosenFile = outputFile; 
		if (chosenFile == null) {
			// Show save file dialog.
			int fcReturnValue = chooser.showSaveDialog(mntmSaveAs);
			if (fcReturnValue == JFileChooser.APPROVE_OPTION) {
				chosenFile = chooser.getSelectedFile();
			} else {
				return;
			}
		}
		
		// Show password dialog. 
//		boolean passwordSuccess = false;
//		do {
		String password = (String) JOptionPane.showInputDialog(mntmOpen, 
				"Enter the encryption password:", 
				"Password entry", 
				JOptionPane.PLAIN_MESSAGE,
				null, 
				null, 
				"");
		byte[] key;
		try {
			key = password.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			lblStatus.setText(LBL_STATUS_BASIC + "Error processing password");
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		}
		SecretKeySpec secretKeySpec = new SecretKeySpec(key, 
				ENCRYPTION_ALGORITHM);
		
		// Take the content from the text pane.
		byte[] plain;
		try {
			plain = contentHolder.getText().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		} 
		
		// Encrypt the content.
		Cipher cipher = null; 
		byte[] encrypted = null;
		try {
			cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
			encrypted = cipher.doFinal(plain);
		} catch (Exception e) {
			lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_ENCRYPTION_ERROR);
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		} 
//		passwordSuccess = true; 
//		} while (!passwordSuccess);
		
		// Write the  encrypted content into a file.
		OutputStream os;
		try {
			os = new FileOutputStream(chosenFile);
			os.write(encrypted);
		} catch (Exception e) {
			lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_SAVE_ERROR);
			e.printStackTrace();
			JOptionPane.showMessageDialog(lblStatus, e.getMessage());
			return;
		}
		
		lblStatus.setText(LBL_STATUS_BASIC + LBL_STATUS_ENCRYPTED);
		}
}
