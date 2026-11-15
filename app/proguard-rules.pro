# Models are mapped by hand from DataSnapshot (no reflection), so Firebase needs no keep rules.
# Credential Manager
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
