package rm.printer;

public class PrinterConfiguration {
    private String printerName;
    private boolean enabled;
    private int printWidth;  // Caratteri per riga
    private boolean autoCut;
    private int feedLines;   // Righe da alimentare dopo stampa

    // Costruttori, getters e setters
    public PrinterConfiguration() {
        this.autoCut = true;
        this.feedLines = 3;
        this.printWidth = 48;
    }

    public String getPrinterName() {
        return printerName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutoCut() {
        return autoCut;
    }

    public int getFeedLines() {
        return feedLines;
    }

    public int getPrintWidth() {
        return printWidth;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPrintWidth(int printWidth) {
        this.printWidth = printWidth;
    }

    public void setAutoCut(boolean autoCut) {
        this.autoCut = autoCut;
    }

    public void setFeedLines(int feedLines) {
        this.feedLines = feedLines;
    }
}