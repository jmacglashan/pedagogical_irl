package parser;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.domain.singleagent.gridworld.state.GridWorldState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class Worker {
	public String workerId;
	public List<RewardTrajectories> rts = new ArrayList<RewardTrajectories>();

	public Worker(String workerId) {
		this.workerId = workerId;
	}


	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(workerId).append("\n");
		for(RewardTrajectories rt : rts){
			buf.append("     " + rt.rewardCode).append("\n");
			EpisodeAnalysis ea = rt.episodes.get(0);
			for(int t = 0; t < ea.numTimeSteps(); t++){
				GridWorldState gws = (GridWorldState)ea.getState(t);
				buf.append("        ").append(gws.agent.x).append(",").append(gws.agent.y);
				if(t < ea.maxTimeStep()){
					buf.append(", ").append(ea.getAction(t).toString());
				}
				buf.append("\n");
			}
		}

		return buf.toString();
	}
}
