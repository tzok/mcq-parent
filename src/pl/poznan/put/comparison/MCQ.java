package pl.poznan.put.comparison;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.poznan.put.helper.TorsionAnglesHelper;
import pl.poznan.put.matching.FragmentComparison;
import pl.poznan.put.matching.FragmentMatch;
import pl.poznan.put.matching.MCQMatcher;
import pl.poznan.put.matching.ResidueComparison;
import pl.poznan.put.matching.SelectionMatch;
import pl.poznan.put.nucleic.RNAChiTorsionAngle;
import pl.poznan.put.nucleic.RNATorsionAngle;
import pl.poznan.put.protein.ProteinChiTorsionAngle;
import pl.poznan.put.protein.ProteinTorsionAngle;
import pl.poznan.put.structure.CompactFragment;
import pl.poznan.put.structure.StructureSelection;
import pl.poznan.put.torsion.TorsionAngle;
import pl.poznan.put.utility.TorsionAngleDelta;
import pl.poznan.put.utility.TorsionAngleDelta.State;

/**
 * Implementation of MCQ global similarity measure based on torsion angle
 * representation.
 * 
 * @author Tomasz Zok (tzok[at]cs.put.poznan.pl)
 */
public class MCQ implements GlobalComparator, LocalComparator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCQ.class);

    public static List<TorsionAngle> getAllAvailableTorsionAngles() {
        List<TorsionAngle> angles = new ArrayList<>();
        angles.addAll(Arrays.asList(RNATorsionAngle.values()));
        angles.addAll(Arrays.asList(RNAChiTorsionAngle.values()));
        angles.addAll(Arrays.asList(ProteinTorsionAngle.values()));
        angles.addAll(Arrays.asList(ProteinChiTorsionAngle.values()));
        return angles;
    }

    private List<TorsionAngle> angles;

    public MCQ(List<TorsionAngle> angles) {
        this.angles = angles;
    }

    public List<TorsionAngle> getAngles() {
        return angles;
    }

    public void setAngles(List<TorsionAngle> angles) {
        this.angles = angles;
    }

    @Override
    public String getName() {
        return "MCQ";
    }

    @Override
    public GlobalComparisonResult compareGlobally(StructureSelection s1,
            StructureSelection s2) throws IncomparableStructuresException {
        MCQMatcher matcher = new MCQMatcher(true, angles);
        SelectionMatch matches = matcher.matchSelections(s1, s2);

        if (matches == null || matches.getSize() == 0) {
            throw new IncomparableStructuresException("No matching fragments "
                    + "found");
        }

        List<Double> deltas = new ArrayList<>();

        for (int i = 0; i < matches.getSize(); i++) {
            FragmentMatch fragment = matches.getFragmentMatch(i);
            MCQ.LOGGER.debug("Taking into account fragments: " + fragment);
            FragmentComparison fragmentComparison = fragment.getBestResult();

            for (ResidueComparison residueComparison : fragmentComparison) {
                for (TorsionAngle torsionAngle : angles) {
                    TorsionAngleDelta angleDelta = residueComparison.getAngleDelta(torsionAngle);

                    if (angleDelta != null
                            && angleDelta.getState() == State.BOTH_VALID) {
                        deltas.add(angleDelta.getDelta());
                    }
                }
            }
        }

        double mcq = TorsionAnglesHelper.calculateMean(deltas);
        return new GlobalComparisonResult(getName(), s1.getName(),
                s2.getName(), matches, mcq, true);
    }

    @Override
    public LocalComparisonResult compareLocally(StructureSelection s1,
            StructureSelection s2) throws IncomparableStructuresException {
        MCQMatcher matcher = new MCQMatcher(true, angles);
        SelectionMatch matches = matcher.matchSelections(s1, s2);
        return new MCQLocalComparisonResult(s1.getName(), s2.getName(),
                matches, angles);
    }

    @Override
    public ModelsComparisonResult compareModels(CompactFragment reference,
            List<CompactFragment> models)
            throws IncomparableStructuresException {
        /*
         * Sanity check
         */
        for (CompactFragment fragment : models) {
            if (fragment.getMoleculeType() != reference.getMoleculeType()
                    || fragment.getSize() != reference.getSize()) {
                throw new IncomparableStructuresException("All models must "
                        + "be of the same type and size as the reference "
                        + "structure");
            }
        }

        MCQMatcher matcher = new MCQMatcher(true, angles);
        List<FragmentMatch> matches = new ArrayList<>();

        for (CompactFragment fragment : models) {
            matches.add(matcher.matchFragments(reference, fragment));
        }

        return new ModelsComparisonResult(angles.get(angles.size() - 1),
                reference, models, matches);
    }
}
