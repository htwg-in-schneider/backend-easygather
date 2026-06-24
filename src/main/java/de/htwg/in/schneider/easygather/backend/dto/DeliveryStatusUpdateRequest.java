package de.htwg.in.schneider.easygather.backend.dto;

import de.htwg.in.schneider.easygather.backend.model.DeliveryStatus;

public class DeliveryStatusUpdateRequest {

    private DeliveryStatus status;

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }
}
