package com.example.mk_backEnd.service;

import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.dto.RegisterRequest;

public interface AuthService {

    User register(RegisterRequest request);

    User login(String username, String password);

    void logout(String userId);
}
