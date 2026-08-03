# Règles ProGuard/R8 spécifiques au projet.
# Voir https://developer.android.com/studio/build/shrinker-r8 pour la référence.

# Moshi génère des adaptateurs par réflexion sur les classes annotées
# @JsonClass ; on les protège pour éviter que R8 ne les renomme/supprime.
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class fr.berrelesalpes.grc.data.model.** { *; }
