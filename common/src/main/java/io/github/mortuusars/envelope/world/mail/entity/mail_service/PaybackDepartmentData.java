package io.github.mortuusars.envelope.world.mail.entity.mail_service;

import io.github.mortuusars.envelope.world.mail.MailId;

import java.util.Map;

public interface PaybackDepartmentData {
    Map<MailId, MailAwaitingPayback> getMailAwaitingPayback();
    void setDirty();
}
