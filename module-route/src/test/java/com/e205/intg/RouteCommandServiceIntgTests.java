package com.e205.intg;

import static com.e205.env.TestConstant.BAG_ID_1;
import static com.e205.env.TestConstant.BAG_ID_2;
import static com.e205.env.TestConstant.MEMBER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.e205.TestConfiguration;
import com.e205.command.RouteCreateCommand;
import com.e205.command.RouteEndCommand;
import com.e205.command.SnapshotUpdateCommand;
import com.e205.command.bag.payload.BagItemPayload;
import com.e205.command.bag.query.ReadAllBagItemsQuery;
import com.e205.command.bag.query.ReadAllItemInfoQuery;
import com.e205.command.bag.service.BagQueryService;
import com.e205.command.item.payload.ItemPayload;
import com.e205.domain.Route;
import com.e205.dto.Snapshot;
import com.e205.dto.SnapshotItem;
import com.e205.dto.TrackPoint;
import com.e205.events.EventPublisher;
import com.e205.repository.RouteRepository;
import com.e205.service.DirectRouteCommandService;
import com.e205.util.GeometryUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Sql("/test-sql/route.sql")
@ActiveProfiles(value = "test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@SpringBootTest(classes = TestConfiguration.class)
public class RouteCommandServiceIntgTests {

  List<SnapshotItem> basedBagItems;
  List<SnapshotItem> currentBagItems;
  List<SnapshotItem> newBagItems;

  RouteCreateCommand requestBagId1;
  RouteCreateCommand requestBagId2;

  @Autowired
  private DirectRouteCommandService routeCommandService;

  @Autowired
  private RouteRepository routeRepository;

  @MockBean
  private BagQueryService bagQueryService;

  @MockBean
  private EventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    requestBagId1 = new RouteCreateCommand(MEMBER_ID_1, BAG_ID_1);
    requestBagId2 = new RouteCreateCommand(MEMBER_ID_1, BAG_ID_2);
    assignSnapshotItem();
  }

  private void mockBagQueryService(List<BagItemPayload> bagItems, List<ItemPayload> itemDetails) {
    given(bagQueryService.readAllBagItemsByBagId(any(ReadAllBagItemsQuery.class))).willReturn(
        bagItems);
    given(bagQueryService.readAllByItemIds(any(ReadAllItemInfoQuery.class))).willReturn(
        itemDetails);
  }

  private Snapshot initializeSnapshot(Integer bagId, List<SnapshotItem> items) {
    Snapshot snapshot = new Snapshot(bagId, items);
    routeRepository.save(Route.toEntity(MEMBER_ID_1, Snapshot.toJson(snapshot)));
    return snapshot;
  }

  @Test
  @DisplayName("이동 생성 시 이전 가방과 현재 가방이 같은 경우 이전 스냅샷 제공 테스트")
  void 이동_생성시_이전_가방과_현재_가방이_같은_경우_이전_스냅샷_제공_테스트() {
    initializeSnapshot(requestBagId1.bagId(), currentBagItems);
    Route previousRoute = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).get();

    mockBagQueryService(
        List.of(
            new BagItemPayload(1, BAG_ID_1, 1, (byte) 1),
            new BagItemPayload(2, BAG_ID_1, 2, (byte) 2)
        ),
        List.of(
            new ItemPayload(1, MEMBER_ID_1, "👛", "지갑", (byte) 1, (byte) 1),
            new ItemPayload(2, MEMBER_ID_1, "💍", "반지", (byte) 1, (byte) 2)
        )
    );

    routeCommandService.createRoute(requestBagId1);
    Route currentRoute = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).get();

    Snapshot previousSnapshot = Snapshot.fromJson(previousRoute.getSnapshot());
    Snapshot currentSnapshot = Snapshot.fromJson(currentRoute.getSnapshot());

    assertThat(currentSnapshot.bagId()).isEqualTo(previousSnapshot.bagId());
    assertThat(currentSnapshot.items()).isEqualTo(previousSnapshot.items());
  }

  @Test
  @DisplayName("이동 생성 시 이전 가방과 현재 가방이 다른 경우 기본 스냅샷 제공 테스트")
  void 이동_생성시_이전_가방과_현재_가방이_다른_경우_기본_스냅샷_제공_테스트() {
    initializeSnapshot(requestBagId1.bagId(), currentBagItems);
    Route previousRoute = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).get();

    mockBagQueryService(
        List.of(
            new BagItemPayload(3, BAG_ID_2, 3, (byte) 1),
            new BagItemPayload(4, BAG_ID_2, 4, (byte) 2)
        ),
        List.of(
            new ItemPayload(3, MEMBER_ID_1, "📱", "폰", (byte) 2, (byte) 1),
            new ItemPayload(4, MEMBER_ID_1, "💼", "가방", (byte) 2, (byte) 2)
        )
    );

    routeCommandService.createRoute(requestBagId2);
    Route currentRoute = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).get();

    Snapshot previousSnapshot = Snapshot.fromJson(previousRoute.getSnapshot());
    Snapshot currentSnapshot = Snapshot.fromJson(currentRoute.getSnapshot());

    assertThat(currentSnapshot.bagId()).isNotEqualTo(previousSnapshot.bagId());
    assertThat(currentSnapshot.items()).isNotEqualTo(previousSnapshot.items());
    assertThat(currentSnapshot.items()).allMatch(item -> !item.isChecked());
  }

  @Test
  @DisplayName("최근 이동이 없는 경우 기본 스냅샷을 포함한 이동 생성 테스트")
  void 최근_이동이_없는_경우_기본_스냅샷을_포함한_이동_생성_테스트() {
    mockBagQueryService(
        List.of(
            new BagItemPayload(1, BAG_ID_1, 1, (byte) 1),
            new BagItemPayload(2, BAG_ID_1, 2, (byte) 2)
        ),
        List.of(
            new ItemPayload(1, MEMBER_ID_1, "👛", "지갑", (byte) 1, (byte) 1),
            new ItemPayload(2, MEMBER_ID_1, "💍", "반지", (byte) 1, (byte) 2)
        )
    );

    routeCommandService.createRoute(requestBagId1);
    Route latestRoute = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).orElseThrow();
    Snapshot snapshot = Snapshot.fromJson(latestRoute.getSnapshot());

    assertThat(snapshot.bagId()).isEqualTo(BAG_ID_1);
    assertThat(snapshot.items()).isEqualTo(basedBagItems);
  }

  @Test
  @DisplayName("스냅샷 수정 테스트")
  void 스냅샷_수정_테스트() {
    initializeSnapshot(requestBagId1.bagId(), currentBagItems);
    Snapshot currentSnapshot = new Snapshot(requestBagId1.bagId(), currentBagItems);
    Route route = routeRepository.findFirstByMemberIdOrderByIdDesc(MEMBER_ID_1).orElseThrow();

    SnapshotUpdateCommand command = new SnapshotUpdateCommand(
        MEMBER_ID_1, route.getId(), currentSnapshot
    );

    routeCommandService.updateSnapshot(command);
    Route updatedRoute = routeRepository.findById(route.getId()).orElseThrow();

    assertThat(updatedRoute.getSnapshot()).isEqualTo(Snapshot.toJson(command.snapshot()));
  }

  @Test
  @DisplayName("이동 종료 테스트")
  void 이동_종료_테스트() {
    Snapshot snapshot = new Snapshot(requestBagId1.bagId(), currentBagItems);
    Route route = routeRepository.save(Route.toEntity(MEMBER_ID_1, Snapshot.toJson(snapshot)));
    List<TrackPoint> trackPoints = List.of(
        TrackPoint.builder().lat(37.7749).lon(-122.4194).build(),
        TrackPoint.builder().lat(34.0522).lon(-118.2437).build()
    );
    RouteEndCommand command = new RouteEndCommand(MEMBER_ID_1, route.getId(), trackPoints);

    routeCommandService.endRoute(command);
    Route endedRoute = routeRepository.findById(route.getId()).orElseThrow();
    List<TrackPoint> savedTrackPoints = GeometryUtils.getPoints(endedRoute.getTrack());

    assertThat(endedRoute.getEndedAt()).isNotNull();
    assertThat(savedTrackPoints).containsExactlyElementsOf(trackPoints);
  }

  private void assignSnapshotItem() {
    basedBagItems = List.of(
        new SnapshotItem("지갑", "👛", 1, false),
        new SnapshotItem("반지", "💍", 1, false)
    );
    currentBagItems = List.of(
        new SnapshotItem("지갑", "👛", 1, true),
        new SnapshotItem("반지", "💍", 1, true)
    );
    newBagItems = List.of(
        new SnapshotItem("폰", "📱", 2, false),
        new SnapshotItem("가방", "💼", 2, false)
    );
  }
}
