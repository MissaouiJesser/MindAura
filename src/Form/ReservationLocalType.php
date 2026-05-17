<?php
namespace App\Form;

use App\Entity\LocalsPsychiatrie;
use App\Entity\ReservationLocal;
use App\Entity\Salle;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\MoneyType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TimeType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\GreaterThan;

class ReservationLocalType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('local', EntityType::class, [
                'class'        => LocalsPsychiatrie::class,
                'choice_label' => 'nomLocal',
                'label'        => 'Local',
                'placeholder'  => '— Sélectionner un local —',
                'required'     => false,
            ])

            // ✅ Fix 2 : salle affiché juste après local, même style EntityType
            ->add('salle', EntityType::class, [
                'class'        => Salle::class,
                'choice_label' => 'nomSalle',
                'label'        => 'Salle',
                'placeholder'  => '— Sélectionner une salle —',
                'required'     => true,
            ])

            ->add('nomCl', TextType::class, [
                'label'    => 'Nom du client',
                'required' => true,
            ])
            ->add('prenomCl', TextType::class, [
                'label'    => 'Prénom du client',
                'required' => true,
            ])

            // ✅ Fix 4 : date strictement supérieure à aujourd'hui
            ->add('dateReservation', DateType::class, [
                'label'       => 'Date de réservation',
                'widget'      => 'single_text',
                'constraints' => [
                    new GreaterThan([
                        'value'   => new \DateTime('today'),
                        'message' => 'La date de réservation doit être supérieure à la date du jour.',
                    ]),
                ],
                'attr' => [
                    'min' => (new \DateTime('tomorrow'))->format('Y-m-d'),
                ],
            ])

            ->add('heureDebutReservation', TimeType::class, [
                'label'  => 'Heure de début',
                'widget' => 'single_text',
            ])
            ->add('heureFinReservation', TimeType::class, [
                'label'  => 'Heure de fin',
                'widget' => 'single_text',
            ])
            ->add('statusReservation', ChoiceType::class, [
                'label'   => 'Statut',
                'choices' => [
                    'En attente' => 'En attente',
                    'Confirmée'  => 'Confirmée',
                    'Annulée'    => 'Annulée',
                    'Terminée'   => 'Terminée',
                ],
            ])

            // ✅ Fix 1 : motif devient une liste déroulante
            ->add('motifReservation', ChoiceType::class, [
                'label'       => 'Motif de la réservation',
                'required'    => false,
                'placeholder' => '— Sélectionner un motif —',
                'choices'     => [
                    'Consultation psychologique' => 'Consultation psychologique',
                    'Séance de coaching'         => 'Séance de coaching',
                    'Thérapie de groupe'         => 'Thérapie de groupe',
                    'Bilan psychiatrique'        => 'Bilan psychiatrique',
                    'Suivi post-traitement'      => 'Suivi post-traitement',
                    'Autre'                      => 'Autre',
                ],
            ])

            // ✅ Fix 3 : prix en lecture seule, calculé automatiquement par JS
            ->add('prixReservation', MoneyType::class, [
                'label'    => 'Prix (TND)',
                'currency' => 'TND',
                'required' => false,
                'attr'     => [
                    'readonly'    => true,
                    'id'          => 'prix_reservation',
                ],
            ])

            ->add('submit', SubmitType::class, [
                'label' => 'Enregistrer',
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['data_class' => ReservationLocal::class]);
    }
}