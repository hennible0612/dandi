package com.e205.byteBuddy;

import com.e205.events.Event;
import com.e205.exception.GlobalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Assert;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EventConverter {

  public static final Map<String, Class<?>> eventClasses = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;

  public Event toEvent(OutboxEvent outboxEvent) {
    try {
      Class<?> eventClass = eventClasses.get(outboxEvent.getEventType());
      Assert.state(eventClass != null, "class type must not be null");

      Event event = (Event) this.objectMapper.readValue(outboxEvent.getPayload(), eventClass);

      Method setEventId = eventClass.getDeclaredMethod("setEventId", String.class);
      setEventId.invoke(event, outboxEvent.getEventId());

      Method setStatus = eventClass.getDeclaredMethod("setStatus", EventStatus.class);
      setStatus.invoke(event, outboxEvent.getStatus());

      log.info("Successfully converted OutboxEvent to Event: {}", event);
      return event;
    } catch (Exception e) {
      log.warn("Failed to convert OutboxEvent to Event", e);
      throw new GlobalException("E008");
    }
  }

  public OutboxEvent toOutboxEvent(Event event, EventStatus status) {
    try {
      Method getEventId = event.getClass().getDeclaredMethod("getEventId");
      String eventId = (String) getEventId.invoke(event);
      Field declaredField = event.getClass().getDeclaredField("status");
      declaredField.setAccessible(true);
      declaredField.set(event, status);
      String payload = this.objectMapper.writeValueAsString(event);

      OutboxEvent outboxEvent = new OutboxEvent(eventId, status, payload, LocalDateTime.now(), event.getType());
      log.info("Successfully converted Event to OutboxEvent: {}", outboxEvent);
      return outboxEvent;
    } catch (Exception e) {
      log.warn("Failed to convert Event to OutboxEvent", e);
      throw new GlobalException("E008");
    }
  }

  public OutboxEvent toOutboxEvent(Event event) {
    try {
      Method getEventId = event.getClass().getDeclaredMethod("getEventId");
      String eventId = (String) getEventId.invoke(event);
      String payload = this.objectMapper.writeValueAsString(event);

      Method getStatus = event.getClass().getDeclaredMethod("getStatus");
      EventStatus status = (EventStatus) getStatus.invoke(event);

      OutboxEvent outboxEvent = new OutboxEvent(eventId, status, payload, LocalDateTime.now(), event.getType());
      log.info("Successfully converted Event to OutboxEvent (status from event): {}", outboxEvent);
      return outboxEvent;
    } catch (Exception e) {
      log.warn("Failed to convert Event to OutboxEvent", e);
      throw new GlobalException("E008");
    }
  }
}
