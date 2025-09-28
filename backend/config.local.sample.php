<?php
// config.local.sample.php
// Copiez ce fichier vers config.local.php et renseignez vos vraies informations de base de données ici.
// Remarque : ce fichier est ignoré par Git (voir backend/.gitignore).

// Définir les constantes suivantes surchargera les valeurs dans config.php.
// Ne publiez jamais votre config.local.php sur GitHub.

define('DB_HOST', 'localhost');          // Exemple : localhost ou 127.0.0.1

define('DB_USER', 'votre_utilisateur');  // Nom d'utilisateur de la base de données

define('DB_PASS', 'votre_mot_de_passe'); // Mot de passe de la base de données

define('DB_NAME', 'smart-hotel');        // Nom de la base de données

// Aucune autre logique n'est nécessaire ici. Uniquement les définitions ci-dessus.
