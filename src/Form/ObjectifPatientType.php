<?php

namespace App\Form;

use App\Entity\Objectif;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ObjectifPatientType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('titre', TextType::class, [
                'label' => 'Titre de votre objectif',
                'attr'  => ['placeholder' => 'Ex: Réduire mon stress quotidien'],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'attr'  => ['rows' => 3, 'placeholder' => 'Décrivez votre objectif...'],
            ])
            ->add('type_objectif', ChoiceType::class, [
                'label'       => "Type d'objectif",
                'choices'     => [
                    'Personnel'     => 'personnel',
                    'Professionnel' => 'professionnel',
                    'Éducatif'      => 'éducatif',
                    'Santé'         => 'santé',
                    'Autre'         => 'autre',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('categorie', ChoiceType::class, [
                'label'       => 'Catégorie',
                'choices'     => [
                    'Développement personnel' => 'développement personnel',
                    'Carrière'                => 'carrière',
                    'Éducation'               => 'éducation',
                    'Santé mentale'           => 'santé mentale',
                    'Autre'                   => 'autre',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('duree_estimee', IntegerType::class, [
                'label' => 'Durée estimée (jours)',
                'attr'  => ['placeholder' => '30'],
            ])
            ->add('date_echeance', DateType::class, [
                'label'  => "Date d'échéance",
                'widget' => 'single_text',
                'input'  => 'datetime_immutable',
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
