package ar.rou.legacy;

public class Utils {
    
    public static String formatUserName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "Anonymous";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}