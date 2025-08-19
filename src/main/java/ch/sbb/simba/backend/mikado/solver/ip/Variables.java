package ch.sbb.simba.backend.mikado.solver.ip;

import static ch.sbb.simba.backend.mikado.solver.RotaziunSolver.numOfSections;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.hasChainAnEmptyTrip;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.areCouplingVariablesNeeded;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.isSidingVariableNeeded;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public class Variables {

    // section chaining
    public Map<Integer,MPVariable> xMap;

    // oneBlock
    public MPVariable[] u;

    // coupling-decoupling
    public MPVariable[] decoup;
    public MPVariable[] h1decoup;
    public MPVariable[] h2decoup;
    public MPVariable[] coup;
    public MPVariable[] h1coup;
    public MPVariable[] h2coup;

    // sidings
    public Map<Integer,Pair<MPVariable,MPVariable>> sMap;

    public Variables(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, List<RotaziunSection> sections, IpSolverParams params) {

        // section chaining: x_i,j
        makeChainingVariables(solver, possibleChains);

        // oneBlock: u_i
        if (params.getStageParams().isOnlyOneBlock()) {
            makeNummerationVariables(solver);
        }

        // coupling-decoupling
        if (params.getStageParams().isWithCouplingDecoupling()){
            makeCouplingDecouplingVariables(solver, params);
        }

        // sidings
        if(params.getStageParams().isWithSidings()){
            makeSidingVariables(solver, params, possibleChains, sections);
        }

    }

    // x_i,j
    private void makeChainingVariables(MPSolver solver, List<Pair<Integer, Integer>> possibleChains) {
        this.xMap = new HashMap<>();
        for(Pair<Integer,Integer> chain : possibleChains){
            this.xMap.put(getKey(new Pair<>(chain.getFirst(), chain.getSecond())), solver.makeIntVar(0, 1, "x" + chain.getFirst() + "_" + chain.getSecond()));
        }
    }

    // u_i
    private void makeNummerationVariables(MPSolver solver) {
        this.u = new MPVariable[numOfSections];
        for (int i = 0; i < numOfSections; i++) {
            this.u[i] = solver.makeIntVar(1, numOfSections, "u" + i);
        }
    }

    private void makeCouplingDecouplingVariables(MPSolver solver, IpSolverParams params) {
        // Decoupling
        // decoup_i, h1decop_i, h2decop_i
        int numOfEnforcements = params.getEnforcementStammPairMap().size();
        this.decoup = new MPVariable[numOfEnforcements];
        this.h1decoup = new MPVariable[numOfEnforcements];
        this.h2decoup = new MPVariable[numOfEnforcements];
        for (int i = 0; i < numOfEnforcements; i++) {
            this.decoup[i] = solver.makeIntVar(0, 1, "decoup" + i);
            this.h1decoup[i] = solver.makeIntVar(0, 1, "h1decoup" + i);
            this.h2decoup[i] = solver.makeIntVar(0, 1, "h2decoup" + i);
        }

        // Coupling
        // coup_i, h1coup_i, h2coup_i
        if(areCouplingVariablesNeeded(params)){
            this.coup = new MPVariable[numOfEnforcements];
            this.h1coup = new MPVariable[numOfEnforcements];
            this.h2coup = new MPVariable[numOfEnforcements];
            for (int i = 0; i < numOfEnforcements; i++) {
                this.coup[i] = solver.makeIntVar(0, 1, "coup" + i);
                this.h1coup[i] = solver.makeIntVar(0, 1, "h1coup" + i);
                this.h2coup[i] = solver.makeIntVar(0, 1, "h2coup" + i);
            }
        }
    }

    // s_first: siding before chain
    // s_second: siding after chain
    private void makeSidingVariables(MPSolver solver, IpSolverParams params, List<Pair<Integer, Integer>> possibleChains, List<RotaziunSection> sections) {
        this.sMap = new HashMap<>();
        for(Pair<Integer,Integer> chain : possibleChains){
            /*
             * Only initialize siding variable if:
             * - a chain contains a siding
             * - a chain has a siding in a capacity-restricted station
             */
            if(isSidingVariableNeeded(params, sections, chain)) {
                int i = chain.getFirst();
                int j = chain.getSecond();
                if(hasChainAnEmptyTrip(sections, chain)){
                    this.sMap.put(getKey(new Pair<>(i, j)), new Pair<>(solver.makeIntVar(0, 1, "sb_"+i+"_"+j), solver.makeIntVar(0, 1, "sa_"+i+"_"+j)));
                } else {
                    // no empty trip: second siding variable is redundant, can be fixed to 0
                    this.sMap.put(getKey(new Pair<>(i, j)), new Pair<>(solver.makeIntVar(0, 1, "sb_"+i+"_"+j), solver.makeIntVar(0, 0, "sa_"+i+"_"+j)));
                }
            }
        }
    }

}