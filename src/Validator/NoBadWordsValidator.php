<?php

namespace App\Validator;

use App\Service\BadWordsFilter;
use Symfony\Component\Validator\Constraint;
use Symfony\Component\Validator\ConstraintValidator;
use Symfony\Component\Validator\Exception\UnexpectedTypeException;

class NoBadWordsValidator extends ConstraintValidator
{
    public function __construct(private BadWordsFilter $filter) {}

    public function validate(mixed $value, Constraint $constraint): void
    {
        if (!$constraint instanceof NoBadWords) {
            throw new UnexpectedTypeException($constraint, NoBadWords::class);
        }

        if (null === $value || '' === $value) {
            return; // Les contraintes NotBlank s'en occupent
        }

        if ($this->filter->containsBadWords((string) $value)) {
            $this->context
                ->buildViolation($constraint->message)
                ->addViolation();
        }
    }
}