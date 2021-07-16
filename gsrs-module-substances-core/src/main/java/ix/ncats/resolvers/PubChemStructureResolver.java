package ix.ncats.resolvers;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.stereotype.Component;

import ix.utils.Util;

@Component
public class PubChemStructureResolver extends AbstractStructureResolver {
    public static final String PUBCHEM_RESOLVER = 
        "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name";
    
    public PubChemStructureResolver () {
        super ("PubChem");
    }

    @Override
    protected UrlAndFormat[] resolvers (String name) throws MalformedURLException {
        return  new UrlAndFormat[] {
            new UrlAndFormat(new URL (PUBCHEM_RESOLVER+ "/"+Util.URLEncode(name)+"/sdf"), "sdf" )
        };
    }
}
