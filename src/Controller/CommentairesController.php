<?php

namespace App\Controller;

use App\Entity\Commentaires;
use App\Entity\Utilisateurs;
use App\Repository\CommentairesRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/commentaires', name: 'commentaires_')]
class CommentairesController extends AbstractController
{
    #[Route('/{id}/edit', name: 'edit', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function edit(
        int $id,
        Request $request,
        CommentairesRepository $repo,
        EntityManagerInterface $em,
        ValidatorInterface $validator
    ): Response {
        /** @var Commentaires|null $commentaire */
        $commentaire = $repo->find($id);
        if (!$commentaire) {
            throw $this->createNotFoundException();
        }

        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');
        /** @var Utilisateurs $currentUser */
        $currentUser = $this->getUser();

        if ($commentaire->getIdUser() !== $currentUser->getIdUtilisateur()) {
            throw $this->createAccessDeniedException('Vous ne pouvez modifier que vos propres commentaires.');
        }

        $nouveauContenu = (string) $request->request->get('contenu', '');
        $commentaire->setContenu(trim($nouveauContenu));

        $errors = $validator->validate($commentaire);
        if (count($errors) > 0) {
            foreach ($errors as $error) {
                $this->addFlash('error', $error->getMessage());
            }
            // Récupération sécurisée de la ressource
            $ressource = $commentaire->getRessource();
            if (!$ressource) {
                $this->addFlash('error', 'La ressource associée n’existe plus.');
                return $this->redirectToRoute('ressources_index');
            }
            return $this->redirectToRoute('ressources_show', [
                'id'        => $ressource->getId(),
                '_fragment' => 'commentaires',
            ]);
        }

        $em->flush();
        $this->addFlash('success', 'Commentaire modifié.');

        $ressource = $commentaire->getRessource();
        if (!$ressource) {
            $this->addFlash('error', 'La ressource associée n’existe plus.');
            return $this->redirectToRoute('ressources_index');
        }
        return $this->redirectToRoute('ressources_show', [
            'id'        => $ressource->getId(),
            '_fragment' => 'commentaires',
        ]);
    }

    #[Route('/{id}/delete', name: 'delete', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function delete(
        int $id,
        Request $request,
        CommentairesRepository $repo,
        EntityManagerInterface $em
    ): Response {
        /** @var Commentaires|null $commentaire */
        $commentaire = $repo->find($id);
        if (!$commentaire) {
            throw $this->createNotFoundException();
        }

        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');
        /** @var Utilisateurs $currentUser */
        $currentUser = $this->getUser();

        if ($commentaire->getIdUser() !== $currentUser->getIdUtilisateur()) {
            throw $this->createAccessDeniedException('Vous ne pouvez supprimer que vos propres commentaires.');
        }

        $ressource = $commentaire->getRessource();
        if (!$ressource) {
            $this->addFlash('error', 'La ressource associée n’existe plus.');
            return $this->redirectToRoute('ressources_index');
        }
        $ressourceId = $ressource->getId();

        $token = (string) $request->request->get('_token', '');
        if ($this->isCsrfTokenValid('delete_comment_' . $id, $token)) {
            $commentaire->setStatus('SUPPRIME');
            $em->flush();
            $this->addFlash('success', 'Commentaire supprimé.');
        } else {
            $this->addFlash('error', 'Token CSRF invalide.');
        }

        return $this->redirectToRoute('ressources_show', [
            'id'        => $ressourceId,
            '_fragment' => 'commentaires',
        ]);
    }

    #[Route('/{id}/like', name: 'like', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function like(
        int $id,
        CommentairesRepository $repo,
        EntityManagerInterface $em
    ): Response {
        /** @var Commentaires|null $commentaire */
        $commentaire = $repo->find($id);
        if (!$commentaire) {
            return $this->json(['error' => 'Non trouvé'], 404);
        }

        $commentaire->incrementLikes();
        $em->flush();

        return $this->json(['likes' => $commentaire->getLikes()]);
    }

    #[Route('/{id}/signaler', name: 'signaler', methods: ['POST'], requirements: ['id' => '\d+'])]
    public function signaler(
        int $id,
        CommentairesRepository $repo,
        EntityManagerInterface $em
    ): Response {
        /** @var Commentaires|null $commentaire */
        $commentaire = $repo->find($id);
        if (!$commentaire) {
            throw $this->createNotFoundException();
        }

        $this->denyAccessUnlessGranted('IS_AUTHENTICATED_FULLY');
        /** @var Utilisateurs $currentUser */
        $currentUser = $this->getUser();

        if ($commentaire->getIdUser() === $currentUser->getIdUtilisateur()) {
            $this->addFlash('error', 'Vous ne pouvez pas signaler votre propre commentaire.');
            $ressource = $commentaire->getRessource();
            if ($ressource) {
                return $this->redirectToRoute('ressources_show', [
                    'id'        => $ressource->getId(),
                    '_fragment' => 'commentaires',
                ]);
            }
            return $this->redirectToRoute('ressources_index');
        }

        $ressource = $commentaire->getRessource();
        if (!$ressource) {
            $this->addFlash('error', 'La ressource associée n’existe plus.');
            return $this->redirectToRoute('ressources_index');
        }

        $commentaire->setStatus('SIGNALE');
        $em->flush();

        $this->addFlash('warning', 'Commentaire signalé. Merci pour votre contribution.');

        return $this->redirectToRoute('ressources_show', [
            'id'        => $ressource->getId(),
            '_fragment' => 'commentaires',
        ]);
    }
}