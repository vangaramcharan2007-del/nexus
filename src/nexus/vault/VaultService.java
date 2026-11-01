package nexus.vault;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

/**
 * Vault & Ledger Cryptographic Service.
 * 
 * Author: Ram Charan Vanga (Reg No: RA2511027010164)
 * Department of Computer Science & Engineering
 * SRM Institute of Science and Technology, Kattankulathur
 */
public class VaultService implements NexusListener {
    private static final VaultService instance = new VaultService();

    private final ImmutableAuditChain auditChain = new ImmutableAuditChain();
    private final EncryptedBackupVault backupVault = new EncryptedBackupVault();

    private VaultService() {
        EventBus.getInstance().subscribe("TRANSACTION_ADDED", this);
        EventBus.getInstance().subscribe("TICKET_BOOKED", this);
    }

    public static VaultService getInstance() {
        return instance;
    }

    public ImmutableAuditChain getAuditChain() { return auditChain; }
    public EncryptedBackupVault getBackupVault() { return backupVault; }

    @Override
    public void onEvent(NexusEvent event) {
        if ("TRANSACTION_ADDED".equals(event.getType())) {
            if (event.getPayload() instanceof nexus.finance.Transaction t) {
                auditChain.addTransaction(t.getId(), t.getDescription(), t.getAmount());
            }
        } else if ("TICKET_BOOKED".equals(event.getType())) {
            if (event.getPayload() instanceof nexus.media.CinemaBooking b) {
                auditChain.addTransaction(b.getId(), "Cinema: " + b.getMovieTitle(), b.getFinalTotal());
            }
        }
    }
}
