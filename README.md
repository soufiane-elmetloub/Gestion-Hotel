# 🏨 Smart Hotel

Solution complète de gestion hôtelière combinant :  
- Application Android (Java + Volley)  
- Backend en PHP + MySQL  
- Interface Web d’administration  

Gère les clients, chambres, réservations, tâches et revenus, avec une interface moderne et support RTL (arabe).  

---

## ✨ Fonctionnalités principales
- 🔐 Authentification : super administrateur et employés  
- 👥 Gestion : clients, chambres, réservations, tâches, revenus  
- 📊 Tableaux de bord et statistiques en temps réel  
- 🌐 APIs unifiées via HTTP/JSON  
- ⚡ Connexion fiable via une classe `Database`  
- 📱 Compatibilité réseau : émulateur `10.0.2.2` ou appareil réel via IP  

---

## 📂 Structure du projet
- `SmartHotelApp/` → Application Android  
- `backend/` → PHP + MySQL  
- `Web/` → Interface Web  

Fichiers importants :  
- `backend/config.php` → Connexion BD  
- `backend/login.php` → Connexion employé  
- `backend/super_admin_login.php` → Connexion super admin  
- `Web/index.html`, `Web/dashboard.html`, `Web/settings.html`  

---

## ✅ Prérequis
- XAMPP (Apache + MySQL)  
- Android Studio (SDK 34+)  
- Appareil Android ou émulateur  

---

## ⚙️ Installation du Backend
1. Créer une base de données : `smart-hotel`  
2. Configurer `backend/config.php` avec les identifiants BD  
3. (Optionnel) Exécuter les scripts d’initialisation (super admin, employés, tâches, revenus)  
4. Tester la connexion via un fichier de test inclus  

---

## 🌐 Interface Web
- Ouvrir : `http://localhost/Smart-Hotel/Web/index.html`  
- Connexion → Accès au tableau de bord, statistiques et paramètres  

---

## 📱 Application Android
1. Ouvrir le projet dans Android Studio  
2. Réseau :  
   - Émulateur : `10.0.2.2`  
   - Appareil réel : IP locale du PC (modifiable depuis l’app)  
3. Fichiers clés :  
   - `NetworkConfig.java`  
   - `SettingsActivity.java`, `SettingsManager.java`  

---

## 🛠️ Dépannage
- **Erreur JSON "Unexpected token '<'"** :  
  Utiliser toujours :
   ```php
    $database = new Database();
    $pdo = $database->getConnection();
    ```
   
🌐 Problèmes réseau :
  - Vérifier que le téléphone et le PC sont sur le même Wi-Fi

🔓 Cleartext traffic :
  - Activer pour les tests HTTP

🔐 Sécurité
- Utiliser HTTPS en production

- Toujours hacher les mots de passe

- Changer les identifiants par défaut avant déploiement

📜 Licence
👁️ Projet éducatif / démonstration. Peut être publié sous licence MIT, Apache-2.0, …
