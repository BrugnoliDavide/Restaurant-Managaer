package rm.model;

public class CashierUser extends User {
    public CashierUser(String username) {
        super(username, "cucina");
    }

    @Override
    public String getWelcomeMessage() {
        return "buongiorno " + username;
    }
}