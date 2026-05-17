package tn.esprit;

import tn.esprit.services.GeminiService;

import static tn.esprit.services.GeminiService.listModels;

public class Main {

    public static void main(String[] args) {
        String response = GeminiService.askGemini("Dis bonjour en français");
        System.out.println(response);

    }
}


