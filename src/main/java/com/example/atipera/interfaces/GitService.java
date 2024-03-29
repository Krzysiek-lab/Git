package com.example.atipera.interfaces;

import com.example.atipera.model.Branch;
import com.example.atipera.model.GitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public interface GitService {
    ResponseEntity<Object> repositories(String header, String userName);
    //tu najlepiej skorzystac z dependency injection czyli zamiast tworzyc w metodach obiekty przez new() to wstrzyknac je 
    //w serwisie i pidac do tych metod jako parametry, tu oczywiscie nie wstrzykne jesli chce by bly
    //prywatne pola, oczywiscie najpierw musze stworzyc beany z tych obiektow poprzez metody z @Bean
    default Set<GitRepository> getRepositories(String header, String userName) throws IOException {
        URL url = new URL("https://api.github.com/users/" + userName + "/repos");
        StringBuilder jsonObject = getStringBuilder(header, url);
        ObjectMapper objectMapper = new ObjectMapper();
        var repos = objectMapper.readValue(String.valueOf(jsonObject), GitRepository[].class);
        Arrays.asList(repos).forEach(e -> {
            try {
                var res = getBranchesForAllRepositories(header, userName, e.getName());
                e.setBranchSet(res);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        return Set.of(repos);
    }

    private Set<Branch> getBranchesForAllRepositories(String header, String userName, String repoName) throws IOException {
        URL url = new URL("https://api.github.com/repos/" + userName + "/" + repoName + "/branches");
        StringBuilder jsonObject = getStringBuilder(header, url);
        ObjectMapper objectMapper = new ObjectMapper();
        return new HashSet<>(Set.of(objectMapper.readValue(String.valueOf(jsonObject), Branch[].class)));
    }

    private StringBuilder getStringBuilder(String header, URL url) throws IOException {
        String[] arr = header.split(": ");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty(arr[0], arr[1]);
//        urlConnection.setRequestProperty("Authorization", "Bearer " + "tu token"); // TODO należy podać token
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder jsonObject = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            jsonObject.append(line);
        }
        bufferedReader.close();
        return jsonObject;
    }
}
