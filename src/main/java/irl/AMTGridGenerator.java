package irl;

import burlap.domain.singleagent.gridworld.GridWorldDomain;
import burlap.domain.singleagent.gridworld.GridWorldVisualizer;
import burlap.domain.singleagent.gridworld.state.GridWorldState;
import burlap.mdp.core.Domain;
import burlap.mdp.core.TransitionProbability;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.Action;
import burlap.mdp.singleagent.FullActionModel;
import burlap.mdp.singleagent.GroundedAction;
import burlap.mdp.singleagent.SADomain;
import burlap.mdp.singleagent.explorer.VisualExplorer;
import burlap.mdp.visualizer.OOStatePainter;
import burlap.mdp.visualizer.StatePainter;
import burlap.mdp.visualizer.StateRenderLayer;
import burlap.mdp.visualizer.Visualizer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class AMTGridGenerator {

	public static SADomain domain(){

		int w = 6;
		int h = 5;

		GridWorldDomain gwd = new GridWorldDomain(w, h);
		SADomain srcDomain = gwd.generateDomain();

		SADomain preDomain = new SADomain();
		new PreGridAction(preDomain, srcDomain.getAction(GridWorldDomain.ACTION_NORTH), 0, 1, w, h);
		new PreGridAction(preDomain, srcDomain.getAction(GridWorldDomain.ACTION_SOUTH), 0, -1, w, h);
		new PreGridAction(preDomain, srcDomain.getAction(GridWorldDomain.ACTION_EAST), 1, 0, w, h);
		new PreGridAction(preDomain, srcDomain.getAction(GridWorldDomain.ACTION_WEST), -1, 0, w, h);

		return preDomain;
	}

	public static int [][] coloredSquareMap(){
		int w = 6;
		int h = 5;

		int [][] map = new int[w][h];

		//cyan
		map[1][0] = 1;
		map[2][0] = 1;
		map[3][0] = 1;
		map[4][0] = 1;
		map[4][1] = 1;
		map[4][2] = 1;
		map[3][2] = 1;

		//orange
		map[1][4] = 2;
		map[2][4] = 2;
		map[3][4] = 2;
		map[4][4] = 2;
		map[1][3] = 2;
		map[1][2] = 2;
		map[1][1] = 2;

		//purple
		map[2][3] = 3;
		map[3][3] = 3;
		map[4][3] = 3;
		map[2][2] = 3;
		map[2][1] = 3;
		map[3][1] = 3;

		//yellow
		map[5][2] = 4;

		return map;
	}


	public static Visualizer gridVis(final int [][] colorMap){
		int [][] wallMap = new int[colorMap.length][colorMap[0].length];

		StatePainter sp = new StatePainter() {
			public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {

				int dwidth = colorMap.length;
				int dheight = colorMap[0].length;

				float domainXScale = dwidth;
				float domainYScale = dheight;

				//determine then normalized width
				float width = (1.0f / domainXScale) * cWidth;
				float height = (1.0f / domainYScale) * cHeight;

				//pass through each cell of the map and if it is a wall, draw it
				for(int i = 0; i < dwidth; i++) {
					for(int j = 0; j < dheight; j++) {

						int cell = colorMap[i][j];

						if(cell != 0) {

							if(cell == 1) {
								g2.setColor(Color.cyan);
							} else if(cell == 2) {
								g2.setColor(Color.orange);
							} else if(cell == 3) {
								g2.setColor(Color.magenta);
							} else if(cell == 4) {
								g2.setColor(Color.yellow);
							}

							float rx = i * width;
							float ry = cHeight - height - j * height;

							g2.fill(new Rectangle2D.Float(rx, ry, width, height));

						}
					}
				}
			}
		};

		StateRenderLayer r = new StateRenderLayer();

		r.addStatePainter(sp);
		OOStatePainter oopainter = new OOStatePainter();
		oopainter.addObjectClassPainter(GridWorldDomain.CLASS_LOCATION, new GridWorldVisualizer.LocationPainter(wallMap));
		oopainter.addObjectClassPainter(GridWorldDomain.CLASS_AGENT, new GridWorldVisualizer.CellPainter(1, Color.gray, wallMap));
		r.addStatePainter(oopainter);

		Visualizer v = new Visualizer(r);

		return v;

	}


	public static class PreGridAction extends Action implements FullActionModel{

		Action srcAction;
		int xd;
		int yd;
		int width;
		int height;

		public PreGridAction(Domain domain, Action srcAction, int xd, int yd, int width, int height) {
			super(srcAction.getName(), domain);
			this.srcAction = srcAction;
			this.xd = xd;
			this.yd = yd;
			this.width = width;
			this.height = height;
		}


		public boolean applicableInState(State s, GroundedAction groundedAction) {

			GridWorldState gws = (GridWorldState)s;
			int nx = gws.agent.x + xd;
			int ny = gws.agent.y + yd;

			if(nx < 0 || ny < 0 || nx >= width || ny >= height){
				return false;
			}

			return true;
		}

		public boolean isPrimitive() {
			return true;
		}

		public boolean isParameterized() {
			return false;
		}

		public GroundedAction associatedGroundedAction() {
			return srcAction.associatedGroundedAction();
		}

		public List<GroundedAction> allApplicableGroundedActions(State s) {
			GroundedAction ga = associatedGroundedAction();
			if(applicableInState(s, ga)){
				return Arrays.asList(ga);
			}
			return new ArrayList<GroundedAction>();
		}

		protected State sampleHelper(State s, GroundedAction groundedAction) {
			return srcAction.sample(s, groundedAction);
		}

		public List<TransitionProbability> transitions(State s, GroundedAction groundedAction) {
			return ((FullActionModel)srcAction).transitions(s, groundedAction);
		}
	}

	public static void main(String[] args) {

		SADomain domain = domain();
		int [][] colorMap = coloredSquareMap();

		State s = new GridWorldState(0, 2);

		Visualizer v = gridVis(colorMap);
		VisualExplorer exp = new VisualExplorer(domain, v, s);

		exp.addKeyAction("w", GridWorldDomain.ACTION_NORTH);
		exp.addKeyAction("s", GridWorldDomain.ACTION_SOUTH);
		exp.addKeyAction("d", GridWorldDomain.ACTION_EAST);
		exp.addKeyAction("a", GridWorldDomain.ACTION_WEST);

		exp.initGUI();

	}

}
