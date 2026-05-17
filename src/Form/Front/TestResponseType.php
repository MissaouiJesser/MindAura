<?php

namespace App\Form\Front;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\SubmitType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class TestResponseType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $questions = $options['questions'];

        foreach ($questions as $question) {
            $id = $question->getIdQuestionReponse();
            $name = 'question_' . $id;
            $required = (bool) $question->isEstObligatoire();

            if ($question->getTypeQuestion() === 'texte_libre') {
                $builder->add($name, TextareaType::class, [
                    'label' => $question->getTexteQuestion(),
                    'required' => $required,
                    'attr' => [
                        'rows' => 4,
                        'class' => 'take-textarea',
                        'placeholder' => 'Votre réponse…',
                    ],
                ]);

                continue;
            }

            $rawOptions = $question->getOptions();
            if ($rawOptions === []) {
                $builder->add($name, TextareaType::class, [
                    'label' => $question->getTexteQuestion(),
                    'required' => $required,
                    'attr' => [
                        'rows' => 3,
                        'class' => 'take-textarea',
                        'placeholder' => 'Aucune option définie — réponse libre',
                    ],
                ]);

                continue;
            }

            $choices = [];
            foreach ($rawOptions as $index => $label) {
                $display = trim((string) $label);
                if ($display === '') {
                    $display = 'Option ' . ((int) $index + 1);
                }
                $key = $display;
                $n = 0;
                while (\array_key_exists($key, $choices)) {
                    $key = $display . ' (' . (++$n) . ')';
                }
                $choices[$key] = $index;
            }

            $builder->add(
                $name,
                ChoiceType::class,
                [
                    'label' => $question->getTexteQuestion(),
                    'choices' => $choices,
                    'expanded' => true,
                    'multiple' => false,
                    'required' => $required,
                ]
            );
        }

        $builder->add('submit', SubmitType::class, [
            'label' => 'Soumettre le test',
        ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'questions' => [],
        ]);

        $resolver->setAllowedTypes('questions', 'iterable');
    }
}

