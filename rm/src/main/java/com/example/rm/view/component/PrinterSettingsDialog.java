package com.example.rm.view.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Dialog per la configurazione della stampante comande.
 * Persiste le impostazioni tramite java.util.prefs.Preferences.
 *
 * <p>Campi configurabili:</p>
 * <ul>
 *     <li>Abilitazione stampa automatica</li>
 *     <li>Nome stampante</li>
 *     <li>Spessore etichetta (larghezza carta in mm)</li>
 *     <li>Caratteri per riga</li>
 *     <li>Taglio automatico</li>
 * </ul>
 */
public class PrinterSettingsDialog {

    private static final Logger logger = Logger.getLogger(PrinterSettingsDialog.class.getName());

    // Chiavi di persistenza (Preferences)
    private static final String PREF_ENABLED       = "printer.enabled";
    private static final String PREF_PRINTER_NAME   = "printer.name";
    private static final String PREF_LABEL_WIDTH    = "printer.labelWidthMm";
    private static final String PREF_CHARS_PER_LINE = "printer.charsPerLine";
    private static final String PREF_AUTO_CUT       = "printer.autoCut";

    // Valori di default
    private static final boolean DEFAULT_ENABLED       = false;
    private static final String  DEFAULT_PRINTER_NAME  = "";
    private static final int     DEFAULT_LABEL_WIDTH   = 80;
    private static final int     DEFAULT_CHARS_PER_LINE = 48;
    private static final boolean DEFAULT_AUTO_CUT      = true;

    @FXML private CheckBox chkEnabled;
    @FXML private TextField txtPrinterName;
    @FXML private Spinner<Integer> spnLabelWidth;
    @FXML private Spinner<Integer> spnCharsPerLine;
    @FXML private CheckBox chkAutoCut;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Button btnTestPrint;
    @FXML private Label lblStatus;

    private Stage stage;

    private static final Preferences prefs =
            Preferences.userNodeForPackage(PrinterSettingsDialog.class);

    // =========================================================================
    //  Metodo statico di apertura
    // =========================================================================

    /**
     * Mostra il dialog di configurazione stampante come finestra modale.
     *
     * @param owner Stage proprietario (può essere null)
     */
    public static void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    PrinterSettingsDialog.class.getResource("/PrinterSettingsDialog.fxml"));
            VBox root = loader.load();

            PrinterSettingsDialog controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.UTILITY);
            dialog.initModality(Modality.WINDOW_MODAL);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Impostazioni Stampante Comande");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            dialog.setWidth(500);
            dialog.setHeight(520);

            controller.stage = dialog;
            controller.loadSettings();
            controller.setupActions();

            dialog.showAndWait();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Errore caricamento PrinterSettingsDialog.fxml", e);
        }
    }

    // =========================================================================
    //  Inizializzazione e caricamento
    // =========================================================================

    private void loadSettings() {
        chkEnabled.setSelected(prefs.getBoolean(PREF_ENABLED, DEFAULT_ENABLED));
        txtPrinterName.setText(prefs.get(PREF_PRINTER_NAME, DEFAULT_PRINTER_NAME));
        chkAutoCut.setSelected(prefs.getBoolean(PREF_AUTO_CUT, DEFAULT_AUTO_CUT));

        // Spinner spessore etichetta: range 20mm - 120mm
        int savedLabelWidth = prefs.getInt(PREF_LABEL_WIDTH, DEFAULT_LABEL_WIDTH);
        SpinnerValueFactory<Integer> labelWidthFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 120, savedLabelWidth, 1);
        spnLabelWidth.setValueFactory(labelWidthFactory);
        spnLabelWidth.setEditable(true);

        // Spinner caratteri per riga: range 20 - 80
        int savedCharsPerLine = prefs.getInt(PREF_CHARS_PER_LINE, DEFAULT_CHARS_PER_LINE);
        SpinnerValueFactory<Integer> charsFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 80, savedCharsPerLine, 1);
        spnCharsPerLine.setValueFactory(charsFactory);
        spnCharsPerLine.setEditable(true);

        // Autoregola i caratteri per riga in base alla larghezza selezionata
        spnLabelWidth.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal <= 60) {
                spnCharsPerLine.getValueFactory().setValue(32);
            } else if (newVal != null) {
                spnCharsPerLine.getValueFactory().setValue(48);
            }
        });
    }

    private void setupActions() {
        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> handleCancel());
        btnTestPrint.setOnAction(e -> handleTestPrint());
    }

    // =========================================================================
    //  Azioni
    // =========================================================================

    private void handleSave() {
        try {
            prefs.putBoolean(PREF_ENABLED, chkEnabled.isSelected());
            prefs.put(PREF_PRINTER_NAME, txtPrinterName.getText().trim());
            prefs.putInt(PREF_LABEL_WIDTH, spnLabelWidth.getValue());
            prefs.putInt(PREF_CHARS_PER_LINE, spnCharsPerLine.getValue());
            prefs.putBoolean(PREF_AUTO_CUT, chkAutoCut.isSelected());
            prefs.flush();

            logger.log(Level.INFO,
                    "Impostazioni stampante salvate: abilitata={0}, larghezza={1}mm, caratteri={2}",
                    new Object[]{chkEnabled.isSelected(), spnLabelWidth.getValue(),
                            spnCharsPerLine.getValue()});

            showAlert(Alert.AlertType.INFORMATION,
                    "Impostazioni salvate",
                    "La configurazione della stampante è stata aggiornata.");

            if (stage != null) {
                stage.close();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore salvataggio impostazioni stampante", e);
            showAlert(Alert.AlertType.ERROR,
                    "Errore",
                    "Impossibile salvare le impostazioni: " + e.getMessage());
        }
    }

    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }

    private void handleTestPrint() {
        lblStatus.setText("Test stampa in corso...");
        lblStatus.setStyle("-fx-text-fill: #2196F3; -fx-font-style: italic;");

        String printerName = txtPrinterName.getText().trim();
        if (printerName.isEmpty()) {
            lblStatus.setText("Errore: specificare il nome della stampante.");
            lblStatus.setStyle("-fx-text-fill: #E74C3C; -fx-font-style: italic;");
            return;
        }

        // Verifica che la stampante sia raggiungibile tramite il sistema
        boolean found = false;
        for (javax.print.PrintService ps : javax.print.PrintServiceLookup.lookupPrintServices(null, null)) {
            if (ps.getName().equalsIgnoreCase(printerName)) {
                found = true;
                break;
            }
        }

        if (found) {
            lblStatus.setText("Stampante \"" + printerName + "\" trovata. "
                    + "Larghezza etichetta: " + spnLabelWidth.getValue() + "mm, "
                    + spnCharsPerLine.getValue() + " car/riga.");
            lblStatus.setStyle("-fx-text-fill: #4CAF50; -fx-font-style: normal;");
        } else {
            lblStatus.setText("Stampante \"" + printerName + "\" non trovata nel sistema. "
                    + "Verificare nome e connessione.");
            lblStatus.setStyle("-fx-text-fill: #FF9800; -fx-font-style: italic;");
        }
    }

    // =========================================================================
    //  Metodi di accesso statico alle impostazioni (per uso da altri servizi)
    // =========================================================================

    /** Indica se la stampa automatica è abilitata. */
    public static boolean isPrintEnabled() {
        return prefs.getBoolean(PREF_ENABLED, DEFAULT_ENABLED);
    }

    /** Restituisce il nome della stampante configurata. */
    public static String getPrinterName() {
        return prefs.get(PREF_PRINTER_NAME, DEFAULT_PRINTER_NAME);
    }

    /** Restituisce lo spessore/larghezza dell'etichetta in mm. */
    public static int getLabelWidthMm() {
        return prefs.getInt(PREF_LABEL_WIDTH, DEFAULT_LABEL_WIDTH);
    }

    /** Restituisce il numero di caratteri per riga. */
    public static int getCharsPerLine() {
        return prefs.getInt(PREF_CHARS_PER_LINE, DEFAULT_CHARS_PER_LINE);
    }

    /** Indica se il taglio automatico è abilitato. */
    public static boolean isAutoCutEnabled() {
        return prefs.getBoolean(PREF_AUTO_CUT, DEFAULT_AUTO_CUT);
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}