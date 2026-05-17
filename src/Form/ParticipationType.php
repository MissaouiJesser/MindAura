<?php
// src/Form/ParticipationType.php

namespace App\Form;

use App\Entity\Evenement;
use App\Entity\Participation;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\TelType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Validator\Constraints\NotBlank;

class ParticipationType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label'       => false,
                'constraints' => [new NotBlank(['message' => 'Le nom est obligatoire.'])],
                'attr'        => ['class' => 'form-control', 'placeholder' => 'Votre nom'],
            ])
            ->add('prenom', TextType::class, [
                'label'       => false,
                'constraints' => [new NotBlank(['message' => 'Le prénom est obligatoire.'])],
                'attr'        => ['class' => 'form-control', 'placeholder' => 'Votre prénom'],
            ])
            ->add('email', EmailType::class, [
                'label'       => false,
                'constraints' => [
                    new NotBlank(['message' => 'L\'email est obligatoire.']),
                    new Email(['message' => 'Adresse email invalide.']),
                ],
                'attr'        => ['class' => 'form-control', 'placeholder' => 'exemple@email.com'],
            ])
            ->add('telephone', TelType::class, [
                'label'    => false,
                'required' => false,
                'attr'     => ['class' => 'form-control', 'placeholder' => '+216 XX XXX XXX'],
            ])

            ->add('evenement', EntityType::class, [
                'label'        => false,
                'class'        => Evenement::class,
                'choice_label' => 'titreEvenement',
                'placeholder'  => '-- Sélectionnez un événement --',
                'constraints'  => [new NotBlank(['message' => 'Veuillez choisir un événement.'])],
                'attr'         => ['class' => 'form-select'],
                'query_builder' => function (\Doctrine\ORM\EntityRepository $er) {
                    return $er->createQueryBuilder('e')
                        ->orderBy('e.datedebutEvenemnt', 'ASC')
                        ->setMaxResults(99); // ✅ Correction : limite pour éviter ORDER BY sans LIMIT
                },
            ])

            ->add('dateInscription', HiddenType::class, ['mapped' => false, 'required' => false])
            ->add('codeQr',          HiddenType::class, ['mapped' => false, 'required' => false])
            ->add('statut',          HiddenType::class, ['mapped' => false, 'required' => false])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Participation::class,
        ]);
    }
}