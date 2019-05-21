package filters.purifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import filters.FiltersStatistics;
import parser.CsvFileWriter;

public class SpecialCaracterPurifier extends FiltersStatistics implements PurifierFilter{
	
	private int charDeleted;
	private int sentenceNumber;
	private List<Character> charList;
	
	public SpecialCaracterPurifier(List<Character> specialCharacters) {
		charDeleted = 0;
		sentenceNumber = 0;
		charList = new ArrayList<Character>();
		for(char c : specialCharacters) {
			charList.add(c);
		}
	}
	
	/*
	 * return false if there is special caracters contained in charList in the parameters
	 * else 
	 * return true
	 * (non-Javadoc)
	 * @see caster.Caster#cast(java.lang.StringBuilder, java.lang.StringBuilder, java.lang.StringBuilder)
	 */
	@Override
	public boolean cast(StringBuilder before, StringBuilder after, StringBuilder comments) {
		if(before == null || after == null || comments == null) {
			return false;
		}
		
		sentenceNumber += 2;
		
		if(outputOn) {
			map.clear();
			//supprime le caractere special en debut de phrase
			if(before.length() > 0 && charList.contains(before.charAt(0))) {
				map.put("before", ""+before.charAt(0));
				before.deleteCharAt(0);
				charDeleted++;
			}
			if(after.length() > 0 && charList.contains(after.charAt(0))) {
				map.put("after", ""+after.charAt(0));
				after.deleteCharAt(0);
				charDeleted++;
			}
			try {
				if(!map.isEmpty()) {
					outputFile.write(map);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			//supprime le caractere special en debut de phrase
			if(before.length() > 0 && charList.contains(before.charAt(0))) {
				before.deleteCharAt(0);
				charDeleted++;
			}
			if(after.length() > 0 && charList.contains(after.charAt(0))) {
				after.deleteCharAt(0);
				charDeleted++;
			}
		}
		
		
		
		return true;
	}

	@Override
	public void printStatistics() {
		System.out.println("The sepecial caracters purifier treated " + sentenceNumber + " sentences, and deleted " + charDeleted + " char.");
	}

	@Override
	public void createCSVOutput() {
		/* creation fichier csv de sortie et du writer */
		File file = new File("rejectedBySpecialCaracterPurifier.csv"); 
		
		//test if the file already exist
		if(file.exists()) {
			System.out.println("le fichier rejectedBySpecialCaracterPurifier.csv existe deja");
			System.exit(0);
		}
		
		//create the different column for the CSV file
		String[] titles = {"before", "after"};
		try {
			outputFile = new CsvFileWriter(file, '\t', titles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
	}

}
