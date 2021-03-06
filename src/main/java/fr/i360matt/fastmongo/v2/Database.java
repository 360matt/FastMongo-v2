package fr.i360matt.fastmongo.v2;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import org.bson.Document;

import java.io.Closeable;
import java.util.Collections;

/**
 *
 This class is used to instantiate a connection to a database.
 It will be reused in the class Manager.

 @see Manager
 @author 360matt
 @version 2.0.0
 */
public class Database implements Closeable {

    protected static Database def;

    protected final MongoDatabase database;
    protected final MongoClient client;

    /**
     * This class used to structure credentials information.
     */
    public static class Auth {
        public String host;
        public int port;
        public String user;
        public String password;
        public String database;
    }


    public Database (final Auth auth) {
        final MongoCredential credential = MongoCredential.createCredential(
                auth.user,
                auth.database,
                auth.password.toCharArray()
        );

        final MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .applyToClusterSettings(builder -> builder.hosts(Collections.singletonList(new ServerAddress(auth.host, auth.port))))
                .build();

        this.client = MongoClients.create(settings);

        this.database = client.getDatabase(auth.database);
        if (getDefault() == null) // set the first database as default
            this.setDefault();
    }

    public Database (final String database, final String url) {
        final MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(url))
                .retryWrites(true)
                .build();

        client = MongoClients.create(settings);

        this.database = client.getDatabase(database);
        if (getDefault() == null) // set the first database as default
            this.setDefault();
    }

    /**
     * Used to define this database as default.
     */
    public void setDefault () {
        def = this;
    }

    /**
     * Static method used to retrieve the default database.
     * @return The default database.
     */
    public static Database getDefault () {
        return def;
    }

    /**
     * Static method used to define a database as default.
     */
    public static void setDefault (final Database database) {
        def = database;
    }

    /**
     * Allow to retrive the legacy MongoDatabase.
     * @return MongoDatabase instance.
     */
    public MongoDatabase getMongoDatabase () {
        return this.database;
    }

    /**
     * Allow to retrieve the legacy MongoClient.
     * @return getMongoClient instance.
     */
    public MongoClient getMongoClient () {
        return this.client;
    }

    /**
     * Allows to find out if a collection exists
     * @param name The name of the collection
     * @return The answer about existence
     */
    public boolean existCollection (final String name) {
        for (final String candidate : this.database.listCollectionNames())
            if (candidate.equalsIgnoreCase(name))
                return true;
        return false;
    }

    /**
     * Allows to create a collection, without error checking, you must use existCollect() before evoking this method.
     * @param name The name of the collection to create
     */
    public void createCollection (final String name) {
        this.database.createCollection(name);
    }

    /**
     * Allows to create a collection, without error checking, you must use existCollect() before evoking this method.
     * @param name The name of the collection to create
     * @param options The options to apply to the collection
     */
    public void createCollection (final String name, final CreateCollectionOptions options) {
        this.database.createCollection(name, options);
    }


    /**
     * Allows to retrieve a collection
     * @param name The name of the collection
     * @return the collection, can be null.
     */
    public MongoCollection<Document> getCollection (final String name) {
        return this.database.getCollection(name);
    }



    public void close () {
        if (this.client == null) return;
        this.client.close();
    }

}
