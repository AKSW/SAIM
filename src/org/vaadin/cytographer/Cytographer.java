package org.vaadin.cytographer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vaadin.data.validator.DoubleValidator;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ClientWidget;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.Window.Notification;
import com.vaadin.ui.themes.Reindeer;

import org.vaadin.cytographer.widgetset.client.ui.VCytographer;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.view.CyNetworkView;
import de.konrad.commons.sparql.PrefixHelper;
import de.uni_leipzig.simba.io.KBInfo;
import de.uni_leipzig.simba.saim.Messages;
import de.uni_leipzig.simba.saim.SAIMApplication;
import de.uni_leipzig.simba.saim.core.Configuration;
import de.uni_leipzig.simba.saim.core.metric.Node;
import de.uni_leipzig.simba.saim.core.metric.Output;
import de.uni_leipzig.simba.saim.gui.widget.form.PreprocessingForm;

/**
 * Server side component for the VCytographer widget.
 */
@ClientWidget(VCytographer.class)
public class Cytographer extends AbstractComponent {
	private static final long serialVersionUID = 8483008141219579936L;
	
	public enum GraphOperation {
		REPAINT, SET_NODE_SIZE, SET_VISUAL_STYLE, SET_TEXT_VISIBILITY, SET_OPTIMIZED_STYLES, SET_ZOOM, FIT 
	}
	private GraphOperation currentOperation = GraphOperation.REPAINT;
		
	private GraphProperties graphProperties;
	private final PaintController paintController = new PaintController();
	
	private Window mainWindow=null;
	private Messages messages=null;
	private Configuration cfg=null;
	
	/**
	 * 
	 */
	public Cytographer(final int width, final int height,Window window) {
		
		Cytoscape.createNewSession();
		String name = "Cytographer";
		CyNetwork network = Cytoscape.createNetwork(name, false);			
		CyNetworkView finalView= Cytoscape.createNetworkView(network);	

		init(network, finalView, name, width, height, window);
	}
	/**
	 * 
	 */
	public Cytographer(final CyNetwork network, final CyNetworkView finalView, final String title, final int width, final int height,Window window) {

		init(network, finalView, title, width, height, window);

	}
	/*
	 * 
	 */
	private void init(final CyNetwork network, final CyNetworkView finalView, final String title, final int width, final int height,Window window){
		
		if((SAIMApplication)getApplication()!= null)
			messages = ((SAIMApplication)getApplication()).messages;

		if((SAIMApplication)getApplication()!= null)
			cfg = ((SAIMApplication)getApplication()).getConfig();

		graphProperties = new GraphProperties(network, finalView, title);
		setMainWindow(window);

		// defaults
		paintController.initDefaults();
		setSize(width,height);
		setImmediate(true);
		setTextVisible(true);
	}
	
	/**
	 * Gets the metrics output node / root node
	 */
	public Node getMetric(){
		for(Node n : graphProperties.getNodeMap().values()){
			if(n instanceof Output)
				return n;
		}return null;
	}

	public void applyLayoutAlgorithm(final CyLayoutAlgorithm loAlgorithm) {
		graphProperties.applyLayoutAlgorithm(loAlgorithm);
		repaintGraph();
	}
	public void setSize(int w,int h ){
		setWidth(w + "px"); //$NON-NLS-1$
		setHeight(h + "px"); //$NON-NLS-1$
		graphProperties.setWidth(w);
		graphProperties.setHeight(h);
	}

	public void setMainWindow(Window window){
		graphProperties.setMainWindow(window);
		mainWindow=window;
	}

	public int  addNode(String name, int x, int y, GraphProperties.Shape shape){
		return graphProperties.addANewNode(name, x, y,shape);
	}

	public void createAnEdge(int nodeAid, int nodeBid, String attribute) {
		graphProperties.createAnEdge(nodeAid, nodeBid, attribute);
	}

	@Override
	public void paintContent(final PaintTarget target) throws PaintException {
		super.paintContent(target);	
		target.addAttribute("operation", currentOperation.toString());
		switch (currentOperation) {
		case SET_NODE_SIZE:
			paintController.paintNodeSize(target, graphProperties);
			break;
			
		case SET_TEXT_VISIBILITY:
			paintController.paintTextVisibility(target, graphProperties);
			break;

		case SET_ZOOM:
			paintController.setZoom(target, graphProperties);
			break;
		case REPAINT:
		case FIT:
			paintController.repaintGraph(target, graphProperties);
			break;
		default:
			;
		}
		currentOperation = GraphOperation.REPAINT;
		graphProperties.setUseFitting(false);
	}

	/**
	 * Receive and handle events and other variable changes from the client.
	 */

	@Override
	public void changeVariables(final Object source, final Map<String, Object> variables) {
		super.changeVariables(source, variables);
		
		if (variables.containsKey("selectedEdges")) {
			graphProperties.clearSelectedEdges();
			final String[] selectedEdges = (String[]) variables.get("selectedEdges");
			for (final String edge : selectedEdges) 
				graphProperties.addSelectedEdge(edge);
		}
		if (variables.containsKey("selectedNodes")) {
			graphProperties.clearSelectedNodes();
			final String[] strs = (String[]) variables.get("selectedNodes");
			for (final String str : strs) 
				graphProperties.addSelectedNode(str);
		}
		if (variables.containsKey("zoomFactor")) {
			graphProperties.setZoomFactor((Integer) variables.get("zoomFactor"));
		}
		if (variables.containsKey("createdANode")) {
			final Object[] nodeData = (Object[]) variables.get("createdANode");
			graphProperties.addANewNode((String) nodeData[0], (Integer) nodeData[1], (Integer) nodeData[2],graphProperties.getShapes((String) nodeData[0]));
		}
		if (variables.containsKey("removedNode")) {
			graphProperties.removeNode((String) variables.get("removedNode"));
			repaintGraph();
		}
		if (variables.containsKey("edgeCreated")) {
			String[] args = (String[]) variables.get("edgeCreated");
			createAnEdge(Integer.parseInt(args[0]),Integer.parseInt(args[1]),args[2]);
			repaintGraph();
		}
		if (variables.containsKey("removedEdge")) {
			graphProperties.removeEdge((String) variables.get("removedEdge"));
		}
		if (variables.containsKey("doubleClick")) {
			final String[] args = (String[]) variables.get("doubleClick");
					
			if(args.length == 6){
				if(args[5].startsWith("Operator") || args[5].startsWith("Output")){
					makeModalWindow(args,args[5]);
				}
				else if(args[5].startsWith("Target")){
					String nodeName = (graphProperties.getNodeNames().get(Integer.valueOf(args[0])));
					// remove prefix
					addProperty(nodeName.substring(nodeName.indexOf('.')+1),cfg.getTarget());
					
				}
				else if(args[5].startsWith("Source")){
					String nodeName = (graphProperties.getNodeNames().get(Integer.valueOf(args[0])));
					// remove prefix
					addProperty(nodeName.substring(nodeName.indexOf('.')+1),cfg.getSource());
				}	
			}
		}
		
		if (variables.containsKey("onNodeMouseUp")) {
			final String[] args = (String[]) variables.get("onNodeMouseUp");	
			Integer id = new Integer(args[0]);
			Double x = Double.parseDouble(args[1]);
			Double y = Double.parseDouble(args[2]);
			graphProperties.setNodePos(id,x,y);
		}
	}

	public void addDefaultProperty(String s, KBInfo info){
			//TODO
			//set defaults 
	}

	/**
	 * Method to add Properties to according KBInfo. 
	 * @param s URI of the property. May or may not be abbreviated.
	 * @param info KBInfo of endpoint property belongs to.
	 */
	private void addProperty(String s, KBInfo info) {
		String prop;
		
		if(s.startsWith("http:")) {//do not have a prefix, so we generate one
			PrefixHelper.generatePrefix(s);
			prop = PrefixHelper.abbreviate(s);
		} else {// have the prefix already
			prop = s;
			s = PrefixHelper.expand(s);
		}
		if(!info.properties.contains(prop)) {
			info.properties.add(prop);
		}

		final Window sub = new Window(messages.getString("Cytographer.definepreprocessingsubwindowname")+prop);
		sub.setModal(true);
		sub.addComponent(new PreprocessingForm(info, prop));
		sub.setResizable(false);
		sub.addStyleName(Reindeer.WINDOW_BLACK);
		sub.setHeight("250px");
		sub.setWidth("250px");	
		getApplication().getMainWindow().addWindow(sub);

		final Button btnok = new Button("Ok");	
		 btnok.addListener(new ClickListener(){
			@Override
			public void buttonClick(ClickEvent event) {
				getApplication().getMainWindow().removeWindow(sub); // close
			}		 
		 });
		 sub.addComponent(btnok);
		String base = PrefixHelper.getBase(s);
		info.prefixes.put(PrefixHelper.getPrefix(base), PrefixHelper.getURI(PrefixHelper.getPrefix(base)));	
	}

	private void makeModalWindow(final String[] args,final String name){
		
		final Window mywindow = new Window("");
		
		final TextField t = new TextField( messages.getString("Cytographer.modalWindowTextField1Label"+name),args[3]);
		final TextField tt = new TextField(messages.getString("Cytographer.modalWindowTextField2Label"+name),args[4]);
		mywindow.addComponent(t);
		mywindow.addComponent(tt);
		t.addValidator( new MyDoubleValidator(messages.getString("Cytographer.thresholdWarning"+name)));
		tt.addValidator(new MyDoubleValidator(messages.getString("Cytographer.thresholdWarning"+name)));
		t.setMaxLength(4);
		tt.setMaxLength(4);
		t.setImmediate(true);
		tt.setImmediate(true);
		
		mywindow.setResizable(false);
		mywindow.setModal(true); 
		mywindow.addStyleName(Reindeer.WINDOW_BLACK);
		mywindow.setHeight("180px");
		mywindow.setWidth("200px");	
		//mywindow.setPositionX(Math.round(Float.valueOf(args[1])));
		//mywindow.setPositionY(Math.round(Float.valueOf(args[2])));
		
		HorizontalLayout layout = new HorizontalLayout();		
		final Button btnok = new Button("Ok");	
		final Button btncancel = new Button("Cancel");
		layout.addComponent(btnok);
		layout.addComponent(btncancel);
		mywindow.addComponent(layout);
		mainWindow.addWindow(mywindow);
		
		btnok.setEnabled(true);
	    btnok.setImmediate(true);
	    btnok.setVisible(true);	 
		btncancel.setEnabled(true);
		btncancel.setImmediate(true);
		btncancel.setVisible(true);	 		
		
		class MyListener implements CloseListener,ClickListener{
			
			private static final long serialVersionUID = -165177940359643613L;
			
			@Override public void windowClose(CloseEvent e) {
				//click();
			}
			
			@Override
			public void buttonClick(ClickEvent event) {
				if(event.getButton() == btnok){
					if(click())
						mainWindow.removeWindow(mywindow); // close
				}else if(event.getButton() == btncancel){
					mainWindow.removeWindow(mywindow); // close
				}				
			}
			
			private boolean click(){
				if(t.isValid() && tt.isValid() && !tt.getValue().toString().isEmpty() ){
					List<Object> value = new ArrayList<Object>();
					value.add(t.getValue());
					value.add(tt.getValue());
					
					graphProperties.setNodeMetadata( args[0], value);
					repaintGraph();
					return true;
				}else{
					mainWindow.showNotification(messages.getString("Cytographer.thresholdWarning"+name), Notification.TYPE_WARNING_MESSAGE);
					return false;
				}
			}
		}
		
		mywindow.addListener(new MyListener());
	    btnok.addListener(new MyListener());
	    btncancel.addListener(new MyListener());
	}

	/**
	 * Change texts visibilities
	 * 
	 * @param b
	 */
	public void setTextVisible(final boolean b) {
		currentOperation = GraphOperation.SET_TEXT_VISIBILITY;
		graphProperties.setTextsVisible(b);
		requestRepaint();
	}
	
	public void setNodeMetadata(String nodeID, List<Object> value){
		graphProperties.setNodeMetadata( nodeID, value);
		repaintGraph();
	}

	/**
	 * Change node size
	 * 
	 * @param nodeSize
	 * @param repaint
	 */
	public void setNodeSize(final double nodeSize, final boolean repaint) {
		graphProperties.setNodeSize(nodeSize);
		if (repaint) {
			currentOperation = GraphOperation.SET_NODE_SIZE;
			requestRepaint();
		}
	}

	/**
	 * Change window size.
	 */
	private void reSize(){
		int hadjust = 100;
		int wadjust = 450;
		
		int wm = Math.round(mainWindow.getWidth()-wadjust);
		int hm = Math.round(mainWindow.getHeight()-hadjust);
		int wg = graphProperties.getWidth();
		int hg = graphProperties.getHeight();
		
		if(wm > 0 &&  hm > 0 && wg != wm && hg != hm )
			setSize(wm,hm);
	}
	/**
	 * Change window size and repaint graph
	 */
	public void repaintGraph() {
		reSize();		
		currentOperation = GraphOperation.REPAINT;
		graphProperties.setZoomFactor(0);
		requestRepaint();
	}

//	public Container getNodeAttributeContainerForSelectedNodes() {
//		return graphProperties.getNodeAttributeContainerForSelectedNodes();
//	}

	public void fitToView() {
		graphProperties.measureDimensions();
		graphProperties.setUseFitting(true);
		graphProperties.setZoomFactor(0);
		currentOperation = GraphOperation.FIT;
		requestRepaint();
	}

	public boolean isTextsVisible() {
		return graphProperties.isTextsVisible();
	}

	public void zoomIn() {
		graphProperties.setZoomFactor(graphProperties.getZoomFactor() + 1);
		currentOperation = GraphOperation.SET_ZOOM;
		requestRepaint();

	}

	public void zoomOut() {
		graphProperties.setZoomFactor(graphProperties.getZoomFactor() - 1);
		currentOperation = GraphOperation.SET_ZOOM;
		requestRepaint();
	}

	public void setNodeColors(int p){
		paintController.setNodeColors(p);
		repaintGraph();
	}

	class MyDoubleValidator extends DoubleValidator {
		private static final long serialVersionUID = -5585916227598767457L;
		public MyDoubleValidator(String msg) {
			super(msg);
		}		
		@Override
		protected boolean isValidString(String value) {
			try {
				double d = Double.parseDouble(value);
				return d>=0 && d <=1;
			} catch (Exception e) {
				return false;
			}
		}
	}
}