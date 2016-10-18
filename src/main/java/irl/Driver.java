package irl;

import burlap.behavior.policy.GreedyQPolicy;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.auxiliary.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.auxiliary.StateReachability;
import burlap.behavior.singleagent.auxiliary.valuefunctionvis.ValueFunctionVisualizerGUI;
import burlap.behavior.singleagent.learnfromdemo.RewardValueProjection;
import burlap.behavior.singleagent.learnfromdemo.mlirl.MLIRL;
import burlap.behavior.singleagent.learnfromdemo.mlirl.MLIRLRequest;
import burlap.behavior.singleagent.learnfromdemo.mlirl.differentiableplanners.DifferentiableSparseSampling;
import burlap.behavior.valuefunction.QFunction;
import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldTerminalFunction;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.TerminalFunction;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.statehashing.SimpleHashableStateFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import parser.Parser;
import parser.RewardTrajectories;
import parser.Worker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class Driver {

	SADomain domain = AMTGridGenerator.domain();
	int [][] colorMap = AMTGridGenerator.coloredSquareMap();
	IRLFeatures features = new IRLFeatures(colorMap);
	//PGridRF rf = new PGridRF(5, 2, features);
	PGridRF rf = new PGridCostRF(5, 2, features);
	TerminalFunction tf = new GridWorldTerminalFunction(5, 2);


	public void runIRLAndVisualize(List<EpisodeAnalysis> episodes){
		rf.randomInit(-1., 0.);
		double beta = 1.;
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(domain, rf, tf, 0.99, new SimpleHashableStateFactory(), 30, -1, beta);
		planner.toggleDebugPrinting(false);

		//define the IRL problem
		MLIRLRequest request = new MLIRLRequest(domain, planner, episodes, rf);
		request.setBoltzmannBeta(beta);

		//run MLIRL on it
		MLIRL irl = new MLIRL(request, 0.1, 0.1, 10);
		irl.performIRL();

		//get all states in the domain so we can visualize the learned reward function for them
		List<State> allStates = StateReachability.getReachableStates(new GridWorldState(0, 0), (SADomain) this.domain, new SimpleHashableStateFactory());

		//get a standard grid world value function visualizer, but give it StateRewardFunctionValue which returns the
		//reward value received upon reaching each state which will thereby let us render the reward function that is
		//learned rather than the value function for it.
		ValueFunctionVisualizerGUI gui = GridWorldDomain.getGridWorldValueFunctionVisualization(
				allStates,
				6,
				5,
				new RewardValueProjection(rf),
				new GreedyQPolicy((QFunction)request.getPlanner())
		);


		gui.initGUI();


	}

	public double [] irl(List<EpisodeAnalysis> episodes){

		rf.randomInit(-1., 0.);
		double beta = 1.;
		DifferentiableSparseSampling planner = new DifferentiableSparseSampling(domain, rf, tf, 0.99, new SimpleHashableStateFactory(), 30, -1, beta);
		planner.toggleDebugPrinting(false);

		//define the IRL problem
		MLIRLRequest request = new MLIRLRequest(domain, planner, episodes, rf);
		request.setBoltzmannBeta(beta);

		//run MLIRL on it
		MLIRL irl = new MLIRL(request, 0.01, 0.1, 10);
		irl.toggleDebugPrinting(false);
		irl.performIRL();

		double [] params = rf.params.clone();
		if(rf instanceof PGridCostRF){
			for(int i = 0; i < params.length; i++){
				params[i] *= -1 * params[i];
			}
		}
		return params;

	}

	public double[] expectedRf(List<EpisodeAnalysis> episodes, int n){
		double [] expected = new double[3];
		for(int i = 0; i < n; i++){
			double [] params = irl(episodes);
			expected[0] += params[0] / n;
			expected[1] += params[1] / n;
			expected[2] += params[2] / n;
			if(Double.isNaN(params[0]) || Double.isNaN(params[1]) || Double.isNaN(params[2])){
				System.out.println("Warning! NaN in output!");
			}
		}
		return expected;
	}

	public void computeResults(List<Worker> workers, String outName){

		Map<String, Map<String, double[]>> results = new HashMap<String, Map<String, double[]>>(workers.size());

		int i = 0;
		for(Worker worker : workers){
			System.out.println("Starting " + worker.workerId + " (" + (i+1) + "/" + workers.size() + ")");
			Map<String, double[]> workerResults = new HashMap<String, double[]>(worker.rts.size());
			for(RewardTrajectories rt : worker.rts){
				System.out.println("Computing " + worker.workerId + ":" + rt.rewardCode);
				double [] result = expectedRf(rt.episodes, 30);
				workerResults.put(rt.rewardCode, result);
			}
			results.put(worker.workerId, workerResults);
			i++;
		}

		System.out.println("Completed. Writing to file...");

		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.writeValue(new File(outName), results);
		} catch(IOException e) {
			e.printStackTrace();
		}

		System.out.println("Process finished.");

	}


	public void visWorker(Worker worker, int testId){
		System.out.println("Showing worker " + worker.workerId + " rf " + worker.rts.get(testId).rewardCode);
		new EpisodeSequenceVisualizer(AMTGridGenerator.gridVis(colorMap), domain, worker.rts.get(testId).episodes);
		runIRLAndVisualize(worker.rts.get(0).episodes);
	}

	public void visWorker(Worker worker, String rfCode){

		int testId = -1;
		for(int i = 0; i < worker.rts.size(); i++){
			if(worker.rts.get(i).rewardCode.equals(rfCode)){
				testId = i;
				break;
			}
		}
		if(testId == -1){
			throw new RuntimeException("RF Code " + rfCode + " not found");
		}
		visWorker(worker, testId);
	}

	public static void main(String[] args) {

		Driver driver = new Driver();

//		String dataPath = "human_doing_trials.json";
//		String outPath = "irl_human_doing.json";

//		String dataPath = "human_showing_trials.json";
//		String outPath = "irl_human_showing.json";

//		String dataPath = "model_pedagogy_trials.json";
//		String outPath = "irl_model_pedagogy.json";

//		String dataPath = "model_standard_trials.json";
//		String outPath = "irl_model_standard.json";


//		String dataPath = "new_set/human_doing_trials.json";
//		String outPath = "new_set/irl_human_doing.json";

		String dataPath = "new_set/human_showing_trials.json";
		String outPath = "new_set/irl_human_showing_0.01.json";

//		String dataPath = "new_set/model_pedagogy_trials.json";
//		String outPath = "new_set/irl_model_pedagogy.json";

//		String dataPath = "new_set/model_standard_trials.json";
//		String outPath = "new_set/irl_model_standard.json";


		Parser p = new Parser(driver.domain);
		p.toggleDebugPrinting();
		List<Worker> workers = p.parseWorkers(dataPath);

		//driver.visWorker(workers.get(2), "1A");

		//double [] expected = driver.expectedRf(workers.get(0).rts.get(0).episodes, 30);
		//System.out.println(Arrays.toString(expected));

		driver.computeResults(workers, outPath);

	}

}
