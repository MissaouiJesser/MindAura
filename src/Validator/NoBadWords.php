<?php

namespace App\Validator;

use Symfony\Component\Validator\Constraint;

#[\Attribute(\Attribute::TARGET_PROPERTY | \Attribute::TARGET_METHOD)]
class NoBadWords extends Constraint
{
    public string $message = 'Ce champ contient des termes inappropriés. Merci de reformuler.';
}