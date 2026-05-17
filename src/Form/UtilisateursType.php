<?php

namespace App\Form;

use App\Entity\Utilisateurs;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\Extension\Core\Type\TelType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;

/**
 * Formulaire de création / édition d'un Utilisateur.
 *
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  POLITIQUE DE VALIDATION                                        ║
 * ║  Aucune contrainte n'est redéfinie ici.                         ║
 * ║  Toutes les règles métier (NotBlank, Length, Regex, Email…)     ║
 * ║  sont portées par les annotations #[Assert\*] de l'entité       ║
 * ║  Utilisateurs — c'est l'unique source de vérité.                ║
 * ║                                                                  ║
 * ║  Exception : le champ `photoProfil` est non mappé               ║
 * ║  (pas de propriété dans l'entité), donc sa contrainte File      ║
 * ║  est déclarée ici uniquement.                                   ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Options disponibles :
 *   - is_edit (bool, défaut false) : passe le formulaire en mode édition.
 *     En mode édition le mot de passe est optionnel.
 */
class UtilisateursType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $isEdit = $options['is_edit'];

        // ── Champs mappés ────────────────────────────────────────────────────
        // Leurs contraintes de validation viennent EXCLUSIVEMENT des annotations
        // #[Assert\*] définies dans l'entité Utilisateurs.
        // ─────────────────────────────────────────────────────────────────────

        $builder
            ->add('prenomUtilisateur', TextType::class, [
                'label' => 'Prénom',
                'attr'  => ['placeholder' => 'Votre prénom'],
            ])

            ->add('nomUtilisateur', TextType::class, [
                'label' => 'Nom',
                'attr'  => ['placeholder' => 'Votre nom'],
            ])

            ->add('emailUtilisateur', EmailType::class, [
                'label' => 'Adresse e-mail',
                'attr'  => ['placeholder' => 'exemple@email.com'],
            ])

            ->add('telephoneUtilisateur', TelType::class, [
                'label' => 'Téléphone',
                'attr'  => ['placeholder' => '+216 XX XXX XXX'],
            ])

            ->add('dateNaissanceUtilisateur', DateType::class, [
                'label'    => 'Date de naissance',
                'widget'   => 'single_text',
                'html5'    => true,
                // required=false pour autoriser null lors du data-binding ;
                // la contrainte #[Assert\NotNull] de l'entité rejette quand même
                // une date vide à la validation.
                'required' => false,
            ])

            ->add('roleUtilisateur', ChoiceType::class, [
                'label'       => 'Rôle',
                'placeholder' => '— Sélectionner un rôle —',
                'choices'     => [
                    'Patient'          => 'ROLE_PATIENT',
                    'Psychologue'      => 'ROLE_PSYCHOLOGUE',
                    'Coach'            => 'ROLE_COACH',
                    'Administrateur'   => 'ROLE_ADMIN',
                    'Utilisateur'      => 'ROLE_USER',
                ],
            ])

            // ── Photo de profil ──────────────────────────────────────────────
            // Champ non mappé (pas de propriété dans l'entité).
            // La contrainte File est la seule règle définie dans le formulaire.
            ->add('photoProfil', FileType::class, [
                'label'       => 'Photo de profil',
                'mapped'      => false,
                'required'    => false,
                'constraints' => [
                    new File([
                        'maxSize'          => '2M',
                        'mimeTypes'        => ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
                        'mimeTypesMessage' => 'Veuillez uploader une image valide (JPG, PNG, WEBP, GIF).',
                    ]),
                ],
            ])

            // ── Mot de passe ─────────────────────────────────────────────────
            // Champ non mappé (plainPassword est virtuel, non persisté).
            // Les contraintes sont héritées de l'entité via validation_groups ;
            // aucune règle n'est redéfinie ici.
            //
            //  • Création (is_edit=false) : champ obligatoire.
            //  • Édition  (is_edit=true)  : champ optionnel ; si laissé vide
            //    le contrôleur ignore le hachage.
            ->add('plainPassword', PasswordType::class, [
                'label'    => $isEdit ? 'Nouveau mot de passe' : 'Mot de passe',
                'mapped'   => false,
                'required' => !$isEdit,
                'attr'     => [
                    'placeholder' => $isEdit
                        ? 'Laisser vide pour ne pas modifier'
                        : 'Min. 8 car., 1 majuscule, 1 chiffre',
                    'autocomplete' => 'new-password',
                ],
            ])

            ->add('plainPasswordConfirm', PasswordType::class, [
                'label'    => $isEdit ? 'Confirmer le nouveau mot de passe' : 'Confirmer le mot de passe',
                'mapped'   => false,
                'required' => !$isEdit,
                'attr'     => [
                    'placeholder'  => 'Répéter le mot de passe',
                    'autocomplete' => 'new-password',
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Utilisateurs::class,
            'is_edit'    => false,
        ]);

        $resolver->setAllowedTypes('is_edit', 'bool');
    }
}