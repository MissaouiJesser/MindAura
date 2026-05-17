<?php

namespace App\Controller;

use App\Service\WeatherPredictionService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/weather')]
class WeatherController extends AbstractController
{
    public function __construct(
        private WeatherPredictionService $weatherService
    ) {}

    // ✅ Test rapide via navigateur
    #[Route('/test', name: 'weather_test', methods: ['GET'])]
    public function test(): JsonResponse
    {
        $result = $this->weatherService->predictWeather([
            'temp_max'      => 32.5,
            'temp_min'      => 20.1,
            'precipitation' => 0.0,
            'windspeed'     => 15.0,
            'day'           => 21,
            'month'         => 4,
        ]);

        return $this->json($result);
    }

    // ✅ Aujourd'hui + Demain automatique (données réelles Tunis)
    #[Route('/forecast', name: 'weather_forecast', methods: ['GET'])]
    public function forecast(): JsonResponse
    {
        $result = $this->weatherService->getForecast();
        return $this->json($result);
    }

    // ✅ Endpoint POST pour le frontend
    #[Route('/predict', name: 'weather_predict', methods: ['POST'])]
    public function predict(Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);

        if (!$data) {
            return $this->json(['success' => false, 'error' => 'Invalid JSON'], 400);
        }

        $result = $this->weatherService->predictWeather([
            'temp_max'      => $data['temp_max']      ?? 0,
            'temp_min'      => $data['temp_min']      ?? 0,
            'precipitation' => $data['precipitation'] ?? 0,
            'windspeed'     => $data['windspeed']     ?? 0,
            'day'           => $data['day']           ?? date('d'),
            'month'         => $data['month']         ?? date('m'),
        ]);

        return $this->json($result);
    }
}