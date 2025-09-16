package ar.rou.model;

public class Preferences {
    private String theme;
    private boolean notifications;
    
    public Preferences() {
    }
    
    public Preferences(String theme, boolean notifications) {
        this.theme = theme;
        this.notifications = notifications;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    public boolean isNotifications() {
        return notifications;
    }
    
    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }
}