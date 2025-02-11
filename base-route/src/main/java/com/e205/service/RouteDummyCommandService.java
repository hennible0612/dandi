package com.e205.service;

import com.e205.command.RouteDummyCreateCommand;

public interface RouteDummyCommandService {

  /**
   * 이동 더미 데이터를 생성합니다.
   * 
   * @param command 더미 이동 생성 명령
   */
  void createRouteDummy(RouteDummyCreateCommand command);
}
