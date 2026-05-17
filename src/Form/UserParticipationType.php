<?php

namespace App\Form;

use App\Entity\Participation;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\TelType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\Regex;

class UserParticipationType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            // ── Nom : pré-rempli + readonly ──
            ->add('nom', TextType::class, [
                'label' => false,
                'data'  => $options['user_nom'],
                'attr'  => [
                    'class'     => 'form-control',
                    'readonly'  => true,
                    'title'     => 'Récupéré depuis votre profil',
                ],
            ])

            // ── Prénom : pré-rempli + readonly ──
            ->add('prenom', TextType::class, [
                'label' => false,
                'data'  => $options['user_prenom'],
                'attr'  => [
                    'class'    => 'form-control',
                    'readonly' => true,
                    'title'    => 'Récupéré depuis votre profil',
                ],
            ])

            // ── Email : pré-rempli + readonly + non mappé ──
            ->add('email', EmailType::class, [
                'label'  => false,
                'mapped' => false,
                'data'   => $options['user_email'],
                'attr'   => [
                    'class'    => 'form-control',
                    'readonly' => true,
                    'title'    => 'Récupéré depuis votre profil',
                ],
            ])

            // ── Téléphone : pré-rempli depuis profil, modifiable ──
            ->add('telephone', TelType::class, [
                'label'    => false,
                'required' => false,
                'data'     => $options['user_tel'],
                'constraints' => [
                    new Regex([
                        'pattern' => '/^[+\d\s\-()]{6,20}$/',
                        'message' => 'Numéro de téléphone invalide.',
                    ]),
                ],
                'attr' => [
                    'class'       => 'form-control',
                    'placeholder' => '+216 XX XXX XXX',
                ],
            ])

            // ── Champs gérés par le contrôleur ──
            ->add('dateInscription', HiddenType::class, ['mapped' => false, 'required' => false])
            ->add('codeQr',          HiddenType::class, ['mapped' => false, 'required' => false])
            ->add('statut',          HiddenType::class, ['mapped' => false, 'required' => false])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Participation::class,
            'user_email'  => '',
            'user_nom'    => '',
            'user_prenom' => '',
            'user_tel'    => '',
        ]);

        $resolver->setAllowedTypes('user_email',  'string');
        $resolver->setAllowedTypes('user_nom',    'string');
        $resolver->setAllowedTypes('user_prenom', 'string');
        $resolver->setAllowedTypes('user_tel',    'string');
    }
}