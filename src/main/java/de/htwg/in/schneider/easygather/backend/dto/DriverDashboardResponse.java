package de.htwg.in.schneider.easygather.backend.dto;

import java.util.List;

import de.htwg.in.schneider.easygather.backend.model.DeliveryOrder;

public class DriverDashboardResponse {

    private List<DeliveryOrder> available;
    private List<DeliveryOrder> myDeliveries;

    public DriverDashboardResponse(List<DeliveryOrder> available, List<DeliveryOrder> myDeliveries) {
        this.available = available;
        this.myDeliveries = myDeliveries;
    }

    public List<DeliveryOrder> getAvailable() {
        return available;
    }

    public void setAvailable(List<DeliveryOrder> available) {
        this.available = available;
    }

    public List<DeliveryOrder> getMyDeliveries() {
        return myDeliveries;
    }

    public void setMyDeliveries(List<DeliveryOrder> myDeliveries) {
        this.myDeliveries = myDeliveries;
    }
}
