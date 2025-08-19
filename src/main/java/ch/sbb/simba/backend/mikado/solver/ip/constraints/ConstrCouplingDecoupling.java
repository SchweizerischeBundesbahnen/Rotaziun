package ch.sbb.simba.backend.mikado.solver.ip.constraints;

import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.parseMapToList;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.DAY_IN_SECONDS;
import static ch.sbb.simba.backend.mikado.solver.ip.IpSolver.INFINITY;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.getKey;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.isCouplingDecouplingPossibleAtStation;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.getIdleTime;
import static ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation.areCouplingVariablesNeeded;

import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.ip.Variables;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import java.util.List;
import java.util.Objects;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;

public final class ConstrCouplingDecoupling {

    private ConstrCouplingDecoupling() {
    }

    public static void makeCouplingDecouplingConstraints(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections) {

        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs = parseMapToList(params.getEnforcementStammPairMap());

        detectDecoupling(solver, possibleChains, v, params, sections, enfStPairs, params.getSectionToDecouplingIdMap().size()+2);
        if(areCouplingVariablesNeeded(params)){
            detectCoupling(solver, possibleChains, v, params, sections, enfStPairs, params.getSectionToCouplingIdMap().size()+2);
        }

        enforcementsNeedTimeForCouplingDecoupling(solver, possibleChains, v, params, sections);

        if(!params.getProhibitedCouplingDecouplingStationIds().isEmpty()){
            prohibitDecouplingAtStations(v, params, enfStPairs);
            prohibitCouplingAtStations(v, params, enfStPairs);
        }

    }

    private static void enforcementsNeedTimeForCouplingDecoupling(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections) {

        List<RotaziunSection> enforcements = params.getEnforcementStammPairMap().keySet().stream().toList();

        MPConstraint[] constrEnfTime = new MPConstraint[possibleChains.size()];
        int idx = 0;
        for (Pair<Integer, Integer> p : possibleChains) {
            if(enforcements.contains(sections.get(p.getFirst())) || enforcements.contains(sections.get(p.getSecond()))) {
                constrEnfTime[idx] = solver.makeConstraint(-INFINITY, DAY_IN_SECONDS +  getIdleTime(sections.get(p.getFirst()), sections.get(p.getSecond()), params), "constrEnfTime_" + idx);
                constrEnfTime[idx].setCoefficient(v.xMap.get(getKey(p)), DAY_IN_SECONDS);
                if(enforcements.contains(sections.get(p.getFirst()))){
                    constrEnfTime[idx].setCoefficient(v.decoup[params.getEnforcementSectionToEnforcementIdMap().get(sections.get(p.getFirst()))], params.getMinTimeForDecoupling());
                }
                if(enforcements.contains(sections.get(p.getSecond())) && areCouplingVariablesNeeded(params)){
                    constrEnfTime[idx].setCoefficient(v.coup[params.getEnforcementSectionToEnforcementIdMap().get(sections.get(p.getSecond()))], params.getMinTimeForCoupling());
                }
                idx++;
            }
        }

    }

    private static void prohibitDecouplingAtStations(Variables v, IpSolverParams params, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs) {

        for (int e = 0; e < enfStPairs.size(); e++) {
            if(isDecouplingProhibitedAtStation(params, enfStPairs, e)){
                if(decouplingDuringJourney(enfStPairs.get(e))){
                    throw new IllegalArgumentException("Prohibited decoupling site: " + enfStPairs.get(e).getFirst().getToStation().getName()
                        + " interferes with enhancement from " + enfStPairs.get(e).getFirst().getFromStation().getName() + " to " + enfStPairs.get(e).getFirst().getToStation().getName()
                    );
                }
                v.h1decoup[e].setBounds(0,0);
                v.h2decoup[e].setBounds(0,0);
                v.decoup[e].setBounds(0,0);
            }
        }
    }

    private static void prohibitCouplingAtStations(Variables v, IpSolverParams params, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs) {

        for (int e = 0; e < enfStPairs.size(); e++) {
            if(isCouplingProhibitedAtStation(params, enfStPairs, e)){
                if(couplingDuringJourney(enfStPairs.get(e))){
                    throw new IllegalArgumentException("Prohibited coupling site: " + enfStPairs.get(e).getFirst().getFromStation().getName()
                        + " interferes with enhancement from " + enfStPairs.get(e).getFirst().getFromStation().getName() + " to " + enfStPairs.get(e).getFirst().getToStation().getName()
                    );
                }
                v.h1coup[e].setBounds(0,0);
                v.h2coup[e].setBounds(0,0);
                v.coup[e].setBounds(0,0);
            }
        }
    }

    private static void detectDecoupling(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {

        detectDecoupling1a(solver, possibleChains, v, params, sections, enfStPairs, m);
        detectDecoupling1b(solver, possibleChains, v, params, sections, enfStPairs, m);

        detectDecoupling2a(solver, possibleChains, v, params, sections, enfStPairs, m);
        detectDecoupling2b(solver, possibleChains, v, params, sections, enfStPairs, m);

        detectDecoupling3(solver, v, enfStPairs);
    }

    private static void detectCoupling(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {

        detectCoupling1a(solver, possibleChains, v, params, sections, enfStPairs, m);
        detectCoupling1b(solver, possibleChains, v, params, sections, enfStPairs, m);

        detectCoupling2a(solver, possibleChains, v, params, sections, enfStPairs, m);
        detectCoupling2b(solver, possibleChains, v, params, sections, enfStPairs, m);

        detectCoupling3(solver, v, enfStPairs);
    }

    private static void detectCoupling1a(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrCoupling1a = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrCoupling1a[i] = solver.makeConstraint(-INFINITY, m-1, "coupling1a_" + i);
            constrCoupling1a[i].setCoefficient(v.h1coup[i], m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrCoupling1a[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfPrevSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrCoupling1a[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfPrevSection(params, sections, p));
                }
            }
        }
    }

    private static void detectCoupling1b(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrCoupling1b = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrCoupling1b[i] = solver.makeConstraint(-INFINITY, 0, "coupling1b_" + i);
            constrCoupling1b[i].setCoefficient(v.h1coup[i], -m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrCoupling1b[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfPrevSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrCoupling1b[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfPrevSection(params, sections, p));
                }
            }
        }
    }

    private static void detectCoupling2a(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrCoupling2a = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrCoupling2a[i] = solver.makeConstraint(-INFINITY, m-1, "coupling2a_" + i);
            constrCoupling2a[i].setCoefficient(v.h2coup[i], m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrCoupling2a[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfPrevSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrCoupling2a[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfPrevSection(params, sections, p));
                }
            }
        }
    }

    private static void detectCoupling2b(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrCoupling2b = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrCoupling2b[i] = solver.makeConstraint(-INFINITY, 0, "coupling2b_" + i);
            constrCoupling2b[i].setCoefficient(v.h2coup[i], -m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrCoupling2b[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfPrevSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getSecond()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrCoupling2b[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfPrevSection(params, sections, p));
                }
            }
        }
    }

    private static void detectCoupling3(MPSolver solver, Variables v, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs) {
        MPConstraint[] constrCoupling3 = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrCoupling3[i] = solver.makeConstraint(0, 0, "coupling3_" + i);
            constrCoupling3[i].setCoefficient(v.h1coup[i], 1);
            constrCoupling3[i].setCoefficient(v.h2coup[i], 1);
            constrCoupling3[i].setCoefficient(v.coup[i], -1);
        }
    }

    private static void detectDecoupling1a(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrDecoupling1a = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrDecoupling1a[i] = solver.makeConstraint(-INFINITY, m-1, "decoupling1a_" + i);
            constrDecoupling1a[i].setCoefficient(v.h1decoup[i], m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrDecoupling1a[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfNextSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrDecoupling1a[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfNextSection(params, sections, p));
                }
            }
        }
    }

    private static void detectDecoupling1b(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrDecoupling1b = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrDecoupling1b[i] = solver.makeConstraint(-INFINITY, 0, "decoupling1b_" + i);
            constrDecoupling1b[i].setCoefficient(v.h1decoup[i], -m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrDecoupling1b[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfNextSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrDecoupling1b[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfNextSection(params, sections, p));
                }
            }
        }
    }

    private static void detectDecoupling2a(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrDecoupling2a = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrDecoupling2a[i] = solver.makeConstraint(-INFINITY, m -1, "decoupling2a_" + i);
            constrDecoupling2a[i].setCoefficient(v.h2decoup[i], m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrDecoupling2a[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfNextSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrDecoupling2a[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfNextSection(params, sections, p));
                }
            }
        }
    }

    private static void detectDecoupling2b(MPSolver solver, List<Pair<Integer, Integer>> possibleChains, Variables v, IpSolverParams params, List<RotaziunSection> sections,
        List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int m) {
        MPConstraint[] constrDecoupling2b = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrDecoupling2b[i] = solver.makeConstraint(-INFINITY, 0, "decoupling2b_" + i);
            constrDecoupling2b[i].setCoefficient(v.h2decoup[i], -m);
            for (Pair<Integer, Integer> p : possibleChains) {
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getSecond().getId())){
                    constrDecoupling2b[i].setCoefficient(v.xMap.get(getKey(p)), -getIdOfNextSection(params, sections, p));
                }
                if(Objects.equals(sections.get(p.getFirst()).getId(), enfStPairs.get(i).getFirst().getId())){
                    constrDecoupling2b[i].setCoefficient(v.xMap.get(getKey(p)), getIdOfNextSection(params, sections, p));
                }
            }
        }
    }

    private static void detectDecoupling3(MPSolver solver, Variables v, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs) {
        MPConstraint[] constrDecoupling3 = new MPConstraint[enfStPairs.size()];
        for (int i = 0; i < enfStPairs.size(); i++) {
            constrDecoupling3[i] = solver.makeConstraint(0, 0, "decoupling3_" + i);
            constrDecoupling3[i].setCoefficient(v.h1decoup[i], 1);
            constrDecoupling3[i].setCoefficient(v.h2decoup[i], 1);
            constrDecoupling3[i].setCoefficient(v.decoup[i], -1);
        }
    }

    private static Integer getIdOfNextSection(IpSolverParams params, List<RotaziunSection> sections, Pair<Integer, Integer> p) {
        return params.getSectionToDecouplingIdMap().get(sections.get(p.getSecond()).getId());
    }

    private static Integer getIdOfPrevSection(IpSolverParams params, List<RotaziunSection> sections, Pair<Integer, Integer> p) {
        return params.getSectionToCouplingIdMap().get(sections.get(p.getFirst()).getId());
    }

    private static boolean isCouplingProhibitedAtStation(IpSolverParams params, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int e) {
        return !isCouplingDecouplingPossibleAtStation(enfStPairs.get(e).getFirst().getFromStation(), params);
    }

    private static boolean isDecouplingProhibitedAtStation(IpSolverParams params, List<Pair<RotaziunSection, RotaziunSection>> enfStPairs, int e) {
        return !isCouplingDecouplingPossibleAtStation(enfStPairs.get(e).getFirst().getToStation(), params);
    }

    private static boolean decouplingDuringJourney(Pair<RotaziunSection, RotaziunSection> verStPair) {
        return !Objects.equals(verStPair.getFirst().getToStation().getId(), verStPair.getSecond().getToStation().getId());
    }

    private static boolean couplingDuringJourney(Pair<RotaziunSection, RotaziunSection> verStPair) {
        return !Objects.equals(verStPair.getFirst().getFromStation().getId(), verStPair.getSecond().getFromStation().getId());
    }
}
