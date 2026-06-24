package com.collegemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner initAdmin(
			com.collegemate.repository.UserRepository userRepository,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
			@org.springframework.beans.factory.annotation.Value("${collegemate.admin.username:admin@collegemate.com}") String adminUsername,
			@org.springframework.beans.factory.annotation.Value("${collegemate.admin.password:admin123}") String adminPassword) {
		return args -> {
			if (userRepository.findByEmail(adminUsername).isEmpty()) {
				com.collegemate.model.User admin = new com.collegemate.model.User(
						adminUsername,
						passwordEncoder.encode(adminPassword),
						"System Admin",
						com.collegemate.model.User.Role.ADMIN
				);
				userRepository.save(admin);
			}
		};
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner cleanDatabase(
			com.collegemate.repository.CollegeRepository collegeRepository,
			com.collegemate.repository.AdvisorProfileRepository advisorProfileRepository,
			com.collegemate.repository.ReviewRepository reviewRepository) {
		return args -> {
			try {
				collegeRepository.findAll().forEach(college -> {
					boolean changed = false;
					String name = stripEmojis(college.getName());
					if (college.getName() != null && !name.equals(college.getName())) {
						college.setName(name);
						changed = true;
					}
					String desc = stripEmojis(college.getDescription());
					if (college.getDescription() != null && !desc.equals(college.getDescription())) {
						college.setDescription(desc);
						changed = true;
					}
					if (changed) {
						collegeRepository.save(college);
						System.out.println("Cleaned database emojis from College: " + college.getName());
					}
				});

				advisorProfileRepository.findAll().forEach(profile -> {
					boolean changed = false;
					String bio = stripEmojis(profile.getBio());
					if (profile.getBio() != null && !bio.equals(profile.getBio())) {
						profile.setBio(bio);
						changed = true;
					}
					String major = stripEmojis(profile.getMajor());
					if (profile.getMajor() != null && !major.equals(profile.getMajor())) {
						profile.setMajor(major);
						changed = true;
					}
					if (changed) {
						advisorProfileRepository.save(profile);
						System.out.println("Cleaned database emojis from Advisor Profile: " + profile.getUserId());
					}
				});

				reviewRepository.findAll().forEach(review -> {
					boolean changed = false;
					String comment = stripEmojis(review.getComment());
					if (review.getComment() != null && !comment.equals(review.getComment())) {
						review.setComment(comment);
						changed = true;
					}
					if (changed) {
						reviewRepository.save(review);
						System.out.println("Cleaned database emojis from Review: " + review.getId());
					}
				});
			} catch (Exception e) {
				System.err.println("Database cleaning error: " + e.getMessage());
			}
		};
	}

	private static String stripEmojis(String input) {
		if (input == null) return null;
		return input.replaceAll("[\\ud800-\\udbff][\\udc00-\\udfff]|[\\u2600-\\u27bf]", "").trim();
	}

	private static void loadEnv() {
		try {
			java.nio.file.Path path = java.nio.file.Paths.get(".env");
			if (!java.nio.file.Files.exists(path)) {
				path = java.nio.file.Paths.get("backend/.env");
			}
			if (java.nio.file.Files.exists(path)) {
				java.nio.file.Files.lines(path).forEach(line -> {
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
						int idx = line.indexOf("=");
						String key = line.substring(0, idx).trim();
						String val = line.substring(idx + 1).trim();
						if (!key.isEmpty() && !val.isEmpty()) {
							System.setProperty(key, val);
						}
					}
				});
			}
		} catch (Exception ignored) {}
	}
}
