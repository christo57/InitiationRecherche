package parser;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import filters.FiltersStatistics;
import filters.globalRejector.GlobalRejectionFilter;
import filters.globalRejector.RollbackFilter;
import filters.globalRejector.SpellingErrorLabelFilter;
import filters.localRejector.EstheticalRestructurationRejector;
import filters.localRejector.LocalRejectionFilter;
import filters.localRejector.NumberRejector;
import filters.purifier.PurifierFilter;
import filters.purifier.SentencePurifier;
import filters.purifier.SpecialCaracterPurifier;

public class ParserXML {

	private List<PurifierFilter> purifiers;
	private List<LocalRejectionFilter> localRejectors;
	private List<GlobalRejectionFilter> globalRejectors;
	private CsvFileWriter writer;

	public ParserXML(CsvFileWriter w) {
		purifiers = new ArrayList<PurifierFilter>();
		localRejectors = new ArrayList<LocalRejectionFilter>();
		globalRejectors = new ArrayList<GlobalRejectionFilter>();

		writer = w;
	}

	public void addPurifier(PurifierFilter... cas) {
		for (PurifierFilter c : cas) {
			purifiers.add(c);
		}
	}

	public void addLocalRejector(LocalRejectionFilter... fil) {
		for (LocalRejectionFilter f : fil) {
			localRejectors.add(f);
		}
	}

	public void addGlobalRejector(GlobalRejectionFilter... fil) {
		for (GlobalRejectionFilter f : fil) {
			globalRejectors.add(f);
		}
	}

	/*
	 * return the content of an attributes attri in the Node n if it exists else
	 * return null
	 */
	public static String getAttributeContent(NamedNodeMap n, String attri) {
		if (n == null || attri == null) {
			return null;
		}
		for (int i = 0; i < n.getLength(); i++) {
			if (n.item(i).getNodeName().equals(attri)) {
				return n.item(i).getTextContent();
			}
		}
		return null;
	}

	/*
	 * traite le fichier voulu
	 */
	public void parser() throws IOException {

		/* ouverture du fichier xml en entre */
		//Document document = ParserXML.getDocumentTraversal("../doss.nosync/wico_v2_complet.xml");
		//Document document = ParserXML.getDocumentTraversal("../alex/truc.xml");

		Document document = ParserXML.getDocumentTraversal("../../test/wico_v2_complet.xml");
		DocumentTraversal traversal = (DocumentTraversal) document;
		if (traversal == null) {
			System.out.println("erreur");
			System.exit(0);
		}



		/* debut de la lecture */
		NodeIterator iterator = traversal.createNodeIterator(document.getDocumentElement(), NodeFilter.SHOW_ELEMENT,
				null, true);
		Node n = iterator.nextNode();

		ArrayList<Node> nodeList = new ArrayList<Node>(); // contains <modif> items that are going to be treated

		if (n.getNodeName().contentEquals("modifs")) {
			NodeList nList = n.getChildNodes(); // var temp


			// add all the <modif> nodes in the nodeList that will be treated
			for (int i = 0; i < nList.getLength() - 1; i++) {
				if (nList.item(i).getNodeName().equals("modif")) {
					nodeList.add(nList.item(i));
				}
			}
		}








		/* local rejector */
		for(int i = nodeList.size()-1 ; i > 0 ; i--) {
			for (LocalRejectionFilter f : localRejectors) {
				if (f.hasToBeRemoved(nodeList.get(i))) {
					nodeList.remove(i);
					break;
				}
			}
		}

		

		// clean the node list that have to be treated
		for (GlobalRejectionFilter f : globalRejectors) {
			f.cleanTheList(nodeList);
		}

		// treat the node list that will be in the output file
		for (Node node : nodeList) {
			/* apply a purification on the case, then add it to the output file */
			traiterModif(node);
		}




		// closing writers
		for (GlobalRejectionFilter f : globalRejectors) {
			((FiltersStatistics) f).closeOutput();
			f = null;
		}
		for (LocalRejectionFilter f : localRejectors) {
			((FiltersStatistics) f).closeOutput();
		}
		for (PurifierFilter f : purifiers) {
			((FiltersStatistics) f).closeOutput();
		}
		writer.close();

	}

	/*
	 * treat the <modif> tag contents
	 */
	public void traiterModif(Node node) throws IOException {

		StringBuilder strBefore = new StringBuilder(), strAfter = new StringBuilder(), strComments = new StringBuilder();
		HashMap<String, String> map = new HashMap<String, String>();

		/* Recupere l'attribut commentaire */
		NamedNodeMap attributes = node.getAttributes();
		for (int j = 0; j < attributes.getLength(); j++) {
			if (attributes.item(j).getNodeName().equals("wp_comment")) {
				strComments.append(attributes.item(j).getTextContent());
			}
		}

		NodeList nSousList = node.getChildNodes();
		/* Parcours les enfants de modif */
		for (int j = 0; j < nSousList.getLength(); j++) {
			Node nTempBefAft = nSousList.item(j);
			if (nTempBefAft.getNodeName().equals("before")) {
				strBefore.append(nTempBefAft.getTextContent());
			} else if (nTempBefAft.getNodeName().equals("after")) {
				strAfter.append(nTempBefAft.getTextContent());
			}
		}
		// on ajoute dans le csv
		if (caster(strBefore, strAfter, strComments, map)) {
			writer.write(map);
			map.clear();
			map = null;
		}
	}

	/*
	 * return a Document : usefull for the XMLparsing
	 */
	public static Document getDocumentTraversal(String fileName) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document docATraiter = null;
		try {
			DocumentBuilder loader = factory.newDocumentBuilder();
			docATraiter = loader.parse(fileName);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}

		return docATraiter;
	}

	/*
	 * apply the cast on the parameters
	 */
	public boolean caster(StringBuilder before, StringBuilder after, StringBuilder comments, Map<String, String> map) {
		for (PurifierFilter c : purifiers) {
			if (!c.cast(before, after, comments)) {
				return false;
			}
		}
		map.put("before", before.toString());
		map.put("after", after.toString());
		map.put("comments", comments.toString());
		return true;
	}

	public static void main(String[] args) throws IOException {


		long startTime = System.currentTimeMillis();

		/* creation fichier csv de sortie et du writer */
		File file = null;

		if (args.length > 0) {
			file = new File(args[0]);
		} else {
			file = new File("sortie.csv");
		}

		// test if the file already exist
		if (file.exists()) {
			System.out.println("le fichier existe deja");
			System.exit(0);
		}

		// create the different column for the CSV file
		String[] titles = { "before", "after", "comments" };
		CsvFileWriter writer = new CsvFileWriter(file, '\t', titles);

		// add the writer to the parser
		ParserXML parser = new ParserXML(writer);
		Character[] specChar = {'*','/','#','$'};
		List<Character> specialCharacters = Arrays.asList(specChar);

		// adding differents globalRejector

		//parser.addGlobalRejector(new SpellingErrorLabelFilter());
		parser.addGlobalRejector(new RollbackFilter());


		// adding differents localRejector
		parser.addLocalRejector(new NumberRejector());
		parser.addLocalRejector(new EstheticalRestructurationRejector());

		// adding differents casters
		parser.addPurifier(new SentencePurifier());
		parser.addPurifier(new SpecialCaracterPurifier(specialCharacters));


		for(GlobalRejectionFilter f : parser.globalRejectors) { 
			((FiltersStatistics)f).activateOutput(); 
		} 
		for(LocalRejectionFilter f : parser.localRejectors) {
			((FiltersStatistics) f).activateOutput(); 
		} 
		for(PurifierFilter f : parser.purifiers) { 
			((FiltersStatistics) f).activateOutput(); 
		}


		// start the treatment
		parser.parser();

		for (GlobalRejectionFilter f : parser.globalRejectors) {
			((FiltersStatistics) f).printStatistics();
		}
		for (LocalRejectionFilter f : parser.localRejectors) {
			((FiltersStatistics) f).printStatistics();
		}
		for (PurifierFilter f : parser.purifiers) {
			((FiltersStatistics) f).printStatistics();
		}
		System.out.println("execution time : " + (System.currentTimeMillis() - startTime) + " ms");
	}

}
