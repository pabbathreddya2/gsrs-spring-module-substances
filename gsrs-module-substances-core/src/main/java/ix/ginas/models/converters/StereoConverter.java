package ix.ginas.models.converters;

//import ix.core.Converter;
import ix.core.models.Structure.Stereo;
import ix.ginas.converters.EntityJsonVarcharConverter;

//@Converter
public class StereoConverter extends EntityJsonVarcharConverter<Stereo> {
	public StereoConverter() {
		super(Stereo.class);
	}
}