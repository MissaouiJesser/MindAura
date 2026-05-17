<?php

namespace App\Form;

use App\Entity\Reponse;
use App\Entity\Reclamation;
use App\Entity\Utilisateurs;
use App\Repository\ReclamationRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Bundle\SecurityBundle\Security;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ReponseType extends AbstractType
{
    public function __construct(
        private readonly RequestStack $requestStack,
        private readonly ReclamationRepository $reclamationRepository,
        private readonly Security $security,
    ) {
    }

    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $context = $options['context'];

        if ($context === 'front_reply') {
            $builder->add('contenu_reponse', TextareaType::class, [
                'label' => false,
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 5,
                    'placeholder' => 'Votre message sera signé avec votre nom de session…',
                    'minlength' => 5,
                    'maxlength' => 5000,
                ],
                'required' => true,
            ]);

            return;
        }

        if ($context === 'front_edit') {
            $builder->add('contenu_reponse', TextareaType::class, [
                'label' => 'Contenu',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 6,
                    'minlength' => 5,
                    'maxlength' => 5000,
                ],
                'required' => true,
            ]);

            return;
        }

        $builder
            ->add('reclamation', EntityType::class, [
                'label' => 'Réclamation',
                'class' => Reclamation::class,
                'choice_label' => 'sujet_reclamation',
                'placeholder' => 'Sélectionnez une réclamation',
                'attr' => ['class' => 'form-control'],
                'required' => true,
            ])
            ->add('contenu_reponse', TextareaType::class, [
                'label' => 'Contenu de la réponse',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 5,
                    'placeholder' => 'Écrivez votre réponse ici...',
                    'maxlength' => 5000,
                ],
                'required' => true,
            ])
        ;

        if ($context === 'admin_new') {
            $builder
                ->add('id_utilisateur', IntegerType::class, [
                    'label' => 'ID Utilisateur',
                    'attr' => [
                        'class' => 'form-control',
                        'min' => 1,
                    ],
                    'required' => true,
                ])
                ->add('rate_reponse', NumberType::class, [
                    'label' => 'Note (0-5)',
                    'scale' => 2,
                    'attr' => [
                        'class' => 'form-control',
                        'min' => 0,
                        'max' => 5,
                        'step' => 0.5,
                    ],
                    'required' => false,
                ])
            ;

            $builder->addEventListener(FormEvents::PRE_SET_DATA, function (FormEvent $event): void {
                $reponse = $event->getData();
                if (!$reponse instanceof Reponse || $reponse->getReclamation() !== null) {
                    return;
                }
                $request = $this->requestStack->getCurrentRequest();
                if ($request === null) {
                    return;
                }
                $id = $request->query->getInt('reclamation_id');
                if ($id <= 0) {
                    return;
                }
                $rec = $this->reclamationRepository->find($id);
                if ($rec instanceof Reclamation) {
                    $reponse->setReclamation($rec);
                }
            });

            $builder->addEventListener(FormEvents::SUBMIT, function (FormEvent $event): void {
                $reponse = $event->getData();
                if (!$reponse instanceof Reponse) {
                    return;
                }
                $user = $this->security->getUser();
                if ($user instanceof Utilisateurs) {
                    $reponse->setId_utilisateur($user->getIdUtilisateur());
                    $reponse->setNom_utilisateur(trim($user->getPrenomUtilisateur().' '.$user->getNomUtilisateur()));
                }
            });
        }

        if ($context === 'admin_edit') {
            $builder
                ->add('rate_reponse', NumberType::class, [
                    'label' => 'Note',
                    'scale' => 2,
                    'attr' => [
                        'class' => 'form-control',
                        'min' => 0,
                        'max' => 5,
                        'step' => 0.5,
                    ],
                    'required' => false,
                ])
                ->add('rate_sum', IntegerType::class, [
                    'label' => 'Somme des notes',
                    'attr' => ['class' => 'form-control', 'min' => 0],
                    'required' => true,
                ])
                ->add('rate_count', IntegerType::class, [
                    'label' => 'Nombre de notes',
                    'attr' => ['class' => 'form-control', 'min' => 0],
                    'required' => true,
                ])
            ;
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Reponse::class,
            'context' => 'admin_new',
        ]);
        $resolver->setAllowedValues('context', ['admin_new', 'admin_edit', 'front_reply', 'front_edit']);
    }
}
