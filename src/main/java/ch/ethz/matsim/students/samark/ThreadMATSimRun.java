package ch.ethz.matsim.students.samark;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class ThreadMATSimRun extends Thread {

	String name;

	public ThreadMATSimRun(String name) {
		this.name = name;
	}

	public void run() {
		try {
			for (int i=1; i<1000; i++) {
				Thread.sleep(1000);
				try {
					Log.write("testLog.txt" , name + " has threadTime [seconds] = " + i);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws InterruptedException, FileNotFoundException {
		PrintWriter pwDefault = new PrintWriter("testLog.txt");
		pwDefault.close();	// Prepare empty defaultLog file for run
		
		Thread t1 = new ThreadMATSimRun("Alpha");
		Thread t2 = new ThreadMATSimRun("Bravo");
		t1.start();
		TimeUnit.MILLISECONDS.sleep(500);
//		Thread.sleep(1000);
		t2.start();
	}

}
