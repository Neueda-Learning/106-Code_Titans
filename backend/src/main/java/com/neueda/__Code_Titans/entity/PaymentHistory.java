package com.neueda.__Code_Titans.entity;


import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("payment_history")
public class PaymentHistory {

    @Id
    @Column("history_id")
    private Long historyId;

    @Column("payment_id")
    private Long paymentId;

    @Column("old_status")
    private String oldStatus;

    @Column("new_status")
    private String newStatus;

    @Column("changed_at")
    private LocalDateTime changedAt;

    @Column("changed_by")
    private String changedBy;

    @Column("remarks")
    private String remarks;

    public PaymentHistory() {
    }

    public PaymentHistory(Long paymentId, String oldStatus, String newStatus, String changedBy, String remarks) {
        this.paymentId = paymentId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.remarks = remarks;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
