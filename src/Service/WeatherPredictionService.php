<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Contracts\HttpClient\Exception\TransportExceptionInterface;
use Symfony\Contracts\HttpClient\Exception\ClientExceptionInterface;
use Symfony\Contracts\HttpClient\Exception\ServerExceptionInterface;

class WeatherPredictionService
{
    private string $flaskApiUrl;

    public function __construct(
        private HttpClientInterface $httpClient
    ) {
        $this->flaskApiUrl = $_ENV['FLASK_API_URL'] ?? 'http://localhost:5000';
    }

    /**
     * @param array<string, mixed> $params
     * @return array<string, mixed>
     */
    public function predictWeather(array $params): array
    {
        try {
            $response = $this->httpClient->request(
                'POST',
                $this->flaskApiUrl . '/api/predict/weather',
                [
                    'json'    => $params,
                    'timeout' => 10,
                    'headers' => [
                        'Content-Type' => 'application/json',
                        'Accept'       => 'application/json',
                    ],
                ]
            );

            $statusCode = $response->getStatusCode();

            if ($statusCode !== 200) {
                return [
                    'success' => false,
                    'error'   => 'Flask API returned status ' . $statusCode,
                ];
            }

            /** @var array<string, mixed> $data */
            $data = $response->toArray();
            return $data;

        } catch (TransportExceptionInterface $e) {
            return [
                'success' => false,
                'error'   => 'Flask API unreachable: ' . $e->getMessage(),
            ];
        } catch (ClientExceptionInterface | ServerExceptionInterface $e) {
            return [
                'success' => false,
                'error'   => 'Flask API error: ' . $e->getMessage(),
            ];
        }
    }

    /**
     * @return array<string, mixed>
     */
    public function predictByDate(
        \DateTime $date,
        float $tempMax,
        float $tempMin,
        float $precipitation = 0,
        float $windspeed = 0
    ): array {
        return $this->predictWeather([
            'temp_max'      => $tempMax,
            'temp_min'      => $tempMin,
            'precipitation' => $precipitation,
            'windspeed'     => $windspeed,
            'day'           => (int) $date->format('d'),
            'month'         => (int) $date->format('m'),
        ]);
    }

    /**
     * @return array<string, mixed>
     */
    public function getForecast(): array
    {
        try {
            $response = $this->httpClient->request(
                'GET',
                $this->flaskApiUrl . '/api/predict/weather/forecast',
                ['timeout' => 10]
            );

            /** @var array<string, mixed> $data */
            $data = $response->toArray();
            return $data;

        } catch (\Exception $e) {
            return ['success' => false, 'error' => $e->getMessage()];
        }
    }
}