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
@DiscriminatorValue("EMPLOYEE")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends User {

    @JsonIgnore
    @OneToMany(mappedBy = "employee")
    private List<Assignment> assignments = new ArrayList<>();
}
