package com.example.demo;

import com.example.Person;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@SpringBootTest
class ObjectMapperTest {
	@Autowired
	ObjectMapper springObjectMapper; // Spring Boot auto-configured ObjectMapper

	ObjectMapper jacksonObjectMapper = new ObjectMapper(); // Jackson default ObjectMapper

	@BeforeEach
	void setup() {
		jacksonObjectMapper = new ObjectMapper();
	}

	@Test
	void test_WRITE_DATES_AS_TIMESTAMPS_WRITE_DURATIONS_AS_TIMESTAMPS() throws JsonProcessingException {
		String springSerialized = springObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
			new TimeClass(1, new Date(), Instant.now(), Duration.ofMinutes(10L))
		);
		log.info(springSerialized);
		// Output
		//{
		//  "id" : 1,
		//  "date" : "2022-01-03T12:52:11.802+00:00",
		//  "instant" : "2022-01-03T12:52:11.802831Z",
		//  "duration" : "PT10M"
		//}
		jacksonObjectMapper.registerModule(new JavaTimeModule()); // For serialization of Instant, Duration
		String jacksonSerialized = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
			new TimeClass(1, new Date(), Instant.now(), Duration.ofMinutes(10L))
		);
		log.info(jacksonSerialized);
		// Output
		//{
		//  "id" : 1,
		//  "date" : 1641214331840,
		//  "instant" : 1641214331.840827200,
		//  "duration" : 600.000000000
		//}
	}
	@AllArgsConstructor
	@Data
	public static class TimeClass {
		private Integer id;
		private Date date;
		private Instant instant;
		private Duration duration;
	}

	@Test
	void test_FAIL_ON_UNKNOWN_PROPERTIES() throws JsonProcessingException {
		String jsonString = "{\"id\": 42, \"name\": \"John Doe\", \"salary\": 123456}";
		Person springDeserialized = springObjectMapper.readValue(jsonString, Person.class);
		log.info(springDeserialized.toString());
		// ObjectMapperTest.Person(id=42, name=John Doe)

		//Person jacksonDeserialized = jacksonObjectMapper.readValue(jsonString, Person.class);
		// above statement throws Exception
		//com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "salary" (class com.example.demo.ObjectMapperTest$Person), not marked as ignorable (2 known properties: "id", "name"])
		// at [Source: (String)"{"id": 42, "name": "John Doe", "salary": 123456}"; line: 1, column: 48] (through reference chain: com.example.demo.ObjectMapperTest$Person["salary"])
        //log.info(jacksonDeserialized.toString());
	}
	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class Person {
		private Integer id;
		private String name;
	}

	@Test
	void test_DEFAULT_VIEW_INCLUSION() throws JsonProcessingException, IOException {
		Person2 p = new Person2(42, "John Doe", "Middle");
		String springSerialized = springObjectMapper.writerWithView(Views.Public.class).writeValueAsString(p);
		String jacksonSerialized = jacksonObjectMapper.writerWithView(Views.Public.class).writeValueAsString(p);
		log.info(springSerialized); // {"name":"John Doe"}
		log.info(jacksonSerialized); // {"name":"John Doe","middleName":"Middle"}

		Person2 sp = springObjectMapper.readerWithView(Views.Public.class).readValue("{\"name\":\"John Doe\",\"middleName\":\"Middle\"}", Person2.class);
		Person2 jp = jacksonObjectMapper.readerWithView(Views.Public.class).readValue("{\"name\":\"John Doe\",\"middleName\":\"Middle\"}", Person2.class);
		log.info(sp.toString()); // ObjectMapperTest.Person2(id=null, name=John Doe, middleName=null)
		log.info(jp.toString()); // ObjectMapperTest.Person2(id=null, name=John Doe, middleName=Middle)
	}
	public static class Views {
		public static class Public {
		}
		public static class Internal {
		}
	}
	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class Person2 {
		@JsonView({Views.Internal.class})
		private Integer id;
		@JsonView({Views.Public.class, Views.Internal.class})
		private String name;

		private String middleName;
	}
}
