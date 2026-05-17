<?php

namespace App\Service;

use Symfony\Component\DependencyInjection\Attribute\Autowire;

class QrCodeService
{
    private string $qrDir;

    public function __construct(
        #[Autowire('%kernel.project_dir%/public/qrcodes')]
        string $qrDir
    ) {
        $this->qrDir = $qrDir;
        if (!is_dir($this->qrDir)) {
            mkdir($this->qrDir, 0777, true);
        }
    }

    /**
     * Génère un QR code à partir d'une API externe (avec fallback image texte)
     * Retourne le nom du fichier (ex: 'abc123.png') ou une chaîne vide en cas d'échec.
     */
    public function generate(string $data, string $filename): string
    {
        $fileName = $filename . '.png';
        $filePath = $this->qrDir . '/' . $fileName;

        // Tentative API externe
        $url = 'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=' . urlencode($data);
        $imageData = @file_get_contents($url);

        if ($imageData === false) {
            // Fallback : génération d'une image avec texte
            $this->generateFallbackPng($data, $filePath);
            // Vérifier si le fichier a bien été créé
            if (!file_exists($filePath)) {
                return '';
            }
        } else {
            file_put_contents($filePath, $imageData);
        }

        return $fileName;
    }

    private function generateFallbackPng(string $data, string $filePath): void
    {
        $img = imagecreate(300, 300);
        if ($img === false) {
            return;
        }
        $bg = imagecolorallocate($img, 255, 255, 255);
        $fg = imagecolorallocate($img, 0, 0, 0);
        
        // Vérifier que la couleur est valide
        if (!is_int($fg)) {
            imagedestroy($img);
            return;
        }
        
        imagestring($img, 5, 10, 140, substr($data, 0, 40), $fg);
        imagepng($img, $filePath);
        imagedestroy($img);
    }

    public function getAbsolutePath(string $fileName): string
    {
        return $this->qrDir . '/' . $fileName;
    }
}