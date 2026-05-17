<?php

namespace App\Form;

use App\Entity\Objectif;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ObjectifType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('titre', TextType::class, [
                'label' => 'Titre de l’objectif',
                
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'attr' => ['rows' => 3],
            ])
            ->add('type_test', ChoiceType::class, [
                'label' => 'Type de test',
                'choices' => [
                    'Personnalité' => 'personnalité',
                    'Stress' => 'stress',
                    'Logique' => 'logique',
                    'Mémoire' => 'mémoire',
                    'Autre' => 'autre',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('niveau_recommande', ChoiceType::class, [
                'label' => 'Niveau recommandé',
                'choices' => [
                    'Débutant' => 'débutant',
                    'Intermédiaire' => 'intermédiaire',
                    'Avancé' => 'avancé',
                    'Expert' => 'expert',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('score_min', IntegerType::class, [
                'label' => 'Score minimum',
                
            ])
            ->add('score_max', IntegerType::class, [
                'label' => 'Score maximum',
                
            ])
            ->add('categorie', ChoiceType::class, [
                'label' => 'Catégorie',
                
                'choices' => [
                    'Développement personnel' => 'développement personnel',
                    'Carrière' => 'carrière',
                    'Éducation' => 'éducation',
                    'Santé mentale' => 'santé mentale',
                    'Autre' => 'autre',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('duree_estimee', IntegerType::class, [
                'label' => 'Durée estimée (jours)',
                
            ])
            ->add('difficule', ChoiceType::class, [
                'label' => 'Difficulté',
                
                'choices' => [
                    'Facile' => 'facile',
                    'Moyen' => 'moyen',
                    'Difficile' => 'difficile',
                    'Très difficile' => 'très difficile',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('date_echeance', DateType::class, [
                'label' => 'Date d’échéance',
                'widget' => 'single_text',
                'input' => 'datetime_immutable',
            ])
            ->add('statut', ChoiceType::class, [
                'label' => 'Statut',
            
                'choices' => [
                    'Actif' => 'actif',
                    'Inactif' => 'inactif',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('est_public', CheckboxType::class, [
                'label' => 'Public',
                
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Objectif::class,
        ]);
    }
}
