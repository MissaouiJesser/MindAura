package tn.esprit.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

/**
 * Utilitaire de pagination pour TableView et ListView.
 * Affiche PAGE_SIZE éléments par page avec contrôles Précédent / Suivant.
 */
public class PaginationHelper {

    public static final int PAGE_SIZE = 5;

    /**
     * Retourne la sous-liste des éléments pour la page donnée.
     */
    public static <T> List<T> getPageItems(List<T> fullList, int page) {
        if (fullList == null || fullList.isEmpty()) return List.of();
        int from = page * PAGE_SIZE;
        if (from >= fullList.size()) return List.of();
        int to = Math.min(from + PAGE_SIZE, fullList.size());
        return fullList.subList(from, to);
    }

    /**
     * Retourne le nombre total de pages.
     */
    public static int getTotalPages(int totalItems) {
        if (totalItems <= 0) return 1;
        return (int) Math.ceil((double) totalItems / PAGE_SIZE);
    }

    /**
     * Crée une barre de pagination (HBox) avec Précédent, label "Page X / Y", Suivant.
     * Les callbacks sont appelés quand on change de page.
     */
    public static HBox createPaginationBar(
            int currentPage,
            int totalPages,
            int totalItems,
            Runnable onPrev,
            Runnable onNext
    ) {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12, 15, 12, 15));
        bar.getStyleClass().add("pagination-bar");

        Button btnPrev = new Button("◀ Précédent");
        btnPrev.getStyleClass().add("btn-neutral");
        btnPrev.setDisable(currentPage <= 0);

        int from = currentPage * PAGE_SIZE + 1;
        int to = Math.min((currentPage + 1) * PAGE_SIZE, totalItems);
        String rangeText = totalItems == 0 ? "0 élément" : from + "-" + to + " / " + totalItems;
        Label lblPage = new Label("Page " + (currentPage + 1) + " / " + Math.max(1, totalPages) + "  (" + rangeText + ")");
        lblPage.getStyleClass().add("pagination-label");

        Button btnNext = new Button("Suivant ▶");
        btnNext.getStyleClass().add("btn-neutral");
        btnNext.setDisable(currentPage >= totalPages - 1 || totalPages <= 1);

        btnPrev.setOnAction(e -> {
            if (currentPage > 0) onPrev.run();
        });
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages - 1) onNext.run();
        });

        bar.getChildren().addAll(btnPrev, lblPage, btnNext);
        return bar;
    }

    /**
     * Version qui retourne les boutons et le label pour permettre de les mettre à jour.
     */
    public static PaginationBar createPaginationBarWithRef(
            int currentPage,
            int totalPages,
            int totalItems,
            Runnable onPrev,
            Runnable onNext
    ) {
        HBox bar = new HBox(15);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(12, 15, 12, 15));
        bar.getStyleClass().add("pagination-bar");

        Button btnPrev = new Button("◀ Précédent");
        btnPrev.getStyleClass().add("btn-neutral");
        btnPrev.setDisable(currentPage <= 0);

        int from = totalItems == 0 ? 0 : currentPage * PAGE_SIZE + 1;
        int to = Math.min((currentPage + 1) * PAGE_SIZE, totalItems);
        String rangeText = totalItems == 0 ? "0 élément" : from + "-" + to + " / " + totalItems;
        Label lblPage = new Label("Page " + (currentPage + 1) + " / " + Math.max(1, totalPages) + "  (" + rangeText + ")");
        lblPage.getStyleClass().add("pagination-label");

        Button btnNext = new Button("Suivant ▶");
        btnNext.getStyleClass().add("btn-neutral");
        btnNext.setDisable(currentPage >= totalPages - 1 || totalPages <= 1);

        btnPrev.setOnAction(e -> { if (currentPage > 0) onPrev.run(); });
        btnNext.setOnAction(e -> { if (currentPage < totalPages - 1) onNext.run(); });

        bar.getChildren().addAll(btnPrev, lblPage, btnNext);
        return new PaginationBar(bar, btnPrev, lblPage, btnNext);
    }

    public static class PaginationBar {
        public final HBox container;
        public final Button btnPrev;
        public final Label lblPage;
        public final Button btnNext;

        public PaginationBar(HBox container, Button btnPrev, Label lblPage, Button btnNext) {
            this.container = container;
            this.btnPrev = btnPrev;
            this.lblPage = lblPage;
            this.btnNext = btnNext;
        }

        public void update(int currentPage, int totalPages, int totalItems, Runnable onPrev, Runnable onNext) {
            btnPrev.setDisable(currentPage <= 0);
            btnNext.setDisable(currentPage >= totalPages - 1 || totalPages <= 1);
            int from = totalItems == 0 ? 0 : currentPage * PAGE_SIZE + 1;
            int to = Math.min((currentPage + 1) * PAGE_SIZE, totalItems);
            String rangeText = totalItems == 0 ? "0 élément" : from + "-" + to + " / " + totalItems;
            lblPage.setText("Page " + (currentPage + 1) + " / " + Math.max(1, totalPages) + "  (" + rangeText + ")");
            btnPrev.setOnAction(e -> { if (currentPage > 0) onPrev.run(); });
            btnNext.setOnAction(e -> { if (currentPage < totalPages - 1) onNext.run(); });
        }
    }
}
