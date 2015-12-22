package android.pem;

import java.lang.RuntimeException;

/**
 * Indicates a breach in Privilege Escalation
 */
public class PrivilegeEscalationException extends RuntimeException {
    public PrivilegeEscalationException() {
	super();
    }
    public PrivilegeEscalationException(String name) {
	super(name);
    }
}
