# Checklist validation UI

Checklist exécutable pour valider les parcours principaux avant livraison.

## Pré-requis

- Backend lancé sur `http://localhost:8090`
- Frontend lancé sur `http://localhost:5173`
- Compte de connexion disponible (ex. bootstrap admin)

## 1. Authentification

- [ ] Connexion avec identifiants valides
- [ ] Refus clair avec identifiants invalides
- [ ] Déconnexion opérationnelle
- [ ] Navigation protégée inaccessible sans session

## 2. Patients

- [ ] Liste patients chargée
- [ ] Recherche paginée fonctionnelle
- [ ] Création patient valide
- [ ] Affichage détail patient

## 3. Admissions

- [ ] Création admission
- [ ] Fiche admission consultable
- [ ] Saisie constantes (création + lecture)
- [ ] Prescriptions (création + modification)
- [ ] Administrations médicamenteuses (création + lecture)
- [ ] Formulaire clinique (chargement + enregistrement)
- [ ] Actions métier: transfert, sortie, déclaration décès

## 4. Urgences

- [ ] Création passage urgence
- [ ] Filtres statut/priorité
- [ ] Triage (option liste + option "Autre")
- [ ] Orientation (option liste + option "Autre")
- [ ] Clôture dossier avec confirmation
- [ ] Timeline visible après actions

## 5. Consultations

- [ ] Création consultation (patient + admission)
- [ ] Filtres patient/admission
- [ ] Détail consultation
- [ ] Ajout observation
- [ ] Ajout diagnostic
- [ ] Ajout demande d’examen
- [ ] Timeline clinique patient visible

## 6. Dossiers médicaux

- [ ] Création dossier médical par patient
- [ ] Lecture dossier existant
- [ ] Mise à jour allergies
- [ ] Mise à jour antécédents
- [ ] Ajout problème
- [ ] Ajout document/note
- [ ] Historique visible et trié

## 7. Reporting

- [ ] KPI chargés (même en valeur placeholder)
- [ ] Audit affiché
- [ ] Déclenchement export activité
- [ ] Déclenchement export occupation
- [ ] Message de retour export affiché

## 8. Vérifications transverses

- [ ] Build frontend OK (`npm run build`)
- [ ] Build backend OK (`./mvnw compile`)
- [ ] Aucune erreur lint frontend
- [ ] Responsive acceptable (desktop + tablette)
- [ ] Messages d’erreur backend affichés proprement
