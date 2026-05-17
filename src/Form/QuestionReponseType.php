<?php

namespace App\Form;

use App\Entity\QuestionReponse;
use App\Entity\TestPsychologique;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class QuestionReponseType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        if (!$options['test_locked']) {
            $builder->add('testPsychologique', EntityType::class, [
                'class'        => TestPsychologique::class,
                'choice_label' => 'titre_test',
                'label'        => 'Test psychologique',
                'placeholder'  => 'Choisir un test',
                'required'     => true,
            ]);
        }

        $builder
            ->add('texte_question', TextareaType::class, [
                'label'    => 'Question',
                'required' => false,
                'attr'     => ['rows' => 3],
            ])
            ->add('type_question', ChoiceType::class, [
                'label'    => 'Type de question',
                'required' => false,
                'choices'  => [
                    'Texte libre'   => 'texte_libre',
                    'Choix unique'  => 'choix_unique',
                    'Choix multiple'=> 'choix_multiple',
                    'Vrai / Faux'   => 'vrai_faux',
                    'Échelle'       => 'echelle',
                ],
                'placeholder' => 'Sélectionnez',
            ])
            ->add('ordre_question', IntegerType::class, [
                'label'    => 'Ordre',
                'required' => false,
            ])
            ->add('option1', TextType::class,    ['label' => 'Option 1', 'required' => false])
            ->add('score1',  IntegerType::class, ['label' => 'Score 1',  'required' => false])
            ->add('option2', TextType::class,    ['label' => 'Option 2', 'required' => false])
            ->add('score2',  IntegerType::class, ['label' => 'Score 2',  'required' => false])
            ->add('option3', TextType::class,    ['label' => 'Option 3', 'required' => false])
            ->add('score3',  IntegerType::class, ['label' => 'Score 3',  'required' => false])
            ->add('option4', TextType::class,    ['label' => 'Option 4', 'required' => false])
            ->add('score4',  IntegerType::class, ['label' => 'Score 4',  'required' => false])
            ->add('option5', TextType::class,    ['label' => 'Option 5', 'required' => false])
            ->add('score5',  IntegerType::class, ['label' => 'Score 5',  'required' => false])
            ->add('est_obligatoire', CheckboxType::class, [
                'label'    => 'Obligatoire',
                'required' => false,
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class'   => QuestionReponse::class,
            'test_locked'  => false,
        ]);
        $resolver->setAllowedTypes('test_locked', 'bool');
    }
}