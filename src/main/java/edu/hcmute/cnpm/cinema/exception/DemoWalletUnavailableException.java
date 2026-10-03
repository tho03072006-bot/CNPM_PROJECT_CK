package edu.hcmute.cnpm.cinema.exception;

/** Recoverable online-wallet readiness error; the existing hold is not changed. */
public class DemoWalletUnavailableException extends BusinessException {
    private static final long serialVersionUID = 1L;
    public DemoWalletUnavailableException(String message) { super(message); }
}
