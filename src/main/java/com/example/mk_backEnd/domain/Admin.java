package com.example.mk_backEnd.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("ADMIN")
@Getter
@Setter
@NoArgsConstructor
public class Admin extends User {

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy")
    private List<JobAd> jobAds = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "recordedBy")
    private List<WorkHourEntry> workHourEntries = new ArrayList<>();
}
