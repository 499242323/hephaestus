package com.example.springaidemo.org.log.support;

import com.example.springaidemo.org.entity.OrgPersonEntity;
import com.example.springaidemo.org.entity.OrgUnitEntity;
import com.example.springaidemo.org.log.dto.OperationLogRecordRequest;
import com.example.springaidemo.org.repository.OrgPersonRepository;
import com.example.springaidemo.org.repository.OrgUnitRepository;
import org.springframework.stereotype.Component;

@Component
public class OperationLogRecordFactory {

    private final OrgPersonRepository personRepository;
    private final OrgUnitRepository unitRepository;

    public OperationLogRecordFactory(OrgPersonRepository personRepository,
                                     OrgUnitRepository unitRepository) {
        this.personRepository = personRepository;
        this.unitRepository = unitRepository;
    }

    public OperationLogRecordRequest create(Long operatorPersonId,
                                            String moduleCode,
                                            String moduleName,
                                            String actionCode,
                                            String actionName,
                                            String targetType,
                                            Object targetId,
                                            String targetName,
                                            String summary,
                                            String detail) {
        OrgPersonEntity operator = operatorPersonId == null ? null : personRepository.getById(operatorPersonId);
        OrgUnitEntity unit = operator == null || operator.getUnitId() == null ? null : unitRepository.getById(operator.getUnitId());
        return new OperationLogRecordRequest(
                operatorPersonId,
                operator == null ? null : operator.getPersonName(),
                operator == null ? null : operator.getUsername(),
                unit == null ? null : unit.getId(),
                unit == null ? null : unit.getUnitName(),
                moduleCode,
                moduleName,
                actionCode,
                actionName,
                targetType,
                targetId == null ? null : String.valueOf(targetId),
                targetName,
                summary,
                detail
        );
    }
}
