package net.fabricmc.mante.discordian;

public class AccountLinkDetails {
    public String uuid;
    public String id;
    public String code;

    public AccountLinkDetails(String uuid, String code) {
        this.uuid = uuid;
        this.code = code;
        this.id = null;
    }
}
