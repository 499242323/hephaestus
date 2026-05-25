package com.example.springaidemo.org.service;

import com.example.springaidemo.org.exception.OrgValidationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class OrgPersistenceGuard {

    public void rethrowUnitCodeConflict(RuntimeException exception) {
        if (isDuplicateConstraint(exception)) {
            throw new OrgValidationException("单位编码已存在");
        }
        throw exception;
    }

    public void rethrowPersonCodeConflict(RuntimeException exception) {
        if (isDuplicateConstraint(exception)) {
            throw new OrgValidationException("人员编码已存在");
        }
        throw exception;
    }

    private boolean isDuplicateConstraint(RuntimeException exception) {
        return exception instanceof DuplicateKeyException
                || exception instanceof DataIntegrityViolationException;
    }
}
