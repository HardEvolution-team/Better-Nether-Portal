package ded.betternetherportal.portal;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;








public final class PortalIndex {

    private PortalIndex() {}

    private static final Map<Integer, Map<Long, Long>> INTERIOR = new HashMap<>();
    private static final Map<Integer, Map<Long, Long>> FRAME = new HashMap<>();

    private static Map<Long, Long> map(Map<Integer, Map<Long, Long>> root, int dim) {
        return root.computeIfAbsent(dim, d -> new HashMap<>());
    }

    public static void register(int dim, BlockPos core, Collection<BlockPos> interior, Collection<BlockPos> frame) {
        long c = core.toLong();
        Map<Long, Long> in = map(INTERIOR, dim);
        for (BlockPos p : interior) {
            in.put(p.toLong(), c);
        }
        Map<Long, Long> fr = map(FRAME, dim);
        for (BlockPos p : frame) {
            fr.put(p.toLong(), c);
        }
    }

    public static void unregister(int dim, BlockPos core, Collection<BlockPos> interior, Collection<BlockPos> frame) {
        long c = core.toLong();
        removeOwned(map(INTERIOR, dim), interior, c);
        removeOwned(map(FRAME, dim), frame, c);
    }

    private static void removeOwned(Map<Long, Long> m, Collection<BlockPos> positions, long core) {
        for (BlockPos p : positions) {
            Long owner = m.get(p.toLong());
            if (owner != null && owner == core) {
                m.remove(p.toLong());
            }
        }
    }

    @Nullable
    public static BlockPos find(int dim, BlockPos pos, boolean interior) {
        Map<Long, Long> m = (interior ? INTERIOR : FRAME).get(dim);
        if (m == null) {
            return null;
        }
        Long core = m.get(pos.toLong());
        return core == null ? null : BlockPos.fromLong(core);
    }

    
    public static void clear() {
        INTERIOR.clear();
        FRAME.clear();
    }
}
