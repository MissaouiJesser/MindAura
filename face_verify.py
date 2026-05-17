"""
face_verify.py — Vérification faciale locale avec DeepFace
===========================================================
Usage : python face_verify.py <chemin_photo_profil> <chemin_capture_webcam>

Retourne un JSON sur stdout :
  Succès  : {"success": true,  "distance": 0.23, "similarity": 87.5, "message": "Identite confirmee"}
  Echec   : {"success": false, "distance": 0.55, "similarity": 42.1, "message": "Visage non reconnu"}
  Erreur  : {"success": false, "distance": -1,   "similarity": 0,    "message": "Erreur : ..."}

Installation (une seule fois) :
  pip install deepface tf-keras opencv-python

Modele utilise : ArcFace (le plus precis, ~99.6% sur LFW benchmark)
Seuil : distance cosinus < 0.40 (defini par DeepFace pour ArcFace)

NOTE PERFORMANCE :
  - 1er appel : 30-60 secondes (telechargement + chargement du modele ArcFace ~500MB)
  - Appels suivants : 5-15 secondes (modele deja en memoire)
"""

import sys
import json
import os


def verify(profile_path: str, capture_path: str) -> dict:
    """Compare deux photos et retourne un dict JSON-serialisable."""

    # --- Vérifications de base ---
    if not os.path.exists(profile_path):
        return {
            "success": False, "distance": -1, "similarity": 0,
            "message": f"Photo de profil introuvable : {profile_path}"
        }

    if not os.path.exists(capture_path):
        return {
            "success": False, "distance": -1, "similarity": 0,
            "message": f"Image capturee introuvable : {capture_path}"
        }

    try:
        # Import tardif pour un message d'erreur clair si DeepFace absent
        from deepface import DeepFace

        # --- Comparaison faciale ---
        # model_name      : ArcFace = meilleur rapport precision/vitesse
        # detector_backend: retinaface = meilleure detection visage partiel
        #                   alternatives si lent : "opencv" ou "ssd"
        # distance_metric : cosine (0=identique, 1=tres different)
        # enforce_detection: False = ne plante pas si visage peu visible
        result = DeepFace.verify(
            img1_path        = profile_path,
            img2_path        = capture_path,
            model_name       = "ArcFace",
            detector_backend = "retinaface",
            distance_metric  = "cosine",
            enforce_detection= False
        )

        distance  = float(result["distance"])
        verified  = bool(result["verified"])   # True si distance < seuil ArcFace (~0.40)

        # Convertir distance cosinus → % de similarite lisible
        # distance 0.0 → 100% | distance 0.4 → 60% | distance 1.0 → 0%
        similarity = max(0.0, round((1.0 - distance) * 100, 1))

        if verified:
            return {
                "success"   : True,
                "distance"  : round(distance, 4),
                "similarity": similarity,
                "message"   : f"Identite confirmee ({similarity:.1f}%)"
            }
        else:
            return {
                "success"   : False,
                "distance"  : round(distance, 4),
                "similarity": similarity,
                "message"   : f"Visage non reconnu (similarite : {similarity:.1f}%)"
            }

    except ModuleNotFoundError:
        return {
            "success": False, "distance": -1, "similarity": 0,
            "message": "DeepFace non installe. Executez : pip install deepface tf-keras opencv-python"
        }

    except ValueError as e:
        msg = str(e)
        if "Face could not be detected" in msg or "no face" in msg.lower():
            return {
                "success": False, "distance": -1, "similarity": 0,
                "message": "Aucun visage detecte. Placez-vous face a la camera, bien eclaire."
            }
        return {"success": False, "distance": -1, "similarity": 0, "message": f"Erreur : {msg}"}

    except Exception as e:
        return {
            "success": False, "distance": -1, "similarity": 0,
            "message": f"Erreur inattendue : {str(e)}"
        }


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(json.dumps({
            "success": False, "distance": -1, "similarity": 0,
            "message": "Usage : python face_verify.py <photo_profil> <capture_webcam>"
        }))
        sys.exit(1)

    profile_path = sys.argv[1]
    capture_path = sys.argv[2]

    result = verify(profile_path, capture_path)

    # Sortie JSON propre sur stdout — Java la lira
    # ensure_ascii=True pour eviter les problemes d'encodage Windows
    print(json.dumps(result, ensure_ascii=True))