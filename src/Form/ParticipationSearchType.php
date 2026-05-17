<?php
// src/Form/ParticipationSearchType.php

namespace App\Form;

use App\Entity\Evenement;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ParticipationSearchType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label'    => 'Recherche',
                'required' => false,
                'attr'     => [
                    'class'       => 'form-control',
                    'placeholder' => 'Nom, prénom ou email…',
                ],
            ])
            ->add('statut', ChoiceType::class, [
                'label'    => 'Statut',
                'required' => false,
                'choices'  => [
                    'Tous'        => '',
                    'Confirmée'   => 'confirmee',
                    'En attente'  => 'en_attente',
                    'Annulée'     => 'annulee',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            ->add('evenement', EntityType::class, [
                'label'       => 'Événement',
                'class'       => Evenement::class,
                'choice_label'=> 'titreEvenement',
                'required'    => false,
                'placeholder' => 'Tous les événements',
                'attr'        => ['class' => 'form-select'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['csrf_protection' => false]);
    }
}
