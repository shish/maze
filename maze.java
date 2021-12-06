/**
 * maze.java - a maze game in java
 * (c) Shish 2003, licensed under the GPL
 */

import java.io.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;


/**
 * The loader class, creates a frame and puts a MazeGame in it
 */
public class maze implements WindowListener {
	private Frame frame;

	public static void main(String[] args) {
		new maze();
	}

	public maze() {
		frame = new Frame("Maze 0.0.0");
		frame.setLayout(new BorderLayout());
		frame.add(new MazeGame(), BorderLayout.CENTER);
		frame.pack();
		frame.setSize(16*16, 16*16+16);
		frame.setResizable(false);
		frame.addWindowListener(this);
		center(frame);
		frame.show();
	}

	public static void center(Window win) {
		int x, y, h, w;
		Dimension d = win.getSize();
		w = d.width;
		h = d.height;
		d = Toolkit.getDefaultToolkit().getScreenSize();
		x = d.width;
		y = d.height;
		win.setLocation((x - w) / 2, (y - h) / 2);
	}

	public void windowOpened(WindowEvent we) {}
	public void windowClosing(WindowEvent we) {System.exit(0);}
	public void windowClosed(WindowEvent we) {}
	public void windowMinimized(WindowEvent we) {}
	public void windowMaximized(WindowEvent we) {}
	public void windowActivated(WindowEvent we) {}
	public void windowDeactivated(WindowEvent we) {}
	public void windowIconified(WindowEvent we) {}
	public void windowDeiconified(WindowEvent we) {}
}


/**
 * The main class, does the logic and such
 */
class MazeGame extends Canvas implements KeyListener {
	private int mapon;
	private int maps=0;
	private char[][][] map;
	private Image buf;
	private Graphics g;
	private int x=1,y=1,xs=0,ys=0;	// current / speed
	private int[] sx,sy,tx,ty,pa;	// target / start / paces
	private int steps=0;
	private char face='e';
	private int ih, iw;

	private static final int TITLE=0, GAME=1, PASS=2, FAIL=3;
	private int state=TITLE;
	private int pause=0;


	/**
	 * Inits stuff, calls loader methods
	 */
	public MazeGame() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("maps.dat"));
			maps = Integer.parseInt(in.readLine());
			map = new char[maps][][];
			tx = new int[maps];
			ty = new int[maps];
			sx = new int[maps];
			sy = new int[maps];
			pa = new int[maps];
			for(int i=0; i<maps; i++) {
				loadmap(i, in);
			}
		}
		catch(Exception e) {
			System.out.println("Error loading maps.dat: "+e);
		}

		addKeyListener(this);
		requestFocus();
	}


	/**
	 * Loads a map from an input stream to a speified slot
	 */
	private void loadmap(int i, BufferedReader in) throws IOException {
		StringTokenizer st = new StringTokenizer(in.readLine());
		int wi = ni(st); // width & height are supplied forwards but 
		map[i] = new char[ni(st)][wi]; // stored backwards
		sx[i] = ni(st);
		sy[i] = ni(st);
		tx[i] = ni(st);
		ty[i] = ni(st);
		pa[i] = ni(st);
		for(int j=0; j<map[i].length; j++) {
			for(int k=0; k<map[i][j].length; k++) {
				map[i][j][k] = (char)in.read();
			}
			in.readLine(); // clear to end of line
		}
		steps = pa[0]; // init paces left
	}


	/**
	 * next int - convinience method to save space
	 */
	private final int ni(StringTokenizer st) {
		return Integer.parseInt(st.nextToken());
	}


	/**
	 * gets input, deals with it
	 */
	public void keyPressed(KeyEvent ke) {
		switch(state) {
			case TITLE: state = GAME; break;
			case GAME: move(ke.getKeyChar()); break;
			case PASS: state = GAME; break;
			case FAIL: state = GAME; break;
		}
		repaint();
	}
	private void move(char key) {
		switch(key) {
			case 'n': case '8': xs= 0; ys=-1; face='n'; break;
			case 'e': case '6': xs= 1; ys= 0; face='e'; break;
			case 's': case '2': xs= 0; ys= 1; face='s'; break;
			case 'w': case '4': xs=-1; ys= 0; face='w'; break;

			case 'f': move(face); break;
					  // FIXME 'b' broke
					  //case 'b': xs=-xs; ys=-ys; break;

					  // FIXME ugly hack - is there no better way?
			case 'r':
					  if(face=='n') face='w';
					  else if(face=='e') face='n';
					  else if(face=='s') face='e';
					  else if(face=='w') face='s';
					  break;

			case 'l':
					  if(face=='n') face='e';
					  else if(face=='e') face='s';
					  else if(face=='s') face='w';
					  else if(face=='w') face='n';
					  break;

			case 'q':
					  System.exit(0);
					  break;
		}
		if(key != 'f' && key != 'l' && key != 'r' && free(x+xs, y+ys)) {
			steps--;
			x+=xs;
			y+=ys;
			if(x == tx[mapon] && y == ty[mapon]) {
				//state = PASS; // the pass message is annoying
				mapon++;
				if(mapon >= maps) mapon = 0;
				x=sx[mapon];
				y=sx[mapon];
				steps = pa[mapon];
				buf = null; // remake the buffer the right size for the new map
			}
		}

		/**
		 * else, heh. You couldn't fail until the above was false, ie
		 * you use l/f/r or hit a wall
		 */
		if(steps < 0) { 
			state = FAIL;
			x=sx[mapon];
			y=sx[mapon];
			steps = pa[mapon];
		}
	}
	public void keyReleased(KeyEvent ke) {}
	public void keyTyped(KeyEvent ke) {}


	/**
	 * returns whether a square is free or not
	 */
	private final boolean free(int x0, int y0) {
		return (map[mapon][y0][x0] == ' ');
	}


	/**
	 * Overrides default update for faster graphics
	 */
	public void update(Graphics g) {
		paint(g);
	}


	/**
	 * Creates the buffer if necessary
	 * Draws the blocks
	 * Prints any messages
	 */
	public void paint(Graphics gtop) {
		if(buf == null) {
			iw = map[mapon][0].length*16;
			ih = map[mapon].length*16+16;

			buf = createImage(iw, ih);
			g = buf.getGraphics();
			getParent().setSize(iw, ih);
		}
		g.setColor(Color.white);
		g.fillRect(0, 0, iw, ih);
		g.setColor(Color.black);
		for(int i=0; i<map[mapon].length; i++) {
			for(int j=0; j<map[mapon][i].length; j++) {
				printBlock(g, map[mapon][i][j], j*16, i*16);
			}
		}
		printBlock(g, face, x*16, y*16);
		printBlock(g, 'o', tx[mapon]*16, ty[mapon]*16);
		g.drawString(getOptions(), 5, ih-2);

		switch(state) {
			case TITLE:	drawCenter("JMaze"); break;
			case PASS:	drawCenter("You Passed!"); break;
			case FAIL:	drawCenter("You Failed!"); break;
		}

		gtop.drawImage(buf, 0, 0, this);
	}


	/**
	 * Draws a single line string in the center of the screen
	 */
	private void drawCenter(String s) {
		int sw = g.getFontMetrics().stringWidth(s);
		int yo = ih/2-7;
		int xo = iw/2-sw/2;
		g.setColor(Color.white);
		g.fillRect(xo-2, yo, sw+4, 14);
		g.setColor(Color.black);
		g.drawRect(xo-2, yo, sw+4, 14);
		g.drawString(s, xo, yo+13);
	}


	/**
	 * returns the available directions
	 */
	private String getOptions() {
		StringBuffer op = new StringBuffer(
				"Available directions: ");
		if(free(x, y-1)) op.append("n, ");
		if(free(x+1, y)) op.append("e, ");
		if(free(x, y+1)) op.append("s, ");
		if(free(x-1, y)) op.append("w, ");
		return op.toString();
	}


	/**
	 * Prints a symbol based on the passed char
	 */
	private void printBlock(Graphics g, char block, int xoff, int yoff) {
		switch(block) {
			// Lines ///////////////
			case '|':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				break;

			case '-':
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				break;


				// Cross ///////////////
			case '+':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				break;


				// T-junctions /////////
			case 'v':
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				g.drawLine(xoff+8, yoff+8, xoff+8, yoff+16);
				break;

			case '^':
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				g.drawLine(xoff+8, yoff, xoff+8, yoff+8);
				break;

			case '<':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				g.drawLine(xoff, yoff+8, xoff+8, yoff+8);
				break;

			case '>':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				g.drawLine(xoff+8, yoff+8, xoff+16, yoff+8);
				break;


				// Corners /////////////
			case 'F':
				g.drawLine(xoff+8, yoff+8, xoff+8, yoff+16);
				g.drawLine(xoff+8, yoff+8, xoff+16, yoff+8);
				break;

			case 'T':
				g.drawLine(xoff, yoff+8, xoff+8, yoff+8);
				g.drawLine(xoff+8, yoff+8, xoff+8, yoff+16);
				break;

			case 'L':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+8);
				g.drawLine(xoff+8, yoff+8, xoff+16, yoff+8);
				break;

			case 'J':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+8);
				g.drawLine(xoff, yoff+8, xoff+8, yoff+8);
				break;


				// Arrows //////////////
			case 'n':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				g.drawLine(xoff, yoff+8, xoff+8, yoff);
				g.drawLine(xoff+8, yoff, xoff+16, yoff+8);
				break;

			case 'e':
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				g.drawLine(xoff+8, yoff, xoff+16, yoff+8);
				g.drawLine(xoff+16, yoff+8, xoff+8, yoff+16);
				break;

			case 's':
				g.drawLine(xoff+8, yoff, xoff+8, yoff+16);
				g.drawLine(xoff, yoff+8, xoff+8, yoff+16);
				g.drawLine(xoff+8, yoff+16, xoff+16, yoff+8);
				break;

			case 'w':
				g.drawLine(xoff, yoff+8, xoff+16, yoff+8);
				g.drawLine(xoff+8, yoff, xoff, yoff+8);
				g.drawLine(xoff, yoff+8, xoff+8, yoff+16);
				break;


				// Other ///////////////
			case 'o':
				g.drawOval(xoff+2, yoff+2, 14, 14);
				break;
		}
	}
}

