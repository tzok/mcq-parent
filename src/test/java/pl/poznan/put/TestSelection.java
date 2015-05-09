package pl.poznan.put;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import pl.poznan.put.matching.StructureSelection;
import pl.poznan.put.pdb.PdbParsingException;
import pl.poznan.put.pdb.analysis.PdbChain;
import pl.poznan.put.pdb.analysis.PdbModel;
import pl.poznan.put.pdb.analysis.PdbParser;

public class TestSelection {
    private final PdbParser parser = new PdbParser();

    private String pdb1OB5;

    @Before
    public void loadPdbFile() throws URISyntaxException, IOException {
        URI uri = getClass().getClassLoader().getResource(".").toURI();
        File dir = new File(uri);
        pdb1OB5 = FileUtils.readFileToString(new File(dir, "../../src/test/resources/1OB5.pdb"), "utf-8");
    }

    @Test
    public void testResidueBonds_74_77A_76() throws PdbParsingException {
        List<PdbModel> models = parser.parse(pdb1OB5);
        assertEquals(1, models.size());
        PdbModel model = models.get(0);

        List<PdbChain> chains = model.getChains();
        assertEquals(7, chains.size());

        PdbChain chainB = null;
        for (PdbChain chain : chains) {
            if (chain.getIdentifier() == 'B') {
                chainB = chain;
                break;
            }
        }
        assertNotNull(chainB);

        StructureSelection selection = new StructureSelection("B", chainB.getResidues());
        assertEquals(2, selection.getCompactFragments().size());
    }
}
