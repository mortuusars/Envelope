package io.github.mortuusars.envelope.world.service;

/**
 * Injected in ServerLevel.<br>
 * Injected interfaces must have all methods as 'default'.<br>
 * Should be added to 'architectury.common.json':
 * <pre>
 * "injected_interfaces": {
 *   "net/minecraft/class_1337": [
 *       "full/package/path/ClassName"
 *   ]
 * }
 * </pre>
 */
public interface MailServiceHolder {
    default MailService getEnvelopeMailService() {
        throw new IllegalStateException("This method must be implemented");
    }
}