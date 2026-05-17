package tn.esprit.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.util.List;

public class YouTubeService {

    private static final String API_KEY = "";
    private YouTube youtube;

    public YouTubeService() throws Exception {
        this.youtube = new YouTube.Builder(
                //crée une connexion HTTPS fiable avec les certificats Google
                GoogleNetHttpTransport.newTrustedTransport(),
                //convertit le JSON de la réponse YouTube en objets Java
                JacksonFactory.getDefaultInstance(),
                request -> {}
        ).setApplicationName("MonApplication").build();
    }

    public List<SearchResult> rechercherVideos(String motCle) throws Exception {
        YouTube.Search.List search = youtube.search().list(List.of("id", "snippet"));
        search.setKey(API_KEY);
        search.setQ(motCle);
        search.setType(List.of("video"));
        // max 10 vidéo
        search.setMaxResults(10L);

        SearchListResponse response = search.execute();
        return response.getItems();
    }
}