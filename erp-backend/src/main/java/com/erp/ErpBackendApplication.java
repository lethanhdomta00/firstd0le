package com.erp;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ErpBackendApplication {

	@PostConstruct
	public void init() {
		// Thiết lập múi giờ chuẩn toàn hệ thống về Việt Nam (GMT+7) để đồng nhất trên mọi Server Cloud (Render, Railway, AWS, Docker)
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ErpBackendApplication.class, args);
	}

}
