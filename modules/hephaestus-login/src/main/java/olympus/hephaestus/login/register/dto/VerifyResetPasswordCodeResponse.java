package olympus.hephaestus.login.register.dto;

import java.util.List;

public class VerifyResetPasswordCodeResponse extends EmailRegisterResponse {

    private List<ResetPasswordAccountItem> accounts;

    public VerifyResetPasswordCodeResponse() {
    }

    public VerifyResetPasswordCodeResponse(boolean success, String message, List<ResetPasswordAccountItem> accounts) {
        super(success, message);
        this.accounts = accounts;
    }

    public static VerifyResetPasswordCodeResponse success(String message, List<ResetPasswordAccountItem> accounts) {
        return new VerifyResetPasswordCodeResponse(true, message, accounts);
    }

    public List<ResetPasswordAccountItem> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<ResetPasswordAccountItem> accounts) {
        this.accounts = accounts;
    }
}
