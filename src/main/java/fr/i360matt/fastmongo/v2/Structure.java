package fr.i360matt.fastmongo.v2;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import fr.i360matt.FastMongoEnabler;
import fr.i360matt.enabler.Enabler;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.Closeable;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * This class is used to load document data from MongoDB, manipulate it and then save it in the DB.
 *
 * @author 360matt
 * @version 2.0.0
 */
public class Structure implements Closeable, Serializable {

    static {
        Enabler.call(FastMongoEnabler.class);
    }

    protected static final UpdateOptions UPSERT = new UpdateOptions().upsert(true);

    protected Bson filter;

    protected Manager manager;
    protected Object id;



    public Structure (final Object id, final Manager manager) {
        this.id = id;
        this.manager = manager;

        this.filter = Filters.eq(manager.getFieldID(), id);

        this.load(); // must load current data before futures saves.
    }

    public Structure (final Document doc, final Manager manager) {
        this.id = doc.getOrDefault(manager.getFieldID(), null);
        this.manager = manager;

        this.filter = Filters.eq(manager.getFieldID(), id);

        try {
            this.docToField(doc);
            // must load current data before futures saves.
            // and must load from Document arg and no re-load.
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows you to convert a Document object to the fields of the current instance using the cache.
     * @param document The document retrieved from the database.
     * @throws IllegalAccessException A field access exception.
     */
    protected void docToField (final Document document) throws IllegalAccessException {
        for (final Field field : this.manager.getFieldsCache(getClass())) {
            final Object obj = document.getOrDefault(field.getName(), null);

            if (obj != null) {
                field.set(this, obj);
            }
        }
    }

    /**
     * Allows to convert the fields of this current instance to the Document object using the cache.
     * @return The Document object.
     * @throws IllegalAccessException A field access exception.
     */
    protected Document fieldToDoc () throws IllegalAccessException {
        final Document document = new Document(this.manager.getFieldID(), this.id);

        for (final Field field : this.manager.getFieldsCache(getClass())) {
            document.put(field.getName(), field.get(this));
        }
        return document;
    }



    // ____________________________________________________________________________________________

    /**
     * Allows to load the data.
     */
    public void load () {
        final Document doc = manager.collection.find(this.filter).first();
        if (doc != null) {
            try {
                this.docToField(doc);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Allow to save the data.
     */
    public void save () {
        try {
            final Document doc = this.fieldToDoc();
            if (doc.isEmpty()) return;

            this.manager.collection.updateOne(
                    this.filter,
                    new Document("$set", doc),
                    UPSERT
            );
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allow to save the data only if the document don't exist in the DB.
     */
    public void saveIfAbsent () {
        try {
            final Document doc = this.fieldToDoc();
            if (doc.isEmpty()) return;

            this.manager.collection.updateOne(
                    this.filter,
                    new Document("$insert", doc),
                    UPSERT
            );
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Allow to check if the document exist or not.
     * @return The stat of the existence.
     */
    public boolean exist () {
        return manager.existObject(this.id);
    }

    /**
     * Allow to remove the document if exist.
     * @return The stat of the existence.
     */
    public void delete () {
        manager.remove(this.id);
    }

    /**
     * Allow to close this instance, and use it in a try-resource.
     */
    public void close () {
        // nothing to free, lol.
    }



}
