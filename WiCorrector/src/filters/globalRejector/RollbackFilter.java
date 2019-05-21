package filters.globalRejector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import filters.FiltersStatistics;
import parser.CsvFileWriter;
import parser.ParserXML;

public class RollbackFilter extends FiltersStatistics implements GlobalRejectionFilter{
	
	private int sentenceTreated;
	private int sentenceRejected;
	public List<String[]>[] listBefAft = null; //list[wordsNumberOfsentenceBefore], String[0] = before, String[1] = after
	
	
	public RollbackFilter() {
		newList(3); 
		sentenceTreated = 0;
		sentenceRejected = 0;
	}
	
	
	/*
	 * create a new listBefAft with the size in parameter (copy the content if it already exists)
	 */
	private void newList(int size) {
		if(listBefAft == null) {
			listBefAft = new ArrayList[size];
			for(int i = 0 ; i < size ; i++) {
				listBefAft[i] = new ArrayList<String[]>();
			}
		}
		else {
			List<String[]>[] listTemp = new ArrayList[size];
			for(int i = 0 ; i < size ; i++) {
				if(i < listBefAft.length && listBefAft[i] != null) {
					listTemp[i] = listBefAft[i];
				}
				else {
					listTemp[i] = new ArrayList<String[]>();
				}
			}
			listBefAft = listTemp;
		}
	}
	
	/*
	 * add the two String in the listBefAft
	 */
	private void addWordsInList(String before, int wordNumbBef, String after) {
		
		if(listBefAft.length < wordNumbBef) {
			newList(wordNumbBef);
		}
		String[] strinngTab = new String[2];
		strinngTab[0] = before;
		strinngTab[1] = after;
		listBefAft[wordNumbBef-1].add(strinngTab);
	}
	
	
	/*
	 * return the word number in the tag : <m>
	 */
	private int getWordNumber(Node mTag){
		return Integer.parseInt(ParserXML.getAttributeContent(mTag.getAttributes(), "num_words"));
	}
	
	
	/*
	 * return true if the word strBef and strAft is already in the list 
	 */
	public boolean alreadySeen(String strBef, int wordNumberBef, String strAft, int wordNumberAft) {
		if(wordNumberBef > listBefAft.length || wordNumberAft > listBefAft.length) {
			return false;
		}
		
		for(String[] strTab : listBefAft[wordNumberAft-1]) {
			if(strTab[1].equals(strBef) && strTab[0].equals(strAft)) {
				return true;
			}
		}
		return false;
	}
	
	
	
	@Override
	/*
	 * nodeList : <modif> tag list
	 * remove from the list the changes that are canceled 
	 * like: A -> B then B -> A 
	 * (non-Javadoc)
	 * @see globalRejector.GlobalRejectionFilter#cleanTheList(java.util.List)
	 */
	public void cleanTheList(List<Node> nodeList) {
		List<Integer> nodeWillBeRemoved = new ArrayList<Integer>();
		int[][] wordsNumber = new int[nodeList.size()][2]; //wordsNumber[index][0] = nbOfWordsBefore , wordsNumber[index][1] = nbOfWordsAfter

		String before = null, after = null;
		/* add all the case to the list to see if there is no rollback */
		for(int k = 0 ; k < nodeList.size() ; k++) {
			Node n = nodeList.get(k);
			NodeList nList = n.getChildNodes();
			
			/* browse the child of <modif> tag */
			for(int j = 0 ; j < nList.getLength() ; j++) {
				Node nTempBefAft = nList.item(j);
				
				//<before> tag
				if(nTempBefAft.getNodeName().equals("before")) {
					NodeList lTemp = nTempBefAft.getChildNodes();
					for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
						if(lTemp.item(i).getNodeName().equals("m")) {
							Node mTag = lTemp.item(i);
							before = mTag.getTextContent();
							wordsNumber[k][0] = getWordNumber(mTag);							
						}
					}
				}
				
				//<after> tag
				else if(nTempBefAft.getNodeName().equals("after")) {
					NodeList lTemp = nTempBefAft.getChildNodes();
					for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
						if(lTemp.item(i).getNodeName().equals("m")) {
							Node mTag = lTemp.item(i);
							after = mTag.getTextContent();
							wordsNumber[k][1] = getWordNumber(mTag);
							addWordsInList(before, wordsNumber[k][0], after);
						}
					}
				}
			}
		}
		
		/* keep only the on which has to be treated */
		for(int k = 0 ; k < nodeList.size() ; k++) {
			Node n = nodeList.get(k);
			NodeList nList = n.getChildNodes();

			
			/* browse the child of <modif> tag */
			for(int j = 0 ; j < nList.getLength() ; j++) {
				Node nTempBefAft = nList.item(j);
				
				//<before> tag
				if(nTempBefAft.getNodeName().equals("before")) {
					NodeList lTemp = nTempBefAft.getChildNodes();
					for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
						if(lTemp.item(i).getNodeName().equals("m")) {
							Node mTag = lTemp.item(i);
							before = mTag.getTextContent();
						}
					}
				}
				
				//<after> tag
				else if(nTempBefAft.getNodeName().equals("after")) {
					NodeList lTemp = nTempBefAft.getChildNodes();
					for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
						if(lTemp.item(i).getNodeName().equals("m")) {
							Node mTag = lTemp.item(i);
							after = mTag.getTextContent();
							

							//test if the correction is a rollback 
							if(alreadySeen(before, wordsNumber[k][0], after, wordsNumber[k][1])){
								nodeWillBeRemoved.add(k);
								
								//output for the rejected case
								if(outputOn) {
									map.clear();
									map.put("before",before);
									map.put("after",after);
									map.put("id", ""+k);
									try {
										outputFile.write(map);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
							else {
							}
						}
					}
				}
			}
		}
		
		sentenceTreated += nodeList.size();
		sentenceRejected += nodeWillBeRemoved.size();
		/* remove the node from the main list */
		for(int i = nodeWillBeRemoved.size()-1 ; i >= 0 ; i--) {
			nodeList.remove((int)nodeWillBeRemoved.get(i));
		}
	}


	@Override
	public void printStatistics() {
		System.out.println("The rollback filter treated " + sentenceTreated + " sentences, and rejected " + sentenceRejected +" sentences.");				
	}


	@Override
	public void createCSVOutput() {
		/* creation fichier csv de sortie et du writer */
		File file = new File("rejectedByRollbackFilter.csv"); 
		
		//test if the file already exist
		if(file.exists()) {
			System.out.println("le fichier rejectedByRollbackFilter.csv existe deja");
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
