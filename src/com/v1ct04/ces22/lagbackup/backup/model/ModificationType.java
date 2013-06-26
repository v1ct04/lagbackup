package com.v1ct04.ces22.lagbackup.backup.model;

public enum ModificationType {
    CREATED,
    MODIFIED,
    DELETED;

    public static final String FILE_DIFF_TYPE_PATTERN = "[CMD]";

    public String toDisplayString() {
        switch (this) {
            case CREATED:
                return "CRIADO";
            case MODIFIED:
                return "MODIFICADO";
            case DELETED:
                return "APAGADO";
            default:
                return null;
        }
    }

    public char getCode() {
        switch (this) {
            case CREATED:
                return 'C';
            case MODIFIED:
                return 'M';
            case DELETED:
                return 'D';
            default:
                return 0;
        }
    }

    public static ModificationType valueOfCode(char code) {
        switch (code) {
            case 'C':
                return CREATED;
            case 'M':
                return MODIFIED;
            case 'D':
                return DELETED;
            default:
                return null;
        }
    }
}
