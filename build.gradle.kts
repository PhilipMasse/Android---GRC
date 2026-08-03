// Fichier de build racine : déclare les plugins utilisés par les modules,
// sans les appliquer ici (appliqués dans app/build.gradle.kts).
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
