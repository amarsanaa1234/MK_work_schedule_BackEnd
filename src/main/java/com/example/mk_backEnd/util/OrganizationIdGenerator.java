package com.example.mk_backEnd.util;

import com.example.mk_backEnd.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OrganizationIdGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 0/O, 1/I тод ялгагдахгүй тэмдэгтүүдийг хассан
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaceRepository;

    public OrganizationIdGenerator(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public String generate() {
        String candidate;
        do {
            candidate = randomCode();
        } while (workspaceRepository.existsByOrganizationId(candidate));
        return candidate;
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
