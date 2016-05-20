package irl;

import burlap.behavior.functionapproximation.FunctionGradient;
import burlap.behavior.functionapproximation.ParametricFunction;
import burlap.behavior.singleagent.learnfromdemo.mlirl.support.DifferentiableRF;
import burlap.debugtools.RandomFactory;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.GroundedAction;

import java.util.Arrays;
import java.util.Random;


/**
 * @author James MacGlashan.
 */
public class PGridRF implements DifferentiableRF {

	IRLFeatures features;
	int goalx;
	int goaly;
	double [] params = new double[3];


	public PGridRF(int goalx, int goaly, IRLFeatures features) {
		this.goaly = goaly;
		this.goalx = goalx;
		this.features = features;
	}

	public PGridRF(IRLFeatures features, int goalx, int goaly, double[] params) {
		this.features = features;
		this.goalx = goalx;
		this.goaly = goaly;
		this.params = params;
	}

	public FunctionGradient gradient(State s, GroundedAction a, State sprime) {
		double [] gradient = features.features(sprime);
		FunctionGradient fg = new FunctionGradient.SparseGradient(3);
		fg.put(0, gradient[0]);
		fg.put(1, gradient[1]);
		fg.put(2, gradient[2]);
		return fg;
	}

	public void randomInit(double lower, double upper){
		Random rand = RandomFactory.getMapped(0);

		double range = upper - lower;
		for(int i = 0; i < this.params.length; i++){
			double roll = rand.nextDouble();
			this.params[i] = roll*range - lower;
		}

	}

	public int numParameters() {
		return 3;
	}

	public double getParameter(int i) {
		return params[i];
	}

	public void setParameter(int i, double p) {
		params[i] = p;
	}

	public void resetParameters() {
		params[0] = 0.;
		params[1] = 0.;
		params[2] = 0.;
	}

	public ParametricFunction copy() {
		return new PGridRF(features, goalx, goaly, params.clone());
	}

	public double reward(State s, GroundedAction a, State sprime) {
		GridWorldState gws = (GridWorldState)sprime;
		int x = gws.agent.x;
		int y = gws.agent.y;
		if(x == goalx && y == goaly){
			return 5.;
		}

		double [] features = this.features.features(sprime);
		double sum = 0.;
		for(int i = 0; i < features.length; i++){
			sum += features[i] * params[i];
		}

		return sum;
	}


	@Override
	public String toString() {
		return Arrays.toString(this.params);
	}
}
