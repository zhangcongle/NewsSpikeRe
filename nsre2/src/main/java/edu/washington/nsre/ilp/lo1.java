package edu.washington.nsre.ilp;

/*
Copyright: Copyright (c) MOSEK ApS, Denmark. All rights reserved.

File:      lo1.java

Purpose:   Demonstrates how to solve a small linear
optimization problem using the MOSEK Java API.
*/

public class lo1 {
	static final int numcon = 3;
	static final int numvar = 4;

	public static void main(String[] args) {
		// Since the value of infinity is ignored, we define it solely
		// for symbolic purposes
		double infinity = 0;
		double c[] = { 3.0, 1.0, 5.0, 1.0 };
		int asub[][] = { { 0, 1 }, { 0, 1, 2 }, { 0, 1 }, { 1, 2 } };
		double aval[][] = { { 3.0, 2.0 }, { 1.0, 1.0, 2.0 }, { 2.0, 3.0 },
				{ 1.0, 3.0 } };
		mosek.Env.boundkey[] bkc = { mosek.Env.boundkey.fx,
				mosek.Env.boundkey.lo, mosek.Env.boundkey.up };
		double blc[] = { 30.0, 15.0, -infinity };
		double buc[] = { 30.0, +infinity, 25.0 };
		mosek.Env.boundkey bkx[] = { mosek.Env.boundkey.lo,
				mosek.Env.boundkey.ra, mosek.Env.boundkey.lo,
				mosek.Env.boundkey.lo };
		double blx[] = { 0.0, 0.0, 0.0, 0.0 };
		double bux[] = { +infinity, 10.0, +infinity, +infinity };
		double[] xx = new double[numvar];

		mosek.Env env = null;
		mosek.Task task = null;

		try {
			// Make mosek environment.
			env = new mosek.Env();

			// Create a task object.
			task = new mosek.Task(env, 0, 0);

			// Directs the log task stream to the user specified
			// method task_msg_obj.stream
			task.set_Stream(mosek.Env.streamtype.log, new mosek.Stream() {
				public void stream(String msg) {
					System.out.print(msg);
				}
			});

			// Append 'numcon' empty constraints.
			// The constraints will initially have no bounds.
			task.appendcons(numcon);

			// Append 'numvar' variables.
			// The variables will initially be fixed at zero (x=0).
			task.appendvars(numvar);

			for (int j = 0; j < numvar; ++j) {
				// Set the linear term c_j in the objective.
				task.putcj(j, c[j]);

				// Set the bounds on variable j.
				// blx[j] <= x_j <= bux[j]
				task.putvarbound(j, bkx[j], blx[j], bux[j]);

				// Input column j of A
				task.putacol(j, /* Variable (column) index. */
						asub[j], /* Row index of non-zeros in column j. */
						aval[j]); /* Non-zero Values of column j. */
			}

			// Set the bounds on constraints.
			// blc[i] <= constraint_i <= buc[i]
			for (int i = 0; i < numcon; ++i)
				task.putconbound(i, bkc[i], blc[i], buc[i]);

			// Input the objective sense (minimize/maximize)
			task.putobjsense(mosek.Env.objsense.maximize);

			// Solve the problem
			task.optimize();

			// Print a summary containing information
			// about the solution for debugging purposes
			task.solutionsummary(mosek.Env.streamtype.msg);

			// Get status information about the solution
			mosek.Env.solsta solsta[] = new mosek.Env.solsta[1];
			task.getsolsta(mosek.Env.soltype.bas, solsta);

			switch (solsta[0]) {
			case optimal:
			case near_optimal:
				task.getxx(mosek.Env.soltype.bas, // Request the basic solution.
						xx);

				System.out.println("Optimal primal solution\n");
				for (int j = 0; j < numvar; ++j)
					System.out.println("x[" + j + "]:" + xx[j]);
				break;
			case dual_infeas_cer:
			case prim_infeas_cer:
			case near_dual_infeas_cer:
			case near_prim_infeas_cer:
				System.out
						.println("Primal or dual infeasibility certificate found.\n");
				break;
			case unknown:
				System.out.println("Unknown solution status.\n");
				break;
			default:
				System.out.println("Other solution status");
				break;
			}
		} catch (mosek.Exception e) {
			System.out.println("An error/warning was encountered");
			System.out.println(e.toString());
			throw e;
		} finally {
			if (task != null)
				task.dispose();
			if (env != null)
				env.dispose();
		}
	}
}
