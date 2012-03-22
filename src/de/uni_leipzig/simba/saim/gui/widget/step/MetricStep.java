package de.uni_leipzig.simba.saim.gui.widget.step;

import de.uni_leipzig.simba.saim.Messages;
import org.vaadin.teemu.wizards.WizardStep;
import com.vaadin.ui.Component;
import de.uni_leipzig.simba.saim.gui.widget.MetricPanel;

public class MetricStep implements WizardStep
{

	@Override
	public String getCaption() {return Messages.getString("configuremetric");}

	@Override
	public Component getContent()
	{
		return new MetricPanel();
	}

	@Override
	public boolean onAdvance()
	{
		return false;
	}

	@Override
	public boolean onBack()
	{
		return true;
	}

}
