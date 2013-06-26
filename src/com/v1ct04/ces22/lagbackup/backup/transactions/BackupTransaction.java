package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;

public interface BackupTransaction<Return, ProgressType> {

    public Return commit(ProgressPublisher<ProgressType> progressPublisher) throws Exception;

    public void revert(ProgressPublisher<ProgressType> progressPublisher) throws Exception;
}
