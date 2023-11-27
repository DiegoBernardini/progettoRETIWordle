package Server.Models;

public class InfoTraduzione {
    public String segment;
    public String translation;

    public InfoTraduzione(String segment, String translation) {
        super();
        this.segment = segment;
        this.translation = translation;
    }

    @Override
    public String toString() {
        return "InfoTraduzione [segment=" + segment + ", translation=" + translation + "]";
    }

    public String getTranslation() {
        return translation;
    }
}
