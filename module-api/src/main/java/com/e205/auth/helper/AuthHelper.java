package com.e205.auth.helper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthHelper {

  public Integer getMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
    }

    String memberId = authentication.getName();
    return Integer.parseInt(memberId);
  }
}
