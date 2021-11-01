package gsrs.api.substances;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.api.AbstractLegacySearchGsrsEntityRestTemplate;
import gsrs.substances.dto.CodeDTO;
import gsrs.substances.dto.NameDTO;
import gsrs.substances.dto.ReferenceDTO;
import gsrs.substances.dto.SubstanceDTO;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SubstanceRestApi extends AbstractLegacySearchGsrsEntityRestTemplate<SubstanceDTO, UUID> implements SubstanceApi{
    public SubstanceRestApi(RestTemplateBuilder restTemplateBuilder, String baseUrl, ObjectMapper mapper) {
        super(restTemplateBuilder, baseUrl, "substances", mapper);
    }

    @Override
    protected SubstanceDTO parseFromJson(JsonNode node) {
        return getObjectMapper().convertValue(node, SubstanceDTO.class);
    }

    @Override
    protected UUID getIdFrom(SubstanceDTO dto) {
        return dto.getUuid();
    }

    @Override
    public <T extends SubstanceDTO> Optional<T> findByResolvedId(String anyKindOfId) throws IOException {
        return super.findByResolvedId(anyKindOfId);
    }

    @Override
    public Optional<SubstanceDTO> findById(Long id) throws IOException {
        return Optional.empty();
    }

    @Override
    public boolean existsById(Long id) throws IOException {
        return false;
    }

    @Override
    public Optional<List<NameDTO>> getNamesOfSubstance(String anyKindOfSubstanceId)  throws IOException{
        ResponseEntity<String> response = doGet("("+anyKindOfSubstanceId+")/names", String.class);
        if(!response.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }
        JsonNode node = getObjectMapper().readTree(response.getBody());
        return Optional.of(getObjectMapper().convertValue(node, new TypeReference<List<NameDTO>>() {}));
    }

    @Override
    public Optional<List<CodeDTO>> getCodesOfSubstance(String anyKindOfSubstanceId) throws IOException{
        ResponseEntity<String> response = doGet("("+anyKindOfSubstanceId+")/codes", String.class);
        if(!response.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }
        JsonNode node = getObjectMapper().readTree(response.getBody());
        return Optional.of(getObjectMapper().convertValue(node, new TypeReference<List<CodeDTO>>() {}));
    }

    @Override
    public Optional<List<ReferenceDTO>> getReferencesOfSubstance(String anyKindOfSubstanceId) throws IOException{
        ResponseEntity<String> response = doGet("("+anyKindOfSubstanceId+")/references", String.class);
        if(!response.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }
        JsonNode node = getObjectMapper().readTree(response.getBody());
        return Optional.of(getObjectMapper().convertValue(node, new TypeReference<List<ReferenceDTO>>() {}));
    }

    @Override
    public Optional<SubstanceDTO> resolveSubstance(String substanceKey, String substanceKeyType) throws IOException {

        //TODO do we need to toUpper() here or is EqualsIgnoreCase OK ?
        String upper = substanceKeyType.toUpperCase();
        if("APPROVAL_ID".equals(upper) || "UUID".equals(upper)){
            return findByResolvedId(upper);
        }
        //root_codes_<CODE_SYSTEM>:"^<CODE>$"
        SearchResult<? extends SubstanceDTO> result = this.search(SearchRequest.builder()
                .q("root_codes_"+substanceKeyType+":\"^"+substanceKey+"$\"&view=full")

                .simpleSearchOnly(true));
        //look for primary?
        SubstanceDTO substanceToReturn=null;
        for(SubstanceDTO substanceDTO : result.getContent()){
            boolean found = substanceDTO.getCodes().stream()
                            .filter(c-> c.getCodeSystem().equals(substanceKeyType) && c.getCode().equals(substanceKey) && "PRIMARY".equalsIgnoreCase(c.getType()))
                    .findAny()
                    .isPresent();
            if(found){
                if(substanceToReturn !=null){
                    //found more than one!!
                    throw new IOException("found more than 1 result for " + substanceKeyType + ": " + substanceKey + " ["+ substanceToReturn.getUuid() + " and " + substanceDTO.getUuid() + "]");
                }
                substanceToReturn = substanceDTO;
            }
        }
        return Optional.ofNullable(substanceToReturn);

    }
}
