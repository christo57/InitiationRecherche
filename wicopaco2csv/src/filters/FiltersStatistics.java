package filters;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import parser.CsvFileWriter;

public abstract class FiltersStatistics {
	
	protected boolean outputOn = false;
	protected CsvFileWriter outputFile = null;
	protected Map<String, String> map = new HashMap<String, String>();
	
	public void activateOutput() {
		outputOn = true;
		createCSVOutput();
	}
	
	
	public void closeOutput() {
		if(outputOn) {
			try {
				outputFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public abstract void createCSVOutput();
	
	public abstract void printStatistics();
}
