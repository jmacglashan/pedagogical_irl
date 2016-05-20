package parser;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.debugtools.DPrint;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.GroundedAction;
import burlap.mdp.singleagent.SADomain;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class Parser {

	public SADomain domain;
	public int dc = 726;

	public Parser(SADomain domain) {
		this.domain = domain;
	}

	public void toggleDebugPrinting(){
		DPrint.toggleCode(dc, !DPrint.mode(dc));
	}

	public List<Worker> parseWorkers(String dataPath){

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = null;
		try {
			node = mapper.readTree(new File(dataPath));
		} catch(IOException e) {
			throw new RuntimeException(e.getMessage());
		}

		List<Worker> workers = new ArrayList<Worker>();

		Iterator<String> workerFields = node.fieldNames();
		while(workerFields.hasNext()){
			String fieldName = workerFields.next();
			DPrint.cl(dc, fieldName);
			JsonNode workerNode = node.get(fieldName);
			Iterator<String> rfFields = workerNode.fieldNames();

			Worker worker = new Worker(fieldName);
			workers.add(worker);
			while(rfFields.hasNext()){
				String rfField = rfFields.next();
				DPrint.cl(dc, "    " + rfField);

				RewardTrajectories rt = new RewardTrajectories(rfField);
				worker.rts.add(rt);

				JsonNode trajectoryNode = workerNode.get(rfField);
				Iterator<JsonNode> stepNodes = trajectoryNode.elements();
				boolean first = true;
				List<State> states = new ArrayList<State>();
				List<GroundedAction> actions = new ArrayList<GroundedAction>();
				List<Double> rewards = new ArrayList<Double>();
				for(JsonNode snode : trajectoryNode){
					int x = snode.get(0).get(0).asInt();
					int y = snode.get(0).get(1).asInt();
					states.add(new GridWorldState(x, y));
					GroundedAction a = action(snode.get(1).asText());
					if(a != null){
						DPrint.cl(dc, "        " + x + "," + y + ", " + a.toString());
						actions.add(a);
						rewards.add(0.);
					}
					else{
						DPrint.cl(dc, "        " + x + "," + y);
					}
				}

				EpisodeAnalysis ea = new EpisodeAnalysis();
				ea.stateSequence = states;
				ea.actionSequence = actions;
				ea.rewardSequence = rewards;

				rt.episodes.add(ea);

			}


		}

		return workers;
	}

	GroundedAction action(String jsonCode){
		String aname = actionName(jsonCode);
		if(aname == null){
			return null;
		}
		GroundedAction ga = domain.getAction(aname).groundedAction();
		return ga;
	}

	String actionName(String jsonCode){
		if(jsonCode.equals("^")){
			return GridWorldDomain.ACTION_NORTH;
		}
		else if(jsonCode.equals("v")){
			return GridWorldDomain.ACTION_SOUTH;
		}
		else if(jsonCode.equals(">")){
			return GridWorldDomain.ACTION_EAST;
		}
		else if(jsonCode.equals("<")){
			return GridWorldDomain.ACTION_WEST;
		}
		return null;
	}


	public static void main(String[] args) {

		GridWorldDomain gwd = new GridWorldDomain(11, 11);
		SADomain domain = gwd.generateDomain();

		Parser p = new Parser(domain);
		p.toggleDebugPrinting();
		List<Worker> workers = p.parseWorkers("human_doing_trials.json");
		for(Worker worker : workers){
			System.out.println(worker.toString());
		}
	}

}
