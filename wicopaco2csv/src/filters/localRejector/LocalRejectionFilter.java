package filters.localRejector;

import org.w3c.dom.Node;

public interface LocalRejectionFilter{

	/*
	 * this method apply to a <modif> tag return
	 * TRUE if it has to be deleted
	 * OR
	 * FALSE if it has to stay in the list
	 */
	public boolean hasToBeRemoved(Node n);
	
	

	/*
	 * return the id of the node n
	 * "none" if there is a problem
	 */
	public default String getIdNode(Node n) {
		String res =  "none";
		Node node = n.getAttributes().getNamedItem("id");
		if(node != null) {
			res = node.getTextContent();
		}
		
		return res;
	}
	
}
