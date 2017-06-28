package com.example.adrialwalters.inventoryapp.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.adrialwalters.inventoryapp.data.InventoryContract.ProductEntry;

public class InventoryDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = ProductEntry.class.getSimpleName();

    /** Name of the database file */
    private static final String DATABASE_NAME = "inventory.db";

    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 5;

    /**
     * Constructs a new instance of {@link InventoryDbHelper}.
     *
     * @param context of the app
     */
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a String that contains the SQL statement to create the products table
        String SQL_CREATE_PRODUCTS_TABLE =
                "CREATE TABLE " + ProductEntry.TABLE_NAME + "(" +
                        ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        ProductEntry.COLUMN_PRODUCT_BRAND + " TEXT NOT NULL, " +
                        ProductEntry.COLUMN_PRODUCT_MODEL + " TEXT NOT NULL, " +
                        ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER DEFAULT 0, " +
                        ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER DEFAULT 0, " +
                        ProductEntry.COLUMN_STRING_IMAGE_URI + " STRING, " +
                        ProductEntry.COLUMN_SUPPLIER_NAME + " TEXT NOT NULL, " +
                        ProductEntry.COLUMN_SUPPLIER_EMAIL + " TEXT);";

        // Create the database
        sqLiteDatabase.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }
}
