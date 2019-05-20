package filters.globalRejector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import filters.FiltersStatistics;
import parser.CsvFileWriter;
import parser.ParserXML;

public class SpellingErrorLabelFilter extends FiltersStatistics implements GlobalRejectionFilter{
	
	private int[] errorLabels;
	private int pointeurBorneInf;
	private int sentenceTreated;
	private int sentenceRejected;
	
	public SpellingErrorLabelFilter() {
		if(!loadLabels()) {
			System.out.println("erreur dans le chargement du dictionnaire d\'etiquettes");
			errorLabels = null;
		}
		pointeurBorneInf = 0;
		sentenceTreated = 0;
		sentenceRejected = 0;
	}

	
	
	private boolean loadLabels() {
		/* ouverture du fichier */
		//Document document = ParserXML.getDocumentTraversal("../doss.nosync/spelling_error-v3.xml");
		Document document = ParserXML.getDocumentTraversal("../../test/spelling_error-v3.xml");
		DocumentTraversal traversal = (DocumentTraversal) document;
		if(traversal == null) {
			return false;
		}
		
		/* lecture du fichier */
		NodeIterator iterator = traversal.createNodeIterator(document.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, true);
		Node n = iterator.nextNode();
		
		ArrayList<Integer> listLabel = new ArrayList<Integer>();
		
		if(n.getNodeName().contentEquals("spelling_labels")) {
			NodeList nList = n.getChildNodes(); //var temp
			String modif_id = null, label = null;


			//add all the <modif> nodes in the nodeList that will be treated
			for(int i = 0 ; i < nList.getLength()-1 ; i++) {				
				if(nList.item(i).getNodeName().equals("annotation")) {
					NodeList nltemp = nList.item(i).getChildNodes();
					
					/* tag <annotation> */
					for(int j = 0 ; j < nltemp.getLength()-1 ; j++) {
						if(nltemp.item(j).getNodeName().equals("modif_id")){
							modif_id = nltemp.item(j).getTextContent();
						}
						/* not used for the moment
						 * else if(nltemp.item(j).getNodeName().equals("label")) {
						 
							label = nltemp.item(j).getTextContent();
						}*/
					}
					
					listLabel.add(Integer.parseInt(modif_id));	
				}
			}
		}
		
		errorLabels = new int[listLabel.size()];
		
		for(int i = 0 ; i < listLabel.size() ; i++) {
			errorLabels[i] = listLabel.get(i);
		}		
				
		return true;
	}
	
	
	private boolean isInTheErrorLabels(int label) {
		for(int i = pointeurBorneInf ; i < errorLabels.length ; i++) {
			if(errorLabels[i] == label) {
				pointeurBorneInf = i;
				return true;
			}
			else if(errorLabels[i] > label) {
				break;
			}
		}
		return false;
	}
	
	@Override
	public void cleanTheList(List<Node> nodeList) {
		List<Integer> nodeWillBeRemoved = new ArrayList<Integer>();
		String before = null, after = null;
		
		for(int k = 0 ; k < nodeList.size() ; k++) {
			Node n = nodeList.get(k);

			if(n.getNodeName().equals("modif")) {
				sentenceTreated++;
				NamedNodeMap attr = n.getAttributes();
				int id = Integer.parseInt(attr.getNamedItem("id").getTextContent());
				
				NodeList nList = n.getChildNodes();
					
				if(!isInTheErrorLabels(id)) {
					nodeWillBeRemoved.add(k);
					
					/* write in the file if the output is on */
					if(outputOn) {
						/* browse <modif> tag */
						for(int j = 0 ; j < nList.getLength() ; j++) {
							
							/* browse the child of <modif> tag */
							for(int l = 0 ; l < nList.getLength() ; l++) {
								Node nTempBefAft = nList.item(l);
								
								//<before> tag
								if(nTempBefAft.getNodeName().equals("before")) {
									NodeList lTemp = nTempBefAft.getChildNodes();
									for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
										if(lTemp.item(i).getNodeName().equals("m")) {
											before = lTemp.item(i).getTextContent();
										}
									}
								}
								
								//<after> tag
								else if(nTempBefAft.getNodeName().equals("after")) {
									NodeList lTemp = nTempBefAft.getChildNodes();
									for(int i = 0 ; i < lTemp.getLength()-1 ; i++) {
										if(lTemp.item(i).getNodeName().equals("m")) {
											after = lTemp.item(i).getTextContent();
										}
									}
								}
							}
						}
						Map<String, String> data = new HashMap<String,String>();
						data.put("before", before);
						data.put("after", after);
						data.put("id", ""+id);
						try {
							outputFile.write(data);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
				
			}
			
		}
		
		
		//remove the node that have to be in the list
		for(int i = nodeWillBeRemoved.size()-1 ; i >= 0 ; i--) {
			sentenceRejected++;
			nodeList.remove((int)nodeWillBeRemoved.get(i));
		}
	}
	
	
	

	@Override
	public void createCSVOutput() {
		/* creation fichier csv de sortie et du writer */
		File file = new File("rejectedBySpellingErrorLabelFilter.csv"); 
		
		//test if the file already exist
		if(file.exists()) {
			System.out.println("le fichier SpellingErrorLabelFilter.csv existe deja");
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

	@Override
	public void printStatistics() {
		System.out.println("The SpellingErrorLabelFilter treated " + sentenceTreated + " sentences, and rejected " + sentenceRejected +" sentences.");				
	}

}
