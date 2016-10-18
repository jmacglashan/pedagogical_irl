package irl;

import burlap.behavior.functionapproximation.FunctionGradient;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.GroundedAction;

/**
 * @author James MacGlashan.
 */
public class PGridCostRF extends PGridRF{

	public PGridCostRF(int goalx, int goaly, IRLFeatures features) {
		super(goalx, goaly, features);
	}

	public PGridCostRF(IRLFeatures features, int goalx, int goaly, double[] params) {
		super(features, goalx, goaly, params);
	}

	@Override
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
			sum += features[i] * -1 * (params[i] * params[i]);
		}

		return sum;
	}

	@Override
	public FunctionGradient gradient(State s, GroundedAction a, State sprime) {
		double [] gradient = features.features(sprime);
		FunctionGradient fg = new FunctionGradient.SparseGradient(3);
		fg.put(0, -2 * params[0] * gradient[0]);
		fg.put(1, -2 * params[1] * gradient[1]);
		fg.put(2, -2 * params[2] * gradient[2]);
		return fg;
	}
}
