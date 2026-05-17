<?php

namespace App\Form;

use App\Entity\TestPsychologique;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class TestPsychologiqueType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('titre_test', TextType::class, [
                'label' => 'Titre du test',
    
                'attr' => ['placeholder' => 'Ex: Test de personnalité']
            ])
            ->add('description_test', TextareaType::class, [
                'label' => 'Description',
                
                'attr' => ['rows' => 3, 'placeholder' => 'Description détaillée du test']
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
            ->add('duree_estimee', IntegerType::class, [
                'label' => 'Durée estimée (minutes)',
                
                'attr' => ['placeholder' => '10']
            ])
            ->add('instructions_test', TextareaType::class, [
                'label' => 'Instructions',
                
                'attr' => ['rows' => 3, 'placeholder' => 'Instructions à afficher avant le test']
            ])
            ->add('est_actif', CheckboxType::class, [
                'label' => 'Actif',
                
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => TestPsychologique::class,
        ]);
    }
}
