package com.stockpro.reportservice.service;

import com.stockpro.reportservice.events.ReportEvent;

public interface ReportEventPublisher {
    void publish(String routingKey, ReportEvent event);
}
