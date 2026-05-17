<?php
namespace App\Form;

use App\Entity\Evenement;
use App\Entity\TypeEvenement;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;

class EvenementType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('identifiantEvenemnt', TextType::class, [
                'label' => 'Identifiant',
                'attr'  => [
                    'class'       => 'form-control',
                    'placeholder' => 'EVT-2025-001',
                ],
            ])
            ->add('titreEvenement', TextType::class, [
                'label' => 'Titre',
                'attr'  => ['class' => 'form-control'],
            ])
            ->add('descriptionEvenement', TextareaType::class, [
                'label'    => 'Description',
                'required' => false,
                'attr'     => ['class' => 'form-control', 'rows' => 4],
            ])
            ->add('datedebutEvenemnt', DateType::class, [
                'label'  => 'Date de début',
                'widget' => 'single_text',
                'attr'   => ['class' => 'form-control'],
            ])
            ->add('datefinEvenemnt', DateType::class, [
                'label'  => 'Date de fin',
                'widget' => 'single_text',
                'attr'   => ['class' => 'form-control'],
            ])
            ->add('lieuEvenement', TextType::class, [
                'label' => 'Lieu',
                'attr'  => ['class' => 'form-control'],
            ])
            ->add('capaciteEvenement', IntegerType::class, [
                'label' => 'Capacité',
                'attr'  => ['class' => 'form-control'],
            ])
            ->add('maxListeAttente', IntegerType::class, [
                'label'    => "Taille max. de la liste d'attente",
                'required' => false,
                'attr'     => [
                    'class'       => 'form-control',
                    'placeholder' => '0 = désactivée, vide = illimitée',
                    'min'         => 0,
                ],
                'help' => "Laissez vide pour une liste d'attente illimitée, ou entrez 0 pour la désactiver.",
            ])
            ->add('typeEvenement', EntityType::class, [
                'class'        => TypeEvenement::class,
                'choice_label' => function (TypeEvenement $t) {
                    return $t->getLibelle()
                        . ' (' . $t->getModalite() . ') — '
                        . ($t->isEstGratuit() ? 'Gratuit' : 'Payant');
                },
                'label' => "Type d'événement",
                'attr'  => ['class' => 'form-select'],
            ])
            // ⚠️ imageFile reste dans le form car mapped: false (non lié à l'entité)
            ->add('imageFile', FileType::class, [
                'label'       => 'Image',
                'mapped'      => false,
                'required'    => false,
                'attr'        => ['class' => 'form-control', 'accept' => 'image/*'],
                'constraints' => [
                    new File([
                        'maxSize'          => '2M',
                        'mimeTypes'        => ['image/jpeg', 'image/png', 'image/webp'],
                        'mimeTypesMessage' => 'Format accepté : JPG, PNG, WebP.',
                        'maxSizeMessage'   => "L'image ne doit pas dépasser 2 Mo.",
                    ]),
                ],
            ])
            ->add('save', SubmitType::class, [
                'label' => 'Enregistrer',
                'attr'  => ['class' => 'btn btn-success mt-3'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['data_class' => Evenement::class]);
    }
}