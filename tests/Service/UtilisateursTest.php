<?php

namespace App\Tests\Service;

use App\Entity\Utilisateurs;
use App\Service\UtilisateursManager;
use PHPUnit\Framework\TestCase;

class UtilisateursTest extends TestCase
{
    public function testValidUtilisateur()
    {
        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $this->assertTrue($manager->validate($utilisateur));
    }

    public function testUtilisateurWithoutNom()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }

    public function testUtilisateurWithoutPrenom()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }

    public function testUtilisateurWithInvalidEmail()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }

    public function testUtilisateurWithShortPassword()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('azer');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }

    public function testUtilisateurWithInvalidTelephone()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('123');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-25 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }

    public function testUtilisateurTooYoung()
    {
        $this->expectException(\InvalidArgumentException::class);

        $utilisateur = new Utilisateurs();
        $utilisateur->setNomUtilisateur('Laifi');
        $utilisateur->setPrenomUtilisateur('Azer');
        $utilisateur->setEmailUtilisateur('laifiazer0@gmail.com');
        $utilisateur->setPlainPassword('Azer1234');
        $utilisateur->setTelephoneUtilisateur('29269297');
        $utilisateur->setDateNaissanceUtilisateur(new \DateTime('-10 years'));

        $manager = new UtilisateursManager();
        $manager->validate($utilisateur);
    }
}