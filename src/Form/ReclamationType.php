<?php

namespace App\Form;

use App\Entity\Reclamation;
use App\Entity\Categorie;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ReclamationType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $context = $options['context'];

        $builder
            ->add('categorie', EntityType::class, [
                'label' => 'Catégorie',
                'class' => Categorie::class,
                'choice_label' => 'nom_categorie',
                'placeholder' => 'Sélectionnez une catégorie',
                'attr' => ['class' => 'form-control'],
                'required' => true,
            ])
            ->add('sujet_reclamation', TextType::class, [
                'label' => 'Sujet de la réclamation',
                'attr' => [
                    'class' => 'form-control',
                    'placeholder' => 'Sujet de votre réclamation',
                    'maxlength' => 255,
                ],
                'required' => true,
            ])
            ->add('description_reclamation', TextareaType::class, [
                'label' => 'Description détaillée',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 5,
                    'placeholder' => 'Décrivez en détail votre réclamation...',
                    'maxlength' => 5000,
                ],
                'required' => true,
            ])
        ;

        if (\in_array($context, ['admin_new', 'admin_edit'], true)) {
            $builder
                ->add('statut_reclamation', ChoiceType::class, [
                    'label' => 'Statut',
                    'choices' => [
                        'En attente' => 'En attente',
                        'En cours de traitement' => 'En cours de traitement',
                        'Résolu' => 'Résolu',
                        'Rejeté' => 'Rejeté',
                    ],
                    'attr' => ['class' => 'form-control'],
                    'required' => true,
                ])
                ->add('id_utilisateur', IntegerType::class, [
                    'label' => 'ID Utilisateur',
                    'attr' => [
                        'class' => 'form-control',
                        'min' => 1,
                    ],
                    'required' => true,
                ])
                ->add('rate_Reclamation', NumberType::class, [
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
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Reclamation::class,
            'context' => 'admin_new',
        ]);
        $resolver->setAllowedValues('context', ['admin_new', 'admin_edit', 'front_new', 'front_edit']);
    }
}
