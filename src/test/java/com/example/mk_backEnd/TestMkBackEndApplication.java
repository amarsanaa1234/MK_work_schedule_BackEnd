package com.example.mk_backEnd;

import org.springframework.boot.SpringApplication;

public class TestMkBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.from(MkBackEndApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
