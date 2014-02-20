/**
 * 
 */
package uk.ac.dmu.iesd.cascade.util;

import java.util.Arrays;

import org.apache.commons.mathforsimplex.analysis.MultivariateRealFunction;
import org.jgap.Chromosome;
import org.jgap.FitnessFunction;

import flanagan.math.Fmath;
import flanagan.math.MinimisationFunction;

/**
 * @author J. Richard Snape
 * @author Dennis Fan
 * 
 * 
 * 
 * New class member to test out Peter B's demand flattening approach with smart signal
 * Same as class member RecoMinimisationFunction, apart from the function method is different
 * In this case, equation in function has change to |Di - Bm|, where <Di> is the same as the 
 * specification described in the paper, and <Bm> is the mean from the baseline load.
 *	
 * Last updated: (20/02/14) JRS Re-factored out of Aggregator implementations
 * 
 * For full history see git logs.
 *
 */
public class MinimisationFunctionObjectiveFlatDemand  extends FitnessFunction implements MinimisationFunction, MultivariateRealFunction {
	
		
		private static final long serialVersionUID = 1L;

		private double[] arr_B;
		private double[] Kneg;
		private double[] Kpos;
		private double[] Cavge;
		private boolean hasSimpleSumConstraint = false;
		private boolean lessThanConstraint;
		private double sumConstraintValue;
		private double penaltyWeight  = 1.0e10;
		private double sumConstraintTolerance;
		private boolean hasEqualsConstraint = false;
		private int numEvaluations = 0;
		boolean printD = false;

		public double function (double[] arr_S) {
			double m =0d;
			double[] d = new double[arr_B.length];
			//mean_B = ArrayUtils.avg(arr_B);
			
			//Note - interestingly - this will predict Baseline + Cavge for a zero
			//signal.  This works.  But if you make it predict Baseline for a zero
			//signal, it blows up!!  Rather sensitive...
			for (int i = 0; i < arr_S.length; i++) {
				if (arr_S[i] < 0) {
					d[i] = arr_B[i] + (arr_S[i] * Kneg[i] * arr_B[i]) + Cavge[i];
				}
				else
				{
					d[i] = arr_B[i] + (arr_S[i] * Kpos[i] * arr_B[i]) + Cavge[i];
				}
				//m += Math.abs(di - mean_B);
			}
			
			
			m=ArrayUtils.sum(ArrayUtils.absoluteValues(ArrayUtils.offset(d, -ArrayUtils.avg(d))));
			
			//m=(ArrayUtils.max(d)/ArrayUtils.avg(d))*1000;
			numEvaluations++;
			m += checkPlusMin1Constraint(arr_S);
			//m += checkPosNegConstraint(arr_S);

			return m;
		} 

		private double checkPlusMin1Constraint(double[] arr_S) {
			double penalty = 0;
			double posValueSum = 0;
			double negValueSum = 0;
			for (int i = 0; i < arr_S.length; i++)
			{
				if (arr_S[i] > 1 && arr_S[i] > posValueSum)		{
					posValueSum = arr_S[i];
				}
				else if (arr_S[i] < -1 && arr_S[i] < negValueSum) {
					negValueSum = arr_S[i];
				}
			}

			if (posValueSum > 1)	{
				penalty += this.penaltyWeight * Math.pow((posValueSum - 1), 2);
			}
			
			if (negValueSum < -1)	{
				penalty += this.penaltyWeight * Math.pow((-1 - negValueSum), 2);
			}
			
			return penalty;
		}

		/**
		 * Enforce the constraint that all positive values of S must sum to (maximum) of 1
		 * and -ve values to (minimum) of -1
		 * @param arr_S
		 * @return
		 */
		private double checkPosNegConstraint(double[] arr_S) {
			double penalty = 0;
			double posValueSum = 0;
			double negValueSum = 0;
			for (int i = 0; i < arr_S.length; i++)
			{
				if (arr_S[i] > 0)		{
					posValueSum += arr_S[i];
				}
				else {
					negValueSum += arr_S[i];
				}
			}

		/*	if (posValueSum > 1)	{
				penalty += this.penaltyWeight * Math.pow((posValueSum - 1), 2);
			}
			
			if (negValueSum < -1)	{
				penalty += this.penaltyWeight * Math.pow((-1 - negValueSum), 2);
			}*/
			
			penalty += this.penaltyWeight * Math.pow((posValueSum + negValueSum), 2);
			
			return penalty;
		}

		public double value (double[] arr_S)	{
			double penalties = 0;
			// Add on constraint penalties here (as the Apache NelderMead doesn't do constraints itself)
			double sumOfArray = ArrayUtils.sum(arr_S);


			if (this.hasEqualsConstraint  && (Math.sqrt(Math.pow(sumOfArray - sumConstraintValue, 2)) > this.sumConstraintTolerance))	{
				penalties = this.penaltyWeight*Fmath.square(sumConstraintValue*(1.0-this.sumConstraintTolerance)-sumOfArray);
			}
			return function(arr_S) + penalties;
		}

		public void addSimpleSumEqualsConstraintForApache(double limit, double tolerance)	{
			this.hasEqualsConstraint = true;
			this.sumConstraintTolerance = tolerance;
			this.sumConstraintValue = limit;

		}

		
		
		public void set_pointer_to_B(double [] b) 
		{
			arr_B = b;
		}

		public void set_pointer_to_Kneg(double [] e) 
		{
			Kneg = e;
		}

		public void set_pointer_to_Kpos(double [] k ) 
		{
			Kpos = k;
		}
		
		public void set_pointer_to_Cavge(double[] c)
		{
			Cavge = c;
		}

		public int getNumEvals()	{
			return numEvaluations;
		}

		/* (non-Javadoc)
		 * @see org.jgap.FitnessFunction#evaluate(org.jgap.Chromosome)
		 */
		@Override
		protected int evaluate(Chromosome arg0) {

			double[] testArray = ArrayUtils.genesToDouble(arg0.getGenes());
			for (int i = 0; i < testArray.length; i++)	{
				testArray[i] -= 0.5;
				testArray[i] *= 2;
			}

			return (int) Math.max(1,(100000 - value(testArray)));
		}

		/**
		 * @param b
		 */
		public void setPrintD(boolean b) {
			// TODO Auto-generated method stub
			this.printD = b;
		}
		

}
