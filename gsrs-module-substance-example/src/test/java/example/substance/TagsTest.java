package example.substance;

import ix.core.models.Keyword;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ix.ginas.utils.validation.validators.TagUtilities;
import static org.junit.Assert.assertEquals;


@SpringBootTest
@Slf4j
public class TagsTest {
    final static boolean logBeforeAfterClass = false;

    Substance createOldSubstance() {
        Substance oldSubstance = new Substance();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        Name oldName3 = new Name();
        Name oldName4 = new Name();
        Name oldName5 = new Name();
        Name oldName6 = new Name();
        oldName1.setName("A");
        oldName2.setName("B");
        oldName3.setName("C [USP]");
        oldName4.setName("D");
        oldName5.setName("E [VANDF]");
        oldSubstance.names.add(oldName1);
        oldSubstance.names.add(oldName2);
        oldSubstance.names.add(oldName3);
        oldSubstance.names.add(oldName4);
        oldSubstance.names.add(oldName5);
        oldSubstance.names.add(oldName6);
        oldSubstance.addTagString("USP");
        oldSubstance.addTagString("VANDF");
        return oldSubstance;
    }

    Substance createNewSubstance() {
        Substance newSubstance = new Substance();
        Name newName1 = new Name();
        Name newName2 = new Name();
        Name newName3 = new Name();
        Name newName4 = new Name();
        newName1.setName("D");
        newName2.setName("E [VANDF]");
        newName3.setName("F");
        newName4.setName("G");
        newSubstance.names.add(newName1);
        newSubstance.names.add(newName2);
        newSubstance.names.add(newName3);
        newSubstance.names.add(newName4);
        newSubstance.addTagString("INN");
        newSubstance.addTagString("VANDF");
        return newSubstance;
    }


    @Test
    void testCanDeleteTag() throws Exception {
        log.info("Testing testCanDeleteTag");
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.removeTagString("VANDF");
    }

    @Test
    void testMiscTags() throws Exception {
        log.info("Testing testMiscTags");

    }

    @Test
    void testExtractTagTermFromName() throws Exception {
        log.info("Testing testExtractTagTermFromName");
        assert (TagUtilities.getBracketTerm("ABC [USP]").equals(Optional.of("USP")));
        assert (TagUtilities.getBracketTerm("ABC [USP]    ").equals(Optional.of("USP")));
        assert (TagUtilities.getBracketTerm("ABC [USP    ]").equals(Optional.of("USP    ")));
        Assertions.assertEquals(TagUtilities.getBracketTerm("ABC"),Optional.empty());
        Assertions.assertEquals(TagUtilities.getBracketTerm("ABC USP]"),Optional.empty());
        Assertions.assertEquals(TagUtilities.getBracketTerm("[USP] ABC"),Optional.empty());
    }

    @Test
    void extractDistinctTagTermsFromNames() throws Exception {
        log.info("extractDistinctTagTermsFromNames");
        Substance s = new Substance();
        s.names = new ArrayList<>();
        s.tags.add(new Keyword("USP"));
        s.tags.add(new Keyword("INN"));
        s.tags.add(new Keyword("GREEN BOOK"));
        s.names.add(new Name("ABC [USP]"));
        s.names.add(new Name("CED [USP]"));
        s.names.add(new Name("PED [INN]"));
        s.names.add(new Name("QAK [INN]"));
        s.names.add(new Name("VAD [VANDF]"));
        Set<String> bracketNameTags = TagUtilities.extractBracketNameTags(s);
        assert(bracketNameTags.equals(new HashSet<>(Arrays.asList("USP","INN","VANDF"))));
        // Should be excluded from set
        s.names.add(new Name(null));
        assert(bracketNameTags.equals(new HashSet<>(Arrays.asList("USP","INN","VANDF"))));
    }


    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstance() throws Exception {

        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstance");
        Substance oldSubstance = this.createOldSubstance();
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
    }

    @Test
    void testCompareTagTermsToNamesTagTermsNewSubstance() throws Exception {

        log.info("Testing testCompareTagTermsToNamesTagTermsNewSubstance");
        Substance newSubstance = this.createNewSubstance();
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(newSubstance),
                        TagUtilities.extractExplicitTags(newSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(newSubstance),
                        TagUtilities.extractBracketNameTags(newSubstance)
                ),
                new HashSet<>(Arrays.asList("INN"))
        );
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesEmpty() throws Exception {
        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstanceIfNamesNull");
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.names = new ArrayList<Name>();
        oldSubstance.tags = new ArrayList<Keyword>();
        oldSubstance.addTagString("USP");
        oldSubstance.addTagString("VANDF");
        // names []
        // tags [a b]
        // in tags missing from names a b
        // in names missing from tags []
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList("USP", "VANDF"))
        );
    }

    @Test
    void testCompareTagTermsToNamesTagTermsOldSubstanceIfTagsEmpty() throws Exception {
        log.info("Testing testCompareTagTermsToNamesTagTermsOldSubstanceIfTagsEmpty");
        Substance oldSubstance = this.createOldSubstance();
        oldSubstance.tags = new ArrayList<Keyword>();
        oldSubstance.names = new ArrayList<Name>();
        Name oldName1 = new Name();
        Name oldName2 = new Name();
        oldName1.setName("C [USP]");
        oldName2.setName("D [VANDF]");
        oldSubstance.names.add(oldName1);
        oldSubstance.names.add(oldName2);
        // names [c d]
        // tags []
        // in tags missing from names []
        // in names missing from tags [c d]

        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractBracketNameTags(oldSubstance),
                        TagUtilities.extractExplicitTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList("USP", "VANDF"))
        );
        assertEquals(
                TagUtilities.getSetAExcludesB(
                        TagUtilities.extractExplicitTags(oldSubstance),
                        TagUtilities.extractBracketNameTags(oldSubstance)
                ),
                new HashSet<>(Arrays.asList())
        );

    }


}
