package gsrs.module.substance.definitional;

import gsrs.module.substance.services.DefinitionalElementImplementation;
import ix.ginas.models.v1.*;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;
@Slf4j
public class SSG1DefinitionalElementImpl implements DefinitionalElementImplementation {
    @Override
    public boolean supports(Substance s) {
        return s instanceof SpecifiedSubstanceGroup1Substance;
    }

    @Override
    public void computeDefinitionalElements(Substance s, Consumer<DefinitionalElement> consumer) {
        SpecifiedSubstanceGroup1Substance ssg1 = (SpecifiedSubstanceGroup1Substance) s;

        if(ssg1.specifiedSubstance.constituents != null) {
            log.trace("total components " + ssg1.specifiedSubstance.constituents.size());
            for (int i =0; i <ssg1.specifiedSubstance.constituents.size(); i++)	{
                SpecifiedSubstanceComponent component = ssg1.specifiedSubstance.constituents.get(i);
                if( component.substance == null ){
                    log.trace("null substance found in component " + i);
                    continue;
                }
                log.trace("processing component " + i + " identified by " + component.substance.refuuid);
                DefinitionalElement componentRefUuid = DefinitionalElement.of("specifiedSubstance.constituents.substance.refuuid",
                        component.substance.refuuid, 1);
                consumer.accept(componentRefUuid);

                if( component.role != null){
                    DefinitionalElement componentType = DefinitionalElement.of("specifiedSubstance.constituents.role",
                            component.role, 2);
                    log.trace("	component.role: " + component.role);
                    consumer.accept(componentType);
                }
                if( component.amount != null) {
                    DefinitionalElement constituentAmountElement = DefinitionalElement.of("specifiedsubstancegroup1.constituents.monomerSubstance.amount",
                            component.amount.toString(), 2);
                    log.trace("looking at constituent amount " + component.amount.toString());
                    consumer.accept(constituentAmountElement);
                }
                log.trace("completed component processing");
            }
        } else {
            log.warn("this SSG1 has no constituents");
        }
        if( ssg1.modifications != null ){
            for(DefinitionalElement e: ssg1.modifications.getDefinitionalElements().getElements()){
                consumer.accept(e);
            }
        }

        if( ssg1.properties != null ) {
            for(Property property : ssg1.properties) {
                if(property.isDefining() && property.getValue() != null) {
                    String defElementName = String.format("properties.%s.value",
                            property.getName());
                    DefinitionalElement propertyValueDefElement =
                            DefinitionalElement.of(defElementName, property.getValue().toString(), 2);
                    consumer.accept(propertyValueDefElement);
                    log.debug("added def element for property " + defElementName);
                    for(Parameter parameter : property.getParameters()) {
                        defElementName = String.format("properties.%s.parameters.%s.value",
                                property.getName(), parameter.getName());
                        if( parameter.getValue() != null) {
                            DefinitionalElement propertyParamValueDefElement =
                                    DefinitionalElement.of(defElementName,
                                            parameter.getValue().toString(), 2);
                            consumer.accept(propertyParamValueDefElement);
                            log.debug("added def element for property parameter " + defElementName);
                        }
                    }
                }
            }
        }
    }
}
