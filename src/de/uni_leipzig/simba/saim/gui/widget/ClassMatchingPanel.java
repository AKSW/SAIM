package de.uni_leipzig.simba.saim.gui.widget;

import java.util.Map.Entry;

import org.vaadin.jonatan.contexthelp.ContextHelp;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;

import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.learning.query.ClassMapper;
import de.uni_leipzig.simba.saim.core.Configuration;

/** Contains instances of ClassMatchingForm and lays them out vertically.*/
public class ClassMatchingPanel extends Panel
{	
	Configuration config = Configuration.getInstance();
	final ComboBox suggestionComboBox = new ComboBox();

	protected void setupContextHelp()
	{
		ContextHelp contextHelp = new ContextHelp();
		getContent().addComponent(contextHelp);
		contextHelp.addHelpForComponent(suggestionComboBox, "Class pairs from LIMES.");
	}

	public ClassMatchingPanel()
	{
		setContent(new VerticalLayout());

		//FormLayout layout = new FormLayout(); // have the label to the right of the combobox and not on top
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSpacing(false);
		layout.setWidth("100%");		
		final ProgressIndicator progress = new ProgressIndicator();
		progress.setSizeUndefined();
		Label suggestionLabel = new Label("Suggestions");		
		suggestionLabel.setSizeUndefined();		
		layout.addComponent(suggestionLabel);
		layout.addComponent(progress);
		progress.setIndeterminate(true);				
		suggestionComboBox.setWidth("100%");		
		layout.addComponent(suggestionComboBox);
		layout.setExpandRatio(suggestionComboBox, 1f);
		suggestionComboBox.setVisible(false);
		this.addComponent(layout);
		{
			HorizontalLayout hori = new HorizontalLayout();
			hori.setWidth("100%");
			suggestionComboBox.setEnabled(false);
			new Thread()
			{

				@Override
				public void run()
				{
					Configuration config = Configuration.getInstance();
					Refresher refresher = new Refresher();
					SuggestionsRefreshListener listener = new SuggestionsRefreshListener();
					refresher.addListener(listener);
					addComponent(refresher);

					suggestionComboBox.removeAllItems();
					ClassMapper classMapper = new ClassMapper();
					Mapping sugg = classMapper.getMappingClasses(config.getSource().endpoint, config.getTarget().endpoint, config.getSource().id, config.getTarget().id);
					for(String class1 : sugg.map.keySet())
						for(Entry<String, Double> class2 : sugg.map.get(class1).entrySet()) {
							suggestionComboBox.addItem(class1+" - "+class2.getKey()+" : "+class2.getValue());
						}
					progress.setEnabled(false);
					removeComponent(progress);
					suggestionComboBox.setVisible(true);
					suggestionComboBox.setEnabled(true);					

					System.out.println("suggested enabled: "+suggestionComboBox.size()+" items");					
					listener.running=false;					
				}
			}.start();
			hori.addComponent(new ClassMatchingForm("Source Class", config.getSource()));
			hori.addComponent(new ClassMatchingForm("Target Class", config.getTarget()));
			this.getContent().addComponent(hori);
		}
		setupContextHelp();
	}

	public class SuggestionsRefreshListener implements RefreshListener
	{
		boolean running = true; 
		private static final long serialVersionUID = -8765221895426102605L;		    
		@Override public void refresh(final Refresher source)	{if(!running) {removeComponent(source);source.setEnabled(false);}}
	}	
}