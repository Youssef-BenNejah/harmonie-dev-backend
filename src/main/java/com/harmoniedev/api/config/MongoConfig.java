package com.harmoniedev.api.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
	@Value("${spring.data.mongodb.uri}")
	private String mongoUri;

	@Bean
	public MongoClient mongoClient() {
		ConnectionString connectionString = new ConnectionString(mongoUri);
		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(connectionString)
				.applyToConnectionPoolSettings(builder -> builder
						.maxSize(50)
						.minSize(5)
						.maxConnectionIdleTime(60, TimeUnit.SECONDS))
				.applyToSocketSettings(builder -> builder
						.connectTimeout(10, TimeUnit.SECONDS)
						.readTimeout(30, TimeUnit.SECONDS))
				.serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
				.build();
		return MongoClients.create(settings);
	}
}
