<?php

namespace App\Form;

use App\Entity\LocalsPsychiatrie;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Regex;

class LocalsPsychiatrieType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        // Détermine si on est en mode création (pas encore d'image stockée)
        // afin de rendre l'image obligatoire uniquement à l'ajout.
        $isNew = $options['is_new'];

        $builder
            ->add('nomLocal', TextType::class, [
                'label' => 'Nom du local',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: Cabinet du Dr. Ben Ali'],
            ])
            ->add('adresseLocal', TextType::class, [
                'label' => 'Adresse',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: 12 Rue de la République'],
            ])
            ->add('villeLocal', TextType::class, [
                'label' => 'Ville',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: Tunis'],
            ])
            // ✅ CORRECTION : ajout des contraintes de validation côté formulaire
            //    (NotBlank, Length min/max, Regex) pour le champ description.
            //    Ces contraintes s'appliquent au champ mapped=true,
            //    elles complètent (et s'assurent de) ce qui est déclaré sur l'entité.
            ->add('descriptionLocal', TextareaType::class, [
                'label'    => 'Description',
                'required' => true,
                'attr'     => [
                    'class'       => 'form-control',
                    'rows'        => 4,
                    'placeholder' => 'Décrivez le local (min. 10 caractères)...',
                    'minlength'   => 10,
                    'maxlength'   => 2000,
                ],
                'constraints' => [
                    new NotBlank(message: 'La description du local est obligatoire.'),
                    new Length([
                        'min'        => 10,
                        'max'        => 2000,
                        'minMessage' => 'La description doit contenir au moins {{ limit }} caractères.',
                        'maxMessage' => 'La description ne peut pas dépasser {{ limit }} caractères.',
                    ]),
                    new Regex([
                        'pattern' => '/^[\p{L}0-9\s\-\'\.\,\!\?\;\:\(\)\n\r]+$/u',
                        'message' => 'La description contient des caractères non autorisés.',
                    ]),
                ],
            ])
            ->add('capaciteLocal', TextType::class, [
                'label' => 'Capacité (personnes)',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: 50'],
            ])
            ->add('typeLocal', ChoiceType::class, [
                'label'   => 'Type de local',
                'choices' => [
                    'Cabinet'             => 'Cabinet',
                    'Clinique'            => 'Clinique',
                    'Hôpital'             => 'Hôpital',
                    'Centre de bien-être' => 'Centre de bien-être',
                    'Salle de thérapie'   => 'Salle de thérapie',
                    'Autre'               => 'Autre',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            ->add('telephoneLocal', IntegerType::class, [
                'label' => 'Téléphone',
                'attr'  => ['class' => 'form-control', 'placeholder' => 'Ex: 21612345'],
            ])
            ->add('emailLocal', EmailType::class, [
                'label'    => 'Email',
                'required' => false,
                'attr'     => ['class' => 'form-control', 'placeholder' => 'Ex: contact@local.tn'],
            ])
            ->add('disponibiliteLocal', ChoiceType::class, [
                'label'   => 'Disponibilité',
                'choices' => [
                    'Disponible'   => 'Disponible',
                    'Indisponible' => 'Indisponible',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            // ✅ CORRECTION : mapped=false car imageFile est un champ virtuel (non persisté en BDD).
            //    L'image est OBLIGATOIRE à la création ($isNew=true) et facultative en modification.
            //    La contrainte @Assert\File de l'entité est ignorée pour les champs mapped=false ;
            //    on la redéclare ici pour qu'elle soit bien validée par le FormValidator.
            ->add('imageFile', FileType::class, [
                'label'    => $isNew
                    ? 'Image du local (JPG, PNG, WEBP — max 2Mo) *'
                    : 'Changer l\'image (JPG, PNG, WEBP — max 2Mo)',
                'required' => $isNew,   // obligatoire uniquement à la création
                'mapped'   => false,
                'attr'     => ['class' => 'form-control', 'accept' => 'image/*'],
                'constraints' => array_filter([
                    // NotBlank uniquement lors de la création
                    $isNew ? new NotBlank(message: "L'image du local est obligatoire.") : null,
                    new File([
                        'maxSize'          => '2M',
                        'mimeTypes'        => ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
                        'maxSizeMessage'   => "L'image ne doit pas dépasser 2 Mo.",
                        'mimeTypesMessage' => "Formats acceptés : JPG, PNG, WEBP, GIF.",
                    ]),
                ]),
            ])
            ->add('submit', SubmitType::class, [
                'label' => $options['submit_label'],
                'attr'  => ['class' => 'btn btn-primary mt-3 w-100'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class'   => LocalsPsychiatrie::class,
            'submit_label' => 'Enregistrer',
            // ✅ CORRECTION : option is_new transmise depuis le contrôleur
            //    pour distinguer création (true) et modification (false).
            'is_new'       => false,
        ]);

        $resolver->setAllowedTypes('is_new', 'bool');
    }
}