package ch.sbb.simba.backend.mikado.solver.blocking;

import static ch.sbb.simba.backend.mikado.solver.ip.chaining.analysis.ChainAnalysis.hasChainEmptyTrip;
import static ch.sbb.simba.backend.mikado.solver.ip.chaining.model.ChainingTypeUtil.isMaintenanceSection;

import ch.sbb.simba.backend.mikado.solver.blocking.converters.CommercialSectionToBlockItem;
import ch.sbb.simba.backend.mikado.solver.blocking.converters.MaintenanceSectionToBlockItem;
import ch.sbb.simba.backend.mikado.solver.blocking.converters.SectionsToEmptyTripBlockItem;
import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlock;
import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlockDay;
import ch.sbb.simba.backend.mikado.solver.blocking.models.RotaziunBlockItem;
import ch.sbb.simba.backend.mikado.solver.models.RotaziunSection;
import ch.sbb.simba.backend.mikado.solver.ip.parameters.IpSolverParams;
import ch.sbb.simba.backend.mikado.solver.parameters.RotaziunResultParams;
import ch.sbb.simba.backend.mikado.solver.utils.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class RotaziunBlockConstructor {

    private RotaziunBlockConstructor(){
    }

    private static Map<RotaziunSection,RotaziunSection> chainMap;

    public static List<RotaziunBlock> makeBlocks(IpSolverParams params, RotaziunResultParams resultParams) {

        if(!resultParams.isSolved()){
            return new ArrayList<>();
        }

        chainMap = resultParams.getSectionChainMap();
        List<RotaziunBlock> blocks = new ArrayList<>();
        Pair<RotaziunSection, RotaziunSection> startChain = getAnyNotAssignedChain();
        while (startChain != null) {
            blocks.add(makeBlock(reorderBlockItemsByEarliestStart(getItemsOfBlock(startChain, params, resultParams))));
            startChain = getAnyNotAssignedChain();
        }
        return blocks;

    }

    private static List<RotaziunBlockItem> getItemsOfBlock(Pair<RotaziunSection, RotaziunSection> chain, IpSolverParams params, RotaziunResultParams resultParams) {

        List<RotaziunBlockItem> blockItems = new ArrayList<>();
        while(chain != null){
            addChainToBlockItems(blockItems, chain, params, resultParams);
            chain = getChain(chain.getSecond());
        }
        return blockItems;
    }

    private static void addChainToBlockItems(List<RotaziunBlockItem> blockItems, Pair<RotaziunSection, RotaziunSection> chain, IpSolverParams params, RotaziunResultParams resultParams) {
        if(hasChainEmptyTrip(chain)){
            blockItems.add(SectionsToEmptyTripBlockItem.convert(chain.getFirst(), chain.getSecond(), params, resultParams));
        }
        if(isMaintenanceSection(chain.getSecond())){
            blockItems.add(MaintenanceSectionToBlockItem.convert(chain.getSecond()));
        } else {
            blockItems.add(CommercialSectionToBlockItem.convert(chain.getSecond()));
        }
        chainMap.remove(chain.getFirst());
    }

    private static RotaziunBlock makeBlock(List<RotaziunBlockItem> items) {
        List<RotaziunBlockDay> mikadoDays = new ArrayList<>();
        int blockDayIndex = 1;

        for(int idx = 0; idx < items.size(); idx++) {
            if (idx == 0 || chainStartsNewDay(items.get(idx-1), items.get(idx))){
                mikadoDays.add(RotaziunBlockDay.builder().items(new ArrayList<>()).blockingDay(blockDayIndex).build());
                blockDayIndex++;
            }
            mikadoDays.get(mikadoDays.size()-1).getItems().add(items.get(idx));
        }

        return RotaziunBlock.builder().days(mikadoDays).build();
    }

    // returns a reordered block, where the first item in the block has the earliest starting time
    private static List<RotaziunBlockItem> reorderBlockItemsByEarliestStart(List<RotaziunBlockItem> itemsInBlock) {
        int startItemId = IntStream.range(0, itemsInBlock.size()).reduce(0, (minIdx, idx) -> itemsInBlock.get(idx).getStart() < itemsInBlock.get(minIdx).getStart() ? idx : minIdx);
        Collections.rotate(itemsInBlock, -startItemId);
        return itemsInBlock;
    }

    private static boolean chainStartsNewDay(RotaziunBlockItem itemFrom, RotaziunBlockItem itemTo) {
        return itemFrom.getEnd() < itemFrom.getStart() || itemFrom.getEnd() > itemTo.getStart();
    }

    private static Pair<RotaziunSection, RotaziunSection> getChain(RotaziunSection fromSection) {
        return chainMap.containsKey(fromSection) ? new Pair<>(fromSection, chainMap.get(fromSection)) : null;
    }

    private static Pair<RotaziunSection, RotaziunSection> getAnyNotAssignedChain() {
        return chainMap.entrySet().stream().findAny().map(entry -> new Pair<>(entry.getKey(), entry.getValue())).orElse(null);
    }

}