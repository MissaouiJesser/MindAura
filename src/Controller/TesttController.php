<?php

namespace App\Controller;

use App\Service\ReclamationPriorityService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class TesttController extends AbstractController
{
    #[Route('/test-priorite')]
    public function test(ReclamationPriorityService $service): Response
    {
        $texte = "Mon colis est bloqué depuis 10 jours, c'est urgent !";

        $result = $service->classifierPriorite(1, $texte);

        return new Response("Priorité détectée : " . $result);
    }
}