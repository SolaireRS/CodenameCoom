module com.client {
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    requires java.sql;           // for javax.imageio
    requires java.naming;        // for javax.naming if needed
    
    // If you need to access internal packages, add these:
    opens com.client to java.base;
    opens com.client.sign to java.base;
    opens com.client.definitions to java.base;
}