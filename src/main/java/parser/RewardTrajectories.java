package parser;

import burlap.behavior.singleagent.EpisodeAnalysis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class RewardTrajectories {
	public String rewardCode;
	public List<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();

	public RewardTrajectories(String rewardCode) {
		this.rewardCode = rewardCode;
	}
}
