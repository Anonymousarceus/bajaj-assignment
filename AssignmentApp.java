package com.example.assignment;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@SpringBootApplication
public class AssignmentApp implements CommandLineRunner {

    public static void main(String[] args) {
        // starting without any web server
        new SpringApplicationBuilder(AssignmentApp.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Override
    public void run(String... args) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            // step 1 generate webhook
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            JSONObject requestBody = new JSONObject();
            requestBody.put("name", "Akshat Mishra");
            requestBody.put("regNo", "22BCE10217");
            requestBody.put("email", "akshatmishra2022@vitbhopal.ac.in");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            JSONObject responseJson = new JSONObject(response.getBody());
            String webhookUrl = responseJson.getString("webhook");
            String accessToken = responseJson.getString("accessToken");

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            // step 2 build and submit final SQL query
            String finalQuery =
                    "SELECT " +
                            "d.DEPARTMENT_NAME, " +
                            "SUM(p.AMOUNT) AS SALARY, " +
                            "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS EMPLOYEE_NAME, " +
                            "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE " +
                    "FROM EMPLOYEE e " +
                    "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                    "JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
                    "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                    "GROUP BY e.EMP_ID, d.DEPARTMENT_NAME, e.FIRST_NAME, e.LAST_NAME, e.DOB " +
                    "HAVING SUM(p.AMOUNT) = ( " +
                        "SELECT MAX(total_salary) " +
                        "FROM ( " +
                            "SELECT SUM(p2.AMOUNT) AS total_salary " +
                            "FROM PAYMENTS p2 " +
                            "JOIN EMPLOYEE e2 ON p2.EMP_ID = e2.EMP_ID " +
                            "WHERE e2.DEPARTMENT = e.DEPARTMENT " +
                            "AND DAY(p2.PAYMENT_TIME) != 1 " +
                            "GROUP BY p2.EMP_ID " +
                        ") AS dept_salaries " +
                    ")";

            JSONObject submitBody = new JSONObject();
            submitBody.put("finalQuery", finalQuery);

            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.set("Authorization", accessToken);

            HttpEntity<String> submitEntity =
                    new HttpEntity<>(submitBody.toString(), submitHeaders);
            ResponseEntity<String> submitResponse =
                    restTemplate.postForEntity(webhookUrl, submitEntity, String.class);

            System.out.println("Response: " + submitResponse.getBody());

        } catch (Exception e) {
            System.out.println("Some error came up, printing stack trace");
            e.printStackTrace();
        }
    }
}
