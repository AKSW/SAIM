package de.uni_leipzig.simba.saim.gui.widget;

import static de.konrad.commons.sparql.SPARQLHelper.*;

import java.util.Collections;
import java.util.List;

import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.Tree.TreeDragMode;

/** Lets the user choose a class from a given SPARQL endpoint. Queries the endpoint for classes and presents them as a tree. */
public class ClassChooser extends Panel
{
	protected final String endpoint,graph;
	protected final Tree tree;
	
	public ClassChooser(String endpoint, String id, String graph)
	{
		tree = new Tree(id+" classes");
		this.endpoint = endpoint;
		this.graph = graph;
		addComponent(tree);
		tree.setImmediate(true);
		for(String clazz: rootClasses(endpoint, graph))
		{
			tree.addItem(new ClassNode(clazz));
		}
		tree.addListener(new ExpandListener()
		{			
			@Override
			public void nodeExpand(ExpandEvent event)
			{
				expandNode((ClassNode) event.getItemId());
				System.out.println("expanding node "+event.getItemId());
			}
		});
		tree.setDragMode(TreeDragMode.NODE);

	}

	protected void expandNode(ClassNode node)
	{
		if(tree.hasChildren(node)) return; // subclasses already loaded
		List<String> subClasses = null;
		try{subClasses  = subclassesOf(node.url, endpoint, graph);}
		catch(Exception e) {System.err.println("Error expanding node "+node.url);}
		if(subClasses==null||subClasses.isEmpty())
		{
			tree.setChildrenAllowed(node, false);
			return;
		}
		Collections.sort(subClasses); // sorting in java and not in the SPARQL query because the sort order may be different for the short short
		for(String subClass: subClasses)
		{
			ClassNode subNode = new ClassNode(subClass);
			tree.addItem(subNode);
			tree.setParent(subNode,node);
		}		
	}


	/** Wraps a class URL and it's displayed short form, e.g. http://dbpedia.org/ontology/City -> City. */
	protected static class ClassNode
	{
		public final String url, shortURL;
		public ClassNode(String longClass, String shortClass)
		{
			this.url = longClass;
			this.shortURL = shortClass;
		}
		public ClassNode(String longClass)
		{
			this(longClass,lastPartOfURL(longClass));
		}
		@Override public String toString() {return shortURL;}
	}

	//	public ClassChooser()
	//	{
	//		Tree tree = new Tree("DBpedia classes");
	//		tree.addItem("owl:Thing");
	//		tree.addItem("Animal");
	//		
	//		tree.addItem("Plant");
	//		tree.addItem("Grass");
	//		tree.setParent("Animal", "owl:Thing");
	//		tree.setParent("Plant", "owl:Thing");
	//		tree.setParent("Grass", "Plant");
	//		this.addComponent(tree);
	//		tree.setChildrenAllowed("Grass",false);
	//		tree.setChildrenAllowed("Animal",false);
	//	}
}