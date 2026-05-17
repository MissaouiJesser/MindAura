<?php

namespace App\Form;

use App\Entity\LocalsPsychiatrie;
use App\Entity\Salle;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Choice;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class SalleType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        // Détermine si on est en mode création pour rendre l'image obligatoire.
        $isNew = $options['is_new'];

        $builder
            ->add('local', EntityType::class, [
                'class'        => LocalsPsychiatrie::class,
                'choice_label' => 'nomLocal',
                'label'        => 'Local psychiatrique',
                'placeholder'  => '-- Choisir un local --',
                'required'     => false,
                'attr'         => ['class' => 'form-select'],
            ])
            ->add('nomSalle', TextType::class, [
                'label' => 'Nom de la salle',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: Salle Bleue'],
            ])
            ->add('typeSalle', ChoiceType::class, [
                'label'   => 'Type de salle',
                'choices' => [
                    'Salle de thérapie'     => 'Salle de thérapie',
                    'Salle de consultation' => 'Salle de consultation',
                    'Salle de groupe'       => 'Salle de groupe',
                    'Salle de relaxation'   => 'Salle de relaxation',
                    "Salle d'attente"       => "Salle d'attente",
                    'Autre'                 => 'Autre',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            ->add('capaciteSalle', TextType::class, [
                'label' => 'Capacité (personnes)',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: 20'],
            ])
            // ✅ CORRECTION : ajout des contraintes de validation côté formulaire
            //    pour le champ Équipements (NotBlank, Length, Regex).
            //    Le champ passe de required=false à required=true.
            ->add('equipements', TextareaType::class, [
                'label'    => 'Équipements',
                'required' => true,
                'attr'     => [
                    'class'       => 'form-control',
                    'rows'        => 3,
                    'placeholder' => 'Ex: Projecteur, climatisation, tableau blanc...',
                    'minlength'   => 3,
                    'maxlength'   => 2000,
                ],
                'constraints' => [
                    new NotBlank(message: 'Veuillez renseigner les équipements de la salle.'),
                    new Length([
                        'min'        => 3,
                        'max'        => 2000,
                        'minMessage' => 'Les équipements doivent contenir au moins {{ limit }} caractères.',
                        'maxMessage' => 'Les équipements ne peuvent pas dépasser {{ limit }} caractères.',
                    ]),
                    new Regex([
                        'pattern' => '/^[\p{L}0-9\s\-\'\.\,\!\?\;\:\(\)\n\r\/]+$/u',
                        'message' => 'Les équipements contiennent des caractères non autorisés.',
                    ]),
                ],
            ])
            // ✅ CORRECTION : ajout des contraintes de validation pour Disponibilité.
            //    Pour les champs mapped=true de type ChoiceType, les contraintes de
            //    l'entité sont normalement validées, MAIS on les ajoute aussi dans
            //    le form pour une validation explicite et cohérente avec les autres champs.
            ->add('disponibiliteSalle', ChoiceType::class, [
                'label'   => 'Disponibilité',
                'choices' => [
                    'Disponible'     => 'Disponible',
                    'Indisponible'   => 'Indisponible',
                    'En maintenance' => 'En maintenance',
                ],
                'attr' => ['class' => 'form-select'],
                'constraints' => [
                    new NotBlank(message: 'La disponibilité est obligatoire.'),
                    new Choice([
                        'choices' => ['Disponible', 'Indisponible', 'En maintenance'],
                        'message' => 'Veuillez choisir une valeur valide.',
                    ]),
                ],
            ])
            ->add('etage', ChoiceType::class, [
                'label'       => 'Étage',
                'required'    => false,
                'placeholder' => '-- Choisir un étage --',
                'choices'     => [
                    'RDC'        => 'RDC',
                    '1er étage'  => '1er étage',
                    '2ème étage' => '2ème étage',
                    '3ème étage' => '3ème étage',
                    '4ème étage' => '4ème étage',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            // ✅ CORRECTION : image obligatoire à la création, facultative en modification.
            //    Comme imageFile est mapped=false, la contrainte @Assert\File de l'entité
            //    n'est pas déclenchée par Symfony — on la redéclare ici explicitement.
            ->add('imageFile', FileType::class, [
                'label'    => $isNew
                    ? 'Image de la salle (JPG, PNG, WEBP — max 2Mo) *'
                    : "Changer l'image (JPG, PNG, WEBP — max 2Mo)",
                'required' => $isNew,
                'mapped'   => false,
                'attr'     => ['class' => 'form-control', 'accept' => 'image/*'],
                'constraints' => array_filter([
                    // NotBlank uniquement lors de la création
                    $isNew ? new NotBlank(message: "L'image de la salle est obligatoire.") : null,
                    new File([
                        'maxSize'          => '2M',
                        'mimeTypes'        => ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
                        'maxSizeMessage'   => "L'image ne doit pas dépasser 2 Mo.",
                        'mimeTypesMessage' => "Formats acceptés : JPG, PNG, WEBP, GIF.",
                    ]),
                ]),
            ])
            // ✅ CORRECTION : ajout des contraintes de validation pour Statut.
            //    Même logique que Disponibilité : NotBlank + Choice dans le form
            //    pour garantir la cohérence de validation.
            ->add('statutSalle', ChoiceType::class, [
                'label'   => 'Statut',
                'choices' => [
                    'Active'        => 'Active',
                    'Inactive'      => 'Inactive',
                    'En rénovation' => 'En rénovation',
                ],
                'attr' => ['class' => 'form-select'],
                'constraints' => [
                    new NotBlank(message: 'Le statut est obligatoire.'),
                    new Choice([
                        'choices' => ['Active', 'Inactive', 'En rénovation'],
                        'message' => 'Veuillez choisir un statut valide.',
                    ]),
                ],
            ])
            ->add('submit', SubmitType::class, [
                'label' => $options['submit_label'],
                'attr'  => ['class' => 'btn btn-primary mt-3 w-100'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class'   => Salle::class,
            'submit_label' => 'Enregistrer',
            // ✅ CORRECTION : option is_new pour distinguer création et modification.
            'is_new'       => false,
        ]);

        $resolver->setAllowedTypes('is_new', 'bool');
    }
}