package com.e205.domain.bag.entity;

import com.e205.common.audit.BaseTime;
import com.e205.domain.bag.dto.BagDataResponse;
import com.e205.log.LoggableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@Getter
@NoArgsConstructor
@Entity
public class Bag extends BaseTime implements LoggableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private Integer memberId;

  @Column(nullable = false, length = 1)
  private char enabled;

  @Column(nullable = false)
  private Byte bagOrder;

  @Column(nullable = false, length = 20)
  private String name;

  public void updateBagOrder(Byte bagOrder) {
    this.bagOrder = bagOrder;
  }

  public void updateName(String newName) {
    this.name = newName;
  }

  public BagDataResponse of() {
    return BagDataResponse.builder()
        .id(this.id)
        .bagOrder(this.bagOrder)
        .enabled(this.enabled)
        .build();
  }
}