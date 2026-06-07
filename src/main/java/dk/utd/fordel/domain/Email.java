package dk.utd.fordel.domain;

import dk.utd.fordel.utils.Assert;

public record Email(String local, String domain) {

    public static final int MAX_EMAIL_SIZE = 350;

    public String toString() {
        return "Email[" + asString() + "]";
    }

    public String asString() {
        return local + "@" + domain;
    }

    public static Email of(String email) throws InvalidEmail {
        Assert.isNotNull(email, "email");

        String[] items = email.split("@", 2);

        if (items.length != 2) {
            throw new InvalidEmail(email + " should have @ sign");
        }

        return Email.of(items[0], items[1]);

    }

    public static Email of(String local, String domain) throws InvalidEmail {
        Assert.isNotNull(local, "local");
        Assert.isNotNull(domain, "domain");

        if (local.isEmpty()) {
            throw new InvalidEmail("local is empty");
        }

        if (domain.isEmpty()) {
            throw new InvalidEmail("domain is empty");
        }

        if (local.length() + domain.length() + 1 > MAX_EMAIL_SIZE) {
            throw new InvalidEmail("email too long");
        }

        return new Email(local.toLowerCase(), domain.toLowerCase());
    }

    public static class InvalidEmail extends Exception {
        public InvalidEmail(String msg) {
            super(msg);
        }
    }

}
