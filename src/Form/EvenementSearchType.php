<?php
namespace App\Form;

use App\Entity\TypeEvenement;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\SearchType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class EvenementSearchType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            // Recherche textuelle (titre, identifiant, lieu, description)
            ->add('query', SearchType::class, [
                'label'    => false,
                'required' => false,
                'attr'     => [
                    'class'       => 'form-control',
                    'placeholder' => 'Rechercher un événement…',
                ],
            ])

            // Filtre par statut automatique
            ->add('statut', ChoiceType::class, [
                'label'       => false,
                'required'    => false,
                'placeholder' => 'Tous les statuts',
                'choices'     => [
                    'À venir'  => 'a_venir',
                    'En cours' => 'en_cours',
                    'Terminé'  => 'termine',
                    'Annulé'   => 'annule',
                ],
                'attr' => ['class' => 'form-select'],
            ])

            // Filtre par type d'événement
            ->add('typeEvenement', EntityType::class, [
                'class'        => TypeEvenement::class,
                'label'        => false,
                'required'     => false,
                'placeholder'  => 'Tous les types',
                'choice_label' => 'libelle',
                'attr'         => ['class' => 'form-select'],
            ])

            // Filtre Gratuit / Payant
            ->add('gratuit', ChoiceType::class, [
                'label'       => false,
                'required'    => false,
                'placeholder' => 'Gratuit / Payant',
                'choices'     => [
                    'Gratuit' => '1',
                    'Payant'  => '0',
                ],
                'attr' => ['class' => 'form-select'],
            ])

            // Filtre date de début (à partir du)
            ->add('dateDebut', DateType::class, [
                'label'    => 'Du',
                'required' => false,
                'widget'   => 'single_text',
                'attr'     => ['class' => 'form-control'],
            ])

            // Filtre date de fin (jusqu'au)
            ->add('dateFin', DateType::class, [
                'label'    => "Jusqu'au",
                'required' => false,
                'widget'   => 'single_text',
                'attr'     => ['class' => 'form-control'],
            ])

            ->add('search', SubmitType::class, [
                'label' => 'Filtrer',
                'attr'  => ['class' => 'btn btn-primary'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'method'          => 'GET',
            'csrf_protection' => false, // Pas de CSRF pour une recherche GET
        ]);
    }

    // Pas de préfixe → ?query=… au lieu de ?search[query]=…
    public function getBlockPrefix(): string
    {
        return '';
    }
}
