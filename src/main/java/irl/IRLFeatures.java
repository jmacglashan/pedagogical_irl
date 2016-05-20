package irl;

import burlap.behavior.functionapproximation.dense.DenseStateFeatures;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.state.State;

/**
 * @author James MacGlashan.
 */
public class IRLFeatures implements DenseStateFeatures {

	protected int[][] map;

	public IRLFeatures(int[][] map) {
		this.map = map;
	}

	public double[] features(State s) {
		GridWorldState gws = (GridWorldState)s;
		int x = gws.agent.x;
		int y = gws.agent.y;

		int cell = map[x][y];


		double [] features = new double[3];
		if(cell != 0 && cell < 4){
			features[cell-1] = 1.;
		}

		return features;
	}

	public DenseStateFeatures copy() {
		return new IRLFeatures(map);
	}
}
