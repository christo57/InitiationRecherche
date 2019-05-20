package filters.localRejector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import filters.FiltersStatistics;
import parser.CsvFileWriter;

public class NumberRejector extends FiltersStatistics implements LocalRejectionFilter{

	private int sentenceTreated;
	private int sentenceRejected;
	
	public NumberRejector() {
		sentenceTreated = 0;
		sentenceRejected = 0;
	}
	
	/*
	 * return true if there is a number in the String str
	 * else return false
	 */
	private boolean isNumberIn(String str) {
		for(int i = 0 ; i < str.length() ; i++) {
			if(str.charAt(i) == '0' || str.charAt(i) == '1' || str.charAt(i) == '2' || str.charAt(i) == '3'
					 || str.charAt(i) == '4' || str.charAt(i) == '5' || str.charAt(i) == '6'
					 || str.charAt(i) == '7' || str.charAt(i) == '8' || str.charAt(i) == '9') {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * the parameters n is a <modif> tag
	 * return true if in <m> tag in <before> or <after> there is a number correction
	 * else
	 * return false
	 * (non-Javadoc)
	 * @see filter.Filter#hasToBeRemoved(org.w3c.dom.Node)
	 */
	@Override
	public boolean hasToBeRemoved(Node n) {
		sentenceTreated++;
		
		// TODO Auto-generated method stub
		NodeList nList = n.getChildNodes();
		boolean rejected = false;
		
		/* Parcours les enfants de modif */
		for(int j = 0 ; j < nList.getLength() ; j++) {
			Node nTempBefAft = nList.item(j);
			//balise before
			if(nTempBefAft.getNodeName().equals("before")) {
				NodeList lTemp = nTempBefAft.getChildNodes();
				for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
					if(lTemp.item(i).getNodeName().equals("m")) {
						Node mTag = lTemp.item(i);
						if(isNumberIn(mTag.getTextContent())){
							//output for the rejected case
							rejected = true;
						}
						if(outputOn) {
							map.put("before",mTag.getTextContent());
						}
					}
				}
			}
			//balise after
			else if(nTempBefAft.getNodeName().equals("after")) {
				NodeList lTemp = nTempBefAft.getChildNodes();
				for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
					if(lTemp.item(i).getNodeName().equals("m")) {
						Node mTag = lTemp.item(i);
						if(rejected || isNumberIn(mTag.getTextContent())) {
							//output for the rejected case
							if(outputOn) {
								map.put("after",mTag.getTextContent());
								map.put("id", ""+getIdNode(n));
								try {
									outputFile.write(map);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							sentenceRejected++;
							return true;
						}
						return false;
					}
				}
			}
		}
		
		return false;
	}

	@Override
	public void printStatistics() {
		System.out.println("The number rejector treated " + sentenceTreated + " sentences, and rejected " + sentenceRejected +" sentences.");		
	}

	@Override
	public void createCSVOutput() {
		/* creation fichier csv de sortie et du writer */
		File file = new File("rejectedByNumberRejector.csv"); 
		
		//test if the file already exist
		if(file.exists()) {
			System.out.println("le fichier rejectedByNumberRejector.csv existe deja");
			System.exit(0);
		}
		
		//create the different column for the CSV file
		String[] titles = { "before" , "after", "id"};
		try {
			outputFile = new CsvFileWriter(file, '\t', titles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}

}
