# GRC Citoyenne — Application Android

Application Android (Kotlin, Jetpack Compose) pour le portail citoyen de la
Mairie de Berre-les-Alpes. Consomme l'API REST du plugin WordPress
[WordPress---GRC](https://github.com/PhilipMasse/WordPress---GRC)
(`/wp-json/grc/v1/`).

## État actuel — v0.3.0

Module des Signalements ajouté, en plus de l'authentification (v0.1.0) et
des démarches administratives (v0.2.x).

- ✅ Connexion (avec gestion de la double authentification, email ou TOTP)
- ✅ Inscription (captcha interne uniquement — voir limitations ci-dessous)
- ✅ Mot de passe oublié / réinitialisation
- ✅ Rafraîchissement automatique du jeton de session
- ✅ Stockage chiffré des jetons (Android Keystore)
- ✅ **Démarches administratives** : liste, choix du type, formulaire
  dynamique (texte, liste déroulante, email, nombre, date via sélecteur
  natif, téléphone avec indicatif pays), détail avec fil de messages, envoi
  de documents multiples (PDF/DOCX/JPG/PNG)
- ✅ **Signalements** : liste "Mes signalements", création avec
  géolocalisation automatique, carte interactive (Google Maps, marqueur
  déplaçable), sélection de catégorie, détection de doublons à proximité,
  envoi de plusieurs photos, détail du dossier
- ⏳ Rendez-vous : **pas encore implémenté**, prévu dans une prochaine version

## ⚠️ Important : ce projet n'a pas été compilé

Le code a été écrit dans un environnement sans SDK Android ni Gradle (pas
d'accès aux dépôts Google/Gradle nécessaires). Il a été relu avec soin et
suit les patterns Android standards, et chaque appel réseau a été vérifié
champ par champ contre le code source réel du plugin WordPress — mais
**sa première compilation doit se faire dans Android Studio**. Il est
probable (sans être certain) que des ajustements mineurs soient nécessaires
à la première ouverture du projet (versions de dépendances, wrapper Gradle
manquant, etc. — voir ci-dessous).

## Prérequis

- Android Studio (Koala ou plus récent recommandé)
- JDK 17
- Un appareil ou émulateur Android 8.0 (API 26) ou supérieur

## Premier lancement

1. **Ouvrir le projet** dans Android Studio (`File → Open`, sélectionner ce dossier).
2. **Régénérer le wrapper Gradle** si Android Studio ne le fait pas automatiquement :
   ```bash
   gradle wrapper --gradle-version 8.7
   ```
   (le fichier `gradle/wrapper/gradle-wrapper.properties` est déjà présent et pointe
   vers Gradle 8.7 ; seul le binaire `gradle-wrapper.jar` doit être généré/téléchargé,
   ce que cet environnement de développement ne pouvait pas faire.)
3. **Synchroniser le projet** (Android Studio le proposera automatiquement).
4. **Lancer** sur un émulateur ou un appareil connecté.

## Configuration de la clé Google Maps

Requiert une clé API Google Maps (Maps SDK for Android activé sur Google
Cloud Console), placée dans `local.properties` — **jamais committée** :

```properties
MAPS_API_KEY=votre_clé_ici
```

Sans cette clé, l'écran "Nouveau signalement" se lance normalement mais la
carte n'affiche rien (zone grise vide).

## Configuration de l'URL de l'API

L'URL de base est définie dans `app/build.gradle.kts` :

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://test3.berrelesalpes.fr/wp-json/grc/v1/\"")
```

- **Build debug** : pointe par défaut vers l'environnement de test
  (`test3.berrelesalpes.fr`).
- **Build release** : pointe vers un domaine de production à ajuster avant
  toute publication (actuellement une valeur d'exemple dans le bloc
  `release` — à corriger avec le vrai domaine de la mairie).

## Architecture

```
app/src/main/java/fr/berrelesalpes/grc/
├── GrcApplication.kt          Point d'assemblage des dépendances (DI manuelle)
├── MainActivity.kt
├── data/
│   ├── model/                 Classes de données (Moshi), miroir exact de l'API REST
│   ├── network/                Retrofit, intercepteurs, rafraîchissement de jeton
│   ├── local/                  Stockage chiffré des jetons (TokenManager)
│   └── repository/             AuthRepository — logique métier d'authentification
└── ui/
    ├── auth/                   Écrans + ViewModels : connexion, inscription, 2FA, mot de passe oublié
    ├── home/                   Écran d'accueil post-connexion
    ├── navigation/              Graphe de navigation Compose
    ├── theme/                   Couleurs/typographie (charte du site)
    └── common/                  Composants réutilisables
```

Pas de framework d'injection de dépendances (Hilt/Koin) pour ce premier lot :
l'assemblage se fait "à la main" dans `GrcApplication`. À reconsidérer si le
projet grossit significativement dans les prochains lots.

## Limitations connues

- **Captcha** : seul le captcha interne (mathématique) du plugin est géré.
  Si la mairie a activé Cloudflare Turnstile, Google reCAPTCHA ou hCaptcha
  côté réglages du plugin (`Réglages → Anti-robot`), l'inscription mobile
  échouera tant que le SDK correspondant n'aura pas été intégré.
- **Lien de réinitialisation de mot de passe** : l'écran existe et fonctionne
  si on lui fournit le jeton, mais **aucun App Link n'est configuré** pour
  intercepter automatiquement le lien reçu par email et ouvrir l'application.
  Cela nécessite de configurer `assetlinks.json` sur le domaine du site et un
  `intent-filter` dans le manifeste — à faire dans un prochain lot.
- **Taille des documents** : aucune vérification de taille n'est faite côté
  application avant envoi (le serveur applique sa propre limite de 8 Mo par
  fichier et refuse proprement les fichiers trop volumineux, mais l'appli ne
  prévient pas l'utilisateur avant l'envoi).
- **Signalements** : le détail d'un signalement est reconstruit à partir de
  la liste complète (GET /mes-demandes), la route de détail individuelle
  n'étant accessible qu'aux agents (session WordPress classique). Sans
  conséquence pratique : la liste renvoie déjà toutes les informations
  utiles par signalement.
- **Rendez-vous** : pas encore implémenté.

## Correspondance avec l'API

Chaque route consommée est documentée dans `GrcApiService.kt`, avec un
renvoi vers le fichier PHP source faisant foi
(`includes/rest/class-grc-rest-citoyen.php` du plugin WordPress). En cas de
divergence constatée entre le comportement réel de l'API et le code de cette
application, le plugin WordPress fait foi.
