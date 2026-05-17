<?php
namespace App\Form;
 
use App\Entity\Evenement;
use App\Entity\Produit;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\MoneyType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;
 
class ProduitType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('description', HiddenType::class, [
                'required' => false,
            ])
            ->add('nom', TextType::class, [
                'label' => 'Nom du produit',
                'attr'  => ['placeholder' => 'Ex: Guide CBT'],
            ])
            ->add('prix', MoneyType::class, [
                'label'    => 'Prix (USD $)',
                'currency' => 'USD',
                'divisor'  => 1,
            ])
            ->add('stock', IntegerType::class, [
                'label'    => 'Stock (laisser vide = illimité)',
                'required' => false,
                'attr'     => ['min' => 0],
            ])
            ->add('estActif', CheckboxType::class, [
                'label'    => 'Produit actif (visible en boutique)',
                'required' => false,
            ])
            ->add('evenement', EntityType::class, [
                'class'        => Evenement::class,
                'choice_label' => 'titreEvenement',
                'label'        => 'Événement associé (optionnel)',
                'required'     => false,
                'placeholder'  => '— Aucun —',
            ])
            ->add('imageFile', FileType::class, [
                'label'    => 'Image du produit',
                'mapped'   => false,  // géré manuellement dans le controller
                'required' => false,
                'constraints' => [
                    new File([
                        'maxSize'  => '2048k',
                        'mimeTypes' => ['image/jpeg', 'image/png', 'image/webp'],
                        'mimeTypesMessage' => 'Format accepté: JPG, PNG, WEBP.',
                    ]),
                ],
            ]);
    }
 
    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['data_class' => Produit::class]);
    }
}