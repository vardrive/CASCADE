/**
 * 
 */
package uk.ac.dmu.iesd.cascade.configwizard;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.dmu.iesd.cascade.agents.aggregators.AggregatorAgent;

/**
 * @author jsnape
 * 
 */
public class AddAggregatorPane extends WizardWorkingPane implements ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2573630327140722754L;

	JComboBox aggList;
	JTextField numAgents;
	JButton addProsumersToThisAggregator;
	Document configObject;
	ArrayList<AggregatorAgent> createdAggregators;
	ButtonGroup opts;
	static String NO_CONFIG = "0";
	static String CONFIG_ALL = "1";
	static String CONFIG_INDIVIDUAL = "2";
	private int configType;

	/**
	 * @param configObject2
	 */
	public AddAggregatorPane(Document configObject2)
	{
		super();

		this.configObject = configObject2;
		this.setName("Add Aggregators");
		TitledBorder title;
		title = BorderFactory.createTitledBorder(this.getName());
		this.setBorder(title);
		this.setSize(WizardFrame.WIZARD_WINDOW_DEFAULT_WIDTH, WizardFrame.WIZARD_WINDOW_DEFAULT_HEIGHT);

		JPanel line1 = new JPanel();
		line1.setBounds(8, 25, this.getWidth() - 20, 60);
		JTextArea aggLabel = new JTextArea("Choose the aggregator class here");
		aggLabel.setLineWrap(true);
		aggLabel.setWrapStyleWord(true);
		aggLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		aggLabel.setEditable(false);
		aggLabel.setBackground(null);
		aggLabel.setBounds(10, 0, 125, 44);
		this.aggList = new JComboBox(WizardFrame.aggregatorClasses.toArray());
		// aggList.setMinimumSize(new Dimension(400,25));
		this.aggList.setBounds(144, 0, line1.getWidth() - 170, 25);
		// line1.setLayout(new BoxLayout(line1,BoxLayout.X_AXIS));
		line1.setLayout(null);
		line1.add(aggLabel);
		line1.add(this.aggList);

		JPanel line2 = new JPanel();
		line2.setBounds(8, 95, this.getWidth() - 20, 60);
		line2.setAlignmentX(Component.LEFT_ALIGNMENT);
		JTextArea numLabel = new JTextArea("How many of these aggregators should be created?");
		numLabel.setLineWrap(true);
		numLabel.setWrapStyleWord(true);
		numLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
		numLabel.setEditable(false);
		numLabel.setBackground(null);
		numLabel.setBounds(10, 8, 239, 32);
		this.numAgents = new JTextField("1");
		this.numAgents.setBounds(264, 5, this.getWidth() - 310, 25);
		line2.setLayout(null);
		line2.add(numLabel);
		line2.add(this.numAgents);

		JPanel configOptions = new JPanel();
		configOptions.setBounds(8, 164, 249, 99);
		TitledBorder optBorder = BorderFactory.createTitledBorder("Configuration options");
		configOptions.setBorder(optBorder);
		configOptions.setLayout(new BoxLayout(configOptions, BoxLayout.Y_AXIS));
		JRadioButton opt1 = new JRadioButton("Use default configuration", true);
		opt1.setActionCommand(AddAggregatorPane.NO_CONFIG);
		opt1.addActionListener(this);
		JRadioButton opt2 = new JRadioButton("Set config to apply to all these aggregators", false);
		opt2.setActionCommand(AddAggregatorPane.CONFIG_ALL);
		opt2.addActionListener(this);
		JRadioButton opt3 = new JRadioButton("Configure each of these individually", false);
		opt3.setActionCommand(AddAggregatorPane.CONFIG_INDIVIDUAL);
		opt3.addActionListener(this);

		this.opts = new ButtonGroup();
		this.opts.add(opt1);
		this.opts.add(opt2);
		this.opts.add(opt3);
		configOptions.add(opt1);
		configOptions.add(opt2);
		configOptions.add(opt3);
		this.setLayout(null);

		this.add(line1);
		this.add(line2);
		this.add(configOptions);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.dmu.iesd.cascade.configwizard.WizardWorkingPane#validateAndSaveToConfig
	 * ()
	 */
	@Override
	public boolean validateAndSaveToConfig()
	{
		boolean validatesOK = true;
		int n = 0;
		Class<? extends AggregatorAgent> selectedAggregator = null;

		String num = this.numAgents.getText();
		try
		{
			n = Integer.parseInt(num);
		}
		catch (NumberFormatException e)
		{
			validatesOK = false;
		}

		if (this.aggList.getSelectedIndex() >= 0)
		{
			selectedAggregator = (Class<? extends AggregatorAgent>) this.aggList.getSelectedItem();
		}
		else
		{
			validatesOK = false;
		}

		if (validatesOK)
		{
			/**
			 * Old idea to instatiate the objects here - better to put it into a
			 * file and then run from the file
			 */

			Element root = this.configObject.getDocumentElement();
			this.setWorkingElement(this.configObject.createElement("aggregator"));
			Attr className = this.configObject.createAttribute("class");
			className.setValue(selectedAggregator.getName());
			Attr sName = this.configObject.createAttribute("shortName");
			sName.setValue(selectedAggregator.getSimpleName());
			Attr numAggs = this.configObject.createAttribute("number");
			numAggs.setValue(num);
			this.getWorkingElement().setAttributeNode(className);
			this.getWorkingElement().setAttributeNode(numAggs);
			this.getWorkingElement().setAttributeNode(sName);
			root.appendChild(this.getWorkingElement());

			/*
			 * createdAggregators = new ArrayList(); for (int i =0; i < n; i++)
			 * { try {
			 * 
			 * createdAggregators.add(selectedAggregator.newInstance()); } catch
			 * (InstantiationException e) { System.err.println(e.getMessage());
			 * e.printStackTrace(); } catch (IllegalAccessException e) {
			 * e.printStackTrace(); } }
			 */

		}

		return validatesOK;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent arg0)
	{
		if (arg0.getSource() == this.addProsumersToThisAggregator)
		{
			WizardPane thisPane = (WizardPane) this.getParent();
			this.validateAndSaveToConfig();
			thisPane.remove(this);
			thisPane.add(new AddProsumerPane(this.configObject, this.getWorkingElement()));
		}
		else if (arg0.getSource() instanceof JRadioButton)
		{
			this.configType = Integer.parseInt(((JRadioButton) arg0.getSource()).getActionCommand());
		}

	}

	protected JTextField getNumAgents()
	{
		return this.numAgents;
	}
}
