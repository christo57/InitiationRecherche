package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class CsvFileWriter {

    private File file;
    private char separator;
    private FileWriter fw;
    private BufferedWriter bw;
    private String[] titles;
    

    public CsvFileWriter(File myFile, char sep, String[] tit) throws IOException {
    		file = myFile;
    		separator = sep;
    		fw = new FileWriter(file);
    	    bw = new BufferedWriter(fw);
    	    titles = tit;
    	    
    	    	// Les titres
        boolean first = true;
        for (String title : titles) {
            if (first) {
                first = false;
            } else {
                bw.write(separator);
            }
            write(title, bw);
        }
        bw.write("\n");
        bw.flush();
    }

  
    public void write(Map<String, String> data) throws IOException {

        if (data == null) {
            throw new IllegalArgumentException("la liste ne peut pas être nulle");
        }
        if (titles == null) {
            throw new IllegalArgumentException("les titres ne peuvent pas être nuls");
        }
        if (data.isEmpty()) {
	        	System.out.println("contenu vide");
	    		System.exit(0);
        }

        
        for (int i = 0 ; i < titles.length ; i++) {
            write(data.get(titles[i]), bw);
            if(i < titles.length-1) {
            		write(""+separator, bw);
            }
        }
        bw.write("\n");
        bw.flush();
    }

    
 

    private void write(String value, BufferedWriter bw) throws IOException {

        if (value == null) {
            value = "";
        }

        bw.write(value);
    }
    
    /*
     * ferme les buffer
     */
    public void close() throws IOException {
		bw.close();
    		fw.close();
    }
}