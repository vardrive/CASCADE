/**
 * 
 */
package behaviour.psychological;

/**
 * @author jsnape
 *
 */
public class SimpleConstruct implements Construct {
	
	private float weight;
	private String name;
	
	/* (non-Javadoc)
	 * @see behaviour.psychological.Construct#evaluate()
	 */
	@Override
	public float evaluate() {
		// TODO Auto-generated method stub
		
		return weight;
	}

	/* (non-Javadoc)
	 * @see behaviour.psychological.Construct#getCurrentValue()
	 */
	@Override
	public float getCurrentValue() {
		// TODO Auto-generated method stub
		return weight;
	}

	/* (non-Javadoc)
	 * @see behaviour.psychological.Construct#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	public SimpleConstruct(String name)
	{
		this.name = name;
		this.weight = 0;
	}
	
	public SimpleConstruct(String name, float initialWeight)
	{
		this.name = name;
		this.weight = initialWeight;
	}
	
	public String toString()
	{
		StringBuilder message = new StringBuilder();
		message.append(super.toString());
		message.append("\n");
		message.append("This is a construct, name: " + this.name + " and weight: "+ this.weight);
		return message.toString();
	}
}
