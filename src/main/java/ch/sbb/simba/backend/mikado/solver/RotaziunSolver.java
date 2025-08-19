package ch.sbb.simba.backend.mikado.solver;

import static ch.sbb.simba.backend.mikado.solver.parameters.InputValidation.MAX_PRECISION_LEVEL;
import static ch.sbb.simba.backend.mikado.solver.utils.HelperMethods.prepenadMaintenanceSections;

import ch.sbb.simba.backend.mikado.solver.blocking.RotaziunBlockConstructor;
import ch.sbb.simba.backend.mikado.solver.ip.IpSolver;
import ch.sbb.simba.backend.mikado.solver.parameters.InputDefaultValues;
import ch.sbb.simba.backend.mikado.solver.parameters.InputValidation;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunInputParams;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunResultParams;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.utils.RotaziunDurationMap;
import ch.sbb.simba.backend.mikado.solver.utils.ModuleAcitvation;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class RotaziunSolver {

    public static int numOfSections;

    private RotaziunSolver() {
    }

    static RotaziunResultParams solve(List<RotaziunSection> commercialSections, List<RotaziunSection> maintenanceSections, RotaziunInputParams inputParams) {

        List<RotaziunSection> allSections = prepenadMaintenanceSections(commercialSections, maintenanceSections);
        RotaziunDurationMap durationMap = new RotaziunDurationMap(allSections);

        InputDefaultValues.setDefaults(inputParams);
        InputValidation.validateInputParameters(allSections, inputParams, durationMap);

        IpSolverParams params = new IpSolverParams(inputParams, allSections, maintenanceSections, durationMap);
        numOfSections = allSections.size();

        logProblemScope(params);
        RotaziunResultParams resultParams = solveWithMultiStageApproach(params, allSections);

        resultParams.setBlocks(RotaziunBlockConstructor.makeBlocks(params,resultParams));
        return resultParams;
    }

    private static RotaziunResultParams solveWithMultiStageApproach(IpSolverParams params, List<RotaziunSection> sections) {

        RotaziunResultParams rotaziunResultParams = RotaziunResultParams.builder().solved(false).build();

        while(!rotaziunResultParams.isSolved() && params.getPrecisionLevel() <= MAX_PRECISION_LEVEL){

            // solver stage 1:
            log.info("IpStage: 1");
            rotaziunResultParams = IpSolver.solve(sections, params.setStageOneParams());

            if(params.isTwoStages()) {
                // solver stage 2
                log.info("IpStage: 2");
                Map<RotaziunSection, RotaziunSection> sectionChainMap = rotaziunResultParams.getSectionChainMap();
                rotaziunResultParams = IpSolver.solve(sections, params.setStageTwoParams(sectionChainMap));
            }

            params.setPrecisionLevel(params.getPrecisionLevel() + 1);
            params.setTwoStages(ModuleAcitvation.solveWithTwoStages(params, sections));

        }

        if(params.isWithMaintenance()){
            // solver stage maintenance
            long preMaintenance = System.nanoTime();
            log.info("IP solver Stage Maintenance has started");
            log.info("solved: " + rotaziunResultParams.isSolved());
            params.setPrecisionLevel(params.getPrecisionLevel() - 1);
            rotaziunResultParams = IpSolver.solve(sections, params.setMaintenanceStageParams(rotaziunResultParams.getSectionChainMap()));
            log.info("MaintenanceTime: " + (System.nanoTime() - preMaintenance) / 1_000_000_000.0);
        }

        return rotaziunResultParams;

    }

    private static void logProblemScope(IpSolverParams params) {
        log.info("2-Stage: " + params.isTwoStages());
        log.info("oneBlock: " + params.isOnlyOneBlock());
        log.info("CouplingDecoupling: " + params.isWithCouplingDecoupling());
        log.info("DecoupVariables: " + (params.getMinTimeForDecoupling() > 0));
        log.info("Siding: " + params.isWithSidings());
        log.info("Maintenance: " + params.isWithMaintenance());
    }

}

