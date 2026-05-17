package tn.esprit.utils;

import tn.esprit.entities.local_psychiatrie;

/**
 * Classe pour afficher les locaux dans une ComboBox de manière lisible
 * Permet une représentation visuelle claire du local sélectionné
 */
public class LocalComboItem {

    private local_psychiatrie local;

    public LocalComboItem(local_psychiatrie local) {
        this.local = local;
    }

    public local_psychiatrie getLocal() {
        return local;
    }

    public int getIdLocal() {
        return local.getId_local();
    }

    /**
     * Affichage dans la ComboBox
     * Format: 🏥 Nom - Ville (Type) - Capacité: XX personnes
     */
    @Override
    public String toString() {
        return String.format("🏥 %s - %s (%s) - Capacité: %s personnes",
                local.getNom_local(),
                local.getVille_local(),
                local.getType_local().getLibelle(),
                local.getCapacite_local());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LocalComboItem that = (LocalComboItem) obj;
        return local.getId_local() == that.local.getId_local();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(local.getId_local());
    }
}