package com.e205.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.e205.entity.LostItem;
import com.e205.entity.LostItemAuth;
import com.e205.event.LostItemReadEvent;
import com.e205.events.EventPublisher;
import com.e205.query.LostItemQuery;
import com.e205.query.LostItemValidRangeQuery;
import com.e205.repository.LostItemAuthRepository;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LostItemQueryServiceTests {

  static Integer memberId = 1;
  static Integer lostItemId = 1;

  LostItemQueryService service;
  @Mock
  LostItemAuthRepository lostItemAuthRepository;
  @Mock
  RouteQueryService routeQueryService;
  @Mock
  EventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    service = new DefaultLostItemQueryService(lostItemAuthRepository, routeQueryService,
        eventPublisher);
  }

  @DisplayName("확인한 적 없고 조회 가능 범위 내에 없으면, 상세조회할 수 없다.")
  @Test
  void When_QueryUncheckedAndInvalidPosition_Then_CannotGetLostItemDetails() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, false);
    setValidPosition(false);

    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    ThrowingCallable expectThrow = () -> service.find(query);

    // then
    assertThatThrownBy(expectThrow).hasMessage("분실물을 조회할 수 있는 범위를 벗어났습니다.");
  }

  @DisplayName("확인한 적 없고 종료된 분실물이라면, 상세조회할 수 없다.")
  @Test
  void When_EndedLostItem_Then_CannotGetLostItemDetails() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, false);
    lostItem.end();

    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    ThrowingCallable expectThrow = () -> service.find(query);

    // then
    assertThatThrownBy(expectThrow).hasMessage("종료된 분실물입니다.");
  }

  @DisplayName("분실물 알림을 확인한 적 있다면, 상세조회할 수 있다.")
  @Test
  void When_QueryWithChecked_Then_CanGetLostItemDetails() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, true);

    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    ThrowingCallable notThrow = () -> service.find(query);

    // then
    assertThatCode(notThrow).doesNotThrowAnyException();
  }

  @DisplayName("분실물 조회 가능 범위 내에 있으면, 상세조회할 수 있다.")
  @Test
  void When_QueryWithValidPosition_Then_CanGetLostItemDetails() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, false);
    setValidPosition(true);
    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    ThrowingCallable notThrow = () -> service.find(query);

    // then
    assertThatCode(notThrow).doesNotThrowAnyException();
  }

  @DisplayName("분실물을 처음 상세 조회하면, 이벤트가 발행된다.")
  @Test
  void When_GetLostItemDetailsFirstTime_Then_PublishEvent() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, false);
    setValidPosition(true);
    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    service.find(query);

    // then
    verify(eventPublisher, times(1)).publish(any(LostItemReadEvent.class));
  }

  @DisplayName("이미 읽은 분실물 상세를 조회하면, 이벤트가 발행되지 않는다.")
  @Test
  void When_GetLostItemDetailsNotFirstTime_Then_NotPublishEvent() {
    // given
    LostItem lostItem = generateLostItem(memberId);

    setRead(lostItem, true);
    LostItemQuery query = new LostItemQuery(memberId, lostItemId);

    // when
    service.find(query);

    // then
    verify(eventPublisher, never()).publish(any(LostItemReadEvent.class));
  }

  private void setValidPosition(boolean valid) {
    given(routeQueryService.isReadableRange(any(LostItemValidRangeQuery.class))).willReturn(valid);
  }

  private void setRead(LostItem lostItem, boolean read) {
    LostItemAuth lostItemAuth = new LostItemAuth(memberId, lostItem);
    if (read) {
      lostItemAuth.read();
    }
    given(lostItemAuthRepository.findLostItemAuthByMemberIdAndLostItemId(memberId,
        lostItemId)).willReturn(Optional.of(lostItemAuth));
  }

  private LostItem generateLostItem(Integer memberId) {
    return new LostItem(memberId, 1, 2, "상황묘사", "물건묘사");
  }
}
