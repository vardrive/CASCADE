package uk.ac.dmu.iesd.cascade.context;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.context.space.graph.NetworkGenerator;
import repast.simphony.context.space.graph.WattsBetaSmallWorldGenerator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.projection.Projection;
import uk.ac.dmu.iesd.cascade.Consts;
import uk.ac.dmu.iesd.cascade.Consts.GENERATOR_TYPE;
import uk.ac.dmu.iesd.cascade.Consts.STORAGE_TYPE;
import uk.ac.dmu.iesd.cascade.FactoryFinder;
import uk.ac.dmu.iesd.cascade.io.CSVReader;
import uk.ac.dmu.iesd.cascade.util.*;

/**
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/05/13 14:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history
 * 
 * 1.0 - Initial scenario creator
 * 1.1 - amend to accommodate refactor of Prosumer into abstract class and 
 * 		specific inherited classes.
 * 1.2 - Class name has been changed;
 *       Restructured the build method by adding private sub-methods;
 *       ContextBuilder type has been changed from ProsumerAgent to Object
 *       Babak 
 * 
 */


/**
 * Builds and returns a context. Building a context consists of filling it with
 * agents, adding projects and so forth. 
 * 
 * TODO: 
 * @param context
 * @return the built context.
 */
public class CascadeContextBuilder implements ContextBuilder<Object> {

	CascadeContext cascadeMainContext;  // cascade main context
	Parameters params; // parameters for the model run environment 	
	int numProsumers; //number of Prosumers
	float[] householdBaseDemandArray = null;
	//int ticksPerDay;
	int numDemandColumns = Consts.NUM_DEMAND_COLUMNS;

	
	/*
	 * Builds the <tt> Cascade Context </tt> (by calling other private sub-methods)
	 * @see uk.ac.dmu.iesd.cascade.context.CascadeContext
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 * @return built context
	 */
	public CascadeContext build(Context context) {
		// Instantiate the Cascade context, passing in the context that was given to this builder
		// to clone any existing parameters
		cascadeMainContext = new CascadeContext(context); //build CascadeContext by passing the context
		readParamsAndInitializeArrays();
		populateContext();
		if (cascadeMainContext.verbose)	
			System.out.println("Cascade Main Context created: "+cascadeMainContext.toString());
		return cascadeMainContext;
	}

	/*
	 * Read the model environment parameters and initialize arrays
	 */
	private void readParamsAndInitializeArrays() {
		// get the parameters from the current run environment
		params = RunEnvironment.getInstance().getParameters();
		String dataFileFolderPath = (String)params.getValue("dataFileFolder");
		String weatherFileName = (String)params.getValue("weatherFile");
		String systemDemandFileName = (String)params.getValue("systemBaseDemandFile");
		String householdAttrFileName = (String)params.getValue("householdBaseAttributeFile");
		String elecLayoutFilename = (String)params.getValue("electricalNetworkLayoutFile");
		numProsumers = (Integer) params.getValue("defaultProsumersPerFeeder");
		cascadeMainContext.setTickPerDay((Integer) params.getValue("ticksPerDay"));
		cascadeMainContext.verbose = (Boolean) params.getValue("verboseOutput");

		/*
		 * Read in the necessary data files and store to the context
		 */
		File dataDirectory = new File(dataFileFolderPath);
		File weatherFile = new File(dataDirectory, weatherFileName);
		CSVReader weatherReader = null;
		File systemDemandFile = new File(dataDirectory, systemDemandFileName);
		CSVReader systemBasePriceReader = null;
		File householdAttrFile = new File(dataDirectory, householdAttrFileName);
		CSVReader baseDemandReader = null;

		try {
			weatherReader = new CSVReader(weatherFile);
			weatherReader.parseByColumn();

			cascadeMainContext.insolationArray = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("insolation"));
			cascadeMainContext.windSpeedArray = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("windSpeed"));
			cascadeMainContext.airTemperatureArray = ArrayUtils.convertStringArraytofloatArray(weatherReader.getColumn("airTemp"));
			cascadeMainContext.weatherDataLength = cascadeMainContext.insolationArray.length;

		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + weatherFile.getAbsolutePath());
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
		if (cascadeMainContext.weatherDataLength % cascadeMainContext.getTickPerDay() != 0)
		{
			System.err.println("Weather data array not a whole number of days. This may cause unexpected behaviour ");
		}

		try {
			systemBasePriceReader = new CSVReader(systemDemandFile);
			systemBasePriceReader.parseByColumn();
			float[] systemBasePriceSignal = ArrayUtils.convertStringArraytofloatArray(systemBasePriceReader.getColumn("demand"));
			cascadeMainContext.systemPriceSignalDataLength = systemBasePriceSignal.length;
			cascadeMainContext.systemPriceSignalDataArray = Arrays.copyOf(systemBasePriceSignal, systemBasePriceSignal.length);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}		
		if (cascadeMainContext.systemPriceSignalDataLength % cascadeMainContext.getTickPerDay()!= 0)
		{
			System.err.println("Base System Demand array not a whole number of days. This may cause unexpected behaviour");
		}

		try {
			baseDemandReader = new CSVReader(householdAttrFile);
			baseDemandReader.parseByColumn();
			numDemandColumns = baseDemandReader.columnsStarting("demand");

			String demandName = "demand" + RandomHelper.nextIntFromTo(0, numDemandColumns - 1);
			if (cascadeMainContext.verbose)
				System.out.println("householdBaseDemandArray is initialised with profile " + demandName);
			householdBaseDemandArray = ArrayUtils.convertStringArraytofloatArray(baseDemandReader.getColumn(demandName));


		} catch (FileNotFoundException e) {
			System.err.println("Could not find file with name " + householdAttrFileName);
			e.printStackTrace();
			RunEnvironment.getInstance().endRun();
		}
	}
	

	/*
	 * Populate the context(by creating agents, actors, objects, etc)
	 */
	private void populateContext() {
		

		
		//Convert base demand to half hourly (or whatever fraction of a day we are working with)
		// NOTE - I am assuming input in kWh.  If in kW, this should average rather than sum!!
		
		// NOT SURE THIS IS A GOOD WAY - SHOULDN'T WE PRE-PROCESS TO THE RIGHT TIME-STEPS?
		// THEREBY MAKING SAME ASSUMPTIONS AS FOR WEATHER ETC
		/*
		 * Populate the context with agents
		 */
		// First Householders
		
		// TODO: Is this needed?
		// Create a sub-context of householders to allow the social networks (and maybe others)
		// to be differentiated by prosumer type
		// thisContext.addSubContext(new DefaultContext("Households"));
		// Context householdContext = RepastEssentials.CreateNetwork(parentContextPath, netName, isDirected, agentClassName, fileName, format)
		
		ProsumerFactory prosumerFactroy = FactoryFinder.createProsumerFactory(this.cascadeMainContext);
		
		for (int i = 0; i < numProsumers; i++) {
			HouseholdProsumer hhProsAgent = prosumerFactroy.createHouseholdProsumer(householdBaseDemandArray, true);
			cascadeMainContext.add(hhProsAgent);			
		}
		
		
		//A 2 MW windmill
		ProsumerAgent firstWindmill = prosumerFactroy.createPureGenerator(2000, GENERATOR_TYPE.WIND);
		//ProsumerAgent firstWindmill = createPureGenerator(2000, GENERATOR_TYPE.WIND);
		cascadeMainContext.add(firstWindmill);
		
	
		//Secondly add aggregator(s)
		AggregatorAgent firstAggregator = new AggregatorAgent(cascadeMainContext, cascadeMainContext.systemPriceSignalDataArray);
		cascadeMainContext.add(firstAggregator);
		
		
		//Create the household social network before other agent types are added to the context.
		NetworkFactory smartFactory = NetworkFactoryFinder.createNetworkFactory(null);
		
		// create a small world social network
		double beta = 0.1;
		int degree = 2;
		boolean directed = true;
		boolean symmetric = true;
		NetworkGenerator gen = new WattsBetaSmallWorldGenerator(beta, degree, symmetric);
		Network social = smartFactory.createNetwork("socialNetwork", cascadeMainContext, gen, directed);
		//set weight of each social contact - initially random
		//this will represent the influence a contact may have on another
		//Note that influence of x on y may not be same as y on x - which is realistic
		for (Object thisEdge : social.getEdges())
		{
			((RepastEdge) thisEdge).setWeight(RandomHelper.nextDouble());			
		}
		//Add in some generators
	
		/*
		 * Create the projections needed in the context and add agents to those projections
		 */
		GeographyParameters geoParams = new GeographyParameters();
		Geography geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("Geography", cascadeMainContext, geoParams);

		// Create null networks for other than social at this point.
		
		// Economic network should be hierarchical aggregator to prosumer 
		// TODO: Decide what economic network between aggregators looks like?
		// TODO: i.e. what is market design for aggregators?
		Network economicNet = smartFactory.createNetwork("economicNetwork", cascadeMainContext, directed);
		
		// TODO: replace this with something better.  Next iteration of code
		// should probably take network design from a file
		for (ProsumerAgent thisAgent:(Iterable<ProsumerAgent>) (cascadeMainContext.getObjects(ProsumerAgent.class)) )
		{
			economicNet.addEdge(firstAggregator, thisAgent);
		}
		
		// We should create a bespoke network for the electrical networks.
		// ProsumerAgents only - edges should have nominal voltage and capacity
		// attributes.  TODO: How do we deal with transformers??
		Network physicalNet = smartFactory.createNetwork("electricalNetwork", cascadeMainContext, directed);
		// TODO: How does info network differ from economic network?
		Network infoNet = smartFactory.createNetwork("infoNetwork", cascadeMainContext, directed);
		
		for (ProsumerAgent thisAgent:(Iterable<ProsumerAgent>) (cascadeMainContext.getObjects(ProsumerAgent.class)) )
		{
			infoNet.addEdge(firstAggregator, thisAgent);
		}

		
	}


}

