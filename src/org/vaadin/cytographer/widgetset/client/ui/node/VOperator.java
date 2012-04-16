package org.vaadin.cytographer.widgetset.client.ui.node;

import org.vaadin.cytographer.widgetset.client.ui.VCytographer;
import org.vaadin.cytographer.widgetset.client.ui.VGraph;
import org.vaadin.cytographer.widgetset.client.ui.VNode;
import org.vaadin.cytographer.widgetset.client.ui.VVisualStyle;
import org.vaadin.cytographer.widgetset.client.ui.shap.VTriangle;
import org.vaadin.gwtgraphics.client.Shape;
import org.vaadin.gwtgraphics.client.shape.Text;

import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;

public class VOperator extends VNode  implements DoubleClickHandler{ 
	
	private String value1 = "1";
	private String value2 = "2";
	
	protected final Text textShape1;
	protected final Text textShape2;

	public VOperator(final VCytographer cytographer, final VGraph graph, final Shape shape, final String name,final VVisualStyle style) {
		super(cytographer,graph,shape,name,style);	
		addDoubleClickHandler(this);
		
		textShape1 = new Text(0,0, value1);
		textShape1.setStrokeOpacity(0);
		textShape2 = new Text(0,0, value2);
		textShape2.setStrokeOpacity(0);
		
		VOperator.setStyle(style, textShape1);
		VOperator.setStyle(style, textShape2);
		
		add(textShape1);
		add(textShape2);
		moveNode(shape.getX(),shape.getY());
	}
	public static void setStyle(final VVisualStyle style,final Text shape){
		shape.setFillColor("#000000");
		shape.setFillOpacity(1);
		shape.setFontSize(style.getNodeFontSize());
		shape.setFontFamily(style.getFontFamily());		
	}
	public static Shape getShape(int x, int y,int nodeSize){
		return new VTriangle(x,y,nodeSize,"#FFFF00"); //yellow 16 VGA
	}	

	public void updateValues(String value1,String value2){
		textShape1.setText(value1);
		textShape2.setText(value2);
	}
	public String getValue1(){
		return value1;
	}
	public String getValue2(){
		return value2;
	}
	@Override
	public void onDoubleClick(DoubleClickEvent event) {
		final int x = event.getX();
		final int y = event.getY();
		String[] values = new String[5];
		values[0] = this.getName();
		values[1] = this.getX()+"";
		values[2] = this.getY()+"";
		values[3] = this.value1;
		values[4] = this.value2;
		
		cytographer.doubleClick(values);	
	
	}	
	@Override
	public void moveNode(final float x, final float y) {
		textShape1.setX((int)x);
		textShape2.setX((int)x);
		textShape1.setY((int)y+textShape.getFontSize()*2);
		textShape2.setY((int)y+textShape.getFontSize()*2+textShape1.getFontSize());
		super.moveNode(x, y);
	}
}