package backend.ServicioWeb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "backend")
public class ServicioWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServicioWebApplication.class, args);
	}

}
