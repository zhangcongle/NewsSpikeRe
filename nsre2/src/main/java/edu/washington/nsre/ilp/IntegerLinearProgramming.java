package edu.washington.nsre.ilp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.washington.nsre.util.D;
import edu.washington.nsre.util.DW;

import com.google.common.collect.HashBiMap;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

public class IntegerLinearProgramming {

	Counter<String> objective;
	mosek.Env.objsense objsense;

	List<Counter<String>> constraints = new ArrayList<Counter<String>>();
	List<mosek.Env.boundkey> constraints_bound_type_list = new ArrayList<mosek.Env.boundkey>();
	List<Double> constraints_lowerbound_list = new ArrayList<Double>();
	List<Double> constraints_upperbound_list = new ArrayList<Double>();

	HashMap<String, double[]> variables_lowerupper = new HashMap<String, double[]>();
	double[] default_variable_lowerupper = new double[2];

	public IntegerLinearProgramming() {

	}

	public void setObjective(Counter<String> objective, boolean isMaximize) {
		this.objective = objective;
		if (isMaximize) {
			objsense = mosek.Env.objsense.maximize;
		} else {
			objsense = mosek.Env.objsense.minimize;
		}
		default_variable_lowerupper = new double[] { 0.0, 1.0 };
	}

	public void addConstraint(Counter<String> constraint,
			boolean hasLowerBound,
			double lower,
			boolean hasUpperBound,
			double upper) {
		constraints.add(constraint);
		mosek.Env.boundkey bound = mosek.Env.boundkey.ra;
		if (hasUpperBound && hasLowerBound) {
			bound = mosek.Env.boundkey.ra;
		} else if (hasUpperBound && !hasLowerBound) {
			bound = mosek.Env.boundkey.up;
		} else if (!hasUpperBound && hasLowerBound) {
			bound = mosek.Env.boundkey.lo;
		} else {
			bound = mosek.Env.boundkey.fr;// free
		}
		constraints_bound_type_list.add(bound);
		constraints_lowerbound_list.add(lower);
		constraints_upperbound_list.add(upper);
	}

	public void setupVariableLowerUpper(String variable, double lower, double upper) {
		variables_lowerupper.put(variable, new double[] { lower, upper });
	}

	public HashMap<String, Double> run() {
		HashBiMap<String, Integer> variable2id = HashBiMap.create();
		for (Entry<String, Double> e : objective.entrySet()) {
			if (!variable2id.containsKey(e.getKey())) {
				variable2id.put(e.getKey(), variable2id.size());
			}
		}
		for (Counter<String> counter : constraints) {
			for (Entry<String, Double> e : counter.entrySet()) {
				if (!variable2id.containsKey(e.getKey())) {
					variable2id.put(e.getKey(), variable2id.size());
				}
			}
		}
		int NUMVAR = variable2id.size();
		int NUMCON = constraints.size();
		int NUMANZ = 0;

		// set up objective function
		double[] c = new double[NUMVAR];
		for (Entry<String, Double> e : objective.entrySet()) {
			int vid = variable2id.get(e.getKey());
			c[vid] = e.getValue();
		}
		// set up constraints
		mosek.Env.boundkey[] bkc = new mosek.Env.boundkey[NUMCON];
		double[] blc = new double[NUMCON];
		double[] buc = new double[NUMCON];
		int asub[][] = new int[NUMCON][];
		double aval[][] = new double[NUMCON][];
		for (int i = 0; i < NUMCON; i++) {
			bkc[i] = constraints_bound_type_list.get(i);
			blc[i] = constraints_lowerbound_list.get(i);
			buc[i] = constraints_upperbound_list.get(i);
			Counter<String> counter = constraints.get(i);
			asub[i] = new int[counter.keySet().size()];
			aval[i] = new double[counter.keySet().size()];
			int k = 0;
			for (Entry<String, Double> e : counter.entrySet()) {
				int vid = variable2id.get(e.getKey());
				asub[i][k] = vid;
				aval[i][k] = e.getValue();
				k++;
				NUMANZ++;
			}
		}

		// set up variable constraints
		mosek.Env.boundkey[] bkx = new mosek.Env.boundkey[NUMVAR];
		double blx[] = new double[NUMVAR];
		double bux[] = new double[NUMVAR];
		{
			for (String v : variable2id.keySet()) {
				int vid = variable2id.get(v);
				double[] lowerupper = default_variable_lowerupper;
				if (variables_lowerupper.containsKey(v)) {
					lowerupper = variables_lowerupper.get(v);
				}
				bkx[vid] = mosek.Env.boundkey.ra;
				blx[vid] = lowerupper[0];
				bux[vid] = lowerupper[1];
			}
		}
		HashMap<String, Double> var2val = new HashMap<String, Double>();

		double[] xx = callILP3(NUMVAR,
				NUMCON,
				NUMANZ,
				c,
				bkx,
				blx,
				bux,
				asub,
				aval,
				bkc,
				blc,
				buc,
				false);
		for (String v : variable2id.keySet()) {
			int vid = variable2id.get(v);
			var2val.put(v, xx[vid]);
		}
		return var2val;
	}

	// public static double[] callILP2(int NUMVAR,
	// int NUMCON,
	// int NUMANZ,
	// double[] c,
	// mosek.Env.boundkey[] bkx,
	// double[] blx,
	// double[] bux,
	// int asub[][],
	// double aval[][],
	// mosek.Env.boundkey[] bkc,
	// double blc[],
	// double buc[]) {
	//
	// double[] xx = new double[NUMVAR];
	//
	// mosek.Env env = null;
	// mosek.Task task = null;
	//
	// try {
	// // Make mosek environment.
	// env = new mosek.Env();
	// // Direct the env log stream to the user specified
	// // method env_msg_obj.stream
	// msgclass env_msg_obj = new msgclass();
	// env.set_Stream(mosek.Env.streamtype.log, env_msg_obj);
	// // Initialize the environment.
	// env.init();
	// // Create a task object linked with the environment env.
	// task = new mosek.Task(env, 0, 0);
	// // Directs the log task stream to the user specified
	// // method task_msg_obj.stream
	// msgclass task_msg_obj = new msgclass();
	// task.set_Stream(mosek.Env.streamtype.log, task_msg_obj);
	// /*
	// * Give MOSEK an estimate of the size of the input data. This is
	// * done to increase the speed of inputting data. However, it is
	// * optional.
	// */
	// task.putmaxnumvar(NUMVAR);
	// task.putmaxnumcon(NUMCON);
	// task.putmaxnumanz(NUMANZ);
	// /*
	// * Append 'NUMCON' empty constraints. The constraints will initially
	// * have no bounds.
	// */
	// task.append(mosek.Env.accmode.con, NUMCON);
	//
	// /*
	// * Append 'NUMVAR' variables. The variables will initially be fixed
	// * at zero (x=0).
	// */
	// task.append(mosek.Env.accmode.var, NUMVAR);
	//
	// /* Optionally add a constant term to the objective. */
	// task.putcfix(0.0);
	//
	// for (int j = 0; j < NUMVAR; ++j) {
	// /* Set the linear term c_j in the objective. */
	// task.putcj(j, c[j]);
	// task.putbound(mosek.Env.accmode.var, j, bkx[j], blx[j], bux[j]);
	//
	// }
	// for (int j = 0; j < NUMCON; j++) {
	// /*
	// * Set the bounds on variable j. blx[j] <= x_j <= bux[j]
	// */
	// task.putbound(mosek.Env.accmode.con, j, bkc[j], blc[j], buc[j]);
	//
	// /* Input row i of A */
	// task.putavec(mosek.Env.accmode.con, /* Input row of A. */
	// j, /* Row index. */
	// asub[j], /* Column indexes of non-zeros in row i. */
	// aval[j]); /* Non-zero Values of row i. */
	// }
	// /*
	// * Set the bounds on constraints. for i=1, ...,NUMCON : blc[i] <=
	// * constraint i <= buc[i]
	// */
	// for (int i = 0; i < NUMCON; ++i)
	// task.putbound(mosek.Env.accmode.con, i, bkc[i], blc[i], buc[i]);
	//
	// /* Specify integer variables. */
	// for (int j = 0; j < NUMVAR; ++j)
	// task.putvartype(j, mosek.Env.variabletype.type_int);
	//
	// /* A maximization problem */
	// task.putobjsense(mosek.Env.objsense.maximize);
	// /* Solve the problem */
	// try {
	// task.optimize();
	// } catch (mosek.Warning e) {
	// System.out.println(" Mosek warning:");
	// System.out.println(e.toString());
	// }
	//
	// // Print a summary containing information
	// // about the solution for debugging purposes
	// task.solutionsummary(mosek.Env.streamtype.msg);
	// task.getsolutionslice(mosek.Env.soltype.itg, // Integer solution.
	// mosek.Env.solitem.xx, // Which part of solution.
	// 0, // Index of first variable.
	// NUMVAR, // Index of last variable+1
	// xx);
	// mosek.Env.solsta solsta[] = new mosek.Env.solsta[1];
	// mosek.Env.prosta prosta[] = new mosek.Env.prosta[1];
	// /* Get status information about the solution */
	// task.getsolutionstatus(mosek.Env.soltype.itg,
	// prosta,
	// solsta);
	// switch (solsta[0]) {
	// case integer_optimal:
	// case near_integer_optimal:
	// // System.out.println("Optimal solution\n");
	// // for (int j = 0; j < NUMVAR; ++j)
	// // System.out.println("x[" + j + "]:" + xx[j]);
	// break;
	// case prim_feas:
	// // System.out.println("Feasible solution\n");
	// // for (int j = 0; j < NUMVAR; ++j)
	// // System.out.println("x[" + j + "]:" + xx[j]);
	// break;
	//
	// case unknown:
	// System.out.println("Unknown solution status.\n");
	// break;
	// default:
	// System.out.println("Other solution status");
	// break;
	// }
	// } catch (mosek.ArrayLengthException e) {
	// System.out.println("Error: An array was too short");
	// System.out.println(e.toString());
	// } catch (mosek.Exception e)
	// /* Catch both mosek.Error and mosek.Warning */
	// {
	// System.out.println("An error or warning was encountered");
	// System.out.println(e.getMessage());
	// }
	//
	// if (task != null)
	// task.dispose();
	// if (env != null)
	// env.dispose();
	// return xx;
	// }

	public static double[] callILP3(int numvar,
			int numcon,
			int NUMANZ,
			double[] c,
			mosek.Env.boundkey[] bkx,
			double[] blx,
			double[] bux,
			int asub[][],
			double aval[][],
			mosek.Env.boundkey[] bkc,
			double blc[],
			double buc[]) {

		double[] xx = new double[numvar];

		mosek.Env env = null;
		mosek.Task task = null;

		try {
			// Make mosek environment.
			env = new mosek.Env();
			// Create a task object linked with the environment env.
			task = new mosek.Task(env, 0, 0);
			// Directs the log task stream to the user specified
			// method task_msg_obj.stream
			task.set_Stream(
					mosek.Env.streamtype.log,
					new mosek.Stream()
					{
						public void stream(String msg) {
							System.out.print(msg);
						}
					});

			/*
			 * Give MOSEK an estimate of the size of the input data. This is
			 * done to increase the speed of inputting data. However, it is
			 * optional.
			 */
			/*
			 * Append 'numcon' empty constraints. The constraints will initially
			 * have no bounds.
			 */
			task.appendcons(numcon);

			/*
			 * Append 'numvar' variables. The variables will initially be fixed
			 * at zero (x=0).
			 */
			task.appendvars(numvar);

			for (int j = 0; j < numvar; ++j)
			{
				/* Set the linear term c_j in the objective. */
				task.putcj(j, c[j]);
				/*
				 * Set the bounds on variable j. blx[j] <= x_j <= bux[j]
				 */
				task.putbound(mosek.Env.accmode.var, j, bkx[j], blx[j], bux[j]);
			}
			/* Specify integer variables. */
			for (int j = 0; j < numvar; ++j)
				task.putvartype(j, mosek.Env.variabletype.type_int);
			/*
			 * Set the bounds on constraints. for i=1, ...,numcon : blc[i] <=
			 * constraint i <= buc[i]
			 */
			for (int i = 0; i < numcon; ++i)
			{
				task.putbound(mosek.Env.accmode.con, i, bkc[i], blc[i], buc[i]);

				/* Input row i of A */
				task.putarow(i, /* Row index. */
						asub[i], /* Column indexes of non-zeros in row i. */
						aval[i]); /* Non-zero Values of row i. */
			}

			/* A maximization problem */
			task.putobjsense(mosek.Env.objsense.maximize);

			/* Solve the problem */
			// a lot of prints...
			mosek.Env.rescode r = task.optimize();

			// Print a summary containing information
			// about the solution for debugging purposes
			task.solutionsummary(mosek.Env.streamtype.msg);

			mosek.Env.solsta solsta[] = new mosek.Env.solsta[1];
			/* Get status information about the solution */
			task.getsolsta(mosek.Env.soltype.itg, solsta);

			task.getxx(mosek.Env.soltype.itg, // Basic solution.
					xx);

			switch (solsta[0])
			{
			case optimal:
			case near_optimal:
				System.out.println("Optimal primal solution\n");
				for (int j = 0; j < numvar; ++j)
					System.out.println("x[" + j + "]:" + xx[j]);
				break;
			case dual_infeas_cer:
			case prim_infeas_cer:
			case near_dual_infeas_cer:
			case near_prim_infeas_cer:
				System.out.println("Primal or dual infeasibility.\n");
				break;
			case unknown:
				System.out.println("Unknown solution status.\n");
				break;
			default:
				System.out.println("Other solution status");
				break;
			}
		} catch (mosek.ArrayLengthException e) {
			System.out.println("Error: An array was too short");
			System.out.println(e.toString());
		} catch (mosek.Exception e)
		/* Catch both mosek.Error and mosek.Warning */
		{
			System.out.println("An error or warning was encountered");
			System.out.println(e.getMessage());
		}

		if (task != null)
			task.dispose();
		if (env != null)
			env.dispose();
		return xx;
	}

	public static double[] callILP3(int numvar,
			int numcon,
			int NUMANZ,
			double[] c,
			mosek.Env.boundkey[] bkx,
			double[] blx,
			double[] bux,
			int asub[][],
			double aval[][],
			mosek.Env.boundkey[] bkc,
			double blc[],
			double buc[],
			final boolean print) {

		double[] xx = new double[numvar];

		mosek.Env env = null;
		mosek.Task task = null;

		try {
			// Make mosek environment.
			env = new mosek.Env();
			// Create a task object linked with the environment env.
			task = new mosek.Task(env, 0, 0);
			// Directs the log task stream to the user specified
			// method task_msg_obj.stream
			task.set_Stream(
					mosek.Env.streamtype.log,
					new mosek.Stream()
					{
						public void stream(String msg) {
							if (print) {
								System.out.print(msg);
							}
						}
					});

			/*
			 * Give MOSEK an estimate of the size of the input data. This is
			 * done to increase the speed of inputting data. However, it is
			 * optional.
			 */
			/*
			 * Append 'numcon' empty constraints. The constraints will initially
			 * have no bounds.
			 */
			task.appendcons(numcon);

			/*
			 * Append 'numvar' variables. The variables will initially be fixed
			 * at zero (x=0).
			 */
			task.appendvars(numvar);

			for (int j = 0; j < numvar; ++j)
			{
				/* Set the linear term c_j in the objective. */
				task.putcj(j, c[j]);
				/*
				 * Set the bounds on variable j. blx[j] <= x_j <= bux[j]
				 */
				task.putbound(mosek.Env.accmode.var, j, bkx[j], blx[j], bux[j]);
			}
			/* Specify integer variables. */
			for (int j = 0; j < numvar; ++j)
				task.putvartype(j, mosek.Env.variabletype.type_int);
			/*
			 * Set the bounds on constraints. for i=1, ...,numcon : blc[i] <=
			 * constraint i <= buc[i]
			 */
			for (int i = 0; i < numcon; ++i)
			{
				task.putbound(mosek.Env.accmode.con, i, bkc[i], blc[i], buc[i]);

				/* Input row i of A */
				task.putarow(i, /* Row index. */
						asub[i], /* Column indexes of non-zeros in row i. */
						aval[i]); /* Non-zero Values of row i. */
			}

			/* A maximization problem */
			task.putobjsense(mosek.Env.objsense.maximize);

			/* Solve the problem */
			// a lot of prints...
			mosek.Env.rescode r = task.optimize();

			// Print a summary containing information
			// about the solution for debugging purposes
			task.solutionsummary(mosek.Env.streamtype.msg);

			mosek.Env.solsta solsta[] = new mosek.Env.solsta[1];
			/* Get status information about the solution */
			task.getsolsta(mosek.Env.soltype.itg, solsta);

			task.getxx(mosek.Env.soltype.itg, // Basic solution.
					xx);

			if (print) {
				switch (solsta[0])
				{
				case optimal:
				case near_optimal:
					System.out.println("Optimal primal solution\n");
					for (int j = 0; j < numvar; ++j)
						System.out.println("x[" + j + "]:" + xx[j]);
					break;
				case dual_infeas_cer:
				case prim_infeas_cer:
				case near_dual_infeas_cer:
				case near_prim_infeas_cer:
					System.out.println("Primal or dual infeasibility.\n");
					break;
				case unknown:
					System.out.println("Unknown solution status.\n");
					break;
				default:
					System.out.println("Other solution status");
					break;
				}
			}
		} catch (mosek.ArrayLengthException e) {
			System.out.println("Error: An array was too short");
			System.out.println(e.toString());
		} catch (mosek.Exception e)
		/* Catch both mosek.Error and mosek.Warning */
		{
			System.out.println("An error or warning was encountered");
			System.out.println(e.getMessage());
		}

		if (task != null)
			task.dispose();
		if (env != null)
			env.dispose();
		return xx;
	}

	public void printILP(DW dw) {
		dw.write("obj", objective, objsense);
		for (int i = 0; i < constraints.size(); i++) {
			Counter<String> c = constraints.get(i);
			dw.write("c", constraints_bound_type_list.get(i), constraints_lowerbound_list.get(i),
					constraints_upperbound_list.get(i), c);
		}
	}

	public static void main(String[] args) {
		IntegerLinearProgramming ilp = new IntegerLinearProgramming();
		Counter<String> obj = new ClassicCounter<String>();
		Counter<String> c1 = new ClassicCounter<String>();
		Counter<String> c2 = new ClassicCounter<String>();
		Counter<String> c3 = new ClassicCounter<String>();
		obj.incrementCount("x0", 3);
		obj.incrementCount("x1", 1);
		obj.incrementCount("x2", 5);
		obj.incrementCount("x3", 1);
		c1.incrementCount("x0", 3);
		c1.incrementCount("x1", 1);
		c1.incrementCount("x2", 2);
		c2.incrementCount("x0", 2);
		c2.incrementCount("x1", 1);
		c2.incrementCount("x2", 3);
		c2.incrementCount("x3", 1);
		c3.incrementCount("x1", 2);
		c3.incrementCount("x3", 3);
		ilp.setObjective(obj, true);
		ilp.addConstraint(c1, true, 30, true, 30);
		ilp.addConstraint(c2, true, 15, false, 0);
		ilp.addConstraint(c3, false, 0, true, 25);
		ilp.setupVariableLowerUpper("x0", 0, 10000);
		ilp.setupVariableLowerUpper("x1", 0, 10);
		ilp.setupVariableLowerUpper("x2", 0, 10000);
		ilp.setupVariableLowerUpper("x3", 0, 10000);
		D.p(ilp.run());
	}
}
